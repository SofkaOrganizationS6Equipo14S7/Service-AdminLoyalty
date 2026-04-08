package com.loyalty.service_admin.application.port.in;

import com.loyalty.service_admin.application.dto.discount.DiscountLimitPriorityRequest;
import com.loyalty.service_admin.application.dto.discount.DiscountLimitPriorityResponse;

import java.util.UUID;

/**
 * DiscountLimitPriorityUseCase - Puerto de entrada para operaciones sobre prioridades de límite de descuento.
 *
 * Abarca:
 * - HU-09: Discount Limits Priorities
 */
public interface DiscountLimitPriorityUseCase {

    /**
     * Crear o actualizar prioridades de límite de descuento.
     */
    DiscountLimitPriorityResponse savePriorities(DiscountLimitPriorityRequest request);

    /**
     * Obtener prioridades de límite de descuento para una configuración.
     */
    DiscountLimitPriorityResponse getPriorities(UUID configId);
}
