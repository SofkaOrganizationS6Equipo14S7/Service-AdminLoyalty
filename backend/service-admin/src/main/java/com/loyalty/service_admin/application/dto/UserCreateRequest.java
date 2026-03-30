package com.loyalty.service_admin.application.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.UUID;

/**
 * DTO para creación de usuario vinculado a ecommerce.
 * SPEC-003 HU-03.1: Crear usuario estándar por STORE_ADMIN
 * 
 * Validaciones:
 * - username: 3-50 caracteres, único globalmente
 * - email: RFC 5322 válido, único globalmente
 * - password: mínimo 12 caracteres (strong password requirement)
 * - role: debe ser "STORE_USER" (solo SUPER_ADMIN/STORE_ADMIN pueden crear, no STORE_ADMIN via API)
 * - ecommerceId: obligatorio (vinculado a ecommerce específico)
 * 
 * CRITERIO-3.1.1: Usuario debe estar vinculado a ecommerce vía ecommerceId
 */
public record UserCreateRequest(
    @NotBlank(message = "El username es obligatorio")
    @Size(min = 3, max = 50, message = "El username debe tener entre 3 y 50 caracteres")
    String username,
    
    @NotBlank(message = "El email es obligatorio")
    @Email(message = "El email debe ser válido")
    String email,
    
    @NotBlank(message = "La contraseña es obligatoria")
    @Size(min = 12, message = "La contraseña debe tener mínimo 12 caracteres")
    String password,
    
    @NotBlank(message = "El rol es obligatorio")
    String role,
    
    @NotBlank(message = "El ecommerceId es obligatorio")
    UUID ecommerceId
) {
}
