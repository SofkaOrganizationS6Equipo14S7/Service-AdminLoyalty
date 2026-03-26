package com.loyalty.service_admin.application.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/**
 * Response del cálculo de descuentos respetando prioridad y límite máximo.
 * @param transactionId ID único de la transacción
 * @param originalDiscounts Descuentos originales identificados
 * @param appliedDiscounts Descuentos finales después de aplicar límite
 * @param totalOriginal Suma total de descuentos antes de límite
 * @param totalApplied Suma total de descuentos después de límite
 * @param maxDiscountLimit Límite máximo configurado
 * @param limitExceeded Indica si el total original superaba el límite
 * @param calculatedAt Timestamp del cálculo (UTC)
 */
public record DiscountCalculateResponse(
    String transactionId,
    List<DiscountItem> originalDiscounts,
    List<DiscountItem> appliedDiscounts,
    BigDecimal totalOriginal,
    BigDecimal totalApplied,
    BigDecimal maxDiscountLimit,
    Boolean limitExceeded,
    Instant calculatedAt
) {
    /**
     * Representa un descuento individual.
     * @param discountType Tipo de descuento
     * @param amount Monto del descuento
     */
    public record DiscountItem(
        String discountType,
        BigDecimal amount
    ) {}
}
