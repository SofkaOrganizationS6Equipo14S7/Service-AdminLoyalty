package com.loyalty.service_admin.application.dto.classificationrule;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;

/**
 * CRITERIO-7.7: Request para actualizar classification_rule
 */
public record ClassificationRuleUpdateRequest(
        @Size(min = 3, max = 100)
        String name,
        
        @Size(max = 500)
        String description,
        
        @DecimalMin("0.01")
        @DecimalMax("100.00")
        BigDecimal discountPercentage,
        
        String metricType,
        
        BigDecimal minValue,
        
        BigDecimal maxValue,
        
        Integer priority
) {
    /**
     * Validar estructura si se proporcionan valores
     */
    public ClassificationRuleUpdateRequest {
        // Solo validar si ambos están presentes
        if (minValue != null && maxValue != null && minValue.compareTo(maxValue) >= 0) {
            throw new IllegalArgumentException("minValue must be less than maxValue");
        }
        
        if (metricType != null && !metricType.isBlank()) {
            String upper = metricType.toUpperCase();
            if (!("TOTAL_SPENT".equals(upper) || "ORDER_COUNT".equals(upper) || 
                  "LOYALTY_POINTS".equals(upper) || "CUSTOM".equals(upper))) {
                throw new IllegalArgumentException("metricType must be one of: total_spent, order_count, loyalty_points, custom");
            }
        }
    }
}
