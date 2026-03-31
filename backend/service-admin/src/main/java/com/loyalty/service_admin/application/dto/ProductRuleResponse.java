package com.loyalty.service_admin.application.dto;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Response DTO for ProductRule
 * Used in GET, POST, PUT endpoints
 */
public record ProductRuleResponse(
    String uid,
    String ecommerceId,
    String name,
    String productType,
    BigDecimal discountPercentage,
    String benefit,
    Boolean isActive,
    Instant createdAt,
    Instant updatedAt
) {}
