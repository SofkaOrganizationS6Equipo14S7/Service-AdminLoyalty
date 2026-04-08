package com.loyalty.service_admin.application.port.out;

import com.loyalty.service_admin.domain.entity.DiscountLimitPriorityEntity;

import java.util.UUID;

/**
 * DiscountLimitPriorityEventPort - Puerto de salida para publicación de eventos sobre prioridades de límite de descuento.
 *
 * Este puerto abstrae la mensajería asíncrona (RabbitMQ) de la lógica de negocio.
 *
 * Abarca eventos de:
 * - HU-09: Discount Limits Priorities events
 */
public interface DiscountLimitPriorityEventPort {

    /**
     * Publicar evento de prioridad creada/actualizada (PRIORITY_UPDATED).
     */
    void publishPriorityUpdated(DiscountLimitPriorityEntity priority, UUID ecommerceId);

    /**
     * Publicar evento de prioridad deletada (PRIORITY_DELETED).
     */
    void publishPriorityDeleted(UUID priorityId, UUID ecommerceId);
}
