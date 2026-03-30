package com.loyalty.service_admin.infrastructure.security;

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Utilidad para acceder al contexto de seguridad del usuario actual.
 * Permite que servicios y repositorios consulten ecommerce_id sin volver a parsear JWT.
 * 
 * Implementa SPEC-002 punto 6: método helper reutilizable
 */
@Component
public class SecurityContextHelper {
    
    /**
     * Obtiene el ecommerce_id del usuario actual desde SecurityContextHolder.
     * Retorna null si el usuario es SUPER_ADMIN (sin restricción).
     * 
     * @return UUID del ecommerce, o null si super admin
     * @throws IllegalStateException si no hay usuario autenticado
     */
    public UUID getCurrentUserEcommerceId() {
        UserPrincipal principal = getCurrentUserPrincipal();
        return principal.getEcommerceId();
    }
    
    /**
     * Obtiene el UserPrincipal del usuario actual.
     * 
     * @return UserPrincipal del usuario autenticado
     * @throws IllegalStateException si no hay usuario autenticado
     */
    public UserPrincipal getCurrentUserPrincipal() {
        Object principal = SecurityContextHolder.getContext()
                .getAuthentication()
                .getPrincipal();
        
        if (!(principal instanceof UserPrincipal)) {
            throw new IllegalStateException("Principal no es UserPrincipal");
        }
        
        return (UserPrincipal) principal;
    }
    
    /**
     * Obtiene el UID (UUID) del usuario actual.
     * Alias de getCurrentUserId() para compatibilidad con SPEC-002.
     * 
     * @return UUID del usuario
     */
    public UUID getCurrentUserUid() {
        return getCurrentUserPrincipal().getUid();
    }
    
    /**
     * Obtiene el rol (role) del usuario actual.
     * 
     * @return String del rol (e.g., "SUPER_ADMIN", "USER")
     */
    public String getCurrentUserRole() {
        return getCurrentUserPrincipal().getRole();
    }
    
    /**
     * Verdadero si el usuario actual tiene restricción de ecommerce.
     * Falso si es SUPER_ADMIN.
     */
    public boolean isCurrentUserEcommerceScoped() {
        return getCurrentUserPrincipal().isEcommerceScoped();
    }
    
    /**
     * Verdadero si el usuario actual es SUPER_ADMIN.
     */
    public boolean isCurrentUserSuperAdmin() {
        return "SUPER_ADMIN".equals(getCurrentUserPrincipal().getRole());
    }
    
    /**
     * Valida si el usuario actual puede actuar sobre otro usuario.
     * 
     * Implementa SPEC-005 RN-05: Gestión de usuarios por contexto de autorización
     * 
     * Reglas de autorización multitenant:
     * 1. SUPER_ADMIN: puede actuar sobre cualquier usuario en cualquier ecommerce
     * 2. STORE_ADMIN: puede actuar sobre usuarios SOLO de su ecommerce_id
     * 3. STORE_USER: puede actuar sobre su propio perfil (uid == target uid)
     * 4. STORE_USER sobre otro usuario: NEGADO (false)
     * 
     * @param targetUserEcommerceId ecommerce_id del usuario objetivo (puede ser null para SUPER_ADMIN)
     * @param targetUserUid uid del usuario objetivo
     * @return true si el usuario actual tiene permiso para actuar sobre el objetivo
     */
    public boolean canActOnUser(UUID targetUserEcommerceId, UUID targetUserUid) {
        UserPrincipal currentPrincipal = getCurrentUserPrincipal();
        String currentRole = currentPrincipal.getRole();
        UUID currentUserUid = currentPrincipal.getUid();
        UUID currentUserEcommerceId = currentPrincipal.getEcommerceId();
        
        // SUPER_ADMIN: acceso global
        if ("SUPER_ADMIN".equals(currentRole)) {
            return true;
        }
        
        // STORE_ADMIN: acceso limitado a su ecommerce
        if ("STORE_ADMIN".equals(currentRole)) {
            return currentUserEcommerceId != null && 
                   currentUserEcommerceId.equals(targetUserEcommerceId);
        }
        
        // STORE_USER: acceso solo a su propio perfil
        if ("STORE_USER".equals(currentRole)) {
            return currentUserUid.equals(targetUserUid);
        }
        
        // Rol desconocido: NEGADO
        return false;
    }
}
