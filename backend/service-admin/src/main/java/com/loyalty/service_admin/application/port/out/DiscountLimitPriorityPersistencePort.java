package com.loyalty.service_admin.application.port.out;

import com.loyalty.service_admin.domain.entity.DiscountLimitPriorityEntity;
import com.loyalty.service_admin.domain.entity.DiscountSettingsEntity;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * DiscountLimitPriorityPersistencePort - Puerto de salida para operaciones de persistencia de prioridades de límite de descuento.
 *
 * Este puerto abstrae el acceso a datos (JPA) de la lógica de negocio.
 */
public interface DiscountLimitPriorityPersistencePort {

    /**
     * Guardar prioridad de límite de descuento.
     */
    DiscountLimitPriorityEntity savePriority(DiscountLimitPriorityEntity priority);

    /**
     * Obtener prioridad por ID.
     */
    Optional<DiscountLimitPriorityEntity> findPriorityById(UUID priorityId);

    /**
     * Listar prioridades por configuración de descuentos.
     */
    List<DiscountLimitPriorityEntity> findPrioritiesByConfig(UUID configId);

    /**
     * Eliminar prioridad por ID.
     */
    void deletePriority(UUID priorityId);

    /**
     * Verificar si existe prioridad con el nombre dado en una configuración.
     */
    boolean existsPriorityWithName(UUID configId, String name);
}
