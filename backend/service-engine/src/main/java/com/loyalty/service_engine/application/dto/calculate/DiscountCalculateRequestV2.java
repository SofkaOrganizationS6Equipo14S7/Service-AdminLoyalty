package com.loyalty.service_engine.application.dto.calculate;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record DiscountCalculateRequestV2(
        @NotNull UUID ecommerceId,
        @NotNull @DecimalMin(value = "0.0", inclusive = true) BigDecimal subtotal,
        @NotNull @DecimalMin(value = "0.0", inclusive = true) BigDecimal total,
        @NotNull @DecimalMin(value = "0.0", inclusive = true) BigDecimal beforeTax,
        @NotNull @DecimalMin(value = "0.0", inclusive = true) BigDecimal afterTax,
        @NotNull @Size(min = 1) List<@Valid DiscountCandidate> discounts
) {
    public record DiscountCandidate(
            @NotBlank String type,
            @NotNull @DecimalMin(value = "0.0", inclusive = false) BigDecimal amount
    ) {
    }
}
