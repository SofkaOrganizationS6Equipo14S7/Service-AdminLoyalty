package com.loyalty.service_admin.application.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import java.util.UUID;

/**
 * DTO para actualización de usuario.
 * SPEC-003 HU-03.3: Actualizar datos de usuario estándar (email, username)
 * 
 * Campos opcionales:
 * - username: 3-50 caracteres, único globalmente (si se proporciona)
 * - email: RFC 5322, único globalmente (si se proporciona)
 * - password: mínimo 12 caracteres (si se proporciona)
 * 
 * Campos NO EDITABLES:
 * - role: no se puede cambiar el rol de un usuario
 * - ecommerceId: usuario permanece en su ecommerce
 * 
 * CRITERIO-3.3.1: STORE_ADMIN puede actualizar username/email de usuarios de su ecommerce
 */
public record UserUpdateRequest(
    @Size(min = 3, max = 50, message = "El username debe tener entre 3 y 50 caracteres")
    String username,
    
    @Email(message = "El email debe ser válido")
    String email,
    
    @Size(min = 12, message = "La contraseña debe tener mínimo 12 caracteres")
    String password
) {
}
