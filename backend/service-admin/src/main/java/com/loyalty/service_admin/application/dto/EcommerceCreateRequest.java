package com.loyalty.service_admin.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Pattern;

/**
 * DTO para crear un nuevo ecommerce.
 * 
 * SPEC-001: Registro y Gestión de Ecommerces
 * HU-13.1: Registro exitoso de un nuevo ecommerce
 * 
 * Validaciones:
 * - name: obligatorio, 3-100 caracteres
 * - slug: obligatorio, único, solo minúsculas/números/guiones
 * 
 * CRITERIO-1.1: Registro exitoso con datos válidos
 * CRITERIO-1.3: Rechazo por datos incompletos
 */
public record EcommerceCreateRequest(
    @NotBlank(message = "El campo 'name' es obligatorio")
    @Size(min = 3, max = 100, message = "El nombre debe tener entre 3 y 100 caracteres")
    String name,
    
    @NotBlank(message = "El campo 'slug' es obligatorio")
    @Size(min = 3, max = 255, message = "El slug debe tener entre 3 y 255 caracteres")
    @Pattern(
        regexp = "^[a-z0-9]([a-z0-9-]{0,253}[a-z0-9])?$",
        message = "El slug debe contener solo minúsculas, números y guiones. Debe empezar y terminar con letra o número"
    )
    String slug
) {}
