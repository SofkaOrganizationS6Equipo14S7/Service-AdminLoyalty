package com.loyalty.service_admin.application.dto;

/**
 * DTO para respuesta de login.
 * Contiene el token JWT y datos básicos del usuario.
 */
public record LoginResponse(
    String token,
    String tipo,
    String username,
    String role
) {
}
