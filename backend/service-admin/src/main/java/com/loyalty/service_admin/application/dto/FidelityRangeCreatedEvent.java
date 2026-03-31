package com.loyalty.service_admin.application.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Event published when a fidelity range is created
 * Consumed by Service-Engine to replicate in cache and database
 */
public record FidelityRangeCreatedEvent(
    String eventType,              // "FIDELITY_RANGE_CREATED"
    UUID uid,                      // Range identifier
    UUID ecommerceId,              // Ecommerce identifier (tenant)
    String name,                   // Level name (Bronce, Plata, etc)
    Integer minPoints,             // Minimum points (inclusive)
    Integer maxPoints,             // Maximum points (inclusive)
    BigDecimal discountPercentage, // Discount percentage [0-100]
    Instant timestamp              // Event timestamp (UTC)
) {
}
