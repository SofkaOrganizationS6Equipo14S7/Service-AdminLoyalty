package com.loyalty.service_admin.application.dto.configuration;

import com.loyalty.service_admin.domain.model.CapAppliesTo;
import com.loyalty.service_admin.domain.model.CapType;
import com.loyalty.service_admin.domain.model.RoundingRule;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record ConfigurationUpdatedEvent(
        String eventType,
        UUID configId,
        UUID ecommerceId,
        long version,
        String currency,
        RoundingRule roundingRule,
        CapType capType,
        BigDecimal capValue,
        CapAppliesTo capAppliesTo,
        List<PriorityItem> priority,
        Instant updatedAt
) {
    public record PriorityItem(
            UUID priorityId,
            String type,
            int order
    ) {
    }
}
