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
 * HU-08.3: Valida que priority no se repita en el ecommerce.
 * 
 * Lógica:
 * 1. Obtener todas las reglas activas de clasificación del ecommerce
 * 2. Extraer priorities existentes (excluyendo la regla siendo actualizada)
 * 3. Si nueva priority ∈ existentes → error 409
 * 4. Si nueva priority ∉ existentes → OK
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class UniquePriorityValidator {

    private final RuleAttributeValueRepository ruleAttributeValueRepository;
    private final RuleAttributeRepository ruleAttributeRepository;

    /**
     * Valida que priority sea única en el ecommerce.
     * 
     * @param activeRules lista de reglas activas de clasificación del ecommerce
     * @param newPriority nuevo valor de priority
     * @param excludeRuleId UUID de la regla siendo actualizada (null si es creación)
     * @throws ConflictException si priority ya existe en otra regla del ecommerce
     */
    public void validateUniquePriority(
            List<RuleEntity> activeRules,
            Integer newPriority,
            UUID excludeRuleId
    ) {
        log.debug("Validating uniqueness of priority {}", newPriority);

        // Filtrar reglas activas y excluir la regla siendo actualizada
        List<RuleEntity> rulesToCheck = activeRules.stream()
                .filter(r -> excludeRuleId == null || !r.getId().equals(excludeRuleId))
                .toList();

        if (rulesToCheck.isEmpty()) {
            log.debug("No existing rules with priority to check against; priority {} is unique", newPriority);
            return;  // No hay conflicto
        }

        // Buscar si la priority ya existe
        boolean priorityExists = rulesToCheck.stream()
                .map(this::extractPriorityFromRule)
                .anyMatch(p -> p.equals(newPriority));

        if (priorityExists) {
            throw new ConflictException(
                    "Duplicate hierarchy_level: priority (" + newPriority +
                            ") already exists for this ecommerce"
            );
        }

        log.debug("Uniqueness validation passed for priority {}", newPriority);
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
