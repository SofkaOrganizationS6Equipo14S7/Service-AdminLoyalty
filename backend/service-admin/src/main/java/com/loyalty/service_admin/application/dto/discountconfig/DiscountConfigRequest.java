package com.loyalty.service_admin.application.dto.discountconfig;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Request para crear o actualizar configuración de descuentos.
 * 
 * @param ecommerceId ID del ecommerce
 * @param maxDiscountCap Tope máximo global de descuentos
 * @param currencyCode Código de moneda (default: USD)
 * @param allowStacking Permitir acumulación de descuentos
 * @param roundingRule Regla de redondeo (ROUND_HALF_UP, ROUND_DOWN, ROUND_CEILING)
 */
public record DiscountConfigRequest(
    UUID ecommerceId,
    BigDecimal maxDiscountCap,
    String currencyCode,
    Boolean allowStacking,
    String roundingRule
) {}
