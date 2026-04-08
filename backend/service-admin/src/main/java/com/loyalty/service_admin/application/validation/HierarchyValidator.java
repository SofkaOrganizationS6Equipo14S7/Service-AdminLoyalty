package com.loyalty.service_admin.application.validation;

import com.loyalty.service_admin.domain.entity.RuleAttributeEntity;
import com.loyalty.service_admin.domain.entity.RuleAttributeValueEntity;
import com.loyalty.service_admin.domain.entity.RuleEntity;
import com.loyalty.service_admin.domain.repository.RuleAttributeRepository;
import com.loyalty.service_admin.domain.repository.RuleAttributeValueRepository;
import com.loyalty.service_admin.infrastructure.exception.ConflictException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

/**
 * HU-08.2: Valida que priority (hierarchy_level) sea ascendente por ecommerce.
 * 
 * Lógica:
 * 1. Obtener todas las reglas activas de clasificación del ecommerce
 * 2. Extraer máximo priority actual (excluyendo la regla siendo actualizada)
 * 3. Si nueva priority ≤ max priority → error 409
 * 4. Si nueva priority > max priority → OK
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class HierarchyValidator {

    private final RuleAttributeValueRepository ruleAttributeValueRepository;
    private final RuleAttributeRepository ruleAttributeRepository;

    /**
     * Valida que priority sea mayor que el máximo existente en el ecommerce.
     * 
     * @param activeRules lista de reglas activas de clasificación del ecommerce
     * @param newPriority nuevo valor de priority
     * @param excludeRuleId UUID de la regla siendo actualizada (null si es creación)
     * @throws ConflictException si nueva priority ≤ max priority existente
     */
    public void validateHierarchy(
            List<RuleEntity> activeRules,
            Integer newPriority,
            UUID excludeRuleId
    ) {
        log.debug("Validating hierarchy for priority {}", newPriority);

        // Filtrar reglas activas y excluir la regla siendo actualizada
        List<RuleEntity> rulesToCheck = activeRules.stream()
                .filter(r -> excludeRuleId == null || !r.getId().equals(excludeRuleId))
                .toList();

        if (rulesToCheck.isEmpty()) {
            log.debug("No existing rules to check hierarchy against; priority {} is valid", newPriority);
            return;  // La primera regla siempre es válida
        }

        // Extraer máximo priority existente
        Integer maxPriority = rulesToCheck.stream()
                .map(this::extractPriorityFromRule)
                .max(Integer::compareTo)
                .orElse(0);

        log.debug("Max existing priority: {}", maxPriority);

        if (newPriority <= maxPriority) {
            throw new ConflictException(
                    "Hierarchy violation: new priority (" + newPriority +
                            ") must be > max existing priority (" + maxPriority + ") for this ecommerce"
            );
        }

        log.debug("Hierarchy validation passed for priority {}", newPriority);
    }

    /**
     * Extrae el valor de priority de los atributos de una regla.
     */
    private Integer extractPriorityFromRule(RuleEntity rule) {
        List<RuleAttributeValueEntity> attrs = ruleAttributeValueRepository.findByRuleIdOrderByCreatedAtAsc(rule.getId());

        return attrs.stream()
                .filter(a -> isAttributeNamed(a, "priority"))
                .findFirst()
                .map(a -> Integer.parseInt(a.getValue()))
                .orElseThrow(() -> new IllegalStateException("priority not found for rule " + rule.getId()));
    }

    /**
     * Verifica si un atributo tiene el nombre esperado.
     */
    private boolean isAttributeNamed(RuleAttributeValueEntity attrValue, String expectedName) {
        RuleAttributeEntity attr = ruleAttributeRepository.findById(attrValue.getAttributeId()).orElse(null);
        return attr != null && expectedName.equals(attr.getAttributeName());
    }
}
