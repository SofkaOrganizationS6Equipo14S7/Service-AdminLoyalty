package com.loyalty.service_admin.application.dto.rules.seasonal;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Event published when a seasonal rule is updated
 * Consumed by Service-Engine to invalidate cache and replicate changes
 */
public record SeasonalRuleUpdatedEvent(
    String eventType,           // "SEASONAL_RULE_UPDATED"
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
