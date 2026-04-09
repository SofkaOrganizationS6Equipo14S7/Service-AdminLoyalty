package com.loyalty.service_admin.application.dto.classificationrule;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Response para classification_rule
 * Retorna información de la regla de clasificación con sus parámetros
 */
public record ClassificationRuleResponse(
        UUID id,
        String name,
        String description,
        BigDecimal discountPercentage,
        String metricType,              // total_spent, order_count, loyalty_points, custom
        BigDecimal minValue,
        BigDecimal maxValue,
        Integer priority,
        Boolean isActive,
        Instant createdAt,
        Instant updatedAt
) {}
