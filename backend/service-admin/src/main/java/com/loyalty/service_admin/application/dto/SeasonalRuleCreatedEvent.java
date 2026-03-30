package com.loyalty.service_admin.application.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Event published when a seasonal rule is created
 * Consumed by Service-Engine to replicate in cache and database
 */
public record SeasonalRuleCreatedEvent(
    String eventType,           // "SEASONAL_RULE_CREATED"
    UUID ruleUid,               // Rule identifier
    UUID ecommerceId,           // Ecommerce identifier
    String name,
    String description,
    BigDecimal discountPercentage,
    String discountType,
    Instant startDate,
    Instant endDate,
    Instant timestamp           // Event timestamp (UTC)
) {
}
