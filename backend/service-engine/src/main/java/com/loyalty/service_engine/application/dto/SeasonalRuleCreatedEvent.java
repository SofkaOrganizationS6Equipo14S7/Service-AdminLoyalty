package com.loyalty.service_engine.application.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Event consumed when a seasonal rule is created
 * Published by Service-Admin
 */
public record SeasonalRuleCreatedEvent(
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
