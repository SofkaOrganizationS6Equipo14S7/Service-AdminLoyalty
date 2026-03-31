package com.loyalty.service_engine.application.dto;

import java.time.OffsetDateTime;

/**
 * DTO que representa la respuesta con la configuración de límite de descuentos (réplica).
 * IDÉNTICA a la versión en Service-Admin para mantener consistencia de API.
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
