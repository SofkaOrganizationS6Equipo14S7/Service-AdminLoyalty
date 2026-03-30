package com.loyalty.service_admin.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * DTO para actualizar el estado de un ecommerce.
 * 
 * SPEC-001: Registro y Gestión de Ecommerces
 * HU-13.3: Actualizar estado de un ecommerce
 * 
 * Validaciones:
 * - status: obligatorio, debe ser ACTIVE o INACTIVE
 * 
 * CRITERIO-3.1: Desactivación exitosa
 * CRITERIO-3.3: Status inválido
 * CRITERIO-3.4: No se pueden actualizar otros campos
 * 
 * Nota: Solo el campo status es actualizable. Name y slug son inmutables una vez creados.
 */
public record EcommerceUpdateStatusRequest(
    @NotBlank(message = "El campo 'status' es obligatorio")
    @Pattern(
        regexp = "^(ACTIVE|INACTIVE)$",
        message = "El status debe ser 'ACTIVE' o 'INACTIVE'"
    )
    String status
) {}
