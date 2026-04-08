package com.loyalty.service_admin.application.port.out;

import com.loyalty.service_admin.domain.entity.DiscountSettingsEntity;

import java.util.UUID;

/**
 * DiscountConfigEventPort - Puerto de salida para publicación de eventos sobre configuración de descuentos.
 *
 * Este puerto abstrae la mensajería asíncrona (RabbitMQ) de la lógica de negocio.
 *
 * Abarca eventos de:
 * - HU-09: Discount Limits Configuration events
 */
public interface DiscountConfigEventPort {

    /**
     * Publicar evento de configuración creada/actualizada (CONFIG_UPDATED).
     */
    void publishConfigUpdated(DiscountSettingsEntity config, UUID ecommerceId);

    /**
     * Publicar evento de configuración deletada (CONFIG_DELETED).
     */
    void publishConfigDeleted(UUID configId, UUID ecommerceId);
}
