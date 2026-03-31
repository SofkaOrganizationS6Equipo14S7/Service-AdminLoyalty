package com.loyalty.service_admin.application.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * DTO for classification rule response.
 * Returned in GET/POST/PUT operations.
 */
public record ClassificationRuleResponse(
    UUID uid,
    UUID tierUid,
    String metricType,
    BigDecimal minValue,
    BigDecimal maxValue,
    Integer priority,
    boolean isActive,
    Instant createdAt,
    Instant updatedAt
) {
}
