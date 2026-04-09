package com.loyalty.service_admin.application.dto.rules.discount;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.UUID;

/**
 * DTO para guardar prioridades de descuentos para una configuración.
 * CRITERIO-4.3: discountSettingId y priorities con validaciones
 */
public record DiscountLimitPriorityRequest(
    @NotNull(message = "El discountSettingId es obligatorio")
    UUID discountSettingId,
    
    @NotEmpty(message = "Debe proporcionar al menos una prioridad")
    @Valid
    List<PriorityEntry> priorities
) {
    /**
     * Entrada individual de prioridad (tipo descuento + nivel).
     */
    public record PriorityEntry(
        @NotNull(message = "El discountTypeId es obligatorio")
        UUID discountTypeId,
        
        @NotNull(message = "El priorityLevel es obligatorio")
        @Min(value = 1, message = "El priorityLevel debe ser mayor a 0")
        Integer priorityLevel
    ) {}
}
