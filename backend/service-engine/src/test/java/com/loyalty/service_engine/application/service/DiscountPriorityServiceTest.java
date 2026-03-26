package com.loyalty.service_engine.application.service;

import com.loyalty.service_engine.application.dto.DiscountPriorityRequest;
import com.loyalty.service_engine.application.dto.DiscountPriorityResponse;
import com.loyalty.service_engine.domain.entity.DiscountConfigEntity;
import com.loyalty.service_engine.domain.entity.DiscountPriorityEntity;
import com.loyalty.service_engine.domain.repository.DiscountConfigRepository;
import com.loyalty.service_engine.domain.repository.DiscountPriorityRepository;
import com.loyalty.service_engine.infrastructure.exception.ResourceNotFoundException;
import com.loyalty.service_engine.infrastructure.exception.BadRequestException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Test suite para DiscountPriorityService.
 * Cubre: CRUD de prioridades, validación de secuencia (1..N), detección de duplicados.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("DiscountPriorityService Tests")
class DiscountPriorityServiceTest {

    @Mock
    private DiscountConfigRepository discountConfigRepository;

    @Mock
    private DiscountPriorityRepository discountPriorityRepository;

    @InjectMocks
    private DiscountPriorityService discountPriorityService;

    private UUID configId;
    private DiscountConfigEntity activeConfig;
    private List<DiscountPriorityRequest.DiscountPriorityItem> validPriorities;

    @BeforeEach
    void setUp() {
        configId = UUID.randomUUID();
        
        activeConfig = new DiscountConfigEntity();
        activeConfig.setId(configId);
        activeConfig.setMaxDiscountLimit(new BigDecimal("100.00"));
        activeConfig.setCurrencyCode("USD");
        activeConfig.setIsActive(true);
        activeConfig.setCreatedByUserId(UUID.randomUUID());
        activeConfig.setCreatedAt(Instant.now());
        activeConfig.setUpdatedAt(Instant.now());
        
        validPriorities = List.of(
            new DiscountPriorityRequest.DiscountPriorityItem("LOYALTY_POINTS", 1),
            new DiscountPriorityRequest.DiscountPriorityItem("COUPON", 2),
            new DiscountPriorityRequest.DiscountPriorityItem("BIRTHDAY", 3),
            new DiscountPriorityRequest.DiscountPriorityItem("SEASONAL", 4)
        );
    }

    // ============ Happy Path Tests ============

    @Test
    @DisplayName("savePriorities_success: Save valid sequential priorities")
    void testSavePrioritiesSuccess() {
        // Arrange
        DiscountPriorityRequest request = new DiscountPriorityRequest(configId.toString(), validPriorities);
        
        when(discountConfigRepository.findById(configId)).thenReturn(Optional.of(activeConfig));
        when(discountPriorityRepository.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        DiscountPriorityResponse result = discountPriorityService.savePriorities(request);

        // Assert
        assertNotNull(result);
        assertEquals(configId, UUID.fromString(result.discountConfigId()));
        assertEquals(4, result.priorities().size());
        
        verify(discountConfigRepository).findById(configId);
        verify(discountPriorityRepository).deleteByDiscountConfigId(configId);
        verify(discountPriorityRepository).saveAll(any());
    }

    @Test
    @DisplayName("getActivePriorities_success: Return active priorities in order")
    void testGetActivePrioritiesSuccess() {
        // Arrange
        List<DiscountPriorityEntity> priorityEntities = new ArrayList<>();
        for (var priority : validPriorities) {
            DiscountPriorityEntity entity = new DiscountPriorityEntity();
            entity.setId(UUID.randomUUID());
            entity.setDiscountConfigId(configId);
            entity.setDiscountType(priority.discountType());
            entity.setPriorityLevel(priority.priorityLevel());
            entity.setCreatedAt(Instant.now());
            priorityEntities.add(entity);
        }
        
        when(discountConfigRepository.findByIsActiveTrue()).thenReturn(Optional.of(activeConfig));
        when(discountPriorityRepository.findByDiscountConfigIdOrderByPriorityLevel(configId))
            .thenReturn(priorityEntities);

        // Act
        DiscountPriorityResponse result = discountPriorityService.getActivePriorities();

        // Assert
        assertNotNull(result);
        assertEquals(4, result.priorities().size());
        assertEquals("LOYALTY_POINTS", result.priorities().get(0).discountType());
        assertEquals("SEASONAL", result.priorities().get(3).discountType());
        
        verify(discountConfigRepository).findByIsActiveTrue();
        verify(discountPriorityRepository).findByDiscountConfigIdOrderByPriorityLevel(configId);
    }

    // ============ Validation Path Tests ============

    @Test
    @DisplayName("savePriorities_invalid: Reject duplicate priority levels")
    void testSavePrioritiesDuplicateLevels() {
        // Arrange
        List<DiscountPriorityRequest.DiscountPriorityItem> invalidPriorities = List.of(
            new DiscountPriorityRequest.DiscountPriorityItem("LOYALTY_POINTS", 1),
            new DiscountPriorityRequest.DiscountPriorityItem("COUPON", 1),  // Duplicate level!
            new DiscountPriorityRequest.DiscountPriorityItem("BIRTHDAY", 2)
        );
        DiscountPriorityRequest request = new DiscountPriorityRequest(configId.toString(), invalidPriorities);
        
        when(discountConfigRepository.findById(configId)).thenReturn(Optional.of(activeConfig));

        // Act & Assert
        assertThrows(BadRequestException.class, 
            () -> discountPriorityService.savePriorities(request),
            "Should reject duplicate priority levels");
        
        verify(discountConfigRepository).findById(configId);
        verify(discountPriorityRepository, never()).saveAll(any());
    }

    @Test
    @DisplayName("savePriorities_invalid: Reject non-sequential priority levels")
    void testSavePrioritiesNonSequential() {
        // Arrange
        List<DiscountPriorityRequest.DiscountPriorityItem> invalidPriorities = List.of(
            new DiscountPriorityRequest.DiscountPriorityItem("LOYALTY_POINTS", 1),
            new DiscountPriorityRequest.DiscountPriorityItem("COUPON", 3),      // Skips 2!
            new DiscountPriorityRequest.DiscountPriorityItem("BIRTHDAY", 5)     // Skips 4!
        );
        DiscountPriorityRequest request = new DiscountPriorityRequest(configId.toString(), invalidPriorities);
        
        when(discountConfigRepository.findById(configId)).thenReturn(Optional.of(activeConfig));

        // Act & Assert
        assertThrows(BadRequestException.class, 
            () -> discountPriorityService.savePriorities(request),
            "Should reject non-sequential priority levels");
        
        verify(discountPriorityRepository, never()).saveAll(any());
    }

    @Test
    @DisplayName("savePriorities_invalid: Reject duplicate discount types")
    void testSavePrioritiesDuplicateTypes() {
        // Arrange
        List<DiscountPriorityRequest.DiscountPriorityItem> invalidPriorities = List.of(
            new DiscountPriorityRequest.DiscountPriorityItem("LOYALTY_POINTS", 1),
            new DiscountPriorityRequest.DiscountPriorityItem("LOYALTY_POINTS", 2),  // Duplicate type!
            new DiscountPriorityRequest.DiscountPriorityItem("COUPON", 3)
        );
        DiscountPriorityRequest request = new DiscountPriorityRequest(configId.toString(), invalidPriorities);
        
        when(discountConfigRepository.findById(configId)).thenReturn(Optional.of(activeConfig));

        // Act & Assert
        assertThrows(BadRequestException.class, 
            () -> discountPriorityService.savePriorities(request),
            "Should reject duplicate discount types");
        
        verify(discountPriorityRepository, never()).saveAll(any());
    }

    @Test
    @DisplayName("savePriorities_configNotFound: Throw when config does not exist")
    void testSavePrioritiesConfigNotFound() {
        // Arrange
        DiscountPriorityRequest request = new DiscountPriorityRequest(configId.toString(), validPriorities);
        
        when(discountConfigRepository.findById(configId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(ResourceNotFoundException.class, 
            () -> discountPriorityService.savePriorities(request),
            "Should reject when config does not exist");
        
        verify(discountPriorityRepository, never()).saveAll(any());
    }

    @Test
    @DisplayName("getActivePriorities_notFound: Throw when no active config")
    void testGetActivePrioritiesNoActiveConfig() {
        // Arrange
        when(discountConfigRepository.findByIsActiveTrue()).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(ResourceNotFoundException.class, 
            () -> discountPriorityService.getActivePriorities(),
            "Should throw when no active config");
    }

    @Test
    @DisplayName("getActivePriorities_notFound: Throw when config has no priorities")
    void testGetActivePrioritiesNotFound() {
        // Arrange
        when(discountConfigRepository.findByIsActiveTrue()).thenReturn(Optional.of(activeConfig));
        when(discountPriorityRepository.findByDiscountConfigIdOrderByPriorityLevel(configId))
            .thenReturn(new ArrayList<>());  // Empty list

        // Act & Assert
        assertThrows(ResourceNotFoundException.class, 
            () -> discountPriorityService.getActivePriorities(),
            "Should throw when no priorities found");
    }

    // ============ Edge Cases ============

    @Test
    @DisplayName("savePriorities_single: Accept single priority")
    void testSavePrioritiesSingleItem() {
        // Arrange
        List<DiscountPriorityRequest.DiscountPriorityItem> singlePriority = List.of(
            new DiscountPriorityRequest.DiscountPriorityItem("LOYALTY_POINTS", 1)
        );
        DiscountPriorityRequest request = new DiscountPriorityRequest(configId.toString(), singlePriority);
        
        when(discountConfigRepository.findById(configId)).thenReturn(Optional.of(activeConfig));
        when(discountPriorityRepository.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        DiscountPriorityResponse result = discountPriorityService.savePriorities(request);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.priorities().size());
        assertEquals("LOYALTY_POINTS", result.priorities().get(0).discountType());
    }

    @Test
    @DisplayName("savePriorities_many: Accept many priorities in sequence")
    void testSavePrioritiesMany() {
        // Arrange
        List<DiscountPriorityRequest.DiscountPriorityItem> manyPriorities = new ArrayList<>();
        for (int i = 1; i <= 10; i++) {
            manyPriorities.add(new DiscountPriorityRequest.DiscountPriorityItem(
                "TYPE_" + i,
                i
            ));
        }
        DiscountPriorityRequest request = new DiscountPriorityRequest(configId.toString(), manyPriorities);
        
        when(discountConfigRepository.findById(configId)).thenReturn(Optional.of(activeConfig));
        when(discountPriorityRepository.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        DiscountPriorityResponse result = discountPriorityService.savePriorities(request);

        // Assert
        assertNotNull(result);
        assertEquals(10, result.priorities().size());
    }

    @Test
    @DisplayName("savePriorities_priority0: Reject priority level 0")
    void testSavePrioritiesPriorityZero() {
        // Arrange
        List<DiscountPriorityRequest.DiscountPriorityItem> invalidPriorities = List.of(
            new DiscountPriorityRequest.DiscountPriorityItem("LOYALTY_POINTS", 0),  // Should start at 1!
            new DiscountPriorityRequest.DiscountPriorityItem("COUPON", 1)
        );
        DiscountPriorityRequest request = new DiscountPriorityRequest(configId.toString(), invalidPriorities);
        
        when(discountConfigRepository.findById(configId)).thenReturn(Optional.of(activeConfig));

        // Act & Assert
        assertThrows(BadRequestException.class, 
            () -> discountPriorityService.savePriorities(request),
            "Should reject priority level 0 (must start at 1)");
    }

    @Test
    @DisplayName("getActivePrioritiesEntity_empty: Return empty list when no config")
    void testGetActivePrioritiesEntityEmpty() {
        // Arrange
        when(discountConfigRepository.findByIsActiveTrue()).thenReturn(Optional.empty());

        // Act
        List<DiscountPriorityEntity> result = discountPriorityService.getActivePrioritiesEntity();

        // Assert
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }
}
