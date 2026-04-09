package com.loyalty.service_admin.application.dto.user;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import java.util.UUID;

public record UserUpdateRequest(
    @Size(min = 3, max = 50, message = "El username debe tener entre 3 y 50 caracteres")
    String username,
    
    @Email(message = "El email debe ser válido")
    String email,
    
    @Size(min = 12, message = "La contraseña debe tener mínimo 12 caracteres")
    String password,
    
    UUID ecommerceId,
    
    Boolean active,
    
    UUID roleId
) {
}
