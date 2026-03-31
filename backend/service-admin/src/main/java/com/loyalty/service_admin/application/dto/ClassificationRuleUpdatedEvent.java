package com.loyalty.service_admin.application.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Event published when a classification rule is updated.
 * Consumed by Service-Engine to replicate in database and invalidate cache.
 */
public record ClassificationRuleUpdatedEvent(
    String eventType,           // "CLASSIFICATION_RULE_UPDATED"
    UUID ruleUid,
    UUID tierUid,
    String metricType,
    BigDecimal minValue,
    BigDecimal maxValue,
    Integer priority,
    Boolean isActive,
    Instant updatedAt
) {
}
