package com.loyalty.service_admin.application.dto;

import java.time.Instant;
import java.util.UUID;

/**
 * Event published when a seasonal rule is deleted (soft delete)
 * Consumed by Service-Engine to invalidate cache and mark as inactive in database
 */
public record SeasonalRuleDeletedEvent(
    String eventType,           // "SEASONAL_RULE_DELETED"
    UUID ruleUid,               // Rule identifier
    UUID ecommerceId,           // Ecommerce identifier
    Instant timestamp           // Event timestamp (UTC)
) {
}
