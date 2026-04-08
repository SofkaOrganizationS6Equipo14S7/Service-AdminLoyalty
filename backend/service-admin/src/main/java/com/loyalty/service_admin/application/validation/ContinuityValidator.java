package com.loyalty.service_admin.application.validation;

import com.loyalty.service_admin.domain.entity.RuleEntity;
import com.loyalty.service_admin.domain.entity.RuleAttributeValueEntity;
import com.loyalty.service_admin.domain.entity.RuleAttributeEntity;
import com.loyalty.service_admin.domain.repository.RuleAttributeRepository;
import com.loyalty.service_admin.domain.repository.RuleAttributeValueRepository;
import com.loyalty.service_admin.infrastructure.exception.BadRequestException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * HU-08.1: Valida que no existan huecos entre rangos de clasificación.
 * 
 * Lógica:
 * 1. Obtener todas las reglas activas del ecommerce (tipo CLASSIFICATION)
 * 2. Ordenar por minValue ascendente
 * 3. Para la nueva regla (minValue, maxValue), verificar:
 *    - Que no haya hueco con la regla anterior
 *    - Que no haya hueco con la regla siguiente
 * 4. Lanzar BadRequestException si hay hueco
 * 
 * Nota: Se ignoran reglas soft-deleted (isActive=false)
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ContinuityValidator {

    private final RuleAttributeValueRepository ruleAttributeValueRepository;
    private final RuleAttributeRepository ruleAttributeRepository;

    /**
     * Valida continuidad de rangos sin huecos.
     * 
     * @param activeRules lista de reglas activas de clasificación del ecommerce (ya filtradas)
     * @param minValue minValue de la nueva/actualizada regla
     * @param maxValue maxValue de la nueva/actualizada regla
     * @param excludeRuleId UUID de la regla siendo actualizada (null si es creación)
     * @throws BadRequestException si hay huecos
     */
    public void validateContinuity(
            List<RuleEntity> activeRules,
            BigDecimal minValue,
            BigDecimal maxValue,
            UUID excludeRuleId
    ) {
        log.debug("Validating continuity for range [{}-{}]", minValue, maxValue);

        // Filtrar reglas activas y excluir la regla siendo actualizada
        List<RuleEntity> rulesToCheck = activeRules.stream()
                .filter(r -> excludeRuleId == null || !r.getId().equals(excludeRuleId))
                .toList();

        if (rulesToCheck.isEmpty()) {
            log.debug("No existing rules to check continuity against");
            return;  // La primera regla siempre es válida
        }

        // Extraer rangos de todas las reglas
        List<RangeData> existingRanges = rulesToCheck.stream()
                .map(this::extractRangeFromRule)
                .sorted(Comparator.comparing(r -> r.minValue))
                .collect(Collectors.toList());

        log.debug("Existing ranges: {}", existingRanges);

        // Buscar huecos antes
        RangeData lastRange = existingRanges.get(existingRanges.size() - 1);
        if (minValue.compareTo(lastRange.maxValue) > 0) {
            throw new BadRequestException(
                    "Gap detected: rule with range [" + minValue + "-" + maxValue +
                            "] has gap with existing rule [" + lastRange.minValue + "-" + lastRange.maxValue + "]"
            );
        }

        // Buscar huecos después
        Optional<RangeData> nextRange = existingRanges.stream()
                .filter(r -> r.minValue.compareTo(maxValue) > 0)
                .min(Comparator.comparing(r -> r.minValue));

        if (nextRange.isPresent() && nextRange.get().minValue.compareTo(maxValue) > 0) {
            throw new BadRequestException(
                    "Gap detected: rule with range [" + minValue + "-" + maxValue +
                            "] has gap with next rule [" + nextRange.get().minValue + "-" + nextRange.get().maxValue + "]"
            );
        }

        log.debug("Continuity validation passed for range [{}-{}]", minValue, maxValue);
    }

    /**
     * Extrae minValue y maxValue de los atributos de una regla.
     */
    private RangeData extractRangeFromRule(RuleEntity rule) {
        List<RuleAttributeValueEntity> attrs = ruleAttributeValueRepository.findByRuleIdOrderByCreatedAtAsc(rule.getId());

        BigDecimal minValue = attrs.stream()
                .filter(a -> isAttributeNamed(a, "minValue"))
                .findFirst()
                .map(a -> new BigDecimal(a.getValue()))
                .orElseThrow(() -> new IllegalStateException("minValue not found for rule " + rule.getId()));

        BigDecimal maxValue = attrs.stream()
                .filter(a -> isAttributeNamed(a, "maxValue"))
                .findFirst()
                .map(a -> new BigDecimal(a.getValue()))
                .orElseThrow(() -> new IllegalStateException("maxValue not found for rule " + rule.getId()));

        return new RangeData(minValue, maxValue);
    }

    /**
     * Verifica si un atributo tiene el nombre esperado.
     */
    private boolean isAttributeNamed(RuleAttributeValueEntity attrValue, String expectedName) {
        RuleAttributeEntity attr = ruleAttributeRepository.findById(attrValue.getAttributeId()).orElse(null);
        return attr != null && expectedName.equals(attr.getAttributeName());
    }

    /**
     * DTO para almacenar rango (minValue, maxValue).
     */
    private record RangeData(BigDecimal minValue, BigDecimal maxValue) {
        @Override
        public String toString() {
            return "[" + minValue + "-" + maxValue + "]";
        }
    }
}
