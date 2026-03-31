package com.loyalty.service_engine.infrastructure.config;

import com.loyalty.service_engine.infrastructure.security.ApiKeyAuthenticationFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Configuración de seguridad del Engine Service - API KEY ONLY.
 * 
 * El Engine NO valida JWT. Solo valida API Keys para autenticación S2S.
 * 
 * Endpoints:
 * - /api/v1/discount/calculate: Solo API Key (S2S)
 * - /health: Público
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {
    
    private final ApiKeyAuthenticationFilter apiKeyAuthenticationFilter;
    
    public SecurityConfig(ApiKeyAuthenticationFilter apiKeyAuthenticationFilter) {
        this.apiKeyAuthenticationFilter = apiKeyAuthenticationFilter;
    }
    
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(HttpMethod.GET, "/api/v1/health").permitAll()
                .requestMatchers(HttpMethod.GET, "/actuator/health", "/actuator/health/**").permitAll()
                .requestMatchers("/swagger-ui/**", "/v3/api-docs/**").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/v1/discount/calculate").authenticated()
                .requestMatchers(HttpMethod.POST, "/api/v1/discounts/calculate").authenticated()
                .anyRequest().denyAll()
            )
            .addFilterBefore(apiKeyAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
        
        return http.build();
    }
}
