package com.loyalty.service_admin.application.dto;

import java.time.Instant;
import java.util.List;

/**
 * Response para la configuración de prioridad de descuentos.
 * @param uid ID único de la configuración de prioridad
 * @param discountConfigId ID de la configuración de tope máximo asociada
 * @param createdAt Timestamp de creación (UTC)
 * @param priorities Lista de elementos DiscountPriority
 */
public record DiscountPriorityResponse(
    String uid,
    String discountConfigId,
    Instant createdAt,
    List<DiscountPriority> priorities
) {
    /**
     * Representa un par de tipo de descuento y su nivel de prioridad.
     * @param discountType Tipo de descuento (ej. LOYALTY_POINTS, COUPON)
     * @param priorityLevel Nivel de prioridad (1 = máxima prioridad)
     */
    public record DiscountPriority(
        String discountType,
        Integer priorityLevel
    ) {}
}
