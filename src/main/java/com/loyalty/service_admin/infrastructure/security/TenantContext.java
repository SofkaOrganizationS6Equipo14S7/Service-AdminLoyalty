package com.loyalty.service_admin.infrastructure.security;

import java.util.UUID;

/**
 * ThreadLocal para aislar el contexto del inquilino (tenant) durante el ciclo de vida
 * de una solicitud HTTP.
 * 
 * SPEC-002 CRITERIO-2.2: El interceptor añade automáticamente WHERE ecommerce_id = ?
 * 
 * El TenantInterceptor configura el tenant_id después de autenticación y antes de que
 * el request se procese en los servicios. Luego, SecurityContextHelper y servicios
 * pueden acceder al tenant actual.
 * 
 * La limpieza automática ocurre en el método finally() de TenantInterceptor.afterCompletion().
 * 
 * USAGE:
 *   TenantContext.setCurrentTenant(UUID.fromString("550e8400-e29b-41d4-a716-446655440000"));
 *   UUID tenantId = TenantContext.getCurrentTenant();
 *   TenantContext.clear(); // en finally block
 */
public class TenantContext {
    
    private static final ThreadLocal<UUID> currentTenant = new ThreadLocal<>();
    
    /**
     * Establece el tenant actual para este thread.
     * Típicamente llamado por TenantInterceptor.preHandle() después de autenticación.
     *
     * @param tenantId UUID del ecommerce (tenant)
     */
    public static void setCurrentTenant(UUID tenantId) {
        if (tenantId != null) {
            currentTenant.set(tenantId);
        }
    }
    
    /**
     * Obtiene el tenant actual para este thread.
     * Si no se ha seteado, retorna null.
     *
     * @return UUID del tenant actual, o null si no se ha seteado
     */
    public static UUID getCurrentTenant() {
        return currentTenant.get();
    }
    
    /**
     * Limpia el tenant del ThreadLocal.
     * DEBE ser llamado en un finally block para evitar memory leaks.
     * 
     * TenantInterceptor.afterCompletion() se encarga de esto automáticamente.
     */
    public static void clear() {
        currentTenant.remove();
    }
}
