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
 * - DELETE /api/v1/users/{uid} (eliminar usuario)
 * 
 * Implementa SPEC-003: Administración de Ecommerce por STORE_ADMIN
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
