package com.loyalty.service_admin.application.dto.customertier;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record CustomerTierResponse(
    UUID id,
    UUID ecommerceId,
    String name,
    BigDecimal discountPercentage,
    Integer hierarchyLevel,
    boolean isActive,
    Instant createdAt,
    Instant updatedAt
) {
}