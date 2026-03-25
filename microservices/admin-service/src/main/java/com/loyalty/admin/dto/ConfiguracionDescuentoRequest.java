package com.loyalty.admin.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class ConfiguracionDescuentoRequest {
    @NotNull(message = "ecommerceId es obligatorio")
    private Long ecommerceId;

    @NotNull(message = "topeMaximo es obligatorio")
    @DecimalMin(value = "0.0", inclusive = false, message = "topeMaximo debe ser mayor a 0")
    private BigDecimal topeMaximo;

    @NotNull(message = "prioridadGlobal es obligatoria")
    private Integer prioridadGlobal;
}
