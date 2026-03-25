package com.loyalty.engine.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class AdminReglaResponse {
    private Long id;
    private Long ecommerceId;
    private String tipo;
    private String nombre;
    private String descripcion;
    private BigDecimal porcentajeDescuento;
    private Integer prioridad;
    private boolean activa;
    private String productoSku;
    private String temporada;
}
