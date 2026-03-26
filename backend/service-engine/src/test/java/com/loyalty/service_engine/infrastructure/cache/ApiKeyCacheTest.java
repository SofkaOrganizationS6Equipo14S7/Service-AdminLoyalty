package com.loyalty.service_engine.infrastructure.cache;

import com.loyalty.service_engine.domain.repository.ApiKeyRepository;
import com.loyalty.service_engine.domain.entity.ApiKeyEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for ApiKeyCache
 * Tests cache lifecycle: initialization, validation, add, remove operations
 * Validates Caffeine cache behavior and database sync
 */
@ExtendWith(MockitoExtension.class)
class ApiKeyCacheTest {
    
    @Mock
    private ApiKeyRepository apiKeyRepository;
    
    @InjectMocks
    private ApiKeyCache apiKeyCache;
    
    private UUID testEcommerceId;
    private String testKeyString;
    private ApiKeyEntity testApiKeyEntity;
    
    @BeforeEach
    void setUp() throws Exception {
        testEcommerceId = UUID.randomUUID();
        testKeyString = UUID.randomUUID().toString();
        
        // Create test entity
        testApiKeyEntity = new ApiKeyEntity();
        setFieldValue(testApiKeyEntity, "id", UUID.randomUUID());
        setFieldValue(testApiKeyEntity, "keyString", testKeyString);
        setFieldValue(testApiKeyEntity, "ecommerceId", testEcommerceId);
        setFieldValue(testApiKeyEntity, "createdAt", Instant.now());
        setFieldValue(testApiKeyEntity, "updatedAt", Instant.now());
        
        // Reset mocks after ApiKeyCache constructor calls loadFromDatabase()
        reset(apiKeyRepository);
    }
    
    // ================== Load from Database ==================
    
    @Test
    void loadFromDatabase_success() throws Exception {
        // Arrange
        ApiKeyEntity key1 = createApiKeyEntity(UUID.randomUUID());
        ApiKeyEntity key2 = createApiKeyEntity(UUID.randomUUID());
        
        when(apiKeyRepository.findAll()).thenReturn(List.of(key1, key2));
        
        // Act
        apiKeyCache.loadFromDatabase();
        
        // Assert
        verify(apiKeyRepository, times(1)).findAll();
        assertTrue(apiKeyCache.validateKey(getFieldValue(key1, "keyString", String.class)));
        assertTrue(apiKeyCache.validateKey(getFieldValue(key2, "keyString", String.class)));
    }
    
    @Test
    void loadFromDatabase_emptyRepository() throws Exception {
        // Arrange
        when(apiKeyRepository.findAll()).thenReturn(List.of());
        
        // Act
        apiKeyCache.loadFromDatabase();
        
        // Assert
        verify(apiKeyRepository, times(1)).findAll();
        assertFalse(apiKeyCache.validateKey("any-key-string"));
    }
    
    // ================== Validate Key ==================
    
    @Test
    void validateKey_validKey_returnsTrue() throws Exception {
        // Arrange
        when(apiKeyRepository.findAll()).thenReturn(List.of(testApiKeyEntity));
        apiKeyCache.loadFromDatabase();
        
        // Act
        boolean result = apiKeyCache.validateKey(testKeyString);
        
        // Assert
        assertTrue(result);
    }
    
    @Test
    void validateKey_invalidKey_returnsFalse() throws Exception {
        // Arrange
        when(apiKeyRepository.findAll()).thenReturn(List.of(testApiKeyEntity));
        apiKeyCache.loadFromDatabase();
        
        // Act
        boolean result = apiKeyCache.validateKey("non-existent-key");
        
        // Assert
        assertFalse(result);
    }
    
    @Test
    void validateKey_emptyCache_returnsFalse() throws Exception {
        // Arrange
        when(apiKeyRepository.findAll()).thenReturn(List.of());
        apiKeyCache.loadFromDatabase();
        
        // Act
        boolean result = apiKeyCache.validateKey("any-key");
        
        // Assert
        assertFalse(result);
    }
    
    // ================== Add Key ==================
    
    @Test
    void addKey_success() throws Exception {
        // Arrange
        when(apiKeyRepository.findAll()).thenReturn(List.of());
        apiKeyCache.loadFromDatabase();
        
        String newKeyString = UUID.randomUUID().toString();
        String ecommerceIdStr = testEcommerceId.toString();
        
        // Act
        apiKeyCache.addKey(newKeyString, ecommerceIdStr);
        
        // Assert — Key is immediately available in cache
        assertTrue(apiKeyCache.validateKey(newKeyString));
    }
    
    @Test
    void addKey_multipleKeys() throws Exception {
        // Arrange
        when(apiKeyRepository.findAll()).thenReturn(List.of());
        apiKeyCache.loadFromDatabase();
        
        String key1 = UUID.randomUUID().toString();
        String key2 = UUID.randomUUID().toString();
        String ecommerceId = testEcommerceId.toString();
        
        // Act
        apiKeyCache.addKey(key1, ecommerceId);
        apiKeyCache.addKey(key2, ecommerceId);
        
        // Assert
        assertTrue(apiKeyCache.validateKey(key1));
        assertTrue(apiKeyCache.validateKey(key2));
    }
    
    @Test
    void addKey_duplicateKey_replacesExisting() throws Exception {
        // Arrange
        when(apiKeyRepository.findAll()).thenReturn(List.of());
        apiKeyCache.loadFromDatabase();
        
        String keyString = UUID.randomUUID().toString();
        String ecommerceId = testEcommerceId.toString();
        
        // Act
        apiKeyCache.addKey(keyString, ecommerceId);
        apiKeyCache.addKey(keyString, UUID.randomUUID().toString()); // Same key, different ecommerce
        
        // Assert
        assertTrue(apiKeyCache.validateKey(keyString)); // Still valid
    }
    
    // ================== Remove Key ==================
    
    @Test
    void removeKey_success() throws Exception {
        // Arrange
        when(apiKeyRepository.findAll()).thenReturn(List.of(testApiKeyEntity));
        apiKeyCache.loadFromDatabase();
        assertTrue(apiKeyCache.validateKey(testKeyString));
        
        // Act
        apiKeyCache.removeKey(testKeyString);
        
        // Assert
        assertFalse(apiKeyCache.validateKey(testKeyString));
    }
    
    @Test
    void removeKey_nonExistentKey() throws Exception {
        // Arrange
        when(apiKeyRepository.findAll()).thenReturn(List.of());
        apiKeyCache.loadFromDatabase();
        
        // Act & Assert — Should not throw exception
        assertDoesNotThrow(() -> apiKeyCache.removeKey("non-existent-key"));
    }
    
    @Test
    void removeKey_verifyIsolation() throws Exception {
        // Arrange
        String key1 = UUID.randomUUID().toString();
        String key2 = UUID.randomUUID().toString();
        String ecommerceId = testEcommerceId.toString();
        
        when(apiKeyRepository.findAll()).thenReturn(List.of());
        apiKeyCache.loadFromDatabase();
        
        apiKeyCache.addKey(key1, ecommerceId);
        apiKeyCache.addKey(key2, ecommerceId);
        
        // Act
        apiKeyCache.removeKey(key1);
        
        // Assert
        assertFalse(apiKeyCache.validateKey(key1));
        assertTrue(apiKeyCache.validateKey(key2));
    }
    
    // ================== Helper Methods ==================
    
    private ApiKeyEntity createApiKeyEntity(UUID keyId) throws Exception {
        ApiKeyEntity entity = new ApiKeyEntity();
        setFieldValue(entity, "id", keyId);
        setFieldValue(entity, "keyString", UUID.randomUUID().toString());
        setFieldValue(entity, "ecommerceId", testEcommerceId);
        setFieldValue(entity, "createdAt", Instant.now());
        setFieldValue(entity, "updatedAt", Instant.now());
        return entity;
    }
    
    private void setFieldValue(Object target, String fieldName, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }
    
    private <T> T getFieldValue(Object target, String fieldName, Class<T> type) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        return type.cast(field.get(target));
    }
}
