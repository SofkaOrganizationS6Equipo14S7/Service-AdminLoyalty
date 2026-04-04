package com.loyalty.service_admin.application.dto.rules.seasonal;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record SeasonalRuleUpdateRequest(
    @Size(min = 1, max = 255, message = "Name must be between 1 and 255 characters")
    String name,

    @Size(max = 1000, message = "Description must not exceed 1000 characters")
    String description,

    UUID discountTypeId,

    @DecimalMin(value = "0.00", message = "Discount percentage must be at least 0.00")
    @DecimalMax(value = "100.00", message = "Discount percentage must not exceed 100.00")
    BigDecimal discountPercentage,

    Instant startDate,

    Instant endDate
) {
}
