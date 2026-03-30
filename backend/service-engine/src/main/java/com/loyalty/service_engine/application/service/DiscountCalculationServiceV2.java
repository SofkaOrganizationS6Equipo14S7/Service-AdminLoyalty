package com.loyalty.service_engine.application.service;

import com.loyalty.service_engine.application.dto.calculate.DiscountCalculateRequestV2;
import com.loyalty.service_engine.application.dto.calculate.DiscountCalculateResponseV2;
import com.loyalty.service_engine.application.dto.configuration.ConfigurationUpdatedEvent;
import com.loyalty.service_engine.domain.model.EngineDiscountConfiguration;
import com.loyalty.service_engine.infrastructure.exception.BadRequestException;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class DiscountCalculationServiceV2 {

    private final EngineConfigurationCacheService configurationCacheService;

    public DiscountCalculationServiceV2(EngineConfigurationCacheService configurationCacheService) {
        this.configurationCacheService = configurationCacheService;
    }

    public DiscountCalculateResponseV2 calculate(DiscountCalculateRequestV2 request) {
        validateRequest(request);
        EngineDiscountConfiguration config = configurationCacheService.get(request.ecommerceId())
                .orElseGet(() -> configurationCacheService.defaultFor(request.ecommerceId()));

        Map<String, EngineDiscountConfiguration.PriorityRule> priorityRuleByType = new HashMap<>();
        for (EngineDiscountConfiguration.PriorityRule rule : config.priority()) {
            priorityRuleByType.put(rule.type().toUpperCase(Locale.ROOT), rule);
        }

        List<RankedDiscount> ranked = request.discounts().stream()
                .map(item -> {
                    String normalized = item.type().trim().toUpperCase(Locale.ROOT);
                    EngineDiscountConfiguration.PriorityRule rule = priorityRuleByType.get(normalized);
                    return rule == null ? null : new RankedDiscount(normalized, item.amount(), rule.order(), rule.priorityId().toString());
                })
                .filter(java.util.Objects::nonNull)
                .sorted(Comparator
                        .comparingInt(RankedDiscount::order)
                        .thenComparing(RankedDiscount::priorityId))
                .toList();

        BigDecimal requested = ranked.stream()
                .map(RankedDiscount::amount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal capAmount = computeCapAmount(config, request);
        BigDecimal remaining = capAmount;
        boolean capped;
        java.util.List<DiscountCalculateResponseV2.AppliedDiscount> applied = new java.util.ArrayList<>();
        for (RankedDiscount item : ranked) {
            BigDecimal appliedAmount;
            if (remaining == null) {
                appliedAmount = applyRounding(item.amount(), config.roundingRule());
            } else if (remaining.signum() <= 0) {
                appliedAmount = BigDecimal.ZERO;
            } else {
                appliedAmount = applyRounding(item.amount().min(remaining), config.roundingRule());
                remaining = remaining.subtract(appliedAmount);
            }
            applied.add(new DiscountCalculateResponseV2.AppliedDiscount(item.type(), item.amount(), appliedAmount, item.order()));
        }

        BigDecimal totalApplied = applied.stream()
                .map(DiscountCalculateResponseV2.AppliedDiscount::appliedAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (capAmount != null && totalApplied.compareTo(capAmount) > 0) {
            totalApplied = capAmount;
            capped = true;
        } else if (capAmount != null) {
            capped = requested.compareTo(capAmount) > 0;
        } else {
            capped = false;
        }

        return new DiscountCalculateResponseV2(
                request.ecommerceId(),
                config.currency(),
                config.roundingRule().name(),
                applyRounding(requested, config.roundingRule()),
                capAmount == null ? null : applyRounding(capAmount, config.roundingRule()),
                applyRounding(totalApplied, config.roundingRule()),
                capped,
                applied,
                Instant.now()
        );
    }

    private void validateRequest(DiscountCalculateRequestV2 request) {
        if (request.subtotal().compareTo(request.total()) > 0) {
            throw new BadRequestException("subtotal cannot be greater than total");
        }
        if (request.beforeTax().compareTo(request.afterTax()) > 0) {
            throw new BadRequestException("beforeTax cannot be greater than afterTax");
        }
        for (DiscountCalculateRequestV2.DiscountCandidate discount : request.discounts()) {
            if (discount.type() == null || discount.type().isBlank()) {
                throw new BadRequestException("discount type is required");
            }
            if (discount.amount() == null || discount.amount().signum() <= 0) {
                throw new BadRequestException("discount amount must be greater than zero");
            }
        }
    }

    private BigDecimal computeCapAmount(EngineDiscountConfiguration config, DiscountCalculateRequestV2 request) {
        if (config.capType() == null || config.capValue() == null || config.capAppliesTo() == null) {
            return null;
        }
        BigDecimal base = switch (config.capAppliesTo()) {
            case SUBTOTAL -> request.subtotal();
            case TOTAL -> request.total();
            case BEFORE_TAX -> request.beforeTax();
            case AFTER_TAX -> request.afterTax();
        };
        if (config.capType() == ConfigurationUpdatedEvent.CapType.PERCENTAGE) {
            return base.multiply(config.capValue()).divide(new BigDecimal("100"), 6, RoundingMode.HALF_UP);
        }
        return null;
    }

    private BigDecimal applyRounding(BigDecimal value, ConfigurationUpdatedEvent.RoundingRule rule) {
        RoundingMode mode = switch (rule) {
            case HALF_UP -> RoundingMode.HALF_UP;
            case DOWN -> RoundingMode.DOWN;
            case UP -> RoundingMode.UP;
        };
        return value.setScale(2, mode);
    }

    private record RankedDiscount(String type, BigDecimal amount, int order, String priorityId) {
    }
}
