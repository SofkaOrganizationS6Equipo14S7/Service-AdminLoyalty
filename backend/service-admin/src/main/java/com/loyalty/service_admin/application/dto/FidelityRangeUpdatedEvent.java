package com.loyalty.service_admin.application.dto;

import java.time.Instant;
import java.util.UUID;

/**
 * Event published when a fidelity range is updated
 * Consumed by Service-Engine to sync changes
 */
public record FidelityRangeUpdatedEvent(
    String eventType,      // "FIDELITY_RANGE_UPDATED"
    UUID uid,              // Range identifier
    UUID ecommerceId,      // Ecommerce identifier (tenant)
    String message,        // Description of what was updated
    Instant timestamp      // Event timestamp (UTC)
) {
}
