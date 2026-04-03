package com.loyalty.service_admin.application.dto.discountconfig;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Response de prioridades de descuentos.
 * 
 * @param discountSettingId ID de la configuración
 * @param priorities Lista de prioridades configuradas
 */
public record DiscountPriorityResponse(
    UUID discountSettingId,
    List<PriorityDetail> priorities
) {
    public record PriorityDetail(
        UUID discountTypeId,
        String discountTypeName,
        Integer priorityLevel,
        Instant createdAt
    ) {}
}
