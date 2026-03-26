package com.loyalty.service_admin.infrastructure.config;

import com.loyalty.service_admin.application.service.AuthService;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Filtro de autenticación que valida tokens JWT en requests.
 * Se aplica a todas las rutas excepto /api/v1/auth/login y /api/v1/auth/logout.
 * 
 * NOTA: En versiones futuras, considerar usar Spring Security WebSecurityConfigurerAdapter
 * en lugar de un filtro manual.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class AuthenticationFilter implements Filter {
    
    private final AuthService authService;
    
    /**
     * Rutas que NO requieren autenticación.
     * Se pueden agregar más rutas según el diseño de la aplicación.
     */
    private static final String[] PUBLIC_ENDPOINTS = {
        "/api/v1/auth/login",
        "/api/v1/auth/logout",
        "/api/v1/health",
        "/swagger-ui",
        "/v3/api-docs"
    };
    
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;
        
        String requestPath = httpRequest.getRequestURI();
        
        // Verificar si la ruta es pública
        if (isPublicEndpoint(requestPath)) {
            chain.doFilter(request, response);
            return;
        }
        
        // Validar token en rutas protegidas
        String authHeader = httpRequest.getHeader("Authorization");
        
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            log.warn("Intento de acceso sin token válido a: {}", requestPath);
            httpResponse.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            httpResponse.setContentType("application/json");
            httpResponse.getWriter().write("{\"error\":\"Unauthorized\",\"message\":\"Token requerido\"}");
            return;
        }
        
        try {
            String token = authHeader.substring(7);
            // Validar token (esto lanzará UnauthorizedException si es inválido)
            authService.getCurrentUser(token);
            chain.doFilter(request, response);
        } catch (Exception e) {
            log.warn("Token inválido o expirado: {}", e.getMessage());
            httpResponse.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            httpResponse.setContentType("application/json");
            httpResponse.getWriter().write("{\"error\":\"Unauthorized\",\"message\":\"Token no válido o expirado\"}");
        }
    }
    
    /**
     * Verifica si una ruta es pública (no requiere autenticación).
     * 
     * @param path ruta del request
     * @return true si es pública, false si requiere autenticación
     */
    private boolean isPublicEndpoint(String path) {
        for (String publicEndpoint : PUBLIC_ENDPOINTS) {
            if (path.startsWith(publicEndpoint)) {
                return true;
            }
        }
        return false;
    }
    
    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        // No requiere inicialización
    }
    
    @Override
    public void destroy() {
        // No requiere limpieza
    }
}
