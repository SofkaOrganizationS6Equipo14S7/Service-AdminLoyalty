package com.loyalty.service_admin.application.dto.configuration;

import com.loyalty.service_admin.domain.model.CapAppliesTo;
import com.loyalty.service_admin.domain.model.CapType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record CapRequest(
        @NotNull CapType type,
        @NotNull @DecimalMin(value = "0.0", inclusive = false) BigDecimal value,
        @NotNull CapAppliesTo appliesTo
) {
}
