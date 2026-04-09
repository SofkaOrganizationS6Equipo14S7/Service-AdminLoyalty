package com.loyalty.service_admin.application.dto.configuration;

import com.loyalty.service_admin.domain.model.RoundingRule;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.UUID;

public record ConfigurationCreateRequest(
        @NotNull UUID ecommerceId,
        @NotBlank @Size(min = 3, max = 3) String currency,
        @NotNull RoundingRule roundingRule,
        @NotNull @Valid CapRequest cap,
        @NotNull @Size(min = 1) List<@Valid DiscountPriorityRequest> priority
) {
}
