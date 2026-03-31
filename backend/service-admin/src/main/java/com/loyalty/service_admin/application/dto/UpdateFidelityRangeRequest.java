package com.loyalty.service_admin.application.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

/**
 * DTO for updating a fidelity range
 * All fields are optional (null fields are not updated)
 */
public record UpdateFidelityRangeRequest(
    @Size(min = 1, max = 255, message = "El nombre debe tener entre 1 y 255 caracteres")
    String name,

    Integer minPoints,

    Integer maxPoints,

    @DecimalMin(value = "0.00", message = "El descuento mínimo es 0%")
    @DecimalMax(value = "100.00", message = "El descuento máximo es 100%")
    BigDecimal discountPercentage
) {
}
