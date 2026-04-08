package com.loyalty.service_admin.application.port.in;

import com.loyalty.service_admin.application.dto.rules.RuleAttributeMetadataDTO;
import com.loyalty.service_admin.application.dto.rules.RuleCreateRequest;
import com.loyalty.service_admin.application.dto.rules.RuleCustomerTierDTO;
import com.loyalty.service_admin.application.dto.rules.RuleResponse;
import com.loyalty.service_admin.application.dto.rules.RuleResponseWithTiers;
import com.loyalty.service_admin.application.dto.classificationrule.ClassificationRuleCreateRequest;
import com.loyalty.service_admin.application.dto.classificationrule.ClassificationRuleResponse;
import com.loyalty.service_admin.application.dto.classificationrule.ClassificationRuleUpdateRequest;
import com.loyalty.service_admin.application.dto.discount.DiscountPriorityDTO;
import com.loyalty.service_admin.application.dto.discount.DiscountTypeDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.UUID;

/**
 * RuleUseCase - Puerto de entrada para operaciones sobre reglas.
 *
 * Abarca:
 * - HU-06: Seasonal Rules
 * - HU-07: Product Rules
 * - HU-14: Rule Activation
 */
public interface RuleUseCase {

    // ===== CRUD RULES =====

    /**
     * Crear una nueva regla de descuento (seasonal, product, classification).
     * Aplicable a HU-06, HU-07, HU-10.
     */
    RuleResponse createRule(UUID ecommerceId, RuleCreateRequest request);

    /**
     * Obtener regla por ID (HU-06, HU-07).
     */
    RuleResponse getRuleById(UUID ecommerceId, UUID ruleId);

    /**
     * Listar reglas (HU-06, HU-07).
     */
    Page<RuleResponse> listRules(UUID ecommerceId, Boolean isActive, Pageable pageable);

    /**
     * Actualizar regla (HU-06, HU-07).
     */
    RuleResponse updateRule(UUID ecommerceId, UUID ruleId, RuleCreateRequest request);

    /**
     * Eliminar regla (soft delete, HU-06, HU-07).
     */
    void deleteRule(UUID ecommerceId, UUID ruleId);

    /**
     * Actualizar estado de regla - activar/desactivar (HU-14).
     */
    RuleResponse updateRuleStatus(UUID ecommerceId, UUID ruleId, Boolean newStatus);

    // ===== RULES WITH CUSTOMER TIERS =====

    /**
     * Asignar customer tiers a una regla (HU-10).
     */
    RuleResponseWithTiers assignCustomerTiersToRule(UUID ecommerceId, UUID ruleId, List<UUID> tierIds);

    /**
     * Obtener tiers asignados a una regla (HU-10).
     */
    List<RuleCustomerTierDTO> getRuleAssignedTiers(UUID ecommerceId, UUID ruleId);

    /**
     * Eliminar tier de una regla (HU-10).
     */
    void deleteCustomerTierFromRule(UUID ecommerceId, UUID ruleId, UUID tierId);

    // ===== METADATA & LOOKUPS =====

    /**
     * Obtener todos los tipos de descuento disponibles.
     */
    List<DiscountTypeDTO> getAllDiscountTypes();

    /**
     * Obtener prioridades de descuento por tipo.
     */
    List<DiscountPriorityDTO> getDiscountPrioritiesByType(UUID discountTypeId);

    /**
     * Obtener atributos disponibles para un tipo de descuento.
     */
    List<RuleAttributeMetadataDTO> getAvailableAttributesForDiscountType(UUID discountTypeId);

    // ===== CLASSIFICATION RULES (HU-08) =====

    /**
     * Crear regla de clasificación para un tier de cliente (HU-08, HU-10).
     */
    ClassificationRuleResponse createClassificationRuleForTier(
            UUID ecommerceId,
            UUID tierId,
            ClassificationRuleCreateRequest request
    );

    /**
     * Listar reglas de clasificación para un tier (HU-08, HU-10).
     */
    List<ClassificationRuleResponse> listClassificationRulesForTier(UUID ecommerceId, UUID tierId);

    /**
     * Actualizar regla de clasificación (HU-08, HU-10).
     */
    ClassificationRuleResponse updateClassificationRuleForTier(
            UUID ecommerceId,
            UUID tierId,
            UUID ruleId,
            ClassificationRuleUpdateRequest request
    );

    /**
     * Eliminar regla de clasificación (HU-08, HU-10).
     */
    void deleteClassificationRuleForTier(UUID ecommerceId, UUID tierId, UUID ruleId);
}
