package com.loyalty.service_admin.application.port.out;

import com.loyalty.service_admin.domain.entity.RuleEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * RulePersistencePort - Puerto de salida para operaciones de persistencia de reglas.
 *
 * Este puerto abstrae el acceso a datos (JPA) de la lógica de negocio.
 * La implementación concreta se proporciona a través de JpaRuleAdapter.
 */
public interface RulePersistencePort {

    /**
     * Guardar una regla (crear o actualizar).
     */
    RuleEntity saveRule(RuleEntity rule);

    /**
     * Obtener regla por ID.
     */
    Optional<RuleEntity> findRuleById(UUID ruleId);

    /**
     * Obtener regla por ID y validar que pertenece al ecommerce (multi-tenancy).
     */
    Optional<RuleEntity> findRuleByIdAndEcommerce(UUID ruleId, UUID ecommerceId);

    /**
     * Listar reglas por ecommerce.
     */
    Page<RuleEntity> findRulesByEcommerce(UUID ecommerceId, Pageable pageable);

    /**
     * Listar reglas activas por ecommerce.
     */
    Page<RuleEntity> findActiveRulesByEcommerce(UUID ecommerceId, Pageable pageable);

    /**
     * Buscar reglas por estado de actividad.
     */
    List<RuleEntity> findRulesByStatus(UUID ecommerceId, Boolean isActive);

    /**
     * Eliminar (soft delete) una regla.
     */
    void deleteRule(RuleEntity rule);

    /**
     * Verifica si existe una regla con el ID dado.
     */
    boolean existsRule(UUID ruleId);

    /**
     * Verifica si existe una regla activa con ciertos atributos.
     */
    boolean existsActiveRuleWithAttribute(UUID ecommerceId, String attributeName, String attributeValue);
}
