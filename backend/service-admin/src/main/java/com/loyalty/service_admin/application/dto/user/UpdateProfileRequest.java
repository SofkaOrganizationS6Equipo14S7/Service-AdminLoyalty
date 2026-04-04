package com.loyalty.service_admin.application.dto.user;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * DTO para actualización de perfil del usuario autenticado.
 * SPEC-004 HU-03: Actualizar mi información de perfil
 * 
 * El usuario STORE_USER puede cambiar:
 * - email: dirección de email (debe ser único globalmente)
 * 
 * Restricciones:
 * - No puede cambiar username (identificador único de login)
 * - No puede cambiar role
 * - No puede cambiar ecommerce_id
 * - Email debe ser único globalmente (CRITERIO-3.3)
 */
public record UpdateProfileRequest(
    @NotBlank(message = "El email es obligatorio")
    @Email(message = "El email debe ser válido")
    String email
) {
}
