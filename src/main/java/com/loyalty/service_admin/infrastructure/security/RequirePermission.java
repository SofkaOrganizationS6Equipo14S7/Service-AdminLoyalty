package com.loyalty.service_admin.infrastructure.security;

import java.lang.annotation.*;

/**
 * Anotación para validar permisos granulares en métodos de controlador.
 * SPEC-004 RN-04: Permisos granulares configurables por STORE_ADMIN.
 * 
 * Uso:
 * @RequirePermission("promotion:read")
 * public ResponseEntity<List<PromoResponse>> getPromotions() { ... }
 * 
 * @RequirePermission(value = {"promotion:write", "promotion:delete"}, requireAll = false)
 * public ResponseEntity<Void> deletePromotion(@PathVariable UUID id) { ... }
 * 
 * Implementada por PermissionAspect que intercepta la ejecución y valida permisos.
 * Si el usuario no tiene el permiso:
 * - Retorna 403 Forbidden con mensaje descriptivo
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RequirePermission {
    
    /**
     * Código(s) de permiso(s) requerido(s).
     * Ejemplo: "promotion:read", "user:write"
     * 
     * @return array de códigos de permiso
     */
    String[] value() default {};
    
    /**
     * Si true, el usuario debe tener TODOS los permisos (AND logic).
     * Si false, el usuario solo necesita tener ALGUNO de los permisos (OR logic).
     * 
     * Default: true (TODOS los permisos)
     * 
     * @return true si require todos, false si requiere alguno
     */
    boolean requireAll() default true;
}
