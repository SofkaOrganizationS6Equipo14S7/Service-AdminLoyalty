package com.loyalty.service_admin.application.dto.customertier;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record CustomerTierCreateRequest(
    @NotNull(message = "ecommerceId is required")
    UUID ecommerceId,

    @NotBlank(message = "Tier name is required")
    String name,


    @NotNull(message = "hierarchyLevel is required")
    @Min(value = 1, message = "hierarchyLevel must be >= 1")
    Integer hierarchyLevel
) {
}