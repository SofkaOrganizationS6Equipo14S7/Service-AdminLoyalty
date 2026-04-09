package com.loyalty.service_admin.application.service;

import com.loyalty.service_admin.application.dto.rules.discount.DiscountLimitPriorityRequest;
import com.loyalty.service_admin.application.dto.rules.discount.DiscountLimitPriorityResponse;
import com.loyalty.service_admin.application.port.out.DiscountLimitPriorityEventPort;
import com.loyalty.service_admin.application.port.out.DiscountLimitPriorityPersistencePort;
import com.loyalty.service_admin.application.validation.DiscountPriorityValidator;
import com.loyalty.service_admin.domain.entity.DiscountPriorityEntity;
import com.loyalty.service_admin.domain.entity.DiscountTypeEntity;
import com.loyalty.service_admin.domain.repository.DiscountTypeRepository;
import com.loyalty.service_admin.infrastructure.exception.BadRequestException;
import com.loyalty.service_admin.infrastructure.exception.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("DiscountLimitPriorityService Unit Tests")
class DiscountLimitPriorityServiceTest {

    @Mock private DiscountLimitPriorityPersistencePort persistencePort;
    @Mock private DiscountLimitPriorityEventPort eventPort;
    @Mock private DiscountTypeRepository discountTypeRepository;
    @Mock private DiscountPriorityValidator priorityValidator;

    @InjectMocks
    private DiscountLimitPriorityService service;

    private UUID configId;
    private UUID typeId1;
    private UUID typeId2;

    @BeforeEach
    void setUp() {
        configId = UUID.randomUUID();
        typeId1 = UUID.randomUUID();
        typeId2 = UUID.randomUUID();
    }

    @Test
    @DisplayName("testSavePriorities_Success")
    void testSavePriorities_Success() {
        // Arrange
        DiscountLimitPriorityRequest request = new DiscountLimitPriorityRequest(configId,
                List.of(
                        new DiscountLimitPriorityRequest.PriorityEntry(typeId1, 1),
                        new DiscountLimitPriorityRequest.PriorityEntry(typeId2, 2)
                ));

        DiscountTypeEntity type1 = new DiscountTypeEntity();
        type1.setId(typeId1);
        type1.setCode("PRODUCT");
        DiscountTypeEntity type2 = new DiscountTypeEntity();
        type2.setId(typeId2);
        type2.setCode("SEASONAL");

        when(persistencePort.findPrioritiesByConfig(configId)).thenReturn(Collections.emptyList());
        when(discountTypeRepository.findById(typeId1)).thenReturn(Optional.of(type1));
        when(discountTypeRepository.findById(typeId2)).thenReturn(Optional.of(type2));

        DiscountPriorityEntity saved1 = DiscountPriorityEntity.builder()
                .id(UUID.randomUUID()).discountSettingId(configId)
                .discountTypeId(typeId1).priorityLevel(1)
                .createdAt(Instant.now()).updatedAt(Instant.now()).build();
        DiscountPriorityEntity saved2 = DiscountPriorityEntity.builder()
                .id(UUID.randomUUID()).discountSettingId(configId)
                .discountTypeId(typeId2).priorityLevel(2)
                .createdAt(Instant.now()).updatedAt(Instant.now()).build();
        when(persistencePort.savePriority(any(DiscountPriorityEntity.class)))
                .thenReturn(saved1).thenReturn(saved2);

        // Act
        DiscountLimitPriorityResponse response = service.savePriorities(request);

        // Assert
        assertNotNull(response);
        assertEquals(configId, response.discountSettingId());
        assertEquals(2, response.priorities().size());
        verify(priorityValidator).validatePriorities(request);
    }

    @Test
    @DisplayName("testSavePriorities_ClearsPreviousPriorities")
    void testSavePriorities_ClearsPreviousPriorities() {
        // Arrange
        DiscountPriorityEntity oldPriority = DiscountPriorityEntity.builder()
                .id(UUID.randomUUID()).discountSettingId(configId)
                .discountTypeId(typeId1).priorityLevel(1)
                .createdAt(Instant.now()).updatedAt(Instant.now()).build();

        DiscountLimitPriorityRequest request = new DiscountLimitPriorityRequest(configId,
                List.of(new DiscountLimitPriorityRequest.PriorityEntry(typeId1, 1)));

        when(persistencePort.findPrioritiesByConfig(configId)).thenReturn(List.of(oldPriority));
        when(discountTypeRepository.findById(typeId1)).thenReturn(Optional.of(new DiscountTypeEntity()));
        when(persistencePort.savePriority(any())).thenReturn(oldPriority);

        // Act
        service.savePriorities(request);

        // Assert
        verify(persistencePort).deletePriority(oldPriority.getId());
    }

    @Test
    @DisplayName("testSavePriorities_InvalidDiscountType_ThrowsBadRequest")
    void testSavePriorities_InvalidDiscountType_ThrowsBadRequest() {
        // Arrange
        DiscountLimitPriorityRequest request = new DiscountLimitPriorityRequest(configId,
                List.of(new DiscountLimitPriorityRequest.PriorityEntry(typeId1, 1)));

        when(persistencePort.findPrioritiesByConfig(configId)).thenReturn(Collections.emptyList());
        when(discountTypeRepository.findById(typeId1)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(BadRequestException.class, () -> service.savePriorities(request));
    }

    @Test
    @DisplayName("testGetPriorities_Success")
    void testGetPriorities_Success() {
        // Arrange
        DiscountPriorityEntity p1 = DiscountPriorityEntity.builder()
                .id(UUID.randomUUID()).discountSettingId(configId)
                .discountTypeId(typeId1).priorityLevel(1)
                .createdAt(Instant.now()).updatedAt(Instant.now()).build();
        when(persistencePort.findPrioritiesByConfig(configId)).thenReturn(List.of(p1));

        // Act
        DiscountLimitPriorityResponse response = service.getPriorities(configId);

        // Assert
        assertNotNull(response);
        assertEquals(configId, response.discountSettingId());
        assertEquals(1, response.priorities().size());
    }

    @Test
    @DisplayName("testGetPriorities_Empty_ThrowsResourceNotFoundException")
    void testGetPriorities_Empty_ThrowsResourceNotFoundException() {
        // Arrange
        when(persistencePort.findPrioritiesByConfig(configId)).thenReturn(Collections.emptyList());

        // Act & Assert
        assertThrows(ResourceNotFoundException.class, () -> service.getPriorities(configId));
    }

    @Test
    @DisplayName("testSavePriorities_EventPublishingFailure_DoesNotThrow")
    void testSavePriorities_EventPublishingFailure_DoesNotThrow() {
        // Arrange
        DiscountLimitPriorityRequest request = new DiscountLimitPriorityRequest(configId,
                List.of(new DiscountLimitPriorityRequest.PriorityEntry(typeId1, 1)));

        when(persistencePort.findPrioritiesByConfig(configId)).thenReturn(Collections.emptyList());
        when(discountTypeRepository.findById(typeId1)).thenReturn(Optional.of(new DiscountTypeEntity()));
        DiscountPriorityEntity saved = DiscountPriorityEntity.builder()
                .id(UUID.randomUUID()).discountSettingId(configId)
                .discountTypeId(typeId1).priorityLevel(1)
                .createdAt(Instant.now()).updatedAt(Instant.now()).build();
        when(persistencePort.savePriority(any())).thenReturn(saved);
        doThrow(new RuntimeException("RabbitMQ down"))
                .when(eventPort).publishPriorityUpdated(any(), any());

        // Act & Assert - should NOT throw despite event failure
        assertDoesNotThrow(() -> service.savePriorities(request));
    }
}
