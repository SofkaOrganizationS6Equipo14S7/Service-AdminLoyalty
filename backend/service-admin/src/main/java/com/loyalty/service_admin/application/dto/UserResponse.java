package com.loyalty.service_admin.application.dto;

import java.time.Instant;
import java.util.UUID;

/**
 * DTO para respuesta con datos del usuario.
 * Utilizado en endpoints:
 * - GET /api/v1/auth/me (usuario actual)
 * - GET /api/v1/users (listar usuarios)
 * - GET /api/v1/users/{uid} (obtener usuario)
 * - POST /api/v1/users (crear usuario)
 * - PUT /api/v1/users/{uid} (actualizar usuario)
 * 
 * Implementa SPEC-002: Gestión de usuarios por ecommerce
 */
public record UserResponse(
    UUID uid,
    String username,
    String role,
    String email,
    UUID ecommerceId,
    Boolean active,
    Instant createdAt,
    Instant updatedAt
) {
}
