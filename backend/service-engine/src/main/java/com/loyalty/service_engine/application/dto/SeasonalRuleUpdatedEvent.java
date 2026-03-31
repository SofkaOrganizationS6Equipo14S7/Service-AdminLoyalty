package com.loyalty.service_engine.application.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Event consumed when a seasonal rule is updated
 * Published by Service-Admin
 */
public record SeasonalRuleUpdatedEvent(
    String eventType,
    UUID ruleUid,
    UUID ecommerceId,
    String name,
    String description,
    BigDecimal discountPercentage,
    String discountType,
    Instant startDate,
    Instant endDate,
    Instant timestamp
) {
}
