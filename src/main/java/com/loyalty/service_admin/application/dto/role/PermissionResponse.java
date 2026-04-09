package com.loyalty.service_admin.application.dto.role;

import java.time.Instant;
import java.util.UUID;

/**
 * DTO para respuesta de permission
 * CRITERIO-8.3: GET /api/v1/permissions
 * CRITERIO-8.4: GET /api/v1/permissions?module=X
 */
public record PermissionResponse(
        UUID id,
        String code,
        String description,
        String module,
        Instant createdAt,
        Instant updatedAt
) {}
