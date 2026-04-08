package com.loyalty.service_admin.application.port.out;

import com.loyalty.service_admin.domain.entity.CustomerTierEntity;

import java.util.UUID;

/**
 * CustomerTierEventPort - Puerto de salida para publicación de eventos sobre customer tiers.
 *
 * Este puerto abstrae la mensajería asíncrona (RabbitMQ) de la lógica de negocio.
 *
 * Abarca eventos de:
 * - HU-10: Customer Tier events (CRUD + activation)
 */
public interface CustomerTierEventPort {

    /**
     * Publicar evento de customer tier creado (TIER_CREATED).
     */
    void publishTierCreated(CustomerTierEntity tier, UUID ecommerceId);

    /**
     * Publicar evento de customer tier actualizado (TIER_UPDATED).
     */
    void publishTierUpdated(CustomerTierEntity tier, UUID ecommerceId);

    /**
     * Publicar evento de customer tier eliminado (TIER_DELETED).
     */
    void publishTierDeleted(UUID tierId, UUID ecommerceId);

    /**
     * Publicar evento de customer tier activado (TIER_ACTIVATED).
     */
    void publishTierActivated(CustomerTierEntity tier, UUID ecommerceId);

    /**
     * Publicar evento de customer tier desactivado (TIER_DEACTIVATED).
     */
    void publishTierDeactivated(CustomerTierEntity tier, UUID ecommerceId);
}
