package com.loyalty.service_admin.application.dto;

import java.time.Instant;
import java.util.UUID;

/**
 * DTO for customer tier response.
 * Returned in GET/POST/PUT operations.
 */
public record CustomerTierResponse(
    UUID uid,
    String name,
    Integer level,
    boolean isActive,
    Instant createdAt,
    Instant updatedAt
) {
}
