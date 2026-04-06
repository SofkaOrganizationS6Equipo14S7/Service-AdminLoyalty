package com.loyalty.service_admin.presentation.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.loyalty.service_admin.application.dto.ecommerce.EcommerceCreateRequest;
import com.loyalty.service_admin.application.dto.ecommerce.EcommerceResponse;
import com.loyalty.service_admin.application.dto.ecommerce.EcommerceUpdateStatusRequest;
import com.loyalty.service_admin.application.port.in.EcommerceUseCase;
import com.loyalty.service_admin.infrastructure.exception.AuthorizationException;
import com.loyalty.service_admin.infrastructure.exception.ConflictException;
import com.loyalty.service_admin.infrastructure.exception.EcommerceNotFoundException;
import com.loyalty.service_admin.infrastructure.security.SecurityContextHelper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit Tests para EcommerceController usando MockMvc
 * 
 * SPEC-015: Pruebas unitarias de endpoints REST
 * Patrón: Arrange-Act-Assert con MockMvc
 * Aislamiento: Se mockea EcommerceUseCase (puerto de entrada)
 */
@WebMvcTest(controllers = {EcommerceController.class})
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("EcommerceController Unit Tests (TDD)")
class EcommerceControllerTest {
    
    @TestConfiguration
    static class TestConfig {
        @Bean
        public com.loyalty.service_admin.infrastructure.security.SecurityContextHelper securityContextHelper() {
            return Mockito.mock(com.loyalty.service_admin.infrastructure.security.SecurityContextHelper.class);
        }

        @Bean
        public com.loyalty.service_admin.infrastructure.security.TenantInterceptor tenantInterceptor(
                com.loyalty.service_admin.infrastructure.security.SecurityContextHelper helper) {
            return new com.loyalty.service_admin.infrastructure.security.TenantInterceptor(helper);
        }
    }
    
    @Autowired
    private MockMvc mockMvc;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    @MockBean
    private EcommerceUseCase ecommerceUseCase;
    
    @MockBean
    private SecurityContextHelper securityContextHelper;
    
    private UUID testEcommerceId;
    private String testSlug;
    private String testName;
    
    @BeforeEach
    void setUp() {
        testEcommerceId = UUID.randomUUID();
        testSlug = "test-store";
        testName = "Test Store";
    }
    
    // ==================== POST /api/v1/ecommerces ====================
    
    @Test
    @WithMockUser(roles = "SUPER_ADMIN")
    void testCreateEcommerce_returns201() throws Exception {
        // Arrange
        EcommerceCreateRequest request = new EcommerceCreateRequest(testName, testSlug);
        EcommerceResponse response = new EcommerceResponse(
                testEcommerceId,
                testName,
                testSlug,
                "ACTIVE",
                Instant.now(),
                Instant.now()
        );
        
        when(ecommerceUseCase.createEcommerce(any(EcommerceCreateRequest.class))).thenReturn(response);
        
        // Act & Assert
        mockMvc.perform(post("/api/v1/ecommerces")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.uid", is(testEcommerceId.toString())))
                .andExpect(jsonPath("$.name", is(testName)))
                .andExpect(jsonPath("$.slug", is(testSlug)))
                .andExpect(jsonPath("$.status", is("ACTIVE")));
        
        verify(ecommerceUseCase, times(1)).createEcommerce(any(EcommerceCreateRequest.class));
    }
    
    @Test
    @WithMockUser(roles = "SUPER_ADMIN")
    void testCreateEcommerce_duplicateSlug_returns409() throws Exception {
        // Arrange
        EcommerceCreateRequest request = new EcommerceCreateRequest(testName, testSlug);
        
        when(ecommerceUseCase.createEcommerce(any(EcommerceCreateRequest.class)))
                .thenThrow(new ConflictException("El slug 'test-store' ya está en uso."));
        
        // Act & Assert
        mockMvc.perform(post("/api/v1/ecommerces")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message", containsString("ya está en uso")));
        
        verify(ecommerceUseCase, times(1)).createEcommerce(any(EcommerceCreateRequest.class));
    }
    
    @Test
    @WithMockUser(roles = "SUPER_ADMIN")
    void testCreateEcommerce_invalidInput_returns400() throws Exception {
        // Arrange: request con campos vacíos/nulos
        String invalidJson = "{\"name\": \"\", \"slug\": null}";
        
        // Act & Assert
        mockMvc.perform(post("/api/v1/ecommerces")
                .contentType(MediaType.APPLICATION_JSON)
                .content(invalidJson))
                .andExpect(status().isBadRequest());
        
        verify(ecommerceUseCase, never()).createEcommerce(any());
    }
    
    // ==================== GET /api/v1/ecommerces ====================
    
    @Test
    @WithMockUser(roles = "SUPER_ADMIN")
    void testListEcommerces_returns200() throws Exception {
        // Arrange
        EcommerceResponse response1 = new EcommerceResponse(
                UUID.randomUUID(),
                "Store 1",
                "store-1",
                "ACTIVE",
                Instant.now(),
                Instant.now()
        );
        
        EcommerceResponse response2 = new EcommerceResponse(
                UUID.randomUUID(),
                "Store 2",
                "store-2",
                "INACTIVE",
                Instant.now(),
                Instant.now()
        );
        
        when(ecommerceUseCase.listEcommerces(any(), anyInt(), anyInt()))
                .thenReturn(new PageImpl<>(List.of(response1, response2)));
        
        // Act & Assert
        mockMvc.perform(get("/api/v1/ecommerces")
                .param("page", "0")
                .param("size", "50"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(2)))
                .andExpect(jsonPath("$.content[0].name", is("Store 1")))
                .andExpect(jsonPath("$.content[1].name", is("Store 2")));
        
        verify(ecommerceUseCase, times(1)).listEcommerces(null, 0, 50);
    }
    
    @Test
    @WithMockUser(roles = "SUPER_ADMIN")
    void testListEcommerces_filterByStatus() throws Exception {
        // Arrange
        EcommerceResponse response = new EcommerceResponse(
                testEcommerceId,
                testName,
                testSlug,
                "ACTIVE",
                Instant.now(),
                Instant.now()
        );
        
        when(ecommerceUseCase.listEcommerces("ACTIVE", 0, 50))
                .thenReturn(new PageImpl<>(List.of(response)));
        
        // Act & Assert
        mockMvc.perform(get("/api/v1/ecommerces")
                .param("status", "ACTIVE")
                .param("page", "0")
                .param("size", "50"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].status", is("ACTIVE")));
        
        verify(ecommerceUseCase, times(1)).listEcommerces("ACTIVE", 0, 50);
    }
    
    @Test
    @WithMockUser(roles = "SUPER_ADMIN")
    void testListEcommerces_invalidStatus_returns400() throws Exception {
        // Arrange
        when(ecommerceUseCase.listEcommerces("INVALID", 0, 50))
                .thenThrow(new com.loyalty.service_admin.infrastructure.exception.BadRequestException("Status inválido"));
        
        // Act & Assert
        mockMvc.perform(get("/api/v1/ecommerces")
                .param("status", "INVALID"))
                .andExpect(status().isBadRequest());
        
        verify(ecommerceUseCase, times(1)).listEcommerces("INVALID", 0, 50);
    }
    
    
    // ==================== GET /api/v1/ecommerces/{uid} ====================
    // NOTA: Tests de autenticación/autorización en GET/PUT requieren tests de integración con @SpringBootTest
    // Aquí solo probamos la lógica del controller (MockMvc sin seguridad activa)
    
    // ==================== PUT /api/v1/ecommerces/{uid}/status ====================
    // NOTA: Tests de autenticación/autorización en GET/PUT requieren tests de integración con @SpringBootTest
    // Aquí solo probamos la lógica del controller (MockMvc sin seguridad activa)
}


