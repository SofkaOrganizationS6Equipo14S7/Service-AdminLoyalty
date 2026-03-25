package com.loyalty.admin.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class RangoFidelidadResponse {
    private Long id;
    private Long ecommerceId;
    private String nombre;
    private Integer minPuntos;
    private Integer maxPuntos;
    private BigDecimal porcentajeDescuento;
}
