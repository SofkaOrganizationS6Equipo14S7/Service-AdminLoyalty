package com.loyalty.service_engine.application.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

/**
 * DTO for customer classification request.
 * Request body for POST /api/v1/customers/classify
 * Flexible: supports multiple metrics (total_spent, order_count, loyalty_points, etc).
 */
public record ClassifyRequestV1(
    @NotNull(message = "Total spent is required")
    @DecimalMin(value = "0", inclusive = true, message = "Total spent must be non-negative")
    BigDecimal totalSpent,

    @NotNull(message = "Order count is required")
    @Min(value = 0, message = "Order count must be non-negative")
    Integer orderCount,

    @Min(value = 0, message = "Loyalty points must be non-negative when provided")
    Integer loyaltyPoints // Optional
) {
}
