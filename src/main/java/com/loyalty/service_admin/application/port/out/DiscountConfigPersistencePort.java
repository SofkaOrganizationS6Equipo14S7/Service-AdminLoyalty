package com.loyalty.service_admin.application.port.out;

import com.loyalty.service_admin.domain.entity.DiscountSettingsEntity;

import java.util.Optional;
import java.util.UUID;

/**
 * DiscountConfigPersistencePort - Puerto de salida para operaciones de persistencia de configuración de descuentos.
 *
 * Este puerto abstrae el acceso a datos (JPA) de la lógica de negocio.
 */
public interface DiscountConfigPersistencePort {

    /**
     * Guardar configuración de descuentos.
     */
    DiscountSettingsEntity saveConfig(DiscountSettingsEntity config);

    /**
     * Obtener configuración activa por ecommerce.
     */
    Optional<DiscountSettingsEntity> findActiveConfigByEcommerce(UUID ecommerceId);

    /**
     * Obtener configuración por ID.
     */
    Optional<DiscountSettingsEntity> findConfigById(UUID configId);

    /**
     * Verificar si existe configuración activa para un ecommerce.
     */
    boolean existsActiveConfigForEcommerce(UUID ecommerceId);
}
