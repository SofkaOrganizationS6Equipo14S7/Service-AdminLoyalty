package com.loyalty.service_engine.application.dto;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * DTO que representa la respuesta con las prioridades de descuentos (réplica).
 * IDÉNTICA a la versión en Service-Admin para mantener consistencia de API.
 */
public record DiscountPriorityResponse(
    String uid,
    String discountConfigId,
    List<PriorityEntry> priorities,
    OffsetDateTime createdAt
) {
    /**
     * Entrada individual de prioridad en la respuesta.
     */
    public record PriorityEntry(
        String discountType,
        Integer priorityLevel,
        OffsetDateTime createdAt
    ) {}
}
