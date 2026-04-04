package com.loyalty.service_admin.application.dto.rules.discount;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * DTO para crear o actualizar la configuración de límite de descuentos.
 */
public record DiscountConfigCreateRequest(
    UUID ecommerceId,
    BigDecimal maxDiscountLimit,
    String currencyCode,
    Boolean allowStacking,
    String roundingRule,
    String capType,
    BigDecimal capValue,
    String capAppliesTo
) {}
