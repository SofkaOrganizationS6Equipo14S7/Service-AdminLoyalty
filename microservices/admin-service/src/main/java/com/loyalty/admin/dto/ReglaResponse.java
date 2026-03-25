package com.loyalty.admin.dto;

import com.loyalty.admin.entity.ReglaTipo;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class ReglaResponse {
    private Long id;
    private Long ecommerceId;
    private ReglaTipo tipo;
    private String nombre;
    private String descripcion;
    private BigDecimal porcentajeDescuento;
    private Integer prioridad;
    private boolean activa;
    private String productoSku;
    private String temporada;
}
