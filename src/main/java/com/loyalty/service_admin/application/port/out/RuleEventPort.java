package com.loyalty.service_admin.application.port.out;

import com.loyalty.service_admin.domain.entity.RuleEntity;

import java.util.Map;
import java.util.UUID;

/**
 * RuleEventPort - Puerto de salida para publicación de eventos sobre reglas.
 *
 * Este puerto abstrae la mensajería asíncrona (RabbitMQ) de la lógica de negocio.
 * La implementación concreta se proporciona a través de RuleEventAdapter.
 *
 * Abarca eventos de:
 * - HU-06: Seasonal Rules events
 * - HU-07: Product Rules events
 * - HU-14: Rule Activation events
 */
public interface RuleEventPort {

    /**
     * Publicar evento de regla creada (RULE_CREATED).
     */
    void publishRuleCreated(
            RuleEntity rule,
            UUID ecommerceId,
            Map<String, String> attributes,
            String ruleType  // SEASONAL, PRODUCT, CLASSIFICATION
    );

    /**
     * Publicar evento de regla actualizada (RULE_UPDATED).
     */
    void publishRuleUpdated(
            RuleEntity rule,
            UUID ecommerceId,
            Map<String, String> attributes,
            String ruleType
    );

    /**
     * Publicar evento de regla eliminada (RULE_DELETED).
     */
    void publishRuleDeleted(UUID ruleId, UUID ecommerceId, String ruleType);

    /**
     * Publicar evento de activación de regla (RULE_ACTIVATED) - HU-14.
     */
    void publishRuleActivated(RuleEntity rule, UUID ecommerceId, String ruleType);

    /**
     * Publicar evento de desactivación de regla (RULE_DEACTIVATED) - HU-14.
     */
    void publishRuleDeactivated(RuleEntity rule, UUID ecommerceId, String ruleType);

    /**
     * Publicar evento de tiers asignados a regla.
     */
    void publishTiersAssignedToRule(UUID ruleId, UUID ecommerceId, Iterable<UUID> tierIds);

    /**
     * Publicar evento de tier removido de regla.
     */
    void publishTierRemovedFromRule(UUID ruleId, UUID ecommerceId, UUID tierId);

    /**
     * Publicar evento de regla de clasificación creada.
     */
    void publishClassificationRuleCreated(UUID tierId, UUID ecommerceId, Map<String, String> attributes);

    /**
     * Publicar evento de regla de clasificación actualizada.
     */
    void publishClassificationRuleUpdated(UUID tierId, UUID ecommerceId, Map<String, String> attributes);

    /**
     * Publicar evento de regla de clasificación eliminada.
     */
    void publishClassificationRuleDeleted(UUID tierId, UUID ruleId, UUID ecommerceId);
}
