package com.loyalty.service_admin.presentation.controller;

import com.loyalty.service_admin.infrastructure.config.AuthenticationFilter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

import java.io.IOException;

/**
 * Test configuration that disables authentication for controller tests
 * by providing a mock AuthenticationFilter that passes all requests through.
 */
@TestConfiguration
public class TestSecurityConfig {
    
    /**
     * Provides a no-op AuthenticationFilter for testing.
     * The bean name matches the auto-generated name of the @Component.
     */
    @Bean(name = "authenticationFilter")
    @Primary
    public AuthenticationFilter testAuthenticationFilter() {
        return new AuthenticationFilter(null) {
            @Override
            public void init(FilterConfig filterConfig) throws ServletException {
                // No-op
            }

            @Override
            public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
                    throws IOException, ServletException {
                // Skip authentication - pass all requests through
                chain.doFilter(request, response);
            }

            @Override
            public void destroy() {
                // No-op
            }
        };
    }
}



