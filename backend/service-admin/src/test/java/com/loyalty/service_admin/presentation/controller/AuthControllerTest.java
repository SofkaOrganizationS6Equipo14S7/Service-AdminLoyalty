package com.loyalty.service_admin.presentation.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.loyalty.service_admin.application.dto.LoginRequest;
import com.loyalty.service_admin.application.dto.LoginResponse;
import com.loyalty.service_admin.application.service.AuthService;
import com.loyalty.service_admin.infrastructure.exception.UnauthorizedException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AuthController.class)
@DisplayName("AuthController Tests")
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AuthService authService;

    @Autowired
    private ObjectMapper objectMapper;

    /**
     * TestConfiguration para proporcionar un AuthService mockeado en el contexto de Spring
     * Usamos mock() de Mockito directamente en lugar de @MockBean que no existe en Spring Boot 3.5.x
     */
    @TestConfiguration
    static class TestConfig {
        @Bean
        @Primary
        public AuthService authService() {
            return mock(AuthService.class);
        }
    }

    private LoginRequest validLoginRequest;
    private LoginResponse loginResponse;

    @BeforeEach
    void setUp() {
        validLoginRequest = new LoginRequest("admin", "admin123");
        loginResponse = new LoginResponse(
                "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9",
                "Bearer",
                "admin",
                "ADMIN"
        );
    }

    @Test
    @DisplayName("POST /api/v1/auth/login con credenciales válidas retorna 200 con token")
    void testLogin_validCredentials_returns200WithToken() throws Exception {
        // Arrange
        when(authService.login(any(LoginRequest.class))).thenReturn(loginResponse);

        // Act & Assert
        mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validLoginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.tipo").value("Bearer"))
                .andExpect(jsonPath("$.username").value("admin"))
                .andExpect(jsonPath("$.role").value("ADMIN"));
    }

    @Test
    @DisplayName("POST /api/v1/auth/login sin username retorna 400")
    void testLogin_missingUsername_returns400() throws Exception {
        // Arrange
        String requestBody = "{\"password\": \"admin123\"}";

        // Act & Assert
        mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /api/v1/auth/login sin password retorna 400")
    void testLogin_missingPassword_returns400() throws Exception {
        // Arrange
        String requestBody = "{\"username\": \"admin\"}";

        // Act & Assert
        mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /api/v1/auth/login con credenciales inválidas retorna 401")
    void testLogin_invalidCredentials_returns401() throws Exception {
        // Arrange
        when(authService.login(any(LoginRequest.class)))
                .thenThrow(new UnauthorizedException("Credenciales inválidas"));

        // Act & Assert
        mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validLoginRequest)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Credenciales inválidas"));
    }

    @Test
    @DisplayName("POST /api/v1/auth/logout con token válido retorna 204")
    void testLogout_validToken_returns204() throws Exception {
        // Arrange
        String token = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9";

        // Act & Assert
        mockMvc.perform(post("/api/v1/auth/logout")
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("POST /api/v1/auth/logout sin token retorna 204 (tolerante)")
    void testLogout_noToken_returns204() throws Exception {
        // Act & Assert
        mockMvc.perform(post("/api/v1/auth/logout"))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("GET /api/v1/auth/me con token válido retorna 200 con datos del usuario")
    void testGetCurrentUser_validToken_returns200() throws Exception {
        // Arrange
        String token = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9";
        com.loyalty.service_admin.application.dto.UserResponse userResponse =
                new com.loyalty.service_admin.application.dto.UserResponse(
                        1L, "admin", "ADMIN", true, java.time.Instant.now(), java.time.Instant.now()
                );
        when(authService.getCurrentUser(token)).thenReturn(userResponse);

        // Act & Assert
        mockMvc.perform(get("/api/v1/auth/me")
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("admin"))
                .andExpect(jsonPath("$.role").value("ADMIN"));
    }

    @Test
    @DisplayName("GET /api/v1/auth/me sin token retorna 401")
    void testGetCurrentUser_noToken_returns401() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/api/v1/auth/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("GET /api/v1/auth/me con token inválido retorna 401")
    void testGetCurrentUser_invalidToken_returns401() throws Exception {
        // Arrange
        String invalidToken = "invalid-token";
        when(authService.getCurrentUser(invalidToken))
                .thenThrow(new UnauthorizedException("Token no válido o expirado"));

        // Act & Assert
        mockMvc.perform(get("/api/v1/auth/me")
                .header("Authorization", "Bearer " + invalidToken))
                .andExpect(status().isUnauthorized());
    }
}
