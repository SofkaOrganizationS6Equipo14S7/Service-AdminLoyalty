package com.loyalty.service_admin.application.dto.rules.discount;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.UUID;

/**
 * DTO para crear o actualizar la configuración de límite de descuentos.
 * CRITERIO-4.1, CRITERIO-4.5: maxDiscountCap debe ser > 0
 */
public record DiscountConfigCreateRequest(
    @NotNull(message = "El ecommerceId es obligatorio")
    UUID ecommerceId,
    
    @NotNull(message = "El maxDiscountCap es obligatorio")
    @DecimalMin(value = "0.01", inclusive = true, message = "El maxDiscountCap debe ser mayor a 0")
    BigDecimal maxDiscountCap,
    
    @NotBlank(message = "El currencyCode es obligatorio")
    String currencyCode,
    
    Boolean allowStacking,
    
    String roundingRule
) {}
