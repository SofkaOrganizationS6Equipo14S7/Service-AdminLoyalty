package com.loyalty.service_admin.application.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * DTO for creating a new customer tier.
 * Request body for POST /api/v1/admin/tiers
 */
public record CustomerTierCreateRequest(
    @NotBlank(message = "Tier name is required")
    String name,

    @NotNull(message = "Tier level is required")
    @Min(value = 1, message = "Tier level must be between 1 and 4")
    Integer level
) {
}
