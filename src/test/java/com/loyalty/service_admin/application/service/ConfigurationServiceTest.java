package com.loyalty.service_admin.application.service;

import com.loyalty.service_admin.application.dto.configuration.*;
import com.loyalty.service_admin.application.mapper.ConfigurationMapper;
import com.loyalty.service_admin.application.port.out.ConfigurationEventPort;
import com.loyalty.service_admin.application.port.out.ConfigurationPersistencePort;
import com.loyalty.service_admin.application.port.out.CurrentUserPort;
import com.loyalty.service_admin.application.validation.ConfigurationBusinessValidator;
import com.loyalty.service_admin.domain.entity.DiscountSettingsEntity;
import com.loyalty.service_admin.infrastructure.exception.AuthorizationException;
import com.loyalty.service_admin.infrastructure.exception.ConfigurationAlreadyExistsException;
import com.loyalty.service_admin.infrastructure.exception.ConfigurationNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ConfigurationService Unit Tests")
class ConfigurationServiceTest {

    @Mock private ConfigurationPersistencePort configurationRepository;
    @Mock private ConfigurationMapper mapper;
    @Mock private ConfigurationEventPort eventPublisher;
    @Mock private CurrentUserPort currentUserPort;
    @Mock private ConfigurationBusinessValidator validator;

    @InjectMocks
    private ConfigurationService configurationService;

    private UUID ecommerceId;
    private DiscountSettingsEntity entity;

    @BeforeEach
    void setUp() {
        ecommerceId = UUID.randomUUID();

        entity = new DiscountSettingsEntity();
        entity.setId(UUID.randomUUID());
        entity.setEcommerceId(ecommerceId);
        entity.setCurrencyCode("USD");
        entity.setRoundingRule("ROUND_HALF_UP");
        entity.setIsActive(true);
        entity.setVersion(1L);
        entity.setPriorities(new ArrayList<>());
        entity.setCreatedAt(Instant.now());
        entity.setUpdatedAt(Instant.now());
    }

    @Test
    @DisplayName("testCreateConfiguration_Success_SuperAdmin")
    void testCreateConfiguration_Success_SuperAdmin() {
        // Arrange
        ConfigurationCreateRequest request = mock(ConfigurationCreateRequest.class);
        when(request.ecommerceId()).thenReturn(ecommerceId);
        when(currentUserPort.isSuperAdmin()).thenReturn(true);
        when(configurationRepository.existsByEcommerceId(ecommerceId)).thenReturn(false);
        when(mapper.toEntity(request)).thenReturn(entity);
        when(configurationRepository.save(entity)).thenReturn(entity);
        ConfigurationWriteData writeData = new ConfigurationWriteData(entity.getId(), 1L);
        when(mapper.toWriteData(entity)).thenReturn(writeData);
        when(mapper.toUpdatedEvent(entity)).thenReturn(mock(ConfigurationUpdatedEvent.class));

        // Act
        ConfigurationWriteData result = configurationService.createConfiguration(request);

        // Assert
        assertNotNull(result);
        assertEquals(entity.getId(), result.configId());
        verify(validator).validateCreate(request);
        verify(configurationRepository).save(entity);
        verify(eventPublisher).publishConfigUpdated(any(ConfigurationUpdatedEvent.class));
    }

    @Test
    @DisplayName("testCreateConfiguration_AlreadyExists_ThrowsConflict")
    void testCreateConfiguration_AlreadyExists_ThrowsConflict() {
        // Arrange
        ConfigurationCreateRequest request = mock(ConfigurationCreateRequest.class);
        when(request.ecommerceId()).thenReturn(ecommerceId);
        when(currentUserPort.isSuperAdmin()).thenReturn(true);
        when(configurationRepository.existsByEcommerceId(ecommerceId)).thenReturn(true);

        // Act & Assert
        assertThrows(ConfigurationAlreadyExistsException.class,
                () -> configurationService.createConfiguration(request));
    }

    @Test
    @DisplayName("testCreateConfiguration_WrongEcommerce_ThrowsAuthorizationException")
    void testCreateConfiguration_WrongEcommerce_ThrowsAuthorizationException() {
        // Arrange
        UUID otherEcommerce = UUID.randomUUID();
        ConfigurationCreateRequest request = mock(ConfigurationCreateRequest.class);
        when(request.ecommerceId()).thenReturn(otherEcommerce);
        when(currentUserPort.isSuperAdmin()).thenReturn(false);
        when(currentUserPort.getCurrentUserEcommerceId()).thenReturn(ecommerceId);

        // Act & Assert
        assertThrows(AuthorizationException.class,
                () -> configurationService.createConfiguration(request));
    }

    @Test
    @DisplayName("testPatchConfiguration_Success")
    void testPatchConfiguration_Success() {
        // Arrange
        ConfigurationPatchRequest request = mock(ConfigurationPatchRequest.class);
        when(currentUserPort.isSuperAdmin()).thenReturn(true);
        when(configurationRepository.findByEcommerceId(ecommerceId)).thenReturn(Optional.of(entity));
        when(configurationRepository.save(entity)).thenReturn(entity);
        ConfigurationWriteData writeData = new ConfigurationWriteData(entity.getId(), 2L);
        when(mapper.toWriteData(entity)).thenReturn(writeData);
        when(mapper.toUpdatedEvent(entity)).thenReturn(mock(ConfigurationUpdatedEvent.class));

        // Act
        ConfigurationWriteData result = configurationService.patchConfiguration(ecommerceId, request);

        // Assert
        assertNotNull(result);
        verify(validator).validatePatch(request);
        verify(mapper).applyPatch(entity, request);
        verify(validator).validateEntityState(entity);
    }

    @Test
    @DisplayName("testPatchConfiguration_NotFound_ThrowsConfigurationNotFoundException")
    void testPatchConfiguration_NotFound_ThrowsConfigurationNotFoundException() {
        // Arrange
        ConfigurationPatchRequest request = mock(ConfigurationPatchRequest.class);
        when(currentUserPort.isSuperAdmin()).thenReturn(true);
        when(configurationRepository.findByEcommerceId(ecommerceId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(ConfigurationNotFoundException.class,
                () -> configurationService.patchConfiguration(ecommerceId, request));
    }

    @Test
    @DisplayName("testPatchConfiguration_WrongEcommerce_ThrowsAuthorizationException")
    void testPatchConfiguration_WrongEcommerce_ThrowsAuthorizationException() {
        // Arrange
        UUID otherEcommerce = UUID.randomUUID();
        ConfigurationPatchRequest request = mock(ConfigurationPatchRequest.class);
        when(currentUserPort.isSuperAdmin()).thenReturn(false);
        when(currentUserPort.getCurrentUserEcommerceId()).thenReturn(ecommerceId);

        // Act & Assert
        assertThrows(AuthorizationException.class,
                () -> configurationService.patchConfiguration(otherEcommerce, request));
    }
}
