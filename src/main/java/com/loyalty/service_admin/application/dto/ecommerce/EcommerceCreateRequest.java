package com.loyalty.service_admin.application.dto.ecommerce;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Pattern;

public record EcommerceCreateRequest(
    @NotBlank(message = "El campo 'name' es obligatorio")
    @Size(min = 3, max = 100, message = "El nombre debe tener entre 3 y 100 caracteres")
    String name,
    
    @NotBlank(message = "El campo 'slug' es obligatorio")
    @Size(min = 3, max = 254, message = "El slug debe tener entre 3 y 254 caracteres")
    @Pattern(
        regexp = "^[a-z0-9]([a-z0-9-]{0,252}[a-z0-9])?$",
        message = "El slug debe contener solo minúsculas, números y guiones. Debe empezar y terminar con letra o número"
    )
    String slug
) {}
