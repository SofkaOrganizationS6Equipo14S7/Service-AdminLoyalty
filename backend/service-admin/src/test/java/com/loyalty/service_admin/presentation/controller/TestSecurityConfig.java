package com.loyalty.service_admin.presentation.controller;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Test configuration for @SpringBootTest integration tests.
 * Replaces the production AuthenticationFilter with a permissive version
 * that lets all requests through without JWT validation.
 */
@TestConfiguration
public class TestSecurityConfig {
    
    /**
     * Provides a no-op filter that bypasses JWT validation for testing.
     * The @Primary annotation ensures this bean takes precedence over the
     * @Component AuthenticationFilter in the application.
     */
    @Bean
    @Primary
    public Filter authenticationFilter() {
        return new Filter() {
            @Override
            public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
                    throws IOException, ServletException {
                // Skip authentication - let all requests through for testing
                chain.doFilter(request, response);
            }
            
            @Override
            public void init(FilterConfig config) throws ServletException {
                // No-op
            }
            
            @Override
            public void destroy() {
                // No-op
            }
        };
    }
}



