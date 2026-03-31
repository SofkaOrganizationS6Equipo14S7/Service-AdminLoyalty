package com.loyalty.service_admin.application.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * DTO for fidelity range response
 * Returned in GET/POST/PUT operations
 */
public record FidelityRangeResponse(
    UUID uid,
    String name,
    Integer minPoints,
    Integer maxPoints,
    BigDecimal discountPercentage,
    boolean isActive,
    Instant createdAt,
    Instant updatedAt
) {
}
