package com.loyalty.service_admin.application.dto;

import java.time.Instant;

/**
 * DTO para respuesta con datos del usuario.
 * Utilizado en endpoint GET /api/v1/auth/me.
 */
public record UserResponse(
    Long id,
    String username,
    String role,
    Boolean active,
    Instant createdAt,
    Instant updatedAt
) {
}
