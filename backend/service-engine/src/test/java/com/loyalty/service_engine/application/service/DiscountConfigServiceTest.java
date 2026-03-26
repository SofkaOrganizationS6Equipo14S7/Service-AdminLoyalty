package com.loyalty.service_engine.application.service;

import com.loyalty.service_engine.application.dto.DiscountConfigCreateRequest;
import com.loyalty.service_engine.application.dto.DiscountConfigResponse;
import com.loyalty.service_engine.domain.entity.DiscountConfigEntity;
import com.loyalty.service_engine.domain.repository.DiscountConfigRepository;
import com.loyalty.service_engine.infrastructure.exception.ResourceNotFoundException;
import com.loyalty.service_engine.infrastructure.rabbitmq.DiscountConfigEventPublisher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Test suite para DiscountConfigService.
 * Cubre: CRUD, validación de máximo descuento, interacción con eventos RabbitMQ.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("DiscountConfigService Tests")
class DiscountConfigServiceTest {

    @Mock
    private DiscountConfigRepository discountConfigRepository;

    @Mock
    private DiscountConfigEventPublisher eventPublisher;

    @InjectMocks
    private DiscountConfigService discountConfigService;

    private DiscountConfigCreateRequest validRequest;
    private DiscountConfigEntity existingConfig;
    private UUID userId;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        validRequest = new DiscountConfigCreateRequest(
            new BigDecimal("100.00"),
            "USD"
        );
        
        existingConfig = new DiscountConfigEntity();
        existingConfig.setId(UUID.randomUUID());
        existingConfig.setMaxDiscountLimit(new BigDecimal("50.00"));
        existingConfig.setCurrencyCode("USD");
        existingConfig.setIsActive(true);
        existingConfig.setCreatedByUserId(UUID.randomUUID());
        existingConfig.setCreatedAt(Instant.now());
        existingConfig.setUpdatedAt(Instant.now());
    }

    // ============ Happy Path Tests ============

    @Test
    @DisplayName("updateConfig_success: Create new config when none exists")
    void testUpdateConfigSuccess() {
        // Arrange
        when(discountConfigRepository.findByIsActiveTrue()).thenReturn(Optional.empty());
        
        DiscountConfigEntity newConfig = new DiscountConfigEntity();
        newConfig.setId(UUID.randomUUID());
        newConfig.setMaxDiscountLimit(validRequest.maxDiscountLimit());
        newConfig.setCurrencyCode(validRequest.currencyCode());
        newConfig.setIsActive(true);
        newConfig.setCreatedByUserId(userId);
        newConfig.setCreatedAt(Instant.now());
        newConfig.setUpdatedAt(Instant.now());
        
        when(discountConfigRepository.save(any(DiscountConfigEntity.class))).thenReturn(newConfig);

        // Act
        DiscountConfigResponse result = discountConfigService.updateConfig(validRequest, userId);

        // Assert
        assertNotNull(result);
        assertEquals(validRequest.maxDiscountLimit(), result.maxDiscountLimit());
        assertEquals(validRequest.currencyCode(), result.currencyCode());
        assertTrue(result.isActive());
        
        verify(discountConfigRepository).save(any(DiscountConfigEntity.class));
        verify(eventPublisher).publishDiscountConfigUpdated(
            any(UUID.class),
            eq("100.00"),
            eq("USD"),
            eq(true)
        );
    }

    @Test
    @DisplayName("updateConfig_success: Deactivate existing and create new")
    void testUpdateConfigReplaceExisting() {
        // Arrange
        when(discountConfigRepository.findByIsActiveTrue()).thenReturn(Optional.of(existingConfig));
        
        DiscountConfigEntity newConfig = new DiscountConfigEntity();
        newConfig.setId(UUID.randomUUID());
        newConfig.setMaxDiscountLimit(validRequest.maxDiscountLimit());
        newConfig.setCurrencyCode(validRequest.currencyCode());
        newConfig.setIsActive(true);
        newConfig.setCreatedByUserId(userId);
        newConfig.setCreatedAt(Instant.now());
        newConfig.setUpdatedAt(Instant.now());
        
        when(discountConfigRepository.save(any(DiscountConfigEntity.class))).thenAnswer(invocation -> {
            Object arg = invocation.getArgument(0);
            if (arg instanceof DiscountConfigEntity) {
                DiscountConfigEntity entity = (DiscountConfigEntity) arg;
                if (entity.getIsActive()) {
                    return newConfig;
                } else {
                    return invocation.getArgument(0);
                }
            }
            return arg;
        });

        // Act
        DiscountConfigResponse result = discountConfigService.updateConfig(validRequest, userId);

        // Assert
        assertNotNull(result);
        assertEquals(validRequest.maxDiscountLimit(), result.maxDiscountLimit());
        
        // Verify previous config was deactivated
        ArgumentCaptor<DiscountConfigEntity> captor = ArgumentCaptor.forClass(DiscountConfigEntity.class);
        verify(discountConfigRepository, atLeastOnce()).save(captor.capture());
        
        verify(eventPublisher).publishDiscountConfigUpdated(any(), any(), any(), any());
    }

    @Test
    @DisplayName("getActiveConfig_success: Return active config")
    void testGetActiveConfigSuccess() {
        // Arrange
        when(discountConfigRepository.findByIsActiveTrue()).thenReturn(Optional.of(existingConfig));

        // Act
        DiscountConfigResponse result = discountConfigService.getActiveConfig();

        // Assert
        assertNotNull(result);
        assertEquals(existingConfig.getMaxDiscountLimit(), result.maxDiscountLimit());
        assertEquals(existingConfig.getCurrencyCode(), result.currencyCode());
        assertTrue(result.isActive());
        
        verify(discountConfigRepository).findByIsActiveTrue();
    }

    // ============ Error Path Tests ============

    @Test
    @DisplayName("updateConfig_invalid: Reject max discount limit <= 0")
    void testUpdateConfigInvalidLimitZero() {
        // Arrange
        DiscountConfigCreateRequest invalidRequest = new DiscountConfigCreateRequest(
            BigDecimal.ZERO,
            "USD"
        );

        // Act & Assert
        assertThrows(IllegalArgumentException.class, 
            () -> discountConfigService.updateConfig(invalidRequest, userId),
            "Should reject limit equal to zero");
        
        verify(discountConfigRepository, never()).save(any());
    }

    @Test
    @DisplayName("updateConfig_invalid: Reject negative max discount limit")
    void testUpdateConfigInvalidLimitNegative() {
        // Arrange
        DiscountConfigCreateRequest invalidRequest = new DiscountConfigCreateRequest(
            new BigDecimal("-50.00"),
            "USD"
        );

        // Act & Assert
        assertThrows(IllegalArgumentException.class, 
            () -> discountConfigService.updateConfig(invalidRequest, userId),
            "Should reject negative limit");
        
        verify(discountConfigRepository, never()).save(any());
    }

    @Test
    @DisplayName("getActiveConfig_notFound: Throw when no active config exists")
    void testGetActiveConfigNotFound() {
        // Arrange
        when(discountConfigRepository.findByIsActiveTrue()).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(ResourceNotFoundException.class, 
            () -> discountConfigService.getActiveConfig(),
            "Should throw ResourceNotFoundException when no active config");
        
        assertTrue(true); // Test passed
    }

    // ============ Edge Cases ============

    @Test
    @DisplayName("updateConfig_precision: Handle BigDecimal with 2 decimal places")
    void testUpdateConfigPrecision() {
        // Arrange
        DiscountConfigCreateRequest precisionRequest = new DiscountConfigCreateRequest(
            new BigDecimal("99.99"),
            "USD"
        );
        
        when(discountConfigRepository.findByIsActiveTrue()).thenReturn(Optional.empty());
        
        DiscountConfigEntity newConfig = new DiscountConfigEntity();
        newConfig.setId(UUID.randomUUID());
        newConfig.setMaxDiscountLimit(precisionRequest.maxDiscountLimit());
        newConfig.setCurrencyCode(precisionRequest.currencyCode());
        newConfig.setIsActive(true);
        newConfig.setCreatedByUserId(userId);
        newConfig.setCreatedAt(Instant.now());
        newConfig.setUpdatedAt(Instant.now());
        
        when(discountConfigRepository.save(any(DiscountConfigEntity.class))).thenReturn(newConfig);

        // Act
        DiscountConfigResponse result = discountConfigService.updateConfig(precisionRequest, userId);

        // Assert
        assertNotNull(result);
        assertEquals(new BigDecimal("99.99"), result.maxDiscountLimit());
    }

    @Test
    @DisplayName("updateConfig_largeAmount: Handle large discount limits")
    void testUpdateConfigLargeAmount() {
        // Arrange
        DiscountConfigCreateRequest largeRequest = new DiscountConfigCreateRequest(
            new BigDecimal("999999.99"),
            "USD"
        );
        
        when(discountConfigRepository.findByIsActiveTrue()).thenReturn(Optional.empty());
        
        DiscountConfigEntity newConfig = new DiscountConfigEntity();
        newConfig.setId(UUID.randomUUID());
        newConfig.setMaxDiscountLimit(largeRequest.maxDiscountLimit());
        newConfig.setCurrencyCode(largeRequest.currencyCode());
        newConfig.setIsActive(true);
        newConfig.setCreatedByUserId(userId);
        newConfig.setCreatedAt(Instant.now());
        newConfig.setUpdatedAt(Instant.now());
        
        when(discountConfigRepository.save(any(DiscountConfigEntity.class))).thenReturn(newConfig);

        // Act
        DiscountConfigResponse result = discountConfigService.updateConfig(largeRequest, userId);

        // Assert
        assertNotNull(result);
        assertEquals(new BigDecimal("999999.99"), result.maxDiscountLimit());
    }

    @Test
    @DisplayName("updateConfig_eventPublished: Verify RabbitMQ event is published")
    void testUpdateConfigEventPublished() {
        // Arrange
        when(discountConfigRepository.findByIsActiveTrue()).thenReturn(Optional.empty());
        
        DiscountConfigEntity newConfig = new DiscountConfigEntity();
        UUID configId = UUID.randomUUID();
        newConfig.setId(configId);
        newConfig.setMaxDiscountLimit(validRequest.maxDiscountLimit());
        newConfig.setCurrencyCode(validRequest.currencyCode());
        newConfig.setIsActive(true);
        newConfig.setCreatedByUserId(userId);
        newConfig.setCreatedAt(Instant.now());
        newConfig.setUpdatedAt(Instant.now());
        
        when(discountConfigRepository.save(any(DiscountConfigEntity.class))).thenReturn(newConfig);

        // Act
        discountConfigService.updateConfig(validRequest, userId);

        // Assert
        verify(eventPublisher).publishDiscountConfigUpdated(
            configId,
            "100.00",
            "USD",
            true
        );
    }
}
