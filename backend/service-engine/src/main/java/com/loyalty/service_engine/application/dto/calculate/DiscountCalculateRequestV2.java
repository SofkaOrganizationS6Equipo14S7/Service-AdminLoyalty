package com.loyalty.service_engine.application.dto.calculate;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * Request para calcular descuentos.
 * Incluye métricas del cliente para clasificación automática de fidelidad.
 * 
 * @param ecommerceId Tenant identifier
 * @param subtotal Subtotal de la transacción
 * @param total Total después de impuestos
 * @param beforeTax Monto antes de impuestos
 * @param afterTax Monto después de impuestos
 * @param totalSpent Gasto total acumulado del cliente (para clasificación)
 * @param orderCount Cantidad de órdenes completadas del cliente (para clasificación)
 * @param loyaltyPoints Puntos de fidelidad acumulados del cliente (para clasificación, opcional)
 * @param discounts Lista de descuentos candidatos a aplicar
 */
public record DiscountCalculateRequestV2(
        @NotNull UUID ecommerceId,
        @NotNull @DecimalMin(value = "0.0", inclusive = true) BigDecimal subtotal,
        @NotNull @DecimalMin(value = "0.0", inclusive = true) BigDecimal total,
        @NotNull @DecimalMin(value = "0.0", inclusive = true) BigDecimal beforeTax,
        @NotNull @DecimalMin(value = "0.0", inclusive = true) BigDecimal afterTax,
        @NotNull @DecimalMin(value = "0.0", inclusive = true) BigDecimal totalSpent,
        @NotNull @Min(value = 0) Integer orderCount,
        @Min(value = 0) Integer loyaltyPoints,
        @NotNull @Size(min = 1) List<@Valid DiscountCandidate> discounts
) {
    public record DiscountCandidate(
            @NotBlank String type,
            @NotNull @DecimalMin(value = "0.0", inclusive = false) BigDecimal amount
    ) {
    }
}
