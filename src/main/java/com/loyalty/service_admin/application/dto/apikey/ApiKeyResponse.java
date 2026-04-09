package com.loyalty.service_admin.application.dto.apikey;

import java.time.Instant;
import java.util.UUID;

/**
 * Response al crear una API Key.
 * @param uid ID único de la API Key
 * @param maskedKey Formato ****XXXX (últimos 4 caracteres)
 * @param expiresAt Timestamp de expiración (UTC)
 * @param ecommerceId ID del ecommerce propietario
 * @param createdAt Timestamp de creación (UTC)
 * @param updatedAt Timestamp de actualización (UTC)
 */
public record ApiKeyResponse(
    UUID uid,
    String maskedKey,
    Instant expiresAt,
    UUID ecommerceId,
    Instant createdAt,
    Instant updatedAt
) {
}
