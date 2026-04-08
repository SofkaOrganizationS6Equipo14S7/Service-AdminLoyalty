package com.loyalty.service_admin.application.dto.role;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * DTO para respuesta de role con permisos anidados
 * CRITERIO-8.2: GET /api/v1/roles/{roleId} - retorna role con lista de permisos
 */
public record RoleWithPermissionsResponse(
        UUID id,
        String name,
        Boolean isActive,
        List<PermissionResponse> permissions,
        Instant createdAt,
        Instant updatedAt
) {}
