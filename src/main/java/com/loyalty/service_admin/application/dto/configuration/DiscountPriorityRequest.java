package com.loyalty.service_admin.application.dto.configuration;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record DiscountPriorityRequest(
        @NotBlank String type,
        @NotNull @Min(1) Integer order
) {
}
