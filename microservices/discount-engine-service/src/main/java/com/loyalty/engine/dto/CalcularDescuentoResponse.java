package com.loyalty.engine.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
public class CalcularDescuentoResponse {
    private String clienteId;
    private String nivelFidelidad;
    private BigDecimal subtotal;
    private BigDecimal descuentoAplicado;
    private BigDecimal total;
    private List<String> reglasAplicadas;
}
