package com.loyalty.service_admin.application.dto;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Response para la configuración de tope máximo de descuentos.
 * @param uid ID único de la configuración
 * @param maxDiscountLimit Límite máximo de descuentos en moneda base
 * @param currencyCode Código de moneda ISO 4217 (ej. USD)
 * @param isActive Flag indicando si la configuración está vigente
 * @param createdAt Timestamp de creación (UTC)
 * @param updatedAt Timestamp de última actualización (UTC)
 */
public record DiscountConfigResponse(
    String uid,
    BigDecimal maxDiscountLimit,
    String currencyCode,
    Boolean isActive,
    Instant createdAt,
    Instant updatedAt
) {}
