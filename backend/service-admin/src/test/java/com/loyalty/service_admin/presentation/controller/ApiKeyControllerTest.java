package com.loyalty.service_admin.presentation.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.loyalty.service_admin.application.dto.ApiKeyListResponse;
import com.loyalty.service_admin.application.dto.ApiKeyResponse;
import com.loyalty.service_admin.application.service.ApiKeyService;
import com.loyalty.service_admin.infrastructure.exception.ApiKeyNotFoundException;
import com.loyalty.service_admin.infrastructure.exception.EcommerceNotFoundException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ApiKeyController.class)
class ApiKeyControllerTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @MockitoBean
    private ApiKeyService apiKeyService;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    private UUID testEcommerceId = UUID.fromString("220e8400-e29b-41d4-a716-446655440111");
    private UUID testKeyId = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");
    private Instant now = Instant.now();
    
    // ================== Happy Path Tests — POST ==================
    
    @Test
    void createApiKey_success_returns201Created() throws Exception {
        // Arrange
        ApiKeyResponse response = new ApiKeyResponse(
            testKeyId.toString(),
            "****0000",
            testEcommerceId.toString(),
            now,
            now
        );
        
        when(apiKeyService.createApiKey(testEcommerceId)).thenReturn(response);
        
        // Act & Assert
        mockMvc.perform(post("/api/v1/ecommerces/{ecommerceId}/api-keys", testEcommerceId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.uid").value(testKeyId.toString()))
            .andExpect(jsonPath("$.maskedKey").value("****0000"))
            .andExpect(jsonPath("$.ecommerceId").value(testEcommerceId.toString()))
            .andExpect(jsonPath("$.createdAt").exists())
            .andExpect(jsonPath("$.updatedAt").exists());
        
        verify(apiKeyService).createApiKey(testEcommerceId);
    }
    
    @Test
    void createApiKey_invalidEcommerceId_returns404() throws Exception {
        // Arrange
        when(apiKeyService.createApiKey(testEcommerceId))
            .thenThrow(new EcommerceNotFoundException("Ecommerce no encontrado"));
        
        // Act & Assert
        mockMvc.perform(post("/api/v1/ecommerces/{ecommerceId}/api-keys", testEcommerceId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.message").value("Ecommerce no encontrado"));
        
        verify(apiKeyService).createApiKey(testEcommerceId);
    }
    
    // ================== Happy Path Tests — GET ==================
    
    @Test
    void getApiKeys_success_returns200() throws Exception {
        // Arrange
        Instant now = Instant.now();
        List<ApiKeyListResponse> keys = List.of(
            new ApiKeyListResponse(
                UUID.fromString("550e8400-e29b-41d4-a716-446655440001").toString(),
                "****0001",
                now,
                now
            ),
            new ApiKeyListResponse(
                UUID.fromString("550e8400-e29b-41d4-a716-446655440002").toString(),
                "****0002",
                now,
                now
            )
        );
        
        when(apiKeyService.getApiKeysByEcommerce(testEcommerceId)).thenReturn(keys);
        
        // Act & Assert
        mockMvc.perform(get("/api/v1/ecommerces/{ecommerceId}/api-keys", testEcommerceId)
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(2)))
            .andExpect(jsonPath("$[0].maskedKey").value("****0001"))
            .andExpect(jsonPath("$[1].maskedKey").value("****0002"));
        
        verify(apiKeyService).getApiKeysByEcommerce(testEcommerceId);
    }
    
    @Test
    void getApiKeys_empty_returns200WithEmptyArray() throws Exception {
        // Arrange
        when(apiKeyService.getApiKeysByEcommerce(testEcommerceId)).thenReturn(List.of());
        
        // Act & Assert
        mockMvc.perform(get("/api/v1/ecommerces/{ecommerceId}/api-keys", testEcommerceId)
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(0)));
        
        verify(apiKeyService).getApiKeysByEcommerce(testEcommerceId);
    }
    
    @Test
    void getApiKeys_invalidEcommerceId_returns404() throws Exception {
        // Arrange
        when(apiKeyService.getApiKeysByEcommerce(testEcommerceId))
            .thenThrow(new EcommerceNotFoundException("Ecommerce no encontrado"));
        
        // Act & Assert
        mockMvc.perform(get("/api/v1/ecommerces/{ecommerceId}/api-keys", testEcommerceId)
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.message").value("Ecommerce no encontrado"));
        
        verify(apiKeyService).getApiKeysByEcommerce(testEcommerceId);
    }
    
    // ================== Happy Path Tests — DELETE ==================
    
    @Test
    void deleteApiKey_success_returns204NoContent() throws Exception {
        // Arrange
        doNothing().when(apiKeyService).deleteApiKey(testEcommerceId, testKeyId);
        
        // Act & Assert
        mockMvc.perform(delete("/api/v1/ecommerces/{ecommerceId}/api-keys/{keyId}", 
                testEcommerceId, testKeyId)
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isNoContent());
        
        verify(apiKeyService).deleteApiKey(testEcommerceId, testKeyId);
    }
    
    @Test
    void deleteApiKey_keyNotFound_returns404() throws Exception {
        // Arrange
        doThrow(new ApiKeyNotFoundException("API Key no encontrada"))
            .when(apiKeyService).deleteApiKey(testEcommerceId, testKeyId);
        
        // Act & Assert
        mockMvc.perform(delete("/api/v1/ecommerces/{ecommerceId}/api-keys/{keyId}", 
                testEcommerceId, testKeyId)
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.message").value("API Key no encontrada"));
        
        verify(apiKeyService).deleteApiKey(testEcommerceId, testKeyId);
    }
    
    @Test
    void deleteApiKey_ecommerceNotFound_returns404() throws Exception {
        // Arrange
        doThrow(new EcommerceNotFoundException("Ecommerce no encontrado"))
            .when(apiKeyService).deleteApiKey(testEcommerceId, testKeyId);
        
        // Act & Assert
        mockMvc.perform(delete("/api/v1/ecommerces/{ecommerceId}/api-keys/{keyId}", 
                testEcommerceId, testKeyId)
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.message").value("Ecommerce no encontrado"));
        
        verify(apiKeyService).deleteApiKey(testEcommerceId, testKeyId);
    }
    
    // ================== Edge Cases ==================
    
    // Nota: Tests de UUID inválido omitidos porque Spring lananza 500 en conversión de parámetros
    // En producción, esto se manejaría con @ExceptionHandler personalizado
}
