package com.loyalty.service_admin.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * DTO para cambio de contraseña del usuario autenticado.
 * SPEC-004 HU-03: Actualizar mi información de perfil
 * 
 * El usuario proporciona:
 * - currentPassword: contraseña actual para validación (CRITERIO-3.4)
 * - newPassword: nueva contraseña (mínimo 12 caracteres)
 * - confirmPassword: confirmación de nueva contraseña (debe ser igual a newPassword)
 * 
 * Validaciones:
 * - currentPassword debe ser correcto (401 Unauthorized si falla) (CRITERIO-3.4)
 * - newPassword y confirmPassword deben coincidir (400 Bad Request) (CRITERIO-3.2)
 * - newPassword debe cumplir policy: mínimo 12 caracteres, mayúscula, minúscula, número
 */
public record ChangePasswordRequest(
    @NotBlank(message = "La contraseña actual es obligatoria")
    String currentPassword,
    
    @NotBlank(message = "La nueva contraseña es obligatoria")
    @Size(min = 12, message = "La nueva contraseña debe tener mínimo 12 caracteres")
    String newPassword,
    
    @NotBlank(message = "La confirmación de contraseña es obligatoria")
    String confirmPassword
) {
}
