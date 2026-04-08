package com.loyalty.service_admin.infrastructure.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.UUID;

/**
 * Interceptor que implementa aislamiento multi-inquilino automático.
 * 
 * SPEC-002 CRITERIO-2.2: "El interceptor añade automáticamente WHERE ecommerce_id = ?"
 * 
 * Funcionalidad:
 * 1. En cada request HTTP, extrae el ecommerce_id del JWT (vía UserPrincipal)
 * 2. Almacena en TenantContext para acceso global durante el ciclo de vida del request
 * 3. En afterCompletion(), limpia el ThreadLocal para evitar memory leaks
 * 
 * ROLES:
 * - SUPER_ADMIN: ecommerce_id = NULL → sin restricción de inquilino
 * - USER: ecommerce_id NOT NULL → restricción automática a su ecommerce
 * 
 * NOTA: La lógica de filtrado en la aplicación debe usar TenantContext.getCurrentTenant()
 * o SystemContext.getEcommerceId() para aplicar WHERE ecommerce_id = ? en queries.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TenantInterceptor implements HandlerInterceptor {
    
    private final SecurityContextHelper securityContextHelper;
    
    /**
     * Pre-procesa el request para establecer el contexto del tenant.
     * Llamado por el DispatcherServlet ANTES de invocar el controller.
     *
     * @param request HTTP request
     * @param response HTTP response
     * @param handler controller handler
     * @return true para continuar; false para interrumpir (no usado aquí)
     */
    @Override
    public boolean preHandle(
            HttpServletRequest request,
            HttpServletResponse response,
            Object handler) {
        
        try {
            // Solo procesar si hay usuario autenticado
            if (SecurityContextHolder.getContext().getAuthentication() != null &&
                SecurityContextHolder.getContext().getAuthentication().isAuthenticated()) {
                
                Object principal = SecurityContextHolder.getContext()
                        .getAuthentication()
                        .getPrincipal();

                if (!(principal instanceof UserPrincipal)) {
                    log.debug("Skipping tenant - principal no es UserPrincipal: {}",
                            principal != null ? principal.getClass().getSimpleName() : "null");
                    return true;
                }

                UUID tenantId = securityContextHelper.getCurrentUserEcommerceId();
                
                // Establece en TenantContext para todo el ciclo de vida del request
                if (tenantId != null) {
                    TenantContext.setCurrentTenant(tenantId);
                    log.debug("TenantInterceptor: Tenant set to {}", tenantId);
                } else {
                    // SUPER_ADMIN tiene ecommerce_id = NULL
                    log.debug("TenantInterceptor: SUPER_ADMIN detected (ecommerce_id = NULL)");
                }
            }
        } catch (Exception e) {
            log.warn("TenantInterceptor: Error during preHandle", e);
            // No falla el request, solo log
        }
        
        return true;
    }
    
    /**
     * Post-procesa el request DESPUÉS de que el controller retorna.
     * Limpia el TenantContext para evitar memory leaks en ThreadPool.
     *
     * @param request HTTP request
     * @param response HTTP response
     * @param handler controller handler
     * @param ex exception si ocurrió (nullable)
     */
    @Override
    public void afterCompletion(
            HttpServletRequest request,
            HttpServletResponse response,
            Object handler,
            Exception ex) {
        
        try {
            // Limpia SIEMPRE para evitar contaminar el siguiente request en el pool
            TenantContext.clear();
            log.debug("TenantInterceptor: TenantContext cleared");
        } catch (Exception e) {
            log.warn("TenantInterceptor: Error during afterCompletion cleanup", e);
        }
    }
}
