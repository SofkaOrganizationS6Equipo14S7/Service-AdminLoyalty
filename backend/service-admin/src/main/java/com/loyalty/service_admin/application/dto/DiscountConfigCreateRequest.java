package com.loyalty.service_admin.application.dto;

import java.math.BigDecimal;

/**
 * Request para crear o actualizar la configuración de tope máximo de descuentos.
 * @param maxDiscountLimit Límite máximo de descuentos (debe ser > 0)
 * @param currencyCode Código de moneda ISO 4217 (ej. USD)
 */
public record DiscountConfigCreateRequest(
    BigDecimal maxDiscountLimit,
    String currencyCode
) {}
