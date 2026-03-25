package com.loyalty.engine.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class CalcularDescuentoRequest {

    @NotBlank(message = "apiKey es obligatoria")
    private String apiKey;

    @NotNull(message = "ecommerceId es obligatorio")
    private Long ecommerceId;

    @NotBlank(message = "clienteId es obligatorio")
    private String clienteId;

    @NotNull(message = "puntosFidelidad es obligatorio")
    private Integer puntosFidelidad;

    @NotEmpty(message = "items es obligatorio")
    private List<ItemRequest> items;

    private String temporada;

    @Data
    public static class ItemRequest {
        @NotBlank(message = "sku es obligatorio")
        private String sku;

        @NotNull(message = "cantidad es obligatoria")
        private Integer cantidad;

        @NotNull(message = "precioUnitario es obligatorio")
        @DecimalMin(value = "0.0", inclusive = false, message = "precioUnitario debe ser mayor a 0")
        private BigDecimal precioUnitario;
    }
}
