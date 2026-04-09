package com.loyalty.service_admin.application.dto.role;

import java.time.Instant;
import java.util.UUID;

/**
 * DTO para respuesta de role - lista y detalle simplificado
 * CRITERIO-8.1: GET /api/v1/roles
 * CRITERIO-8.2 (parcial): GET /api/v1/roles/{roleId} - versión sin permisos
 */
public record RoleResponse(
        UUID id,
        String name,
        Boolean isActive,
        Instant createdAt,
        Instant updatedAt
) {}
