package com.loyalty.service_admin.application.dto.rules.discount;

import java.math.BigDecimal;

/**
 * DTO para crear o actualizar la configuración de límite de descuentos.
 */
public record DiscountConfigCreateRequest(
    String ecommerceId,
    String maxDiscountLimit,
    String currencyCode
) {}
