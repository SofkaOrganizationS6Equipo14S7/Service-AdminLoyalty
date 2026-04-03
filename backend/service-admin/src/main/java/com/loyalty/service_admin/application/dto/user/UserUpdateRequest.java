package com.loyalty.service_admin.application.dto.user;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import java.util.UUID;

/**
 * DTO para actualización de usuario.
 * SPEC-002 HU-03.3: Actualizar datos de usuario estándar
 * SPEC-005 HU-02.3: Modificación tenant-scoped y propia contraseña
 * 
 * Campos opcionales para STORE_ADMIN:
 * - username: 3-50 caracteres, único globalmente
 * - email: RFC 5322, único globalmente
 * - password: mínimo 12 caracteres
 * 
 * Campos opcionales SOLO para SUPER_ADMIN:
 * - ecommerceId: UUID del ecommerce (valida que existe)
 * - active: boolean para activar/desactivar usuario
 * 
 * Restricciones:
 * - role: NUNCA se puede cambiar via API
 * - STORE_USER editando su perfil: NO puede cambiar ecommerceId
 * - STORE_USER editando su perfil: NO puede cambiar active
 * 
 * CRITERIO-2.3.1: SUPER_ADMIN actualiza credenciales
 * CRITERIO-2.3.1B: STORE_ADMIN actualiza usuarios de su ecommerce
 * CRITERIO-2.3.1C: Usuario cambia su propia contraseña
 */
public record UserUpdateRequest(
    @Size(min = 3, max = 50, message = "El username debe tener entre 3 y 50 caracteres")
    String username,
    
    @Email(message = "El email debe ser válido")
    String email,
    
    @Size(min = 12, message = "La contraseña debe tener mínimo 12 caracteres")
    String password,
    
    // SPEC-005: Nuevos campos para SUPER_ADMIN
    UUID ecommerceId,
    
    Boolean active
) {
}
