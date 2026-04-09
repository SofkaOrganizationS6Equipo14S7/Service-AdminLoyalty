package com.loyalty.service_admin.application.service;

import com.loyalty.service_admin.application.dto.apikey.ApiKeyCreatedResponse;
import com.loyalty.service_admin.application.dto.apikey.ApiKeyEventPayload;
import com.loyalty.service_admin.application.dto.apikey.ApiKeyListResponse;
import com.loyalty.service_admin.application.port.out.ApiKeyEventPort;
import com.loyalty.service_admin.application.port.out.ApiKeyPersistencePort;
import com.loyalty.service_admin.domain.entity.ApiKeyEntity;
import com.loyalty.service_admin.infrastructure.exception.ApiKeyNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ApiKeyService Unit Tests")
class ApiKeyServiceTest {

    @Mock private ApiKeyPersistencePort persistencePort;
    @Mock private ApiKeyEventPort eventPort;
    @Mock private EcommerceService ecommerceService;

    @InjectMocks
    private ApiKeyService apiKeyService;

    private UUID ecommerceId;
    private UUID keyId;
    private ApiKeyEntity apiKeyEntity;

    @BeforeEach
    void setUp() {
        ecommerceId = UUID.randomUUID();
        keyId = UUID.randomUUID();

        apiKeyEntity = new ApiKeyEntity();
        apiKeyEntity.setId(keyId);
        apiKeyEntity.setEcommerceId(ecommerceId);
        apiKeyEntity.setHashedKey("abc123hashedkey");
        apiKeyEntity.setIsActive(true);
        apiKeyEntity.setExpiresAt(Instant.now().plusSeconds(86400));
        apiKeyEntity.setCreatedAt(Instant.now());
        apiKeyEntity.setUpdatedAt(Instant.now());
    }

    @Test
    @DisplayName("testCreateApiKey_Success")
    void testCreateApiKey_Success() {
        // Arrange
        when(persistencePort.save(any(ApiKeyEntity.class))).thenAnswer(invocation -> {
            ApiKeyEntity entity = invocation.getArgument(0);
            entity.setId(keyId);
            entity.setCreatedAt(Instant.now());
            entity.setUpdatedAt(Instant.now());
            return entity;
        });

        // Act
        ApiKeyCreatedResponse result = apiKeyService.createApiKey(ecommerceId);

        // Assert
        assertNotNull(result);
        assertNotNull(result.key());
        assertEquals(ecommerceId, result.ecommerceId());
        verify(ecommerceService).validateEcommerceExists(ecommerceId);
        verify(persistencePort).save(any(ApiKeyEntity.class));
        verify(eventPort).publishApiKeyCreated(any(ApiKeyEventPayload.class));
    }

    @Test
    @DisplayName("testGetApiKeysByEcommerce_Success")
    void testGetApiKeysByEcommerce_Success() {
        // Arrange
        when(persistencePort.findByEcommerceId(ecommerceId)).thenReturn(List.of(apiKeyEntity));

        // Act
        List<ApiKeyListResponse> result = apiKeyService.getApiKeysByEcommerce(ecommerceId);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        assertTrue(result.get(0).maskedKey().startsWith("****"));
        verify(ecommerceService).validateEcommerceExists(ecommerceId);
    }

    @Test
    @DisplayName("testGetApiKeysByEcommerce_Empty")
    void testGetApiKeysByEcommerce_Empty() {
        // Arrange
        when(persistencePort.findByEcommerceId(ecommerceId)).thenReturn(List.of());

        // Act
        List<ApiKeyListResponse> result = apiKeyService.getApiKeysByEcommerce(ecommerceId);

        // Assert
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("testDeleteApiKey_Success")
    void testDeleteApiKey_Success() {
        // Arrange
        when(persistencePort.findById(keyId)).thenReturn(Optional.of(apiKeyEntity));

        // Act
        apiKeyService.deleteApiKey(ecommerceId, keyId);

        // Assert
        verify(ecommerceService).validateEcommerceExists(ecommerceId);
        verify(eventPort).publishApiKeyDeleted(any(ApiKeyEventPayload.class));
        verify(persistencePort).deleteById(keyId);
    }

    @Test
    @DisplayName("testDeleteApiKey_NotFound_ThrowsApiKeyNotFoundException")
    void testDeleteApiKey_NotFound_ThrowsApiKeyNotFoundException() {
        // Arrange
        when(persistencePort.findById(keyId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(ApiKeyNotFoundException.class, () -> apiKeyService.deleteApiKey(ecommerceId, keyId));
    }

    @Test
    @DisplayName("testDeleteApiKey_WrongEcommerce_ThrowsApiKeyNotFoundException")
    void testDeleteApiKey_WrongEcommerce_ThrowsApiKeyNotFoundException() {
        // Arrange
        UUID otherEcommerceId = UUID.randomUUID();
        when(persistencePort.findById(keyId)).thenReturn(Optional.of(apiKeyEntity));

        // Act & Assert
        assertThrows(ApiKeyNotFoundException.class, () -> apiKeyService.deleteApiKey(otherEcommerceId, keyId));
    }
}
