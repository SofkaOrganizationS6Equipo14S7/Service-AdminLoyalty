package com.loyalty.service_admin.application.dto.rules.discount;

import java.time.OffsetDateTime;

/**
 * DTO que representa la respuesta con la configuración de límite de descuentos.
 */
public record DiscountConfigResponse(
    String uid,
    String ecommerceId,
    String maxDiscountLimit,
    String currencyCode,
    Boolean isActive,
    OffsetDateTime createdAt,
    OffsetDateTime updatedAt
) {}
