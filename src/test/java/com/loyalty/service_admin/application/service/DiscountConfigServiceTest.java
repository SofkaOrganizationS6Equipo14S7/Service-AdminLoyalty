package com.loyalty.service_admin.application.service;

import com.loyalty.service_admin.application.dto.rules.discount.DiscountConfigCreateRequest;
import com.loyalty.service_admin.application.dto.rules.discount.DiscountConfigResponse;
import com.loyalty.service_admin.application.port.out.DiscountConfigEventPort;
import com.loyalty.service_admin.application.port.out.DiscountConfigPersistencePort;
import com.loyalty.service_admin.domain.entity.DiscountSettingsEntity;
import com.loyalty.service_admin.domain.repository.DiscountLimitPriorityRepository;
import com.loyalty.service_admin.infrastructure.exception.BadRequestException;
import com.loyalty.service_admin.infrastructure.exception.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("DiscountConfigService Unit Tests")
class DiscountConfigServiceTest {

    @Mock private DiscountConfigPersistencePort persistencePort;
    @Mock private DiscountConfigEventPort eventPort;
    @Mock private DiscountLimitPriorityRepository priorityRepository;

    @InjectMocks
    private DiscountConfigService discountConfigService;

    private UUID ecommerceId;
    private DiscountSettingsEntity savedEntity;

    @BeforeEach
    void setUp() {
        ecommerceId = UUID.randomUUID();

        savedEntity = new DiscountSettingsEntity();
        savedEntity.setId(UUID.randomUUID());
        savedEntity.setEcommerceId(ecommerceId);
        savedEntity.setMaxDiscountCap(new BigDecimal("100.00"));
        savedEntity.setCurrencyCode("USD");
        savedEntity.setAllowStacking(true);
        savedEntity.setRoundingRule("ROUND_HALF_UP");
        savedEntity.setIsActive(true);
        savedEntity.setVersion(1L);
        savedEntity.setCreatedAt(Instant.now());
        savedEntity.setUpdatedAt(Instant.now());
    }

    @Test
    @DisplayName("testUpdateConfig_Success_NoPreviousConfig")
    void testUpdateConfig_Success_NoPreviousConfig() {
        // Arrange
        DiscountConfigCreateRequest request = new DiscountConfigCreateRequest(
                ecommerceId, new BigDecimal("100.00"), "USD", true, "ROUND_HALF_UP");
        when(persistencePort.findActiveConfigByEcommerce(ecommerceId)).thenReturn(Optional.empty());
        when(persistencePort.saveConfig(any(DiscountSettingsEntity.class))).thenReturn(savedEntity);

        // Act
        DiscountConfigResponse response = discountConfigService.updateConfig(request);

        // Assert
        assertNotNull(response);
        assertEquals(ecommerceId, response.ecommerceId());
        assertEquals("USD", response.currencyCode());
        verify(persistencePort).saveConfig(any(DiscountSettingsEntity.class));
        verify(eventPort).publishConfigUpdated(any(DiscountSettingsEntity.class), eq(ecommerceId));
    }

    @Test
    @DisplayName("testUpdateConfig_Success_DeactivatesPrevious")
    void testUpdateConfig_Success_DeactivatesPrevious() {
        // Arrange
        DiscountSettingsEntity oldConfig = new DiscountSettingsEntity();
        oldConfig.setId(UUID.randomUUID());
        oldConfig.setIsActive(true);

        DiscountConfigCreateRequest request = new DiscountConfigCreateRequest(
                ecommerceId, new BigDecimal("200.00"), "EUR", false, "ROUND_DOWN");
        when(persistencePort.findActiveConfigByEcommerce(ecommerceId)).thenReturn(Optional.of(oldConfig));
        when(persistencePort.saveConfig(any(DiscountSettingsEntity.class))).thenAnswer(inv -> {
            DiscountSettingsEntity arg = inv.getArgument(0);
            if (arg == oldConfig) return oldConfig;
            return savedEntity;
        });

        // Act
        DiscountConfigResponse response = discountConfigService.updateConfig(request);

        // Assert
        assertNotNull(response);
        assertFalse(oldConfig.getIsActive());
        verify(persistencePort, times(2)).saveConfig(any(DiscountSettingsEntity.class));
    }

    @Test
    @DisplayName("testUpdateConfig_InvalidCurrencyCode_ThrowsBadRequest")
    void testUpdateConfig_InvalidCurrencyCode_ThrowsBadRequest() {
        // Arrange
        DiscountConfigCreateRequest request = new DiscountConfigCreateRequest(
                ecommerceId, new BigDecimal("100.00"), "AB", true, null);

        // Act & Assert
        assertThrows(BadRequestException.class,
                () -> discountConfigService.updateConfig(request));
    }

    @Test
    @DisplayName("testUpdateConfig_NullCurrencyCode_ThrowsBadRequest")
    void testUpdateConfig_NullCurrencyCode_ThrowsBadRequest() {
        // Arrange
        DiscountConfigCreateRequest request = new DiscountConfigCreateRequest(
                ecommerceId, new BigDecimal("100.00"), null, true, null);

        // Act & Assert
        assertThrows(BadRequestException.class,
                () -> discountConfigService.updateConfig(request));
    }

    @Test
    @DisplayName("testGetActiveConfig_Success")
    void testGetActiveConfig_Success() {
        // Arrange
        when(persistencePort.findActiveConfigByEcommerce(ecommerceId)).thenReturn(Optional.of(savedEntity));

        // Act
        DiscountConfigResponse response = discountConfigService.getActiveConfig(ecommerceId);

        // Assert
        assertNotNull(response);
        assertEquals(savedEntity.getId(), response.uid());
        assertEquals(ecommerceId, response.ecommerceId());
        assertTrue(response.isActive());
    }

    @Test
    @DisplayName("testGetActiveConfig_NotFound_ThrowsResourceNotFoundException")
    void testGetActiveConfig_NotFound_ThrowsResourceNotFoundException() {
        // Arrange
        when(persistencePort.findActiveConfigByEcommerce(ecommerceId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(ResourceNotFoundException.class,
                () -> discountConfigService.getActiveConfig(ecommerceId));
    }

    @Test
    @DisplayName("testGetActiveConfigEntity_Success")
    void testGetActiveConfigEntity_Success() {
        // Arrange
        when(persistencePort.findActiveConfigByEcommerce(ecommerceId)).thenReturn(Optional.of(savedEntity));

        // Act
        DiscountSettingsEntity result = discountConfigService.getActiveConfigEntity(ecommerceId);

        // Assert
        assertNotNull(result);
        assertEquals(savedEntity.getId(), result.getId());
    }

    @Test
    @DisplayName("testGetActiveConfigEntity_NotFound_ThrowsResourceNotFoundException")
    void testGetActiveConfigEntity_NotFound_ThrowsResourceNotFoundException() {
        // Arrange
        when(persistencePort.findActiveConfigByEcommerce(ecommerceId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(ResourceNotFoundException.class,
                () -> discountConfigService.getActiveConfigEntity(ecommerceId));
    }

    @Test
    @DisplayName("testUpdateConfig_NullDefaults_AppliedCorrectly")
    void testUpdateConfig_NullDefaults_AppliedCorrectly() {
        // Arrange
        DiscountConfigCreateRequest request = new DiscountConfigCreateRequest(
                ecommerceId, new BigDecimal("50.00"), "USD", null, null);
        when(persistencePort.findActiveConfigByEcommerce(ecommerceId)).thenReturn(Optional.empty());
        when(persistencePort.saveConfig(any(DiscountSettingsEntity.class))).thenAnswer(inv -> {
            DiscountSettingsEntity entity = inv.getArgument(0);
            entity.setId(UUID.randomUUID());
            entity.setCreatedAt(Instant.now());
            entity.setUpdatedAt(Instant.now());
            return entity;
        });

        // Act
        DiscountConfigResponse response = discountConfigService.updateConfig(request);

        // Assert
        assertNotNull(response);
        verify(persistencePort).saveConfig(argThat(entity ->
                entity.getAllowStacking() == true && "ROUND_HALF_UP".equals(entity.getRoundingRule())));
    }
}
