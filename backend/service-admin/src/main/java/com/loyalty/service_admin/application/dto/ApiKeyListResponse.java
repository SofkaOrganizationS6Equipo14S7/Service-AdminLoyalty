package com.loyalty.service_admin.application.dto;

import java.time.Instant;

/**
 * Response al listar API Keys. Cada item muestra solo datos públicos.
 * @param uid ID único de la API Key
 * @param maskedKey Formato ****XXXX (últimos 4 caracteres)
 * @param createdAt Timestamp de creación (UTC)
 * @param updatedAt Timestamp de actualización (UTC)
 */
public record ApiKeyListResponse(
    String uid,
    String maskedKey,
    Instant createdAt,
    Instant updatedAt
) {
}
