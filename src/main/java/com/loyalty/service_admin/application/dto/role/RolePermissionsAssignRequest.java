package com.loyalty.service_admin.application.dto.role;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.UUID;

/**
 * DTO para request de asignar permisos a un role
 * CRITERIO-8.5: POST /api/v1/roles/{roleId}/permissions
 * Asigna una lista de permissionIds al role especificado
 */
public record RolePermissionsAssignRequest(
        @NotNull(message = "Permission IDs are required")
        @NotEmpty(message = "At least one permission must be provided")
        List<UUID> permissionIds
) {}
