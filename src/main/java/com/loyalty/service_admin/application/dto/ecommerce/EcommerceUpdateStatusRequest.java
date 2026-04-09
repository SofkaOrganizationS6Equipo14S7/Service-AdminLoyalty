package com.loyalty.service_admin.application.dto.ecommerce;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record EcommerceUpdateStatusRequest(
    @NotBlank(message = "El campo 'status' es obligatorio")
    @Pattern(
        regexp = "^(ACTIVE|INACTIVE)$",
        message = "El status debe ser 'ACTIVE' o 'INACTIVE'"
    )
    String status
) {}
