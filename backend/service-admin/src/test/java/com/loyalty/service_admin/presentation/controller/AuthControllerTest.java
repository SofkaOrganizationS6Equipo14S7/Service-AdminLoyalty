package com.loyalty.service_admin.presentation.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.loyalty.service_admin.application.dto.LoginRequest;
import com.loyalty.service_admin.application.dto.LoginResponse;
import com.loyalty.service_admin.application.dto.UserResponse;
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

import java.time.Instant;

import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
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

    @TestConfiguration
    static class TestConfig {
        @Bean
        @Primary
        public AuthService authService() {
            return mock(AuthService.class);
        }
    }

    private static final String API_BASE = "/api/v1/auth";
    private static final String VALID_TOKEN = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJhZG1pbiIsInVzZXJJZCI6MSwicm9sZSI6IkFETUluIn0.token";

    @BeforeEach
    void setUp() {
        // Setup is done here if needed
    }

    @Test
    @DisplayName("POST /api/v1/auth/login con credenciales válidas retorna 200 con token")
    void testLogin_validCredentials_returns200WithToken() throws Exception {
        // Arrange
        LoginRequest request = new LoginRequest("admin", "admin123");
        LoginResponse response = new LoginResponse(VALID_TOKEN, "Bearer", "admin", "ADMIN");
        when(authService.login(request)).thenReturn(response);

        // Act & Assert
        mockMvc.perform(post(API_BASE + "/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token", notNullValue()))
                .andExpect(jsonPath("$.tipo").value("Bearer"))
                .andExpect(jsonPath("$.username").value("admin"))
                .andExpect(jsonPath("$.role").value("ADMIN"));

        verify(authService, times(1)).login(request);
    }

    @Test
    @DisplayName("POST /api/v1/auth/login sin username retorna 400")
    void testLogin_missingUsername_returns400() throws Exception {
        // Arrange
        String requestBody = "{\"password\": \"admin123\"}";

        // Act & Assert
        mockMvc.perform(post(API_BASE + "/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody)
                        .with(csrf()))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /api/v1/auth/login sin password retorna 400")
    void testLogin_missingPassword_returns400() throws Exception {
        // Arrange
        String requestBody = "{\"username\": \"admin\"}";

        // Act & Assert
        mockMvc.perform(post(API_BASE + "/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody)
                        .with(csrf()))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /api/v1/auth/login con credenciales inválidas retorna 401")
    void testLogin_invalidCredentials_returns401() throws Exception {
        // Arrange
        LoginRequest request = new LoginRequest("admin", "wrongpass");
        when(authService.login(request))
                .thenThrow(new UnauthorizedException("Credenciales inválidas"));

        // Act & Assert
        mockMvc.perform(post(API_BASE + "/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .with(csrf()))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Credenciales inválidas"));

        verify(authService, times(1)).login(request);
    }

    @Test
    @DisplayName("POST /api/v1/auth/logout con token válido retorna 204")
    void testLogout_validToken_returns204() throws Exception {
        // Arrange
        doNothing().when(authService).logout(VALID_TOKEN);

        // Act & Assert
        mockMvc.perform(post(API_BASE + "/logout")
                        .header("Authorization", "Bearer " + VALID_TOKEN)
                        .with(csrf()))
                .andExpect(status().isNoContent());

        verify(authService, times(1)).logout(VALID_TOKEN);
    }

    @Test
    @DisplayName("POST /api/v1/auth/logout sin token retorna 204 (tolerante)")
    void testLogout_noToken_returns204() throws Exception {
        // Arrange
        doNothing().when(authService).logout(null);

        // Act & Assert
        mockMvc.perform(post(API_BASE + "/logout")
                        .with(csrf()))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("GET /api/v1/auth/me con token válido retorna 200 con datos del usuario")
    void testGetCurrentUser_validToken_returns200() throws Exception {
        // Arrange
        UserResponse userResponse = new UserResponse(1L, "admin", "ADMIN", true, Instant.now(), Instant.now());
        when(authService.getCurrentUser(VALID_TOKEN)).thenReturn(userResponse);

        // Act & Assert
        mockMvc.perform(get(API_BASE + "/me")
                        .header("Authorization", "Bearer " + VALID_TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.username").value("admin"))
                .andExpect(jsonPath("$.role").value("ADMIN"))
                .andExpect(jsonPath("$.active").value(true));

        verify(authService, times(1)).getCurrentUser(VALID_TOKEN);
    }

    @Test
    @DisplayName("GET /api/v1/auth/me sin token retorna 401")
    void testGetCurrentUser_noToken_returns401() throws Exception {
        // Act & Assert
        mockMvc.perform(get(API_BASE + "/me"))
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
        mockMvc.perform(get(API_BASE + "/me")
                        .header("Authorization", "Bearer " + invalidToken))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Token no válido o expirado"));

        verify(authService, times(1)).getCurrentUser(invalidToken);
    }
}
