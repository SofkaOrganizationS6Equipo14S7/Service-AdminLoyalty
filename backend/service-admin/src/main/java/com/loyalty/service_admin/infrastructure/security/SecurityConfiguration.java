package com.loyalty.service_admin.infrastructure.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

/**
 * Configuración de seguridad para autenticación.
 * 
 * Responsabilidades:
 * - BCryptPasswordEncoder con strength óptimo (10-12)
 * - Configuración de Spring Security (future)
 * 
 * Cumple con SPEC-001 v1.1 RN-11:
 * - BCrypt strength = 11 (recomendado, balance entre seguridad y performance)
 * - Strength < 10: vulnerable a fuerza bruta
 * - Strength > 12: DoS involuntario bajo carga por excesivo uso de CPU
 */
@Configuration
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
}
