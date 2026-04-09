package com.loyalty.service_admin.application.dto.customertier;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CustomerTierUpdateRequest(
    @NotBlank(message = "Tier name is required")
    String name,

    @NotNull(message = "hierarchyLevel is required")
    @Min(value = 1, message = "hierarchyLevel must be >= 1")
    Integer hierarchyLevel
) {
}
