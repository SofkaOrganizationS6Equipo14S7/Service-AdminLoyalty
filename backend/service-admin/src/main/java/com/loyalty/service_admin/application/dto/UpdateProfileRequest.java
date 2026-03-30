package com.loyalty.service_admin.application.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * DTO para actualización de perfil del usuario autenticado.
 * SPEC-004 HU-03: Actualizar mi información de perfil
 * 
 * El usuario STORE_USER puede cambiar:
 * - name: nombre de usuario (1-100 caracteres)
 * - email: dirección de email (debe ser único globalmente)
 * 
 * Restricciones:
 * - No puede cambiar username (identificador único de login)
 * - No puede cambiar role
 * - No puede cambiar ecommerce_id
 * - Email debe ser único globalmente (CRITERIO-3.3)
 */
public record UpdateProfileRequest(
    @NotBlank(message = "El nombre es obligatorio")
    @Size(min = 1, max = 100, message = "El nombre debe tener entre 1 y 100 caracteres")
    String name,
    
    @NotBlank(message = "El email es obligatorio")
    @Email(message = "El email debe ser válido")
    String email
) {
}
