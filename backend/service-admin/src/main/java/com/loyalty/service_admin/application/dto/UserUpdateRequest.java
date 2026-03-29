package com.loyalty.service_admin.application.dto;

import jakarta.validation.constraints.Size;
import java.util.UUID;

/**
 * DTO para actualización de usuario.
 * SPEC-002 HU-04/HU-05: Actualizar usuario (username, password, active, ecommerce)
 * 
 * Campos opcionales:
 * - username: 3-50 caracteres (si se proporciona)
 * - password: mínimo 12 caracteres (si se proporciona)
 * - active: true/false para activar/desactivar usuario
 * - ecommerceId: cambiar ecommerce vinculado (SUPER_ADMIN only)
 * 
 * CRITERIO-5.1: Solo SUPER_ADMIN puede actualizar usuarios
 */
public record UserUpdateRequest(
    @Size(min = 3, max = 50, message = "El username debe tener entre 3 y 50 caracteres")
    String username,
    
    @Size(min = 12, message = "La contraseña debe tener mínimo 12 caracteres")
    String password,
    
    Boolean active,
    
    UUID ecommerceId
) {
}
