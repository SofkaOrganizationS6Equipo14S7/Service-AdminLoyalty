package com.loyalty.engine.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class AdminConfiguracionResponse {
    private Long ecommerceId;
    private BigDecimal topeMaximo;
    private Integer prioridadGlobal;
    private List<RangoFidelidadResponse> rangosFidelidad;

    @Data
    public static class RangoFidelidadResponse {
        private Long id;
        private Long ecommerceId;
        private String nombre;
        private Integer minPuntos;
        private Integer maxPuntos;
        private BigDecimal porcentajeDescuento;
    }
}
