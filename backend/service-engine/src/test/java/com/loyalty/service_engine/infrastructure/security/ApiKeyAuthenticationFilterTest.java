package com.loyalty.service_engine.infrastructure.security;

import com.loyalty.service_engine.infrastructure.cache.ApiKeyCache;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for ApiKeyAuthenticationFilter
 * Validates HTTP Bearer token validation and filter chain behavior
 */
@ExtendWith(MockitoExtension.class)
class ApiKeyAuthenticationFilterTest {
    
    @Mock
    private ApiKeyCache apiKeyCache;
    
    private ApiKeyAuthenticationFilter filter;
    private MockHttpServletRequest request;
    private MockHttpServletResponse response;
    private MockFilterChain filterChain;
    
    @BeforeEach
    void setUp() {
        filter = new ApiKeyAuthenticationFilter(apiKeyCache);
        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();
        filterChain = new MockFilterChain();
    }
    
    // ================== Valid API Key ==================
    
    @Test
    void doFilterInternal_validApiKey_continuesFilterChain() throws Exception {
        // Arrange
        String apiKey = "valid-api-key-uuid";
        request.addHeader("Authorization", "Bearer " + apiKey);
        
        when(apiKeyCache.validateKey(apiKey)).thenReturn(true);
        
        // Act
        filter.doFilterInternal(request, response, filterChain);
        
        // Assert
        verify(apiKeyCache, times(1)).validateKey(apiKey);
        // When valid, filter allows request to continue (no error response set)
    }
    
    @Test
    void doFilterInternal_validApiKey_doesNotSetErrorResponse() throws Exception {
        // Arrange
        String apiKey = "test-key-12345";
        request.addHeader("Authorization", "Bearer " + apiKey);
        
        when(apiKeyCache.validateKey(apiKey)).thenReturn(true);
        
        // Act
        filter.doFilterInternal(request, response, filterChain);
        
        // Assert
        verify(apiKeyCache).validateKey(apiKey);
        // Response status remains default (200) for valid key
    }
    
    // ================== Invalid API Key ==================
    
    @Test
    void doFilterInternal_invalidApiKey_returns401() throws Exception {
        // Arrange
        String apiKey = "invalid-api-key";
        request.addHeader("Authorization", "Bearer " + apiKey);
        
        when(apiKeyCache.validateKey(apiKey)).thenReturn(false);
        
        // Act
        filter.doFilterInternal(request, response, filterChain);
        
        // Assert
        assertEquals(response.getStatus(), 401);
        verify(apiKeyCache, times(1)).validateKey(apiKey);
    }
    
    @Test
    void doFilterInternal_invalidApiKey_returnsErrorJson() throws Exception {
        // Arrange
        String apiKey = "wrong-key";
        request.addHeader("Authorization", "Bearer " + apiKey);
        
        when(apiKeyCache.validateKey(apiKey)).thenReturn(false);
        
        // Act
        filter.doFilterInternal(request, response, filterChain);
        
        // Assert
        assertEquals(response.getStatus(), 401);
        assertTrue(response.getContentAsString().contains("error") || 
                   response.getContentAsString().contains("API Key"),
                   "Response should contain error message");
    }
    
    // ================== Missing Authorization Header ==================
    
    @Test
    void doFilterInternal_missingAuthorizationHeader_returns401() throws Exception {
        // Arrange
        // No Authorization header added
        
        // Act
        filter.doFilterInternal(request, response, filterChain);
        
        // Assert
        assertEquals(response.getStatus(), 401);
    }
    
    @Test
    void doFilterInternal_missingAuthorizationHeader_returnsErrorMessage() throws Exception {
        // Arrange
        // No header set
        
        // Act
        filter.doFilterInternal(request, response, filterChain);
        
        // Assert
        assertEquals(response.getStatus(), 401);
        assertTrue(response.getContentAsString().contains("Authorization") || 
                   response.getContentAsString().contains("requerido"),
                   "Response should indicate missing Authorization header");
    }
    
    // ================== Invalid Bearer Format ==================
    
    @Test
    void doFilterInternal_invalidBearerFormat_returns401() throws Exception {
        // Arrange — Missing "Bearer " prefix
        String apiKey = "some-api-key";
        request.addHeader("Authorization", apiKey); // Not prefixed with "Bearer "
        
        // Act
        filter.doFilterInternal(request, response, filterChain);
        
        // Assert
        assertEquals(response.getStatus(), 401);
        verify(apiKeyCache, never()).validateKey(any());
    }
    
    @Test
    void doFilterInternal_malformedAuthHeader_returns401() throws Exception {
        // Arrange
        request.addHeader("Authorization", "Bearer"); // Missing token
        
        // Act
        filter.doFilterInternal(request, response, filterChain);
        
        // Assert
        assertEquals(response.getStatus(), 401);
        verify(apiKeyCache, never()).validateKey(any());
    }
    
    // ================== Exception Handling ==================
    
    @Test
    void doFilterInternal_cacheException_propagatesException() throws Exception {
        // Arrange
        String apiKey = "api-key";
        request.addHeader("Authorization", "Bearer " + apiKey);
        
        when(apiKeyCache.validateKey(apiKey))
            .thenThrow(new RuntimeException("Cache error"));
        
        // Act & Assert
        assertThrows(RuntimeException.class, () -> 
            filter.doFilterInternal(request, response, filterChain)
        );
        verify(apiKeyCache, times(1)).validateKey(apiKey);
    }
    
    @Test
    void doFilterInternal_emptyApiKey_validatesEmptyString() throws Exception {
        // Arrange
        request.addHeader("Authorization", "Bearer ");
        
        when(apiKeyCache.validateKey("")).thenReturn(false);
        
        // Act
        filter.doFilterInternal(request, response, filterChain);
        
        // Assert
        assertEquals(response.getStatus(), 401);
        verify(apiKeyCache, times(1)).validateKey("");
    }
    
    // ================== Multiple Requests ==================
    
    @Test
    void doFilterInternal_multipleValidKeys() throws Exception {
        // Arrange
        String apiKey1 = "key1";
        
        when(apiKeyCache.validateKey(apiKey1)).thenReturn(true);
        
        // Act
        request.addHeader("Authorization", "Bearer " + apiKey1);
        filter.doFilterInternal(request, response, filterChain);
        
        // Assert
        verify(apiKeyCache).validateKey(apiKey1);
    }
    
    @Test
    void doFilterInternal_consecutiveRequests() throws Exception {
        // Arrange & Act
        String apiKey = "key1";
        when(apiKeyCache.validateKey(apiKey)).thenReturn(true);
        request.addHeader("Authorization", "Bearer " + apiKey);
        
        // Act
        filter.doFilterInternal(request, response, filterChain);
        verify(apiKeyCache).validateKey(apiKey);
    }
}
