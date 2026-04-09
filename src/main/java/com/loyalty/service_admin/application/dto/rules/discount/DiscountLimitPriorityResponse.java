package com.loyalty.service_admin.application.dto.rules.discount;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * DTO que representa la respuesta con las prioridades de descuentos configuradas.
 * CRITERIO-4.4: Incluye timestamps e IDs tipados como UUID
 */
public record DiscountLimitPriorityResponse(
    UUID uid,
    UUID discountSettingId,
    List<PriorityEntry> priorities,
    Instant createdAt,
    Instant updatedAt
) {
    /**
     * Entrada individual de prioridad en la respuesta.
     */
    public record PriorityEntry(
        UUID discountTypeId,
        Integer priorityLevel,
        Instant createdAt
    ) {}
}
