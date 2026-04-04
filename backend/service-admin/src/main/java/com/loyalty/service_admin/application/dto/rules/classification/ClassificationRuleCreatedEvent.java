package com.loyalty.service_admin.application.dto.rules.classification;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Event published when a classification rule is created.
 * Consumed by Service-Engine to replicate in database and invalidate cache.
 */
public record ClassificationRuleCreatedEvent(
    String eventType,           // "CLASSIFICATION_RULE_CREATED"
    UUID ruleUid,
    UUID tierUid,
    String metricType,
    BigDecimal minValue,
    BigDecimal maxValue,
    Integer priority,
    Instant createdAt
) {
}
