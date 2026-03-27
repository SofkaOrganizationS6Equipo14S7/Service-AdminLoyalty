package com.loyalty.service_admin.application.dto;

import jakarta.validation.constraints.Size;
import java.util.UUID;

/**
 * DTO para actualización de usuario.
 * Campos opcionales: username, ecommerceId.
 * NO se permite actualizar password o role por este endpoint.
 * 
 * Implementa SPEC-002 HU-04: Actualizar usuario (cambio de ecommerce/username)
 */
public record UserUpdateRequest(
    @Size(min = 3, max = 50, message = "El username debe tener entre 3 y 50 caracteres")
    String username,
    
    UUID ecommerceId
) {
}
