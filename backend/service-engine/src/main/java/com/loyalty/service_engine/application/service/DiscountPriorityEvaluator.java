package com.loyalty.service_engine.application.service;

import com.loyalty.service_engine.application.dto.AppliedRuleDetail;
import com.loyalty.service_engine.application.dto.DiscountCalculateRequestV2;
import com.loyalty.service_engine.domain.entity.ClassificationRuleReplicaEntity;
import com.loyalty.service_engine.domain.entity.DiscountPriorityEntity;
import com.loyalty.service_engine.domain.entity.DiscountConfigEntity;
import com.loyalty.service_engine.domain.repository.ClassificationRuleReplicaRepository;
import com.loyalty.service_engine.domain.repository.DiscountPriorityRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Evaluador de descuentos por prioridad.
 * 
 * Flujo:
 * 1. Obtiene prioridades de descuento para el ecommerce
 * 2. Para cada prioridad (1, 2, 3, ...):
 *    - Obtiene reglas activas de ese tipo
 *    - Evalúa logic_conditions contra request + cliente
 *    - Calcula descuentos (PERCENTAGE o FIXED_AMOUNT)
 *    - Aplica según applied_with (INDIVIDUAL, CUMULATIVE, EXCLUSIVE)
 * 3. Retorna lista de reglas aplicadas en orden
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class DiscountPriorityEvaluator {

    private final ClassificationRuleReplicaRepository ruleRepository;
    private final DiscountPriorityRepository priorityRepository;
    private final LogicConditionEvaluator logicEvaluator;

    /**
     * Evalúa descuentos en orden de prioridad
     * 
     * @param request datos de carrito y cliente
     * @param subtotal monto subtotal calculado
     * @param customerTier tier de clasificación del cliente
     * @param ecommerceId identidad del e-commerce
     * @return lista de reglas aplicadas (puede estar vacía)
     */
    public List<AppliedRuleDetail> evaluateByPriority(
        DiscountCalculateRequestV2 request,
        BigDecimal subtotal,
        String customerTier,
        UUID ecommerceId
    ) {
        log.info("Starting discount evaluation for ecommerce={}, subtotal={}", ecommerceId, subtotal);

        // Obtener orden de evaluación de prioridades
        List<DiscountPriorityEntity> priorities = priorityRepository
            .findByDiscountConfigIdOrderByPriorityLevel(
                // Nota: Se asume que ecommerceId mapea a discountConfigId
                // En producción, usar lookup correcto
                ecommerceId
            );

        if (priorities.isEmpty()) {
            log.debug("No discount priorities configured for ecommerce={}", ecommerceId);
            return List.of();
        }

        List<AppliedRuleDetail> appliedRules = new ArrayList<>();

        // Evaluar por cada nivel de prioridad
        for (DiscountPriorityEntity priority : priorities) {
            log.debug("Evaluating priority level={}, discountType={}", priority.getPriorityLevel(), priority.getDiscountType());

            // Obtener reglas activas de este tipo
            List<ClassificationRuleReplicaEntity> rulesForType = ruleRepository
                .findByEcommerceIdAndDiscountTypeCodeAndIsActiveTrueOrderByPriorityLevelAsc(
                    ecommerceId,
                    getMappedDiscountTypeCode(priority.getDiscountType())
                );

            log.debug("Found {} rules for type={}", rulesForType.size(), priority.getDiscountType());

            // Evaluar cada regla
            for (ClassificationRuleReplicaEntity rule : rulesForType) {
                try {
                    // Evaluar si la regla aplica
                    boolean conditionsMatch = logicEvaluator.evaluateCondition(
                        rule.getLogicConditions(),
                        request,
                        subtotal,
                        customerTier
                    );

                    if (!conditionsMatch) {
                        log.debug("Rule {} conditions not matched", rule.getId());
                        continue;
                    }

                    // Calcular descuento
                    BigDecimal discountAmount = calculateDiscount(rule, subtotal);
                    BigDecimal discountPercentage = rule.getDiscountType().equals("PERCENTAGE")
                        ? rule.getDiscountValue()
                        : null;

                    // Crear detalle de regla aplicada
                    AppliedRuleDetail detail = new AppliedRuleDetail(
                        rule.getId(),
                        rule.getName(),
                        rule.getDiscountTypeCode(),
                        rule.getDiscountType(),
                        rule.getAppliedWith(),
                        discountPercentage,
                        discountAmount,
                        rule.getPriorityLevel()
                    );

                   appliedRules.add(detail);
                    log.info("Rule applied: ruleId={}, discountAmount={}, appliedWith={}",
                        rule.getId(), discountAmount, rule.getAppliedWith());

                } catch (Exception e) {
                    log.error("Error evaluating rule {}: {}", rule.getId(), e.getMessage(), e);
                    // Continuar con siguiente regla en caso de error
                }
            }
        }

        log.info("Discount evaluation complete, {} rules applied", appliedRules.size());
        return appliedRules;
    }

    /**
     * Calcula monto de descuento según tipo
     */
    private BigDecimal calculateDiscount(ClassificationRuleReplicaEntity rule, BigDecimal subtotal) {
        if ("PERCENTAGE".equals(rule.getDiscountType())) {
            // Descuento porcentual
            BigDecimal percentage = rule.getDiscountValue().divide(new BigDecimal("100"), 10, java.math.RoundingMode.HALF_UP);
            BigDecimal discount = subtotal.multiply(percentage);
            log.debug("Calculated percentage discount: subtotal={}, percentage={}, discount={}", 
                subtotal, rule.getDiscountValue(), discount);
            return discount;
        } else if ("FIXED_AMOUNT".equals(rule.getDiscountType())) {
            // Descuento de monto fijo
            log.debug("Calculated fixed discount: amount={}", rule.getDiscountValue());
            return rule.getDiscountValue();
        } else {
            log.warn("Unknown discount type: {}", rule.getDiscountType());
            return BigDecimal.ZERO;
        }
    }

    /**
     * Mapea nombre de tipo de descuento a código de tipo de regla
     * (conversión simple entre nomenclaturas)
     */
    private String getMappedDiscountTypeCode(String discountType) {
        // Mapeo simple ej. "FIDELITY" -> "FIDELITY"
        // En producción: consultar tabla de mapeos
        return discountType.toUpperCase();
    }
}
