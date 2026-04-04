package com.loyalty.service_admin.application.dto.rules.classification;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.UUID;

/**
 * DTO for creating a new classification rule.
 * Request body for POST /api/v1/admin/classification-rules
 */
public record ClassificationRuleCreateRequest(
    @NotNull(message = "Tier UID is required")
    UUID tierUid,

    @NotBlank(message = "Metric type is required")
    String metricType, // 'loyalty_points', 'total_spent', 'order_count', 'custom'

    @NotNull(message = "Min value is required")
    @DecimalMin(value = "0", message = "Min value must be non-negative")
    BigDecimal minValue,

    @DecimalMin(value = "0", message = "Max value must be non-negative when provided")
    BigDecimal maxValue, // Optional, can be null (no upper limit)

    @NotNull(message = "Priority is required")
    @Min(value = 1, message = "Priority must be >= 1")
    Integer priority
) {
}
