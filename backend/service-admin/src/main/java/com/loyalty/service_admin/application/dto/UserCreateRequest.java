package com.loyalty.service_admin.application.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.UUID;

/**
 * DTO para creación de usuario.
 * SPEC-002 HU-03.1: Crear usuario estándar por STORE_ADMIN
 * SPEC-005 HU-02.1: Crear STORE_ADMIN para ecommerce por SUPER_ADMIN
 * 
 * Validaciones:
 * - username: 3-50 caracteres, único globalmente
 * - email: RFC 5322 válido, único globalmente
 * - password: mínimo 12 caracteres (strong password requirement)
 * - role: "SUPER_ADMIN", "STORE_ADMIN", o "STORE_USER"
 * - ecommerceId: OBLIGATORIO si role != SUPER_ADMIN, PROHIBIDO si role == SUPER_ADMIN
 * 
 * Reglas:
 * - SUPER_ADMIN: NO puede tener ecommerceId (RN-01)
 * - STORE_ADMIN/STORE_USER: DEBEN tener ecommerceId
 * 
 * CRITERIO-2.1.1: SUPER_ADMIN crea STORE_ADMIN para ecommerce válido
 * CRITERIO-2.1.4: STORE_ADMIN crea STORE_USER dentro de su ecommerce
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
    
    // SPEC-005: Optional para SUPER_ADMIN (será null), Obligatorio para STORE_ADMIN/STORE_USER
    UUID ecommerceId
) {
}
