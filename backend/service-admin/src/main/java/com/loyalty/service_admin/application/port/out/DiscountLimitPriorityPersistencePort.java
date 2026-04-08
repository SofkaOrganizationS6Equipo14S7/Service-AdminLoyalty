package com.loyalty.service_admin.application.port.out;

import com.loyalty.service_admin.domain.entity.DiscountPriorityEntity;

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
     * Guardar prioridad de descuento.
     */
    DiscountPriorityEntity savePriority(DiscountPriorityEntity priority);

    /**
     * Obtener prioridad por ID.
     */
    Optional<DiscountPriorityEntity> findPriorityById(UUID priorityId);

    /**
     * Listar prioridades por configuración de descuentos.
     */
    List<DiscountPriorityEntity> findPrioritiesByConfig(UUID configId);

    /**
     * Eliminar prioridad por ID.
     */
    void deletePriority(UUID priorityId);

    /**
     * Verificar si existe una prioridad con el nivel específico en una configuración.
     */
    boolean existsPriorityWithLevel(UUID configId, Integer priorityLevel);
}
