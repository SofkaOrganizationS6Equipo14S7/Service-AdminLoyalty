package com.loyalty.admin.dto;

import com.loyalty.admin.entity.ReglaTipo;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class ReglaRequest {
    @NotNull(message = "ecommerceId es obligatorio")
    private Long ecommerceId;

    @NotNull(message = "tipo es obligatorio")
    private ReglaTipo tipo;

    @NotBlank(message = "nombre es obligatorio")
    private String nombre;

    private String descripcion;

    @NotNull(message = "porcentajeDescuento es obligatorio")
    @DecimalMin(value = "0.0", inclusive = false, message = "porcentajeDescuento debe ser mayor a 0")
    private BigDecimal porcentajeDescuento;

    @NotNull(message = "prioridad es obligatoria")
    private Integer prioridad;

    private boolean activa;

    private String productoSku;
    private String temporada;
}
