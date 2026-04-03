package com.loyalty.service_admin.application.dto.rules.classification;

import java.time.Instant;
import java.util.UUID;

/**
 * Event published when a classification rule is deleted (soft-delete).
 * Consumed by Service-Engine to replicate in database and invalidate cache.
 */
public record ClassificationRuleDeletedEvent(
    String eventType,           // "CLASSIFICATION_RULE_DELETED"
    UUID ruleUid,
    UUID tierUid,
    Instant deletedAt
) {
}
