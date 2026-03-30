package com.loyalty.service_admin.application.dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.Instant;

public record SeasonalRuleCreateRequest(
    @NotBlank(message = "Name is required")
    @Size(min = 1, max = 255, message = "Name must be between 1 and 255 characters")
    String name,

    @Size(max = 1000, message = "Description must not exceed 1000 characters")
    String description,

    @NotNull(message = "Discount percentage is required")
    @DecimalMin(value = "0.00", message = "Discount percentage must be at least 0.00")
    @DecimalMax(value = "100.00", message = "Discount percentage must not exceed 100.00")
    BigDecimal discountPercentage,

    @NotBlank(message = "Discount type is required")
    @Pattern(regexp = "PERCENTAGE|FIXED_AMOUNT", message = "Discount type must be PERCENTAGE or FIXED_AMOUNT")
    String discountType,

    @NotNull(message = "Start date is required")
    Instant startDate,

    @NotNull(message = "End date is required")
    Instant endDate
) {
}
