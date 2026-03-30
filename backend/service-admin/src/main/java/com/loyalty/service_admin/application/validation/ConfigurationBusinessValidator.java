package com.loyalty.service_admin.application.validation;

import com.loyalty.service_admin.application.dto.configuration.ConfigurationCreateRequest;
import com.loyalty.service_admin.application.dto.configuration.ConfigurationPatchRequest;
import com.loyalty.service_admin.domain.entity.DiscountConfigurationEntity;
import com.loyalty.service_admin.infrastructure.exception.BadRequestException;
import org.springframework.stereotype.Component;

import java.util.Currency;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

@Component
public class ConfigurationBusinessValidator {

    public void validateCreate(ConfigurationCreateRequest request) {
        validateCurrency(request.currency());
        validatePriorityPairs(
                request.priority().stream().map(i -> new PriorityPair(i.type(), i.order())).toList()
        );
        validateCapValue(request.cap().value().doubleValue());
    }

    public void validatePatch(ConfigurationPatchRequest request) {
        if (request.currency() != null) {
            validateCurrency(request.currency());
        }
        if (request.priority() != null) {
            validatePriorityPairs(
                    request.priority().stream().map(i -> new PriorityPair(i.type(), i.order())).toList()
            );
        }
        if (request.cap() != null) {
            validateCapValue(request.cap().value().doubleValue());
        }
    }

    public void validateEntityState(DiscountConfigurationEntity entity) {
        if (entity.getCurrency() == null) {
            throw new BadRequestException("VALIDATION_ERROR: currency is required");
        }
        validateCurrency(entity.getCurrency());
        if (entity.getCapValue() == null) {
            throw new BadRequestException("VALIDATION_ERROR: cap.value is required");
        }
        validateCapValue(entity.getCapValue().doubleValue());
        validatePriorityPairs(entity.getPriorities().stream()
                .map(item -> new PriorityPair(item.getDiscountType(), item.getOrder()))
                .toList());
    }

    private void validateCurrency(String currencyCode) {
        String normalized = currencyCode == null ? "" : currencyCode.trim().toUpperCase(Locale.ROOT);
        if (normalized.length() != 3) {
            throw new BadRequestException("VALIDATION_ERROR: currency must be a valid ISO 4217 code");
        }
        try {
            Currency.getInstance(normalized);
        } catch (Exception ex) {
            throw new BadRequestException("VALIDATION_ERROR: currency must be a valid ISO 4217 code");
        }
    }

    private void validateCapValue(double value) {
        if (value <= 0) {
            throw new BadRequestException("VALIDATION_ERROR: cap.value must be greater than zero");
        }
    }

    private void validatePriorityPairs(java.util.List<PriorityPair> priorityPairs) {
        Set<String> types = new HashSet<>();
        Set<Integer> orders = new HashSet<>();

        for (PriorityPair pair : priorityPairs) {
            if (pair.type() == null || pair.type().isBlank()) {
                throw new BadRequestException("VALIDATION_ERROR: priority.type is required");
            }
            if (pair.order() == null || pair.order() <= 0) {
                throw new BadRequestException("VALIDATION_ERROR: order must be greater than zero");
            }
            if (!types.add(pair.type().trim().toUpperCase(Locale.ROOT))) {
                throw new BadRequestException("VALIDATION_ERROR: duplicated discount type in priority");
            }
            if (!orders.add(pair.order())) {
                throw new BadRequestException("VALIDATION_ERROR: duplicated order in priority");
            }
        }
    }

    private record PriorityPair(String type, Integer order) {
    }
}
