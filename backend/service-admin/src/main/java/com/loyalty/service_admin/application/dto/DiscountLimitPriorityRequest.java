package com.loyalty.service_admin.application.dto;

import java.util.List;

/**
 * DTO para guardar prioridades de descuentos para una configuración.
 */
public record DiscountLimitPriorityRequest(
    String discountConfigId,
    List<PriorityEntry> priorities
) {
    /**
     * Entrada individual de prioridad (tipo descuento + nivel).
     */
    public record PriorityEntry(
        String discountType,
        Integer priorityLevel
    ) {}
}
