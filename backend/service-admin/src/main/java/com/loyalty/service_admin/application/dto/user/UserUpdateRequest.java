package com.loyalty.service_admin.application.dto.user;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import java.util.UUID;

/**
 * DTO para actualización de usuario.
 * SPEC-002 HU-01.3: Actualizar datos de usuario estándar
 * 
 * Campos opcionales para actualizar:
 * - username: 3-50 caracteres, único globalmente
 * - email: RFC 5322, único globalmente
 * - password: mínimo 12 caracteres
 * - ecommerceId: UUID del ecommerce (solo SUPER_ADMIN)
 * - active: boolean (solo SUPER_ADMIN)
 * 
 * Campos PROHIBIDOS (inmutables):
 * - roleId: NUNCA se puede cambiar via API (CRITERIO-1.4)
 *   Si se intenta enviar, retorna HTTP 400 Bad Request
 * 
 * Restricciones por rol:
 * - STORE_ADMIN editando otros: NO puede cambiar ecommerceId
 * - STORE_ADMIN editando otros: NO puede cambiar active
 * - STORE_USER editando su perfil: NO puede cambiar ecommerceId o active
 */
public record UserUpdateRequest(
    @Size(min = 3, max = 50, message = "El username debe tener entre 3 y 50 caracteres")
    String username,
    
    @Email(message = "El email debe ser válido")
    String email,
    
    @Size(min = 12, message = "La contraseña debe tener mínimo 12 caracteres")
    String password,
    
    // Campos solo SUPER_ADMIN
    UUID ecommerceId,
    
    Boolean active,
    
    // CRITERIO-1.4: Campo que siempre debe ser nulo (es inmutable)
    UUID roleId
) {
}
