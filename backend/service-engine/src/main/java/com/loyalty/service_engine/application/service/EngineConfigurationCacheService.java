package com.loyalty.service_engine.application.service;

import com.loyalty.service_engine.application.dto.configuration.ConfigurationUpdatedEvent;
import com.loyalty.service_engine.domain.model.EngineDiscountConfiguration;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Comparator;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
@Slf4j
public class EngineConfigurationCacheService {

    private final Map<UUID, EngineDiscountConfiguration> configCache = new ConcurrentHashMap<>();

    public Optional<EngineDiscountConfiguration> get(UUID ecommerceId) {
        return Optional.ofNullable(configCache.get(ecommerceId));
    }

    public void upsertFromEvent(ConfigurationUpdatedEvent event) {
        validateEvent(event);
        EngineDiscountConfiguration newConfig = map(event);
        configCache.compute(event.ecommerceId(), (key, current) -> {
            if (current == null || newConfig.version() >= current.version()) {
                return newConfig;
            }
            return current;
        });
        log.info("Configuration cached for ecommerce={} version={}", event.ecommerceId(), event.version());
    }

    public EngineDiscountConfiguration defaultFor(UUID ecommerceId) {
        return new EngineDiscountConfiguration(
                null,
                ecommerceId,
                0L,
                "COP",
                ConfigurationUpdatedEvent.RoundingRule.HALF_UP,
                null,
                null,
                null,
                java.util.List.of(),
                Instant.now()
        );
    }

    private EngineDiscountConfiguration map(ConfigurationUpdatedEvent event) {
        java.util.List<EngineDiscountConfiguration.PriorityRule> priorities = event.priority() == null
                ? java.util.List.of()
                : event.priority().stream()
                .map(p -> new EngineDiscountConfiguration.PriorityRule(
                        p.priorityId(),
                        p.type().trim().toUpperCase(Locale.ROOT),
                        p.order()
                ))
                .sorted(Comparator
                        .comparingInt(EngineDiscountConfiguration.PriorityRule::order)
                        .thenComparing(p -> p.priorityId().toString()))
                .toList();

        return new EngineDiscountConfiguration(
                event.configId(),
                event.ecommerceId(),
                event.version(),
                event.currency().trim().toUpperCase(Locale.ROOT),
                event.roundingRule(),
                event.capType(),
                event.capValue(),
                event.capAppliesTo(),
                priorities,
                event.updatedAt()
        );
    }

    private void validateEvent(ConfigurationUpdatedEvent event) {
        if (event == null || event.ecommerceId() == null) {
            throw new IllegalArgumentException("Invalid config event: ecommerceId is required");
        }
        if (!"CONFIG_UPDATED".equals(event.eventType())) {
            throw new IllegalArgumentException("Invalid config event type");
        }
        if (event.currency() == null || event.currency().isBlank()) {
            throw new IllegalArgumentException("Invalid config event: currency is required");
        }
        if (event.roundingRule() == null) {
            throw new IllegalArgumentException("Invalid config event: roundingRule is required");
        }
        if (event.capType() != null && event.capValue() == null) {
            throw new IllegalArgumentException("Invalid config event: capValue is required when capType exists");
        }
        if (event.capValue() != null && event.capValue().signum() <= 0) {
            throw new IllegalArgumentException("Invalid config event: capValue must be greater than zero");
        }
    }
}
