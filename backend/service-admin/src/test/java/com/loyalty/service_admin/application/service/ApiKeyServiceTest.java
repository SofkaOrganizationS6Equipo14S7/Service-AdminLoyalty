package com.loyalty.service_admin.application.service;

import com.loyalty.service_admin.application.dto.ApiKeyListResponse;
import com.loyalty.service_admin.application.dto.ApiKeyResponse;
import com.loyalty.service_admin.domain.entity.ApiKeyEntity;
import com.loyalty.service_admin.domain.repository.ApiKeyRepository;
import com.loyalty.service_admin.infrastructure.exception.ApiKeyNotFoundException;
import com.loyalty.service_admin.infrastructure.exception.EcommerceNotFoundException;
import com.loyalty.service_admin.infrastructure.rabbitmq.ApiKeyEventPublisher;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ApiKeyServiceTest {
    
    @Mock
    private ApiKeyRepository apiKeyRepository;
    
    @Mock
    private ApiKeyEventPublisher apiKeyEventPublisher;
    
    @Mock
    private EcommerceService ecommerceService;
    
    @InjectMocks
    private ApiKeyService apiKeyService;
    
    private UUID testEcommerceId = UUID.fromString("220e8400-e29b-41d4-a716-446655440111");
    private UUID testKeyId = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");
    
    /**
     * Helper para crear un ApiKeyEntity con valores de prueba usando reflection.
     */
    private ApiKeyEntity createApiKeyEntity(UUID id, String keyString, UUID ecommerceId) throws Exception {
        ApiKeyEntity entity = new ApiKeyEntity();
        
        // Usar reflection para setear los campos si los setters no están disponibles
        setField(entity, "id", id);
        setField(entity, "keyString", keyString);
        setField(entity, "ecommerceId", ecommerceId);
        setField(entity, "createdAt", Instant.now());
        setField(entity, "updatedAt", Instant.now());
        
        return entity;
    }
    
    /**
     * Helper para setear campos via reflection.
     */
    private void setField(Object obj, String fieldName, Object value) throws Exception {
        Field field = ApiKeyEntity.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(obj, value);
    }
    
    // ================== Happy Path Tests ==================
    
    @Test
    void createApiKey_success() throws Exception {
        // Arrange
        doNothing().when(ecommerceService).validateEcommerceExists(testEcommerceId);
        
        ApiKeyEntity savedEntity = createApiKeyEntity(testKeyId, "550e8400-e29b-41d4-a716-446655440000", testEcommerceId);
        
        when(apiKeyRepository.save(any(ApiKeyEntity.class))).thenReturn(savedEntity);
        
        // Act
        ApiKeyResponse response = apiKeyService.createApiKey(testEcommerceId);
        
        // Assert
        assertNotNull(response);
        assertEquals(testKeyId.toString(), response.uid());
        assertEquals("****0000", response.maskedKey());
        assertEquals(testEcommerceId.toString(), response.ecommerceId());
        
        // Verify interactions
        verify(ecommerceService).validateEcommerceExists(testEcommerceId);
        verify(apiKeyRepository).save(any(ApiKeyEntity.class));
        verify(apiKeyEventPublisher).publishApiKeyCreated(any());
    }
    
    @Test
    void createApiKey_verifyMaskingFormat() throws Exception {
        // Arrange
        doNothing().when(ecommerceService).validateEcommerceExists(testEcommerceId);
        
        ApiKeyEntity savedEntity = createApiKeyEntity(testKeyId, "a1b2c3d4-e5f6-47g8-h9i0-j1k2l3m4n5o6", testEcommerceId);
        
        when(apiKeyRepository.save(any(ApiKeyEntity.class))).thenReturn(savedEntity);
        
        // Act
        ApiKeyResponse response = apiKeyService.createApiKey(testEcommerceId);
        
        // Assert
        assertEquals("****n5o6", response.maskedKey());
    }
    
    @Test
    void getApiKeysByEcommerce_success() throws Exception {
        // Arrange
        doNothing().when(ecommerceService).validateEcommerceExists(testEcommerceId);
        
        ApiKeyEntity key1 = createApiKeyEntity(
            UUID.fromString("550e8400-e29b-41d4-a716-446655440001"),
            "key-001",
            testEcommerceId
        );
        
        ApiKeyEntity key2 = createApiKeyEntity(
            UUID.fromString("550e8400-e29b-41d4-a716-446655440002"),
            "key-002",
            testEcommerceId
        );
        
        when(apiKeyRepository.findByEcommerceId(testEcommerceId))
            .thenReturn(List.of(key1, key2));
        
        // Act
        List<ApiKeyListResponse> responses = apiKeyService.getApiKeysByEcommerce(testEcommerceId);
        
        // Assert
        assertEquals(2, responses.size());
        assertTrue(responses.get(0).maskedKey().endsWith("01"));
        assertTrue(responses.get(1).maskedKey().endsWith("02"));
        
        verify(ecommerceService).validateEcommerceExists(testEcommerceId);
        verify(apiKeyRepository).findByEcommerceId(testEcommerceId);
    }
    
    @Test
    void getApiKeysByEcommerce_empty() {
        // Arrange
        doNothing().when(ecommerceService).validateEcommerceExists(testEcommerceId);
        when(apiKeyRepository.findByEcommerceId(testEcommerceId))
            .thenReturn(List.of());
        
        // Act
        List<ApiKeyListResponse> responses = apiKeyService.getApiKeysByEcommerce(testEcommerceId);
        
        // Assert
        assertTrue(responses.isEmpty());
    }
    
    @Test
    void deleteApiKey_success() throws Exception {
        // Arrange
        doNothing().when(ecommerceService).validateEcommerceExists(testEcommerceId);
        
        ApiKeyEntity keyToDelete = createApiKeyEntity(testKeyId, "550e8400-e29b-41d4-a716-446655440000", testEcommerceId);
        
        when(apiKeyRepository.findById(testKeyId)).thenReturn(Optional.of(keyToDelete));
        doNothing().when(apiKeyRepository).delete(keyToDelete);
        
        // Act
        apiKeyService.deleteApiKey(testEcommerceId, testKeyId);
        
        // Assert
        verify(ecommerceService).validateEcommerceExists(testEcommerceId);
        verify(apiKeyRepository).findById(testKeyId);
        verify(apiKeyEventPublisher).publishApiKeyDeleted(any());
        verify(apiKeyRepository).delete(keyToDelete);
    }
    
    // ================== Error Path Tests ==================
    
    @Test
    void createApiKey_ecommerceNotFound_throwsException() {
        // Arrange
        doThrow(new EcommerceNotFoundException("Ecommerce no encontrado"))
            .when(ecommerceService).validateEcommerceExists(testEcommerceId);
        
        // Act & Assert
        assertThrows(EcommerceNotFoundException.class, 
            () -> apiKeyService.createApiKey(testEcommerceId));
        
        verify(ecommerceService).validateEcommerceExists(testEcommerceId);
        verify(apiKeyRepository, never()).save(any());
    }
    
    @Test
    void getApiKeysByEcommerce_ecommerceNotFound_throwsException() {
        // Arrange
        doThrow(new EcommerceNotFoundException("Ecommerce no encontrado"))
            .when(ecommerceService).validateEcommerceExists(testEcommerceId);
        
        // Act & Assert
        assertThrows(EcommerceNotFoundException.class, 
            () -> apiKeyService.getApiKeysByEcommerce(testEcommerceId));
    }
    
    @Test
    void deleteApiKey_keyNotFound_throwsException() {
        // Arrange
        doNothing().when(ecommerceService).validateEcommerceExists(testEcommerceId);
        when(apiKeyRepository.findById(testKeyId)).thenReturn(Optional.empty());
        
        // Act & Assert
        assertThrows(ApiKeyNotFoundException.class, 
            () -> apiKeyService.deleteApiKey(testEcommerceId, testKeyId));
        
        verify(apiKeyRepository, never()).delete(any());
    }
    
    @Test
    void deleteApiKey_keyNotBelongsToEcommerce_throwsException() throws Exception {
        // Arrange
        UUID differentEcommerceId = UUID.fromString("330e8400-e29b-41d4-a716-446655440222");
        doNothing().when(ecommerceService).validateEcommerceExists(testEcommerceId);
        
        ApiKeyEntity keyEntity = createApiKeyEntity(testKeyId, "some-key", differentEcommerceId);
        
        when(apiKeyRepository.findById(testKeyId)).thenReturn(Optional.of(keyEntity));
        
        // Act & Assert
        assertThrows(ApiKeyNotFoundException.class, 
            () -> apiKeyService.deleteApiKey(testEcommerceId, testKeyId));
        
        verify(apiKeyRepository, never()).delete(any());
    }
}
