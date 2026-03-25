package com.loyalty.admin.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class RangoFidelidadRequest {
    @NotNull(message = "ecommerceId es obligatorio")
    private Long ecommerceId;

    @NotBlank(message = "nombre es obligatorio")
    private String nombre;

    @NotNull(message = "minPuntos es obligatorio")
    private Integer minPuntos;

    @NotNull(message = "maxPuntos es obligatorio")
    private Integer maxPuntos;

    @NotNull(message = "porcentajeDescuento es obligatorio")
    @DecimalMin(value = "0.0", inclusive = false, message = "porcentajeDescuento debe ser mayor a 0")
    private BigDecimal porcentajeDescuento;
}
