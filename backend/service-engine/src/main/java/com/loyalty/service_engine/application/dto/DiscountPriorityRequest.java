package com.loyalty.service_engine.application.dto;

import java.util.List;

/**
 * Request para crear o actualizar la configuración de prioridad de descuentos.
 * @param discountConfigId ID de la configuración de tope máximo asociada
 * @param priorities Lista de elementos DiscountPriority con tipos y niveles
 */
public record DiscountPriorityRequest(
    String discountConfigId,
    List<DiscountPriorityItem> priorities
) {
    /**
     * Representa un par de tipo de descuento y su nivel de prioridad.
     * @param discountType Tipo de descuento (ej. LOYALTY_POINTS, COUPON)
     * @param priorityLevel Nivel de prioridad (1 = máxima prioridad, debe ser secuencial)
     */
    public record DiscountPriorityItem(
        String discountType,
        Integer priorityLevel
    ) {}
}
