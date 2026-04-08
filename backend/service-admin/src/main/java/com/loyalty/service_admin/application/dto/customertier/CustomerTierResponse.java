package com.loyalty.service_admin.application.dto.customertier;

import java.time.Instant;
import java.util.UUID;

public record CustomerTierResponse(
    UUID id,
    UUID ecommerceId,
    String name,
    Integer hierarchyLevel,
    boolean isActive,
    Instant createdAt,
    Instant updatedAt
) {
}