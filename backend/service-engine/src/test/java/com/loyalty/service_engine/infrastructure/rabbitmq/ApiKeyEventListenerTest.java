package com.loyalty.service_engine.infrastructure.rabbitmq;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.loyalty.service_engine.application.dto.ApiKeyEventPayload;
import com.loyalty.service_engine.infrastructure.cache.ApiKeyCache;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for ApiKeyEventListener
 * Validates RabbitMQ event consumption and cache synchronization
 */
@ExtendWith(MockitoExtension.class)
class ApiKeyEventListenerTest {
    
    @Mock
    private ApiKeyCache apiKeyCache;
    
    private ApiKeyEventListener eventListener;
    private ObjectMapper realObjectMapper;
    
    private UUID testEcommerceId;
    private UUID testKeyId;
    private String testKeyString;
    
    @BeforeEach
    void setUp() {
        testEcommerceId = UUID.randomUUID();
        testKeyId = UUID.randomUUID();
        testKeyString = UUID.randomUUID().toString();
        realObjectMapper = new ObjectMapper();
        
        // Use real ObjectMapper for JSON deserialization
        eventListener = new ApiKeyEventListener(apiKeyCache, realObjectMapper);
    }
    
    // ================== API_KEY_CREATED Event ==================
    
    @Test
    void onApiKeyEvent_createdEvent_addsKeyToCache() throws Exception {
        // Arrange
        ApiKeyEventPayload payload = new ApiKeyEventPayload(
            "API_KEY_CREATED",
            testKeyId.toString(),
            testKeyString,
            testEcommerceId.toString(),
            System.currentTimeMillis() + ""
        );
        String jsonPayload = realObjectMapper.writeValueAsString(payload);
        
        // Act
        eventListener.onApiKeyEvent(jsonPayload);
        
        // Assert
        verify(apiKeyCache, times(1)).addKey(testKeyString, testEcommerceId.toString());
    }
    
    @Test
    void onApiKeyEvent_deletedEvent_removesKeyFromCache() throws Exception {
        // Arrange
        ApiKeyEventPayload payload = new ApiKeyEventPayload(
            "API_KEY_DELETED",
            testKeyId.toString(),
            testKeyString,
            testEcommerceId.toString(),
            System.currentTimeMillis() + ""
        );
        String jsonPayload = realObjectMapper.writeValueAsString(payload);
        
        // Act
        eventListener.onApiKeyEvent(jsonPayload);
        
        // Assert
        verify(apiKeyCache, times(1)).removeKey(testKeyString);
    }
    
    @Test
    void onApiKeyEvent_multipleCreatedEvents() throws Exception {
        // Arrange
        String ecommerceId = UUID.randomUUID().toString();
        
        ApiKeyEventPayload payload1 = new ApiKeyEventPayload(
            "API_KEY_CREATED",
            UUID.randomUUID().toString(),
            "key-1",
            ecommerceId,
            System.currentTimeMillis() + ""
        );
        
        ApiKeyEventPayload payload2 = new ApiKeyEventPayload(
            "API_KEY_CREATED",
            UUID.randomUUID().toString(),
            "key-2",
            ecommerceId,
            System.currentTimeMillis() + ""
        );
        
        // Act
        eventListener.onApiKeyEvent(realObjectMapper.writeValueAsString(payload1));
        eventListener.onApiKeyEvent(realObjectMapper.writeValueAsString(payload2));
        
        // Assert
        verify(apiKeyCache).addKey("key-1", ecommerceId);
        verify(apiKeyCache).addKey("key-2", ecommerceId);
        verify(apiKeyCache, times(2)).addKey(anyString(), eq(ecommerceId));
    }
    
    @Test
    void onApiKeyEvent_multipleDeletedEvents() throws Exception {
        // Arrange
        ApiKeyEventPayload payload1 = new ApiKeyEventPayload(
            "API_KEY_DELETED",
            UUID.randomUUID().toString(),
            "key-to-delete-1",
            UUID.randomUUID().toString(),
            System.currentTimeMillis() + ""
        );
        
        ApiKeyEventPayload payload2 = new ApiKeyEventPayload(
            "API_KEY_DELETED",
            UUID.randomUUID().toString(),
            "key-to-delete-2",
            UUID.randomUUID().toString(),
            System.currentTimeMillis() + ""
        );
        
        // Act
        eventListener.onApiKeyEvent(realObjectMapper.writeValueAsString(payload1));
        eventListener.onApiKeyEvent(realObjectMapper.writeValueAsString(payload2));
        
        // Assert
        verify(apiKeyCache).removeKey("key-to-delete-1");
        verify(apiKeyCache).removeKey("key-to-delete-2");
        verify(apiKeyCache, times(2)).removeKey(anyString());
    }
    
    // ================== Event Parsing ==================
    
    @Test
    void onApiKeyEvent_parseJsonCorrectly() throws Exception {
        // Arrange
        ApiKeyEventPayload originalPayload = new ApiKeyEventPayload(
            "API_KEY_CREATED",
            testKeyId.toString(),
            testKeyString,
            testEcommerceId.toString(),
            "1699564800000"
        );
        String jsonPayload = realObjectMapper.writeValueAsString(originalPayload);
        
        // Act
        eventListener.onApiKeyEvent(jsonPayload);
        
        // Assert
        ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> ecommerceCaptor = ArgumentCaptor.forClass(String.class);
        verify(apiKeyCache).addKey(keyCaptor.capture(), ecommerceCaptor.capture());
        
        assertEquals(testKeyString, keyCaptor.getValue());
        assertEquals(testEcommerceId.toString(), ecommerceCaptor.getValue());
    }
    
    // ================== Error Handling ==================
    
    @Test
    void onApiKeyEvent_invalidJson_doesNotThrow() {
        // Arrange
        String invalidJson = "{invalid json}";
        
        // Act & Assert
        assertDoesNotThrow(() -> eventListener.onApiKeyEvent(invalidJson));
        verify(apiKeyCache, never()).addKey(anyString(), anyString());
        verify(apiKeyCache, never()).removeKey(anyString());
    }
    
    @Test
    void onApiKeyEvent_unknownEventType_doesNotThrow() throws Exception {
        // Arrange
        ApiKeyEventPayload payload = new ApiKeyEventPayload(
            "UNKNOWN_EVENT_TYPE",
            testKeyId.toString(),
            testKeyString,
            testEcommerceId.toString(),
            System.currentTimeMillis() + ""
        );
        
        // Act & Assert
        assertDoesNotThrow(() -> eventListener.onApiKeyEvent(
            realObjectMapper.writeValueAsString(payload)
        ));
        
        verify(apiKeyCache, never()).addKey(anyString(), anyString());
        verify(apiKeyCache, never()).removeKey(anyString());
    }
    
    @Test
    void onApiKeyEvent_cacheThrowsException_doesNotPropagate() throws Exception {
        // Arrange
        ApiKeyEventPayload payload = new ApiKeyEventPayload(
            "API_KEY_CREATED",
            testKeyId.toString(),
            testKeyString,
            testEcommerceId.toString(),
            System.currentTimeMillis() + ""
        );
        
        doThrow(new RuntimeException("Cache error")).when(apiKeyCache)
            .addKey(anyString(), anyString());
        
        // Act & Assert
        assertDoesNotThrow(() -> eventListener.onApiKeyEvent(
            realObjectMapper.writeValueAsString(payload)
        ));
    }
    
    // ================== Cache Synchronization ==================
    
    @Test
    void onApiKeyEvent_createThenDelete_keyLifecycle() throws Exception {
        // Arrange
        String ecommerceId = UUID.randomUUID().toString();
        String keyString = "sync-test-key";
        
        ApiKeyEventPayload createPayload = new ApiKeyEventPayload(
            "API_KEY_CREATED",
            UUID.randomUUID().toString(),
            keyString,
            ecommerceId,
            System.currentTimeMillis() + ""
        );
        
        ApiKeyEventPayload deletePayload = new ApiKeyEventPayload(
            "API_KEY_DELETED",
            UUID.randomUUID().toString(),
            keyString,
            ecommerceId,
            System.currentTimeMillis() + ""
        );
        
        // Act
        eventListener.onApiKeyEvent(realObjectMapper.writeValueAsString(createPayload));
        eventListener.onApiKeyEvent(realObjectMapper.writeValueAsString(deletePayload));
        
        // Assert
        verify(apiKeyCache).addKey(keyString, ecommerceId);
        verify(apiKeyCache).removeKey(keyString);
    }
}
