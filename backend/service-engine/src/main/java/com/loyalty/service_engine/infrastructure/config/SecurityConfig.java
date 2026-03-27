package com.loyalty.service_engine.infrastructure.config;

import com.loyalty.service_engine.infrastructure.security.ApiKeyAuthenticationFilter;
import com.loyalty.service_engine.infrastructure.security.JwtAuthenticationFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Configuración de seguridad del Engine Service - HYBRID AUTH MODEL.
 * 
 * Registra dos filters de autenticación:
 * 1. JwtAuthenticationFilter - Para Management endpoints (JWT con roles)
 * 2. ApiKeyAuthenticationFilter - Para Transaction endpoints (API Key S2S)
 * 
 * El flujo es:
 * - Primero intenta validar JWT (Management Traffic)
 * - Si JWT falla o no existe, intenta validar API Key (Transaction Traffic)
 * - Si ambos fallan, retorna 401 Unauthorized
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {
    
    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final ApiKeyAuthenticationFilter apiKeyAuthenticationFilter;
    
    public SecurityConfig(
        JwtAuthenticationFilter jwtAuthenticationFilter,
        ApiKeyAuthenticationFilter apiKeyAuthenticationFilter
    ) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        this.apiKeyAuthenticationFilter = apiKeyAuthenticationFilter;
    }
    
    /**
     * Configura la cadena de filtros de seguridad con Dual-Filter Strategy.
     * 
     * Autorización:
        * - Endpoints públicos: health + docs
     * - Management endpoints (/config, /priority): Requieren JWT + ROLE_ADMIN
     * - Transaction endpoints (/calculate): Requieren API Key (autenticado)
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            // Deshabilitar CSRF para APIs stateless
            .csrf(csrf -> csrf.disable())
            
            // Usar sesiones stateless
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            
            // Autorización por endpoint
            .authorizeHttpRequests(auth -> auth
                // Endpoints públicos
                .requestMatchers(HttpMethod.GET, "/api/v1/health").permitAll()
                .requestMatchers(HttpMethod.GET, "/actuator/health", "/actuator/health/**").permitAll()
                .requestMatchers("/swagger-ui/**", "/v3/api-docs/**").permitAll()

                // Management endpoints: Solo JWT + rol ADMIN
                .requestMatchers(HttpMethod.POST, "/api/v1/discount/config").hasRole("ADMIN")
                .requestMatchers(HttpMethod.GET, "/api/v1/discount/config").hasRole("ADMIN")
                .requestMatchers(HttpMethod.POST, "/api/v1/discount/priority").hasRole("ADMIN")
                .requestMatchers(HttpMethod.GET, "/api/v1/discount/priority").hasRole("ADMIN")
                
                // Transaction endpoint: Solo API Key (S2S)
                .requestMatchers(HttpMethod.POST, "/api/v1/discount/calculate").authenticated()
                
                // Todos los demás requieren autenticación
                .anyRequest().authenticated()
            )
            
            // Registrar filters en orden específico:
            // Entre la estructura de Spring Security, los filters se insertan con addFilterBefore
            // en orden inverso, así que registramos el más "interior" primero
            // 1. API Key Filter (más interior, se ejecuta segundo)
            .addFilterBefore(apiKeyAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
            // 2. JWT Filter (más exterior, se ejecuta primero)
            .addFilterBefore(jwtAuthenticationFilter, ApiKeyAuthenticationFilter.class);
        
        return http.build();
    }
}
