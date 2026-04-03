package com.loyalty.service_admin.application.dto.customertier;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.UUID;

public record CustomerTierCreateRequest(
    @NotNull(message = "ecommerceId is required")
    UUID ecommerceId,

    @NotNull(message = "discountTypeId is required")
    UUID discountTypeId,

    @NotBlank(message = "Tier name is required")
    String name,

    @NotNull(message = "discountPercentage is required")
    @DecimalMin(value = "0", message = "discountPercentage must be >= 0")
    @DecimalMax(value = "100", message = "discountPercentage must be <= 100")
    BigDecimal discountPercentage,

    @NotNull(message = "hierarchyLevel is required")
    @Min(value = 1, message = "hierarchyLevel must be >= 1")
    Integer hierarchyLevel
) {
}