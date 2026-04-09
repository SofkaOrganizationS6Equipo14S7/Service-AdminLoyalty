package com.loyalty.service_admin.presentation.controller;

import com.loyalty.service_admin.application.dto.role.*;
import com.loyalty.service_admin.application.service.RoleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Controller para gestión de roles y permisos
 * HU-08: Roles y Permisos
 * 
 * Endpoints:
 * - CRITERIO-8.1: GET /api/v1/roles - Listar roles
 * - CRITERIO-8.2: GET /api/v1/roles/{roleId} - Detalle de rol con permisos
 * - CRITERIO-8.3: GET /api/v1/permissions - Listar permisos
 * - CRITERIO-8.4: GET /api/v1/permissions?module=X - Filtrar permisos por módulo
 * - CRITERIO-8.5: POST /api/v1/roles/{roleId}/permissions - Asignar permisos
 */
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Slf4j
public class RoleController {

    private final RoleService roleService;

    /**
     * CRITERIO-8.1: GET /api/v1/roles - Listar todos los roles
     * Retorna lista de roles disponibles en el sistema
     */
    @GetMapping("/roles")
    public ResponseEntity<List<RoleResponse>> listRoles() {
        log.info("GET /api/v1/roles - Listing all roles");
        List<RoleResponse> roles = roleService.listAllRoles();
        return ResponseEntity.ok(roles);
    }

    /**
     * CRITERIO-8.2: GET /api/v1/roles/{roleId} - Obtener rol con sus permisos
     * Retorna detalle del rol incluyendo lista de permisos asignados
     */
    @GetMapping("/roles/{roleId}")
    public ResponseEntity<RoleWithPermissionsResponse> getRoleDetails(
            @PathVariable UUID roleId
    ) {
        log.info("GET /api/v1/roles/{} - Getting role details", roleId);
        RoleWithPermissionsResponse response = roleService.getRoleWithPermissions(roleId);
        return ResponseEntity.ok(response);
    }

    /**
     * CRITERIO-8.3: GET /api/v1/permissions - Listar todos los permisos
     * Retorna lista completa de permisos disponibles en el sistema
     */
    @GetMapping("/permissions")
    public ResponseEntity<List<PermissionResponse>> listPermissions(
            @RequestParam(required = false) String module
    ) {
        if (module != null && !module.isEmpty()) {
            // CRITERIO-8.4: Filtrar por módulo si se proporciona parámetro
            log.info("GET /api/v1/permissions - Listing permissions by module: {}", module);
            List<PermissionResponse> permissions = roleService.listPermissionsByModule(module);
            return ResponseEntity.ok(permissions);
        }
        
        // CRITERIO-8.3: Listar todos los permisos
        log.info("GET /api/v1/permissions - Listing all permissions");
        List<PermissionResponse> permissions = roleService.listAllPermissions();
        return ResponseEntity.ok(permissions);
    }

    /**
     * CRITERIO-8.5: POST /api/v1/roles/{roleId}/permissions - Asignar permisos a un rol
     * Reemplaza completamente los permisos del rol especificado
     * Request body: { "permissionIds": ["uuid1", "uuid2", ...] }
     */
    @PostMapping("/roles/{roleId}/permissions")
    public ResponseEntity<RoleWithPermissionsResponse> assignPermissionsToRole(
            @PathVariable UUID roleId,
            @Valid @RequestBody RolePermissionsAssignRequest request
    ) {
        log.info("POST /api/v1/roles/{}/permissions - Assigning {} permissions", 
            roleId, request.permissionIds().size());
        
        RoleWithPermissionsResponse response = roleService.assignPermissionsToRole(roleId, request);
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }
}
