package com.loyalty.engine.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ClasificarClienteResponse {
    private String nivel;
    private String descripcion;
}
