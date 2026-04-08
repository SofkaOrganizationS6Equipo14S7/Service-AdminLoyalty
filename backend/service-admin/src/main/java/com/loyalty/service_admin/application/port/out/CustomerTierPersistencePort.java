package com.loyalty.service_admin.application.port.out;

import com.loyalty.service_admin.domain.entity.CustomerTierEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * CustomerTierPersistencePort - Puerto de salida para operaciones de persistencia de customer tiers.
 *
 * Este puerto abstrae el acceso a datos (JPA) de la lógica de negocio.
 */
public interface CustomerTierPersistencePort {

    /**
     * Guardar un customer tier (crear o actualizar).
     */
    CustomerTierEntity saveTier(CustomerTierEntity tier);

    /**
     * Obtener customer tier por ID.
     */
    Optional<CustomerTierEntity> findTierById(UUID tierId);

    /**
     * Listar tiers activos, ordenados por nivel jerárquico.
     */
    List<CustomerTierEntity> findActivetiersOrderedByHierarchy();

    /**
     * Listar TODOS los tiers.
     */
    List<CustomerTierEntity> findAllTiersOrderedByHierarchy();

    /**
     * Listar tiers con paginación.
     */
    Page<CustomerTierEntity> findTiersPaginated(Pageable pageable, Boolean isActive);

    /**
     * Verificar si existe un tier activo con el mismo nombre y ecommerce.
     */
    boolean existsActiveTierWithNameAndEcommerce(UUID ecommerceId, String name);

    /**
     * Verificar si existe un tier con el nombre dado (incluyendo inactivos).
     */
    boolean existsTierWithName(UUID ecommerceId, String name);

    /**
     * Eliminar (soft delete) un customer tier.
     */
    void deleteTier(CustomerTierEntity tier);
}
