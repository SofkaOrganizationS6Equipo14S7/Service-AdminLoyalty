package com.loyalty.service_admin.application.dto.rules.seasonal;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Response DTO for SeasonalRule
 * Used in GET, POST, PUT endpoints
 */
public record SeasonalRuleResponse(
    String uid,
    String ecommerceId,
    String name,
    String description,
    BigDecimal discountPercentage,
    String discountType,
    Instant startDate,
    Instant endDate,
    Boolean isActive,
    Instant createdAt,
    Instant updatedAt
) {
}
