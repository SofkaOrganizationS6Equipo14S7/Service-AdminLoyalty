package com.loyalty.service_admin.application.dto.discountconfig;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Response de configuración de descuentos.
 * 
 * @param id UUID único de la configuración
 * @param ecommerceId ID del ecommerce propietario
 * @param maxDiscountCap Tope máximo global de descuentos
 * @param currencyCode Código de moneda
 * @param allowStacking Permitir acumulación
 * @param roundingRule Regla de redondeo aplicada
 * @param isActive Si la configuración está activa
 * @param version Número de versión para control de concurrencia
 * @param createdAt Timestamp de creación (UTC)
 * @param updatedAt Timestamp de última actualización (UTC)
 */
public record DiscountConfigResponse(
    UUID id,
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
