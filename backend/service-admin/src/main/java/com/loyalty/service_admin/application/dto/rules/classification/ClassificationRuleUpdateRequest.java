package com.loyalty.service_admin.application.dto.rules.classification;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import java.math.BigDecimal;

/**
 * DTO for updating an existing classification rule.
 * Request body for PUT /api/v1/admin/classification-rules/{uid}
 * All fields are optional (partial update).
 */
public record ClassificationRuleUpdateRequest(
    @DecimalMin(value = "0", message = "Min value must be non-negative when provided")
    BigDecimal minValue,

    @DecimalMin(value = "0", message = "Max value must be non-negative when provided")
    BigDecimal maxValue,

    @Min(value = 1, message = "Priority must be >= 1 when provided")
    Integer priority,

    Boolean isActive
) {
}
