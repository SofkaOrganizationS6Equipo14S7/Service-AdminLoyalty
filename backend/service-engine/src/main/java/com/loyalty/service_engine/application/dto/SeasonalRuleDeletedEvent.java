package com.loyalty.service_engine.application.dto;

import java.time.Instant;
import java.util.UUID;

/**
 * Event consumed when a seasonal rule is deleted
 * Published by Service-Admin
 */
public record SeasonalRuleDeletedEvent(
    String eventType,
    UUID ruleUid,
    UUID ecommerceId,
    Instant timestamp
) {
}
