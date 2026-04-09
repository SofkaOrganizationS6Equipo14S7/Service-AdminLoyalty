package com.loyalty.service_admin.application.service;

import com.loyalty.service_admin.application.dto.role.*;
import com.loyalty.service_admin.domain.entity.PermissionEntity;
import com.loyalty.service_admin.domain.entity.RoleEntity;
import com.loyalty.service_admin.domain.entity.RolePermissionEntity;
import com.loyalty.service_admin.domain.repository.PermissionRepository;
import com.loyalty.service_admin.domain.repository.RolePermissionRepository;
import com.loyalty.service_admin.domain.repository.RoleRepository;
import com.loyalty.service_admin.infrastructure.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Servicio para gestión de roles y permisos
 * HU-08: Roles y Permisos
 * - CRITERIO-8.1: Listar todos los roles
 * - CRITERIO-8.2: Obtener rol con sus permisos
 * - CRITERIO-8.3: Listar todos los permisos
 * - CRITERIO-8.4: Filtrar permisos por módulo
 * - CRITERIO-8.5: Asignar permisos a un rol
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RoleService {

    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;
    private final RolePermissionRepository rolePermissionRepository;

    /**
     * CRITERIO-8.1: GET /api/v1/roles - Listar todos los roles
     */
    @Transactional(readOnly = true)
    public List<RoleResponse> listAllRoles() {
        log.info("Listing all roles");
        return roleRepository.findAll().stream()
                .map(this::toRoleResponse)
                .collect(Collectors.toList());
    }

    /**
     * CRITERIO-8.2: GET /api/v1/roles/{roleId} - Obtener rol con sus permisos
     */
    @Transactional(readOnly = true)
    public RoleWithPermissionsResponse getRoleWithPermissions(UUID roleId) {
        log.info("Getting role with permissions: roleId={}", roleId);
        
        RoleEntity role = roleRepository.findById(roleId)
                .orElseThrow(() -> new ResourceNotFoundException("Role not found with id: " + roleId));
        
        // Obtener los permisos del rol
        List<RolePermissionEntity> rolePermissions = rolePermissionRepository.findByRoleId(roleId);
        List<PermissionResponse> permissions = rolePermissions.stream()
                .map(rp -> toPermissionResponse(rp.getPermission()))
                .collect(Collectors.toList());
        
        return new RoleWithPermissionsResponse(
                role.getId(),
                role.getName(),
                role.getIsActive(),
                permissions,
                role.getCreatedAt(),
                role.getUpdatedAt()
        );
    }

    /**
     * CRITERIO-8.3: GET /api/v1/permissions - Listar todos los permisos
     */
    @Transactional(readOnly = true)
    public List<PermissionResponse> listAllPermissions() {
        log.info("Listing all permissions");
        return permissionRepository.findAll().stream()
                .map(this::toPermissionResponse)
                .collect(Collectors.toList());
    }

    /**
     * CRITERIO-8.4: GET /api/v1/permissions?module=X - Filtrar permisos por módulo
     */
    @Transactional(readOnly = true)
    public List<PermissionResponse> listPermissionsByModule(String module) {
        log.info("Listing permissions by module: module={}", module);
        return permissionRepository.findAll().stream()
                .filter(permission -> permission.getModule().equalsIgnoreCase(module))
                .map(this::toPermissionResponse)
                .collect(Collectors.toList());
    }

    /**
     * CRITERIO-8.5: POST /api/v1/roles/{roleId}/permissions - Asignar permisos a un rol
     * Reemplaza completamente los permisos del rol (acción atómica)
     */
    @Transactional
    public RoleWithPermissionsResponse assignPermissionsToRole(UUID roleId, RolePermissionsAssignRequest request) {
        log.info("Assigning permissions to role: roleId={}, permissionCount={}", roleId, request.permissionIds().size());
        
        // Validar que el rol existe
        RoleEntity role = roleRepository.findById(roleId)
                .orElseThrow(() -> new ResourceNotFoundException("Role not found with id: " + roleId));
        
        // Validar que todos los permissionIds existen
        Set<UUID> requestedPermissionIds = request.permissionIds().stream()
                .collect(Collectors.toSet());
        
        List<PermissionEntity> permissions = permissionRepository.findAllById(requestedPermissionIds);
        
        if (permissions.size() != requestedPermissionIds.size()) {
            log.warn("Some permissions not found. Requested: {}, Found: {}", 
                requestedPermissionIds.size(), permissions.size());
            throw new ResourceNotFoundException(
                "One or more permissions not found with ids: " + requestedPermissionIds
            );
        }
        
        // Eliminar los permisos existentes del rol (operación atómica)
        List<RolePermissionEntity> existingPermissions = rolePermissionRepository.findByRoleId(roleId);
        rolePermissionRepository.deleteAll(existingPermissions);
        log.debug("Removed {} existing permissions from role", existingPermissions.size());
        
        // Asignar los nuevos permisos
        List<RolePermissionEntity> newRolePermissions = permissions.stream()
                .map(permission -> {
                    RolePermissionEntity rp = new RolePermissionEntity();
                    rp.setRole(role);
                    rp.setPermission(permission);
                    return rp;
                })
                .collect(Collectors.toList());
        
        rolePermissionRepository.saveAll(newRolePermissions);
        log.info("Successfully assigned {} permissions to role {}", permissions.size(), roleId);
        
        // Retornar el rol actualizado con los permisos
        List<PermissionResponse> permissionResponses = permissions.stream()
                .map(this::toPermissionResponse)
                .collect(Collectors.toList());
        
        return new RoleWithPermissionsResponse(
                role.getId(),
                role.getName(),
                role.getIsActive(),
                permissionResponses,
                role.getCreatedAt(),
                role.getUpdatedAt()
        );
    }

    // ==================== MAPPERS ====================

    /**
     * Mapea RoleEntity a RoleResponse
     */
    private RoleResponse toRoleResponse(RoleEntity role) {
        return new RoleResponse(
                role.getId(),
                role.getName(),
                role.getIsActive(),
                role.getCreatedAt(),
                role.getUpdatedAt()
        );
    }

    /**
     * Mapea PermissionEntity a PermissionResponse
     */
    private PermissionResponse toPermissionResponse(PermissionEntity permission) {
        return new PermissionResponse(
                permission.getId(),
                permission.getCode(),
                permission.getDescription(),
                permission.getModule(),
                permission.getCreatedAt(),
                permission.getUpdatedAt()
        );
    }
}
