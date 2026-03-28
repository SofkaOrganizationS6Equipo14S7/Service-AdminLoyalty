package com.loyalty.service_engine.domain.model;

import com.loyalty.service_engine.application.dto.configuration.ConfigurationUpdatedEvent;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record EngineDiscountConfiguration(
        UUID configId,
        UUID ecommerceId,
        long version,
        String currency,
        ConfigurationUpdatedEvent.RoundingRule roundingRule,
        ConfigurationUpdatedEvent.CapType capType,
        BigDecimal capValue,
        ConfigurationUpdatedEvent.CapAppliesTo capAppliesTo,
        List<PriorityRule> priority,
        Instant updatedAt
) {
    public record PriorityRule(
            UUID priorityId,
            String type,
            int order
    ) {
    }
}
