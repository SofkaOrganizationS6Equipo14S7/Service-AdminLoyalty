package com.loyalty.service_admin.presentation.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.loyalty.service_admin.application.dto.ApiKeyListResponse;
import com.loyalty.service_admin.application.dto.ApiKeyResponse;
import com.loyalty.service_admin.application.service.ApiKeyService;
import com.loyalty.service_admin.application.service.AuthService;
import com.loyalty.service_admin.infrastructure.exception.ApiKeyNotFoundException;
import com.loyalty.service_admin.infrastructure.exception.EcommerceNotFoundException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.context.annotation.Import;
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
@Import(TestSecurityConfig.class)
class ApiKeyControllerTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @MockitoBean
    private ApiKeyService apiKeyService;
    
    @MockitoBean
    private AuthService authService;
    
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
    
    /**
     * Additional validation tests to reach ≥ 80% coverage
     */
    
    @Test
    void createApiKey_invalidEcommerceIdFormat_returns500() throws Exception {
        // Note: Spring returns 500 on parameter conversion failure, not 400.
        // In production, this would be handled with a custom @ExceptionHandler.
        String invalidId = "not-a-uuid";
        
        mockMvc.perform(post("/api/v1/ecommerces/{ecommerceId}/api-keys", invalidId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
            .andExpect(status().isInternalServerError());
    }
    
    @Test
    void getApiKeys_invalidEcommerceIdFormat_returns500() throws Exception {
        String invalidId = "invalid-format";
        
        mockMvc.perform(get("/api/v1/ecommerces/{ecommerceId}/api-keys", invalidId)
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isInternalServerError());
    }
    
    @Test
    void deleteApiKey_invalidEcommerceIdFormat_returns500() throws Exception {
        String invalidEcommerceId = "not-a-uuid";
        
        mockMvc.perform(delete("/api/v1/ecommerces/{ecommerceId}/api-keys/{keyId}", 
                invalidEcommerceId, testKeyId)
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isInternalServerError());
    }
    
    @Test
    void deleteApiKey_invalidKeyIdFormat_returns500() throws Exception {
        String invalidKeyId = "invalid-key-id";
        
        mockMvc.perform(delete("/api/v1/ecommerces/{ecommerceId}/api-keys/{keyId}", 
                testEcommerceId, invalidKeyId)
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isInternalServerError());
    }
    
    /**
     * CRITERIO-3.1.2: Verify masking format in response
     */
    @Test
    void createApiKey_responseMaskedKeyFormat() throws Exception {
        // Arrange
        ApiKeyResponse response = new ApiKeyResponse(
            testKeyId.toString(),
            "****abcd",  // Masked format
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
            .andExpect(jsonPath("$.maskedKey").value("****abcd"))
            .andExpect(jsonPath("$.maskedKey").value(org.hamcrest.Matchers.startsWith("****")));
    }
    
    /**
     * Test that response contains all required fields.
     */
    @Test
    void createApiKey_responseHasAllRequiredFields() throws Exception {
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
            .andExpect(jsonPath("$", hasKey("uid")))
            .andExpect(jsonPath("$", hasKey("maskedKey")))
            .andExpect(jsonPath("$", hasKey("ecommerceId")))
            .andExpect(jsonPath("$", hasKey("createdAt")))
            .andExpect(jsonPath("$", hasKey("updatedAt")));
    }
    
    /**
     * Test error response contains message field.
     */
    @Test
    void createApiKey_errorResponseContainsMessage() throws Exception {
        // Arrange
        when(apiKeyService.createApiKey(testEcommerceId))
            .thenThrow(new EcommerceNotFoundException("Ecommerce no encontrado"));
        
        // Act & Assert
        mockMvc.perform(post("/api/v1/ecommerces/{ecommerceId}/api-keys", testEcommerceId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.message").exists());
    }
    
    /**
     * Test multiple deletes (verify service is called correctly).
     */
    @Test
    void deleteApiKey_multipleKeys_eachCallsService() throws Exception {
        // Arrange
        UUID keyId1 = UUID.randomUUID();
        UUID keyId2 = UUID.randomUUID();
        
        doNothing().when(apiKeyService).deleteApiKey(testEcommerceId, keyId1);
        doNothing().when(apiKeyService).deleteApiKey(testEcommerceId, keyId2);
        
        // Act & Assert
        mockMvc.perform(delete("/api/v1/ecommerces/{ecommerceId}/api-keys/{keyId}", 
                testEcommerceId, keyId1))
            .andExpect(status().isNoContent());
        
        mockMvc.perform(delete("/api/v1/ecommerces/{ecommerceId}/api-keys/{keyId}", 
                testEcommerceId, keyId2))
            .andExpect(status().isNoContent());
        
        verify(apiKeyService).deleteApiKey(testEcommerceId, keyId1);
        verify(apiKeyService).deleteApiKey(testEcommerceId, keyId2);
    }
}
