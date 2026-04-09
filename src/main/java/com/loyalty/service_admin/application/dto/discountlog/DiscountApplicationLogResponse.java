package com.loyalty.service_admin.application.dto.discountlog;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Response para discount_application_log.
 * Solo lectura - sin endpoints PUT/DELETE (CRITERIO-9.4).
 * 
 * @param id UUID del registro de descuento aplicado
 * @param ecommerceId UUID del ecommerce
 * @param externalOrderId ID de orden externa (referencia a sistema externo)
 * @param originalAmount Monto original antes de descuento
 * @param discountApplied Monto del descuento aplicado
 * @param finalAmount Monto final después del descuento
 * @param appliedRulesDetails Detalles en JSON de qué reglas se aplicaron
 * @param createdAt Timestamp de cuándo se registró
 */
public record DiscountApplicationLogResponse(
    UUID id,
    UUID ecommerceId,
    String externalOrderId,
    BigDecimal originalAmount,
    BigDecimal discountApplied,
    BigDecimal finalAmount,
    String appliedRulesDetails,
    Instant createdAt
) {
}
