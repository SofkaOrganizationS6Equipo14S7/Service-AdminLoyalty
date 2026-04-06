package com.loyalty.service_admin.application.service;

import com.loyalty.service_admin.application.dto.ecommerce.EcommerceCreateRequest;
import com.loyalty.service_admin.application.dto.ecommerce.EcommerceResponse;
import com.loyalty.service_admin.application.dto.ecommerce.EcommerceUpdateStatusRequest;
import com.loyalty.service_admin.application.port.out.EcommercePersistencePort;
import com.loyalty.service_admin.application.port.out.EcommerceEventPort;
import com.loyalty.service_admin.domain.entity.EcommerceEntity;
import com.loyalty.service_admin.domain.entity.UserEntity;
import com.loyalty.service_admin.domain.entity.ApiKeyEntity;
import com.loyalty.service_admin.domain.model.ecommerce.EcommerceStatus;
import com.loyalty.service_admin.infrastructure.exception.ConflictException;
import com.loyalty.service_admin.infrastructure.exception.BadRequestException;
import com.loyalty.service_admin.infrastructure.exception.EcommerceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit Tests para EcommerceService (implementación del puerto EcommerceUseCase)
 * 
 * SPEC-015: Pruebas unitarias con Mockito
 * Patrón: Arrange-Act-Assert
 * Aislamiento: Se mockean todos los puertos (persistencia y eventos)
 */
@ExtendWith(MockitoExtension.class)
class EcommerceServiceTest {
    
    @Mock
    private EcommercePersistencePort persistencePort;
    
    @Mock
    private EcommerceEventPort eventPort;
    
    @InjectMocks
    private EcommerceService ecommerceService;
    
    private UUID testEcommerceId;
    private String testSlug;
    private String testName;
    
    @BeforeEach
    void setUp() {
        testEcommerceId = UUID.randomUUID();
        testSlug = "test-store";
        testName = "Test Store";
    }
    
    // ==================== createEcommerce() Tests ====================
    
    @Test
    void testCreateEcommerce_success() {
        // Arrange
        EcommerceCreateRequest request = new EcommerceCreateRequest(testName, testSlug);
        
        EcommerceEntity savedEntity = EcommerceEntity.builder()
                .id(testEcommerceId)
                .name(testName)
                .slug(testSlug)
                .status(EcommerceStatus.ACTIVE)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
        
        when(persistencePort.existsBySlug(testSlug)).thenReturn(false);
        when(persistencePort.save(any(EcommerceEntity.class))).thenReturn(savedEntity);
        doNothing().when(eventPort).publishEcommerceCreated(testEcommerceId, testName, testSlug);
        
        // Act
        EcommerceResponse response = ecommerceService.createEcommerce(request);
        
        // Assert
        assertNotNull(response);
        assertEquals(testEcommerceId, response.uid());
        assertEquals(testName, response.name());
        assertEquals(testSlug, response.slug());
        assertEquals("ACTIVE", response.status());
        
        verify(persistencePort, times(1)).existsBySlug(testSlug);
        verify(persistencePort, times(1)).save(any(EcommerceEntity.class));
        verify(eventPort, times(1)).publishEcommerceCreated(testEcommerceId, testName, testSlug);
    }
    
    @Test
    void testCreateEcommerce_duplicateSlug_throwsConflict() {
        // Arrange
        EcommerceCreateRequest request = new EcommerceCreateRequest(testName, testSlug);
        when(persistencePort.existsBySlug(testSlug)).thenReturn(true);
        
        // Act & Assert
        ConflictException exception = assertThrows(ConflictException.class, 
            () -> ecommerceService.createEcommerce(request)
        );
        assertTrue(exception.getMessage().contains("ya está en uso"));
        
        verify(persistencePort, times(1)).existsBySlug(testSlug);
        verify(persistencePort, never()).save(any());
        verify(eventPort, never()).publishEcommerceCreated(any(), any(), any());
    }
    
    @Test
    void testCreateEcommerce_eventPublishFailure_throwsException() {
        // Arrange
        EcommerceCreateRequest request = new EcommerceCreateRequest(testName, testSlug);
        
        EcommerceEntity savedEntity = EcommerceEntity.builder()
                .id(testEcommerceId)
                .name(testName)
                .slug(testSlug)
                .status(EcommerceStatus.ACTIVE)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
        
        when(persistencePort.existsBySlug(testSlug)).thenReturn(false);
        when(persistencePort.save(any(EcommerceEntity.class))).thenReturn(savedEntity);
        doThrow(new RuntimeException("RabbitMQ unavailable"))
                .when(eventPort).publishEcommerceCreated(testEcommerceId, testName, testSlug);
        
        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class,
            () -> ecommerceService.createEcommerce(request)
        );
        assertTrue(exception.getMessage().contains("No se pudo publicar"));
        
        verify(persistencePort, times(1)).save(any(EcommerceEntity.class));
        verify(eventPort, times(1)).publishEcommerceCreated(testEcommerceId, testName, testSlug);
    }
    
    // ==================== updateEcommerceStatus() Tests ====================
    
    @Test
    void testUpdateEcommerceStatus_success() {
        // Arrange
        EcommerceUpdateStatusRequest request = new EcommerceUpdateStatusRequest("INACTIVE");
        
        EcommerceEntity existingEntity = EcommerceEntity.builder()
                .id(testEcommerceId)
                .name(testName)
                .slug(testSlug)
                .status(EcommerceStatus.ACTIVE)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
        
        EcommerceEntity updatedEntity = EcommerceEntity.builder()
                .id(testEcommerceId)
                .name(testName)
                .slug(testSlug)
                .status(EcommerceStatus.INACTIVE)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
        
        when(persistencePort.findById(testEcommerceId)).thenReturn(Optional.of(existingEntity));
        when(persistencePort.save(any(EcommerceEntity.class))).thenReturn(updatedEntity);
        when(persistencePort.findUsersByEcommerceId(testEcommerceId)).thenReturn(new ArrayList<>());
        when(persistencePort.findApiKeysByEcommerceId(testEcommerceId)).thenReturn(new ArrayList<>());
        doNothing().when(eventPort).publishEcommerceStatusChanged(testEcommerceId, "INACTIVE");
        
        // Act
        EcommerceResponse response = ecommerceService.updateEcommerceStatus(testEcommerceId, request);
        
        // Assert
        assertNotNull(response);
        assertEquals(testEcommerceId, response.uid());
        assertEquals("INACTIVE", response.status());
        
        verify(persistencePort, times(1)).findById(testEcommerceId);
        verify(persistencePort, times(1)).save(any(EcommerceEntity.class));
        verify(persistencePort, times(1)).findUsersByEcommerceId(testEcommerceId);
        verify(persistencePort, times(1)).findApiKeysByEcommerceId(testEcommerceId);
        verify(eventPort, times(1)).publishEcommerceStatusChanged(testEcommerceId, "INACTIVE");
    }
    
    @Test
    void testUpdateEcommerceStatus_cascadeInactivateUsers() {
        // Arrange
        EcommerceUpdateStatusRequest request = new EcommerceUpdateStatusRequest("INACTIVE");
        
        EcommerceEntity existingEntity = EcommerceEntity.builder()
                .id(testEcommerceId)
                .name(testName)
                .slug(testSlug)
                .status(EcommerceStatus.ACTIVE)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
        
        EcommerceEntity updatedEntity = EcommerceEntity.builder()
                .id(testEcommerceId)
                .name(testName)
                .slug(testSlug)
                .status(EcommerceStatus.INACTIVE)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
        
        // Crear 3 usuarios para inactivar
        List<UserEntity> users = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            UserEntity user = new UserEntity();
            user.setId(UUID.randomUUID());
            user.setUsername("user" + i);
            user.setEmail("user" + i + "@example.com");
            user.setEcommerceId(testEcommerceId);
            user.setIsActive(true);
            users.add(user);
        }
        
        when(persistencePort.findById(testEcommerceId)).thenReturn(Optional.of(existingEntity));
        when(persistencePort.save(any(EcommerceEntity.class))).thenReturn(updatedEntity);
        when(persistencePort.findUsersByEcommerceId(testEcommerceId)).thenReturn(users);
        when(persistencePort.findApiKeysByEcommerceId(testEcommerceId)).thenReturn(new ArrayList<>());
        doNothing().when(persistencePort).inactivateUsers(any());
        doNothing().when(eventPort).publishEcommerceStatusChanged(testEcommerceId, "INACTIVE");
        
        // Act
        EcommerceResponse response = ecommerceService.updateEcommerceStatus(testEcommerceId, request);
        
        // Assert
        assertNotNull(response);
        assertEquals("INACTIVE", response.status());
        
        verify(persistencePort, times(1)).findUsersByEcommerceId(testEcommerceId);
        verify(persistencePort, times(1)).inactivateUsers(argThat(list -> list.size() == 3));
    }
    
    @Test
    void testUpdateEcommerceStatus_cascadeDeactivateApiKeys() {
        // Arrange
        EcommerceUpdateStatusRequest request = new EcommerceUpdateStatusRequest("INACTIVE");
        
        EcommerceEntity existingEntity = EcommerceEntity.builder()
                .id(testEcommerceId)
                .name(testName)
                .slug(testSlug)
                .status(EcommerceStatus.ACTIVE)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
        
        EcommerceEntity updatedEntity = EcommerceEntity.builder()
                .id(testEcommerceId)
                .name(testName)
                .slug(testSlug)
                .status(EcommerceStatus.INACTIVE)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
        
        // Crear 2 API Keys para desactivar
        List<ApiKeyEntity> apiKeys = new ArrayList<>();
        for (int i = 0; i < 2; i++) {
            ApiKeyEntity key = new ApiKeyEntity();
            key.setId(UUID.randomUUID());
            key.setEcommerceId(testEcommerceId);
            key.setHashedKey("hash" + i);
            key.setIsActive(true);
            key.setExpiresAt(Instant.now().plusSeconds(86400));
            apiKeys.add(key);
        }
        
        when(persistencePort.findById(testEcommerceId)).thenReturn(Optional.of(existingEntity));
        when(persistencePort.save(any(EcommerceEntity.class))).thenReturn(updatedEntity);
        when(persistencePort.findUsersByEcommerceId(testEcommerceId)).thenReturn(new ArrayList<>());
        when(persistencePort.findApiKeysByEcommerceId(testEcommerceId)).thenReturn(apiKeys);
        doNothing().when(persistencePort).deactivateApiKeys(any());
        doNothing().when(eventPort).publishEcommerceStatusChanged(testEcommerceId, "INACTIVE");
        
        // Act
        EcommerceResponse response = ecommerceService.updateEcommerceStatus(testEcommerceId, request);
        
        // Assert
        assertNotNull(response);
        assertEquals("INACTIVE", response.status());
        
        verify(persistencePort, times(1)).findApiKeysByEcommerceId(testEcommerceId);
        verify(persistencePort, times(1)).deactivateApiKeys(argThat(list -> list.size() == 2));
    }
    
    @Test
    void testUpdateEcommerceStatus_noStatusChange_noEvent() {
        // Arrange
        EcommerceUpdateStatusRequest request = new EcommerceUpdateStatusRequest("ACTIVE");
        
        EcommerceEntity existingEntity = EcommerceEntity.builder()
                .id(testEcommerceId)
                .name(testName)
                .slug(testSlug)
                .status(EcommerceStatus.ACTIVE)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
        
        when(persistencePort.findById(testEcommerceId)).thenReturn(Optional.of(existingEntity));
        
        // Act
        EcommerceResponse response = ecommerceService.updateEcommerceStatus(testEcommerceId, request);
        
        // Assert
        assertNotNull(response);
        assertEquals("ACTIVE", response.status());
        
        // Verificar que NO se guardó ni se publicó evento
        verify(persistencePort, never()).save(any());
        verify(persistencePort, never()).findUsersByEcommerceId(any());
        verify(persistencePort, never()).findApiKeysByEcommerceId(any());
        verify(eventPort, never()).publishEcommerceStatusChanged(any(), any());
    }
    
    @Test
    void testUpdateEcommerceStatus_notFound_throwsException() {
        // Arrange
        EcommerceUpdateStatusRequest request = new EcommerceUpdateStatusRequest("INACTIVE");
        when(persistencePort.findById(testEcommerceId)).thenReturn(Optional.empty());
        
        // Act & Assert
        EcommerceNotFoundException exception = assertThrows(EcommerceNotFoundException.class,
            () -> ecommerceService.updateEcommerceStatus(testEcommerceId, request)
        );
        assertTrue(exception.getMessage().contains("no existe"));
        
        verify(persistencePort, times(1)).findById(testEcommerceId);
        verify(persistencePort, never()).save(any());
        verify(eventPort, never()).publishEcommerceStatusChanged(any(), any());
    }
    
    @Test
    void testUpdateEcommerceStatus_invalidStatus_throwsBadRequest() {
        // Arrange
        EcommerceUpdateStatusRequest request = new EcommerceUpdateStatusRequest("INVALID_STATUS");
        
        EcommerceEntity existingEntity = EcommerceEntity.builder()
                .id(testEcommerceId)
                .name(testName)
                .slug(testSlug)
                .status(EcommerceStatus.ACTIVE)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
        
        when(persistencePort.findById(testEcommerceId)).thenReturn(Optional.of(existingEntity));
        
        // Act & Assert
        BadRequestException exception = assertThrows(BadRequestException.class,
            () -> ecommerceService.updateEcommerceStatus(testEcommerceId, request)
        );
        assertTrue(exception.getMessage().contains("inválido"));
        
        verify(persistencePort, times(1)).findById(testEcommerceId);
        verify(persistencePort, never()).save(any());
    }
    
    // ==================== getEcommerceById() Tests ====================
    
    @Test
    void testGetEcommerceById_success() {
        // Arrange
        EcommerceEntity entity = EcommerceEntity.builder()
                .id(testEcommerceId)
                .name(testName)
                .slug(testSlug)
                .status(EcommerceStatus.ACTIVE)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
        
        when(persistencePort.findById(testEcommerceId)).thenReturn(Optional.of(entity));
        
        // Act
        EcommerceResponse response = ecommerceService.getEcommerceById(testEcommerceId);
        
        // Assert
        assertNotNull(response);
        assertEquals(testEcommerceId, response.uid());
        assertEquals(testName, response.name());
        assertEquals(testSlug, response.slug());
        
        verify(persistencePort, times(1)).findById(testEcommerceId);
    }
    
    @Test
    void testGetEcommerceById_notFound_throwsException() {
        // Arrange
        when(persistencePort.findById(testEcommerceId)).thenReturn(Optional.empty());
        
        // Act & Assert
        EcommerceNotFoundException exception = assertThrows(EcommerceNotFoundException.class,
            () -> ecommerceService.getEcommerceById(testEcommerceId)
        );
        assertTrue(exception.getMessage().contains("no existe"));
        
        verify(persistencePort, times(1)).findById(testEcommerceId);
    }
    
    // ==================== validateEcommerceExists() Tests ====================
    
    @Test
    void testValidateEcommerceExists_success() {
        // Arrange
        when(persistencePort.existsById(testEcommerceId)).thenReturn(true);
        
        // Act & Assert
        assertDoesNotThrow(() -> ecommerceService.validateEcommerceExists(testEcommerceId));
        
        verify(persistencePort, times(1)).existsById(testEcommerceId);
    }
    
    @Test
    void testValidateEcommerceExists_notFound_throwsException() {
        // Arrange
        when(persistencePort.existsById(testEcommerceId)).thenReturn(false);
        
        // Act & Assert
        EcommerceNotFoundException exception = assertThrows(EcommerceNotFoundException.class,
            () -> ecommerceService.validateEcommerceExists(testEcommerceId)
        );
        assertTrue(exception.getMessage().contains("no existe"));
        
        verify(persistencePort, times(1)).existsById(testEcommerceId);
    }
    
    @Test
    void testValidateEcommerceExists_nullId_throwsException() {
        // Act & Assert
        EcommerceNotFoundException exception = assertThrows(EcommerceNotFoundException.class,
            () -> ecommerceService.validateEcommerceExists(null)
        );
        assertTrue(exception.getMessage().contains("null"));
        
        verify(persistencePort, never()).existsById(any());
    }
    
    // ==================== listEcommerces() Tests ====================
    
    @Test
    void testListEcommerces_allEcommerces() {
        // Arrange
        EcommerceEntity entity1 = EcommerceEntity.builder()
                .id(UUID.randomUUID())
                .name("Store 1")
                .slug("store-1")
                .status(EcommerceStatus.ACTIVE)
                .build();
        
        EcommerceEntity entity2 = EcommerceEntity.builder()
                .id(UUID.randomUUID())
                .name("Store 2")
                .slug("store-2")
                .status(EcommerceStatus.INACTIVE)
                .build();
        
        List<EcommerceEntity> entities = List.of(entity1, entity2);
        Page<EcommerceEntity> page = new PageImpl<>(entities);
        
        when(persistencePort.findAll(isNull(), any())).thenReturn(page);
        
        // Act
        Page<EcommerceResponse> response = ecommerceService.listEcommerces(null, 0, 50);
        
        // Assert
        assertNotNull(response);
        assertEquals(2, response.getContent().size());
        
        verify(persistencePort, times(1)).findAll(isNull(), any());
    }
    
    @Test
    void testListEcommerces_filterByStatus() {
        // Arrange
        EcommerceEntity entity = EcommerceEntity.builder()
                .id(UUID.randomUUID())
                .name("Store 1")
                .slug("store-1")
                .status(EcommerceStatus.ACTIVE)
                .build();
        
        List<EcommerceEntity> entities = List.of(entity);
        Page<EcommerceEntity> page = new PageImpl<>(entities);
        
        when(persistencePort.findAll(any(), any())).thenReturn(page);
        
        // Act
        Page<EcommerceResponse> response = ecommerceService.listEcommerces("ACTIVE", 0, 50);
        
        // Assert
        assertNotNull(response);
        assertEquals(1, response.getContent().size());
        assertEquals("ACTIVE", response.getContent().get(0).status());
        
        verify(persistencePort, times(1)).findAll(any(), any());
    }
    
    @Test
    void testListEcommerces_invalidStatus_throwsBadRequest() {
        // Act & Assert
        BadRequestException exception = assertThrows(BadRequestException.class,
            () -> ecommerceService.listEcommerces("INVALID", 0, 50)
        );
        assertTrue(exception.getMessage().contains("inválido"));
        
        verify(persistencePort, never()).findAll(any(), any());
    }
}
