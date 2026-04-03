package com.loyalty.service_admin.application.dto.discountconfig;

import java.time.Instant;
import java.util.UUID;

/**
 * Request para configurar prioridades de descuentos.
 * 
 * @param discountSettingId ID de la configuración de descuentos
 * @param priorities Array de prioridades [{ discountTypeId, priorityLevel }]
 */
public record DiscountPriorityRequest(
    UUID discountSettingId,
    java.util.List<PriorityItem> priorities
) {
    public record PriorityItem(UUID discountTypeId, Integer priorityLevel) {}
}
