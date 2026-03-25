package com.loyalty.admin.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class EcommerceRequest {
    @NotBlank(message = "nombre es obligatorio")
    private String nombre;

    @NotBlank(message = "apiKey es obligatoria")
    private String apiKey;

    private boolean activo;
}
