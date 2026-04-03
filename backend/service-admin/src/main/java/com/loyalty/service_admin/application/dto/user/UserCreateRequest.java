package com.loyalty.service_admin.application.dto.user;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.UUID;

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
    
    @NotNull(message = "El roleId es obligatorio")
    UUID roleId,
    
    UUID ecommerceId
) {
}
