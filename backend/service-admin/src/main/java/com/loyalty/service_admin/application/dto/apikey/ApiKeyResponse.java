package com.loyalty.service_admin.application.dto.apikey;

import java.time.Instant;

/**
 * Response al crear una API Key.
 * @param uid ID único de la API Key
 * @param maskedKey Formato ****XXXX (últimos 4 caracteres)
 * @param ecommerceId ID del ecommerce propietario
 * @param createdAt Timestamp de creación (UTC)
 * @param updatedAt Timestamp de actualización (UTC)
 */
public record ApiKeyResponse(
    String uid,
    String maskedKey,
    String ecommerceId,
    Instant createdAt,
    Instant updatedAt
) {
}
