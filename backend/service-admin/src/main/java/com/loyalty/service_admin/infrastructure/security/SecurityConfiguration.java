package com.loyalty.service_admin.infrastructure.security;

import com.loyalty.service_admin.application.service.AuthService;
import com.loyalty.service_admin.infrastructure.config.AuthenticationFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Configuración de seguridad para autenticación.
 * 
 * Responsabilidades:
 * - BCryptPasswordEncoder con strength óptimo (10-12)
 * - Registro de AuthenticationFilter como @Bean (SPEC-002 lección aprendida)
 * - Configuración de Spring Security (future)
 * 
 * NOTA IMPORTANTE: AuthenticationFilter se registra como @Bean (NO @Component)
 * para evitar conflictos en el ciclo de vida de Spring y en tests del controller.
 * Ver SPEC-002 punto 2.
 * 
 * Cumple con SPEC-001 v1.1 RN-11:
 * - BCrypt strength = 11 (recomendado, balance entre seguridad y performance)
 * - Strength < 10: vulnerable a fuerza bruta
 * - Strength > 12: DoS involuntario bajo carga por excesivo uso de CPU
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfiguration {
    
    /**
     * Bean para BCryptPasswordEncoder con strength = 11.
     * 
     * @return passwordEncoder configurado
     */
    @Bean
    public BCryptPasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(11);
    }
    
    /**
     * Registra el AuthenticationFilter como @Bean.
     * 
     * LECCIÓN APRENDIDA (SPEC-002): Registrar como @Bean en configuración
     * en lugar de @Component evita:
     * - Conflictos de ciclo de vida de Spring
     * - Problemas en tests de controladores
     * - Múltiples instancias del filter
     * 
     * @param authService inyección automática
     * @param jwtProvider inyección automática
     * @return bean del filter
     */
    @Bean
    public AuthenticationFilter authenticationFilter(AuthService authService, JwtProvider jwtProvider) {
        return new AuthenticationFilter(authService, jwtProvider);
    }
    
    /**
     * Configura la seguridad HTTP del servicio admin.
     * 
     * Endpoints públicos:
     * - POST /api/v1/auth/login
     * - POST /api/v1/auth/logout
     * - GET /api/v1/health
     * - Swagger/OpenAPI docs
     * 
     * El resto de endpoints requieren JWT válido.
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, AuthenticationFilter authenticationFilter) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(HttpMethod.POST, "/api/v1/auth/login").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/v1/auth/logout").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/v1/health").permitAll()
                .requestMatchers("/swagger-ui/**", "/v3/api-docs/**").permitAll()
                .anyRequest().authenticated()
            )
            .httpBasic(httpBasic -> httpBasic.disable())
            .formLogin(formLogin -> formLogin.disable())
            .addFilterBefore(authenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
