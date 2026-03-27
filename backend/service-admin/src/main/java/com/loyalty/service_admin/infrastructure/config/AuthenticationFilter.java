package com.loyalty.service_admin.infrastructure.config;

import com.loyalty.service_admin.application.service.AuthService;
import com.loyalty.service_admin.infrastructure.security.JwtProvider;
import com.loyalty.service_admin.infrastructure.security.UserPrincipal;
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
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.io.IOException;
import java.util.UUID;

/**
 * Filtro de autenticación que valida tokens JWT en requests.
 * Se aplica a todas las rutas excepto /api/v1/auth/login y /api/v1/auth/logout.
 * 
 * IMPORTANTE: Este filter DEBE registrarse como @Bean en SecurityConfiguration (NO @Component).
 * Ver SPEC-002 punto 2: Lección aprendida de tests anteriores evita conflictos de ciclo de vida.
 * 
 * Responsabilidades:
 * - Validar JWT en header Authorization: Bearer {token}
 * - Extraer claims: username, role, ecommerce_id (SPEC-002)
 * - Crear UserPrincipal con ecommerce_id para aislamiento multi-tenant
 * - Guardar en SecurityContextHolder para acceso desde servicios
 */
@RequiredArgsConstructor
@Slf4j
public class AuthenticationFilter implements Filter {
    
    private final AuthService authService;
    private final JwtProvider jwtProvider;
    
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
            
            // Validar token
            if (!jwtProvider.validateToken(token)) {
                log.warn("Token inválido o expirado en request: {}", requestPath);
                httpResponse.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                httpResponse.setContentType("application/json");
                httpResponse.getWriter().write("{\"error\":\"Unauthorized\",\"message\":\"Token no válido o expirado\"}");
                return;
            }
            
            // Extraer claims del JWT
            String username = jwtProvider.getUsernameFromToken(token);
            Long userId = jwtProvider.getUserIdFromToken(token);
            String role = jwtProvider.getRoleFromToken(token);
            UUID ecommerceId = jwtProvider.getEcommerceIdFromToken(token); // null si super admin
            
            // Crear UserPrincipal con ecommerce_id
            UUID uid = UUID.nameUUIDFromBytes(("user-" + userId).getBytes());
            UserPrincipal principal = new UserPrincipal(
                    uid,
                    username,
                    "", // password no se necesita aquí
                    role,
                    ecommerceId,
                    true
            );
            
            // Guardar en SecurityContextHolder
            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
            SecurityContextHolder.getContext().setAuthentication(authentication);
            
            log.debug("Usuario autenticado: {} con ecommerce: {}", username, ecommerceId);
            chain.doFilter(request, response);
            
        } catch (Exception e) {
            log.warn("Error extrayendo claims del token: {}", e.getMessage());
            httpResponse.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            httpResponse.setContentType("application/json");
            httpResponse.getWriter().write("{\"error\":\"Unauthorized\",\"message\":\"Token inválido\"}");
        }
    }
    
    /**
     * Verifica si la ruta es un endpoint público.
     */
    private boolean isPublicEndpoint(String requestPath) {
        for (String publicEndpoint : PUBLIC_ENDPOINTS) {
            if (requestPath.startsWith(publicEndpoint)) {
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
