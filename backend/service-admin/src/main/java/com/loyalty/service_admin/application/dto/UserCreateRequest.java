package com.loyalty.service_admin.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.UUID;

/**
 * DTO para creación de usuario vinculado a ecommerce.
 * SPEC-002 HU-01: Crear perfil de usuario asociado a ecommerce
 * 
 * Validaciones:
 * - username: 3-50 caracteres
 * - password: mínimo 12 caracteres (strong password requirement)
 * - role: debe ser "USER" (solo SUPER_ADMIN pueden crear SUPER_ADMIN)
 * - ecommerceId: obligatorio (vinculado a ecommerce específico)
 * 
 * CRITERIO-1.1: Usuario debe estar vinculado a ecommerce vía ecommerceId
 */
public record UserCreateRequest(
    @NotBlank(message = "El username es obligatorio")
    @Size(min = 3, max = 50, message = "El username debe tener entre 3 y 50 caracteres")
    String username,
    
    @NotBlank(message = "La contraseña es obligatoria")
    @Size(min = 12, message = "La contraseña debe tener mínimo 12 caracteres")
    String password,
    
    @NotBlank(message = "El rol es obligatorio")
    String role,
    
    @NotBlank(message = "El ecommerceId es obligatorio")
    UUID ecommerceId
) {
}
