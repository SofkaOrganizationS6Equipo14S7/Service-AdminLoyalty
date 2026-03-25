package com.loyalty.admin.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
public class ConfiguracionDescuentoResponse {
    private Long ecommerceId;
    private BigDecimal topeMaximo;
    private Integer prioridadGlobal;
    private List<RangoFidelidadResponse> rangosFidelidad;
}
