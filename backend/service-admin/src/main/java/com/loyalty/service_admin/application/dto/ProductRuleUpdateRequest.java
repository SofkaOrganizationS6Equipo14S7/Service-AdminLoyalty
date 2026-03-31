package com.loyalty.service_admin.application.dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;

/**
 * Request DTO for updating a ProductRule
 */
@SuppressWarnings("SpellCheckingInspection")
public record ProductRuleUpdateRequest(
    @Size(max = 255, message = "name must not exceed 255 characters")
    String name,

    @DecimalMin(value = "0", message = "discountPercentage must be >= 0")
    @DecimalMax(value = "100", message = "discountPercentage must be <= 100")
    BigDecimal discountPercentage,

    @Size(max = 255, message = "benefit must not exceed 255 characters")
    String benefit
) {}
