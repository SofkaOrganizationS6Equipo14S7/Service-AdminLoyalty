package com.loyalty.service_admin.application.dto.classificationrule;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;

/**
 * CRITERIO-7.3, 7.4: Request para crear classification_rule vinculada a customer-tier
 * Internamente crea un RuleEntity con type=CLASSIFICATION
 * 
 * Atributos almacenados en rule_attributes:
 * - metricType: total_spent, order_count, loyalty_points, custom
 * - minValue, maxValue: rango de clasificación (validado en service-engine)
 * - priority: orden de evaluación
 */
public record ClassificationRuleCreateRequest(
        @NotNull(message = "discountPriorityId is required")
        String discountPriorityId,  // UUID string para vincular a discount_priority
        
        @NotBlank(message = "name is required")
        @Size(min = 3, max = 100)
        String name,
        
        @Size(max = 500)
        String description,
        
        @NotNull(message = "discountPercentage is required")
        @DecimalMin("0.01")
        @DecimalMax("100.00")
        BigDecimal discountPercentage,
        
        // Atributos de clasificación
        @NotBlank(message = "metricType is required (total_spent|order_count|loyalty_points|custom)")
        String metricType,
        
        @NotNull(message = "minValue is required")
        @DecimalMin("0.00")
        BigDecimal minValue,
        
        @NotNull(message = "maxValue is required")
        @DecimalMin("0.01")
        BigDecimal maxValue,
        
        @NotNull(message = "priority is required")
        @Min(1)
        Integer priority
) {
    /**
     * Validar estructura de metricType
     */
    public ClassificationRuleCreateRequest {
        // Validar que minValue < maxValue
        if (minValue != null && maxValue != null && minValue.compareTo(maxValue) >= 0) {
            throw new IllegalArgumentException("minValue must be less than maxValue (CRITERIO-7.3)");
        }
        
        // Validar que metricType está en enum permitido
        if (metricType != null && !metricType.isBlank()) {
            String upper = metricType.toUpperCase();
            if (!("TOTAL_SPENT".equals(upper) || "ORDER_COUNT".equals(upper) || 
                  "LOYALTY_POINTS".equals(upper) || "CUSTOM".equals(upper))) {
                throw new IllegalArgumentException("metricType must be one of: total_spent, order_count, loyalty_points, custom (CRITERIO-7.4)");
            }
        }
    }
}
