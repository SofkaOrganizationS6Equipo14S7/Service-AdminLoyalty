package com.loyalty.service_admin.application.dto.apikey;

import java.time.Instant;
import java.util.UUID;

/**
 * Response al listar API Keys. Cada item muestra solo datos públicos.
 * @param uid ID único de la API Key
 * @param maskedKey Formato ****XXXX (últimos 4 caracteres)
 * @param expiresAt Timestamp de expiración (UTC)
 * @param isActive Indica si la key está activa
 * @param createdAt Timestamp de creación (UTC)
 * @param updatedAt Timestamp de actualización (UTC)
 */
public record ApiKeyListResponse(
    UUID uid,
    String maskedKey,
    Instant expiresAt,
    Boolean isActive,
    Instant createdAt,
    Instant updatedAt
) {
}
