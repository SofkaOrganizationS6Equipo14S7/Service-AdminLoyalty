package com.loyalty.service_admin.infrastructure.config;

import com.loyalty.service_admin.infrastructure.security.TenantInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Configuración de Spring Web MVC para LOYALTY service-admin.
 * 
 * Registra los interceptores necesarios para:
 * - Multi-tenant isolation (TenantInterceptor)
 * - Context propagation through ThreadLocal
 */
@Configuration
@RequiredArgsConstructor
public class WebMvcConfig implements WebMvcConfigurer {
    
    private final TenantInterceptor tenantInterceptor;
    
    /**
     * Registra los interceptores en el DispatcherServlet.
     * TenantInterceptor se ejecutará DESPUÉS de Spring Security (por orden de registro).
     * 
     * El flujo es:
     * 1. Spring Security autenticación + autorización
     * 2. TenantInterceptor.preHandle() → establece TenantContext
     * 3. Controller + Servicios (pueden usar TenantContext.getCurrentTenant())
     * 4. TenantInterceptor.afterCompletion() → limpia TenantContext
     * 
     * SPEC-002 CRITERIO-2.2: "El interceptor añade automáticamente WHERE ecommerce_id = ?"
     */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(tenantInterceptor)
                .addPathPatterns("/api/**")
                .excludePathPatterns(
                    "/api/auth/login",  // login no necesita tenant
                    "/api/auth/register" // registro no necesita tenant
                );
    }
}
