package com.loyalty.service_admin.infrastructure.security;

import com.loyalty.service_admin.application.service.AuthService;
import com.loyalty.service_admin.infrastructure.config.AuthenticationFilter;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

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
     * Registra el filter en el servlet filter chain.
     * 
     * @param authenticationFilter bean del filter
     * @return FilterRegistrationBean configurado
     */
    @Bean
    public FilterRegistrationBean<AuthenticationFilter> filterRegistrationBean(
            AuthenticationFilter authenticationFilter) {
        FilterRegistrationBean<AuthenticationFilter> registrationBean = new FilterRegistrationBean<>();
        registrationBean.setFilter(authenticationFilter);
        registrationBean.addUrlPatterns("/*");
        registrationBean.setOrder(1);
        return registrationBean;
    }
}
