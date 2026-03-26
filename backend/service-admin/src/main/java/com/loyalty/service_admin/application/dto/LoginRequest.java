package com.loyalty.service_admin.application.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * DTO para solicitud de login.
 * Contiene las credenciales del usuario.
 */
public record LoginRequest(
    @NotBlank(message = "username es obligatorio")
    String username,
    
    @NotBlank(message = "password es obligatorio")
    String password
) {
}
