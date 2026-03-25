package com.loyalty.engine.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ClasificarClienteRequest {
    @NotNull(message = "puntosFidelidad es obligatorio")
    private Integer puntosFidelidad;
}
