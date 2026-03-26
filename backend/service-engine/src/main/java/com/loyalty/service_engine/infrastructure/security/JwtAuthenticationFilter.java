package com.loyalty.service_engine.infrastructure.security;

import com.loyalty.service_engine.infrastructure.util.JwtUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Filter para validar JWT (JSON Web Tokens) en requests Management.
 * 
 * Flujo:
 * 1. Extrae el token del header "Authorization: Bearer {token}"
 * 2. Valida que el token sea válido
 * 3. Extrae username y role del token
 * 4. Crea un Authentication con el username y authorities
 * 5. Lo setea en el SecurityContext de Spring
 * 6. Si falla, continúa al siguiente filter (ApiKeyAuthenticationFilter)
 * 
 * Este filter se registra ANTES de ApiKeyAuthenticationFilter en SecurityConfig.
 */
@Component
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    
    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";
    
    private final JwtUtil jwtUtil;
    
    public JwtAuthenticationFilter(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }
    
    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                   HttpServletResponse response,
                                   FilterChain filterChain) throws ServletException, IOException {
        try {
            // Extraer header Authorization
            String authHeader = request.getHeader(AUTHORIZATION_HEADER);
            
            // Si hay header con Bearer prefix, intentar validar como JWT
            if (authHeader != null && authHeader.startsWith(BEARER_PREFIX)) {
                String token = authHeader.substring(BEARER_PREFIX.length());
                
                // Validar que el token sea un JWT válido
                if (jwtUtil.validateToken(token)) {
                    String username = jwtUtil.extractUsername(token);
                    String role = jwtUtil.extractRole(token);
                    
                    // Crear authorities con el role extraído del JWT
                    List<GrantedAuthority> authorities = new ArrayList<>();
                    authorities.add(new SimpleGrantedAuthority(role));
                    
                    // Crear Authentication con username y role
                    Authentication authentication = new UsernamePasswordAuthenticationToken(
                        username,      // principal (userId extraído del JWT)
                        null,          // credentials (no necesario)
                        authorities    // authorities (ROLE_ADMIN, ROLE_USER, etc.)
                    );
                    
                    // Setear en el SecurityContext
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                    log.debug("JWT validated for user: {} with role: {}", username, role);
                } else {
                    // JWT inválido → continuar al siguiente filter (ApiKeyAuthenticationFilter)
                    log.debug("JWT validation failed, continuing to next filter");
                }
            }
        } catch (Exception e) {
            // Error durante validación de JWT → continuar al siguiente filter
            log.debug("Exception validating JWT: {}", e.getMessage());
        }
        
        // Continuar con la cadena de filtros
        filterChain.doFilter(request, response);
    }
}
