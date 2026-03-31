package com.loyalty.service_admin.application.dto;

import java.time.Instant;
import java.util.UUID;

/**
 * Event published when a fidelity range is deleted (soft-delete)
 * Consumed by Service-Engine to remove from cache
 */
public record FidelityRangeDeletedEvent(
    String eventType,  // "FIDELITY_RANGE_DELETED"
    UUID uid,          // Range identifier
    UUID ecommerceId,  // Ecommerce identifier (tenant)
    Instant timestamp  // Event timestamp (UTC)
) {
}
