package com.loyalty.service_admin.application.dto;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * DTO que representa la respuesta con las prioridades de descuentos configuradas.
 */
public record DiscountLimitPriorityResponse(
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
