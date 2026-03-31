package com.loyalty.service_admin.application.dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;

/**
 * Request DTO for creating a ProductRule
 */
@SuppressWarnings("SpellCheckingInspection")
public record ProductRuleCreateRequest(
    @NotBlank(message = "name is required")
    @Size(max = 255, message = "name must not exceed 255 characters")
    String name,

    @NotBlank(message = "productType is required")
    @Size(max = 100, message = "productType must not exceed 100 characters")
    String productType,

    @NotNull(message = "discountPercentage is required")
    @DecimalMin(value = "0", message = "discountPercentage must be >= 0")
    @DecimalMax(value = "100", message = "discountPercentage must be <= 100")
    BigDecimal discountPercentage,

    @Size(max = 255, message = "benefit must not exceed 255 characters")
    String benefit
) {}
