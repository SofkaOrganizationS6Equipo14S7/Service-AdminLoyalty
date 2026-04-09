package com.loyalty.service_admin.application.dto.rules.discount;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * DTO que representa la respuesta con la configuración de límite de descuentos.
 * CRITERIO-4.2, CRITERIO-4.4: Incluye version, allowStacking, roundingRule
 */
public record DiscountConfigResponse(
    UUID uid,
    UUID ecommerceId,
    BigDecimal maxDiscountCap,
    String currencyCode,
    Boolean allowStacking,
    String roundingRule,
    Boolean isActive,
    Long version,
    Instant createdAt,
    Instant updatedAt
) {}
