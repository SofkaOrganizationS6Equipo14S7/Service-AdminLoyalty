package com.loyalty.service_admin.application.service;

import com.loyalty.service_admin.domain.entity.RolePermissionEntity;
import com.loyalty.service_admin.domain.repository.RolePermissionRepository;
import com.loyalty.service_admin.infrastructure.security.SecurityContextHelper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Servicio de validación de permisos granulares.
 * SPEC-004 RN-04: Permisos granulares configurables por STORE_ADMIN.
 * 
 * Responsabilidades:
 * - Obtener permisos de un rol
 * - Validar si un usuario tiene un permiso específico
 * - Cache en memoria (Caffeine) para permisos frecuentes
 * - Inicializar permisos en contexto de seguridad
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class PermissionService {
    
    private final RolePermissionRepository rolePermissionRepository;
    private final SecurityContextHelper securityContextHelper;
    
    /**
     * Obtiene todos los permisos de un rol específico.
     * Cacheable para mejorar performance en validaciones repetidas.
     * 
     * @param role nombre del rol (SUPER_ADMIN, STORE_ADMIN, STORE_USER)
     * @return Set de códigos de permiso (ej {"promotion:read", "promotion:write"})
     */
    @Cacheable(value = "rolePermissions", key = "#role")
    public Set<String> getPermissionsForRole(String role) {
        log.debug("Obteniendo permisos para rol: {}", role);
        return rolePermissionRepository.findPermissionCodesByRole(role);
    }
    
    /**
     * Obtiene todos los permisos del usuario actual autenticado.
     * 
     * @return Set de códigos de permiso del usuario actual
     */
    public Set<String> getCurrentUserPermissions() {
        String currentRole = securityContextHelper.getCurrentUserRole();
        return getPermissionsForRole(currentRole);
    }
    
    /**
     * Valida si el usuario actual tiene un permiso específico.
     * SPEC-004 RN-04: Validar permisos en cada request.
     * 
     * Ejemplo:
     * - hasPermission("promotion:read") → true si usuario puede leer promociones
     * - hasPermission("promotion:write") → true si usuario puede crear/editar promociones
     * 
     * Implementación actual:
     * - Permisos asignados por rol (tabla role_permissions)
     * - TODO: Permitir sobrescrituras a nivel de usuario (tabla user_permissions)
     * 
     * @param permissionCode código del permiso a validar (ej "promotion:read", "user:write")
     * @return true si usuario tiene el permiso, false en caso contrario
     */
    public boolean hasPermission(String permissionCode) {
        Set<String> userPermissions = getCurrentUserPermissions();
        boolean hasPermission = userPermissions.contains(permissionCode);
        
        if (!hasPermission) {
            log.warn("Usuario {} intenta acceder a permiso no autorizado: {}. Permisos actuales: {}", 
                    securityContextHelper.getCurrentUserUid(), permissionCode, userPermissions);
        }
        
        return hasPermission;
    }
    
    /**
     * Valida si el usuario actual tiene TODOS los permisos en la lista.
     * Útil para operaciones que requieren múltiples permisos.
     * 
     * @param permissionCodes lista de códigos de permiso a validar
     * @return true si usuario tiene TODOS los permisos, false si falta alguno
     */
    public boolean hasAllPermissions(String... permissionCodes) {
        Set<String> userPermissions = getCurrentUserPermissions();
        return userPermissions.containsAll(Set.of(permissionCodes));
    }
    
    /**
     * Valida si el usuario actual tiene ALGUNO de los permisos en la lista.
     * Útil para operaciones alternativas.
     * 
     * @param permissionCodes lista de códigos de permiso a validar
     * @return true si usuario tiene ALGUNO de los permisos, false si no tiene ninguno
     */
    public boolean hasAnyPermission(String... permissionCodes) {
        Set<String> userPermissions = getCurrentUserPermissions();
        for (String code : permissionCodes) {
            if (userPermissions.contains(code)) {
                return true;
            }
        }
        return false;
    }
}
