package com.loyalty.service_admin.application.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/**
 * Request para calcular descuentos aplicando prioridad y límite máximo.
 * @param transactionId ID único de la transacción
 * @param discounts Lista de descuentos identificados para la transacción
 */
public record DiscountCalculateRequest(
    String transactionId,
    List<DiscountItem> discounts
) {
    /**
     * Representa un descuento individual a aplicar.
     * @param discountType Tipo de descuento
     * @param amount Monto del descuento
     */
    public record DiscountItem(
        String discountType,
        BigDecimal amount
    ) {}
}
