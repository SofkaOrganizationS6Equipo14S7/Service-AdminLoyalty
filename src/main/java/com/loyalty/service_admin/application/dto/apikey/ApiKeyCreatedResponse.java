package com.loyalty.service_admin.application.dto.apikey;

import java.time.Instant;
import java.util.UUID;

/**
 * Response al crear una API Key (201 Created).
 * Contiene la clave COMPLETA sin enmascarar (una sola vez).
 * 
 * @param uid ID único de la API Key
 * @param key La clave completa en texto plano (solo en creación)
 * @param expiresAt Timestamp de expiración (UTC)
 * @param ecommerceId ID del ecommerce propietario
 * @param createdAt Timestamp de creación (UTC)
 * @param updatedAt Timestamp de actualización (UTC)
 */
public record ApiKeyCreatedResponse(
    UUID uid,
    String key,
    Instant expiresAt,
    UUID ecommerceId,
    Instant createdAt,
    Instant updatedAt
) {}
