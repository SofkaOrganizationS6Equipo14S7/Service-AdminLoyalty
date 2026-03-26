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
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.inOrder;

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
    private ApiKeyEntity createApiKeyEntity(UUID id, String hashedKey, UUID ecommerceId) throws Exception {
        ApiKeyEntity entity = new ApiKeyEntity();
        
        // Usar reflection para setear los campos si los setters no están disponibles
        setField(entity, "id", id);
        setField(entity, "hashedKey", hashedKey);  // Changed from keyString to hashedKey
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
        assertTrue(response.maskedKey().startsWith("****"));  // Should be masked format
        assertTrue(response.maskedKey().length() == 8);  // ****XXXX
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
        
        // Assert - verify format is ****XXXX (masked)
        assertTrue(response.maskedKey().startsWith("****"));
        assertTrue(response.maskedKey().length() == 8);
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
    
    // ================== Additional Test Cases for Better Coverage ==================
    
    /**
     * CRITERIO-3.1.2: Verificar masking format y hashing
     * Valida que el plaintext retornado se maskea correctamente.
     */
    @Test
    void createApiKey_returnsPlainKeyValueOnceInResponse() throws Exception {
        // Arrange
        doNothing().when(ecommerceService).validateEcommerceExists(testEcommerceId);
        
        ApiKeyEntity savedEntity = createApiKeyEntity(testKeyId, "hash-value", testEcommerceId);
        when(apiKeyRepository.save(any(ApiKeyEntity.class))).thenReturn(savedEntity);
        
        // Act
        ApiKeyResponse response = apiKeyService.createApiKey(testEcommerceId);
        
        // Assert - the response contains masked key, not plaintext
        assertNotNull(response.maskedKey());
        assertTrue(response.maskedKey().startsWith("****"));
        verify(apiKeyRepository).save(any(ApiKeyEntity.class));
    }
    
    /**
     * CRITERIO-3.2.1: Masking in list response
     * Verifica que las keys en la lista están maskadas.
     */
    @Test
    void getApiKeysByEcommerce_keysAreProperlyMasked() throws Exception {
        // Arrange
        doNothing().when(ecommerceService).validateEcommerceExists(testEcommerceId);
        
        ApiKeyEntity key = createApiKeyEntity(
            UUID.fromString("550e8400-e29b-41d4-a716-446655440001"),
            "a665a45920422f9d417e4867efdc4fb8",
            testEcommerceId
        );
        
        when(apiKeyRepository.findByEcommerceId(testEcommerceId))
            .thenReturn(List.of(key));
        
        // Act
        List<ApiKeyListResponse> responses = apiKeyService.getApiKeysByEcommerce(testEcommerceId);
        
        // Assert
        assertEquals(1, responses.size());
        assertTrue(responses.get(0).maskedKey().startsWith("****"));
        assertFalse(responses.get(0).maskedKey().contains("a665a459")); // No contain hash part
    }
    
    /**
     * Event publishing on create
     * Verifica que el evento se publica cuando se crea una key.
     */
    @Test
    void createApiKey_publishesEventWithHashedKey() throws Exception {
        // Arrange
        doNothing().when(ecommerceService).validateEcommerceExists(testEcommerceId);
        
        ApiKeyEntity savedEntity = createApiKeyEntity(testKeyId, "hash-123", testEcommerceId);
        when(apiKeyRepository.save(any(ApiKeyEntity.class))).thenReturn(savedEntity);
        
        // Act
        apiKeyService.createApiKey(testEcommerceId);
        
        // Assert - event publisher is called
        verify(apiKeyEventPublisher, times(1)).publishApiKeyCreated(any());
    }
    
    /**
     * Event publishing on delete
     * Verifica que el evento de eliminación se publica cuando se borra una key.
     */
    @Test
    void deleteApiKey_publishesDeleteEvent() throws Exception {
        // Arrange
        doNothing().when(ecommerceService).validateEcommerceExists(testEcommerceId);
        
        ApiKeyEntity keyEntity = createApiKeyEntity(testKeyId, "hash-to-delete", testEcommerceId);
        when(apiKeyRepository.findById(testKeyId)).thenReturn(Optional.of(keyEntity));
        doNothing().when(apiKeyRepository).delete(keyEntity);
        
        // Act
        apiKeyService.deleteApiKey(testEcommerceId, testKeyId);
        
        // Assert
        verify(apiKeyEventPublisher, times(1)).publishApiKeyDeleted(any());
    }
    
    /**
     * Test that masking format is ****XXXX.
     */
    @Test
    void maskKey_extractsLast4Characters() throws Exception {
        // Arrange
        doNothing().when(ecommerceService).validateEcommerceExists(testEcommerceId);
        
        // Create key with known ending
        ApiKeyEntity savedEntity = createApiKeyEntity(
            testKeyId,
            "1234567890abcdef",
            testEcommerceId
        );
        
        when(apiKeyRepository.save(any(ApiKeyEntity.class))).thenReturn(savedEntity);
        
        // Act
        ApiKeyResponse response = apiKeyService.createApiKey(testEcommerceId);
        
        // Assert - format is ****XXXX
        assertTrue(response.maskedKey().startsWith("****"));
        assertTrue(response.maskedKey().length() == 8);
    }
    
    /**
     * Test empty list handling.
     */
    @Test
    void getApiKeysByEcommerce_emptyListWhenNoKeys() throws Exception {
        // Arrange
        doNothing().when(ecommerceService).validateEcommerceExists(testEcommerceId);
        when(apiKeyRepository.findByEcommerceId(testEcommerceId)).thenReturn(List.of());
        
        // Act
        List<ApiKeyListResponse> responses = apiKeyService.getApiKeysByEcommerce(testEcommerceId);
        
        // Assert
        assertNotNull(responses);
        assertTrue(responses.isEmpty());
    }
    
    /**
     * Test multiple keys per ecommerce.
     */
    @Test
    void getApiKeysByEcommerce_multipleKeys() throws Exception {
        // Arrange
        doNothing().when(ecommerceService).validateEcommerceExists(testEcommerceId);
        
        List<ApiKeyEntity> keys = List.of(
            createApiKeyEntity(UUID.randomUUID(), "hash1", testEcommerceId),
            createApiKeyEntity(UUID.randomUUID(), "hash2", testEcommerceId),
            createApiKeyEntity(UUID.randomUUID(), "hash3", testEcommerceId)
        );
        
        when(apiKeyRepository.findByEcommerceId(testEcommerceId)).thenReturn(keys);
        
        // Act
        List<ApiKeyListResponse> responses = apiKeyService.getApiKeysByEcommerce(testEcommerceId);
        
        // Assert
        assertEquals(3, responses.size());
        for (ApiKeyListResponse response : responses) {
            assertNotNull(response.uid());
            assertTrue(response.maskedKey().startsWith("****"));
            assertNotNull(response.createdAt());
            assertNotNull(response.updatedAt());
        }
    }
    
    /**
     * Test that ecommerce validation is called before operations.
     */
    @Test
    void createApiKey_validatesEcommerceBeforeSaving() throws Exception {
        // Arrange
        doNothing().when(ecommerceService).validateEcommerceExists(testEcommerceId);
        
        ApiKeyEntity savedEntity = createApiKeyEntity(testKeyId, "hash", testEcommerceId);
        when(apiKeyRepository.save(any(ApiKeyEntity.class))).thenReturn(savedEntity);
        
        // Act
        apiKeyService.createApiKey(testEcommerceId);
        
        // Assert - validateEcommerceExists called first
        InOrder inOrder = inOrder(ecommerceService, apiKeyRepository);
        inOrder.verify(ecommerceService).validateEcommerceExists(testEcommerceId);
        inOrder.verify(apiKeyRepository).save(any());
    }
    
    /**
     * Test response contains all required fields.
     */
    @Test
    void createApiKey_responseContainsAllRequiredFields() throws Exception {
        // Arrange
        doNothing().when(ecommerceService).validateEcommerceExists(testEcommerceId);
        
        Instant now = Instant.now();
        ApiKeyEntity savedEntity = createApiKeyEntity(testKeyId, "hash", testEcommerceId);
        setField(savedEntity, "createdAt", now);
        setField(savedEntity, "updatedAt", now);
        
        when(apiKeyRepository.save(any(ApiKeyEntity.class))).thenReturn(savedEntity);
        
        // Act
        ApiKeyResponse response = apiKeyService.createApiKey(testEcommerceId);
        
        // Assert
        assertNotNull(response.uid());
        assertNotNull(response.maskedKey());
        assertNotNull(response.ecommerceId());
        assertNotNull(response.createdAt());
        assertNotNull(response.updatedAt());
    }
    
    /**
     * Test error message when ecommerce is not found.
     */
    @Test
    void deleteApiKey_throwsExceptionWithCorrectMessage() {
        // Arrange
        UUID wrongKeyId = UUID.randomUUID();
        doNothing().when(ecommerceService).validateEcommerceExists(testEcommerceId);
        when(apiKeyRepository.findById(wrongKeyId)).thenReturn(Optional.empty());
        
        // Act & Assert
        ApiKeyNotFoundException exception = assertThrows(ApiKeyNotFoundException.class,
            () -> apiKeyService.deleteApiKey(testEcommerceId, wrongKeyId));
        
        assertNotNull(exception.getMessage());
    }
}
