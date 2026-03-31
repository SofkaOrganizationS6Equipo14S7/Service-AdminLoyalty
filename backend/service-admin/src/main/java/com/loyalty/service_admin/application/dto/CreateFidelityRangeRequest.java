package com.loyalty.service_admin.application.dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * DTO for creating a new fidelity range
 * 
 * Request validation:
 * - name must be non-blank and max 255 chars
 * - minPoints must be >= 0
 * - maxPoints must be > 0
 * - discountPercentage must be between 0 and 100
 */
public record CreateFidelityRangeRequest(
    @NotBlank(message = "El nombre del rango es obligatorio")
    @Size(min = 1, max = 255, message = "El nombre debe tener entre 1 y 255 caracteres")
    String name,

    @NotNull(message = "minPoints es obligatorio")
    @PositiveOrZero(message = "minPoints debe ser >= 0")
    Integer minPoints,

    @NotNull(message = "maxPoints es obligatorio")
    @Positive(message = "maxPoints debe ser > 0")
    Integer maxPoints,

    @NotNull(message = "discountPercentage es obligatorio")
    @DecimalMin(value = "0.00", message = "El descuento mínimo es 0%")
    @DecimalMax(value = "100.00", message = "El descuento máximo es 100%")
    BigDecimal discountPercentage
) {
}
