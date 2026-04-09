package com.loyalty.service_admin.application.service;

import com.loyalty.service_admin.application.dto.customertier.CustomerTierCreateRequest;
import com.loyalty.service_admin.application.dto.customertier.CustomerTierResponse;
import com.loyalty.service_admin.application.dto.customertier.CustomerTierUpdateRequest;
import com.loyalty.service_admin.application.port.out.CustomerTierEventPort;
import com.loyalty.service_admin.application.port.out.CustomerTierPersistencePort;
import com.loyalty.service_admin.domain.entity.CustomerTierEntity;
import com.loyalty.service_admin.infrastructure.exception.BadRequestException;
import com.loyalty.service_admin.infrastructure.exception.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("CustomerTierService Unit Tests")
class CustomerTierServiceTest {

    @Mock private CustomerTierPersistencePort persistencePort;
    @Mock private CustomerTierEventPort eventPort;

    @InjectMocks
    private CustomerTierService customerTierService;

    private UUID ecommerceId;
    private UUID tierId;
    private CustomerTierEntity tierEntity;

    @BeforeEach
    void setUp() {
        ecommerceId = UUID.randomUUID();
        tierId = UUID.randomUUID();

        tierEntity = CustomerTierEntity.builder()
                .id(tierId)
                .ecommerceId(ecommerceId)
                .name("Gold")
                .hierarchyLevel(1)
                .isActive(true)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
    }

    @Test
    @DisplayName("testCreate_Success")
    void testCreate_Success() {
        // Arrange
        CustomerTierCreateRequest request = new CustomerTierCreateRequest(ecommerceId, "Gold", 1);
        when(persistencePort.existsActiveTierWithNameAndEcommerce(ecommerceId, "Gold")).thenReturn(false);
        when(persistencePort.saveTier(any(CustomerTierEntity.class))).thenReturn(tierEntity);

        // Act
        CustomerTierResponse result = customerTierService.create(request);

        // Assert
        assertNotNull(result);
        assertEquals("Gold", result.name());
        assertEquals(1, result.hierarchyLevel());
        verify(eventPort).publishTierCreated(any(CustomerTierEntity.class), eq(ecommerceId));
    }

    @Test
    @DisplayName("testCreate_DuplicateName_ThrowsBadRequest")
    void testCreate_DuplicateName_ThrowsBadRequest() {
        // Arrange
        CustomerTierCreateRequest request = new CustomerTierCreateRequest(ecommerceId, "Gold", 1);
        when(persistencePort.existsActiveTierWithNameAndEcommerce(ecommerceId, "Gold")).thenReturn(true);

        // Act & Assert
        assertThrows(BadRequestException.class, () -> customerTierService.create(request));
    }

    @Test
    @DisplayName("testGetById_Success")
    void testGetById_Success() {
        // Arrange
        when(persistencePort.findTierById(tierId)).thenReturn(Optional.of(tierEntity));

        // Act
        CustomerTierResponse result = customerTierService.getById(tierId);

        // Assert
        assertNotNull(result);
        assertEquals(tierId, result.id());
    }

    @Test
    @DisplayName("testGetById_NotFound_ThrowsResourceNotFoundException")
    void testGetById_NotFound_ThrowsResourceNotFoundException() {
        // Arrange
        when(persistencePort.findTierById(tierId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(ResourceNotFoundException.class, () -> customerTierService.getById(tierId));
    }

    @Test
    @DisplayName("testListActive_Success")
    void testListActive_Success() {
        // Arrange
        when(persistencePort.findActivetiersOrderedByHierarchy()).thenReturn(List.of(tierEntity));

        // Act
        List<CustomerTierResponse> result = customerTierService.listActive();

        // Assert
        assertEquals(1, result.size());
    }

    @Test
    @DisplayName("testListAll_Success")
    void testListAll_Success() {
        // Arrange
        when(persistencePort.findAllTiersOrderedByHierarchy()).thenReturn(List.of(tierEntity));

        // Act
        List<CustomerTierResponse> result = customerTierService.listAll();

        // Assert
        assertEquals(1, result.size());
    }

    @Test
    @DisplayName("testDelete_Success")
    void testDelete_Success() {
        // Arrange
        when(persistencePort.findTierById(tierId)).thenReturn(Optional.of(tierEntity));

        // Act
        customerTierService.delete(tierId);

        // Assert
        verify(persistencePort).deleteTier(tierEntity);
        verify(eventPort).publishTierDeleted(tierId, ecommerceId);
    }

    @Test
    @DisplayName("testDelete_NotFound_ThrowsResourceNotFoundException")
    void testDelete_NotFound_ThrowsResourceNotFoundException() {
        // Arrange
        when(persistencePort.findTierById(tierId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(ResourceNotFoundException.class, () -> customerTierService.delete(tierId));
    }

    @Test
    @DisplayName("testListPaginated_Success")
    void testListPaginated_Success() {
        // Arrange
        Pageable pageable = PageRequest.of(0, 10);
        when(persistencePort.findTiersPaginated(pageable, true)).thenReturn(new PageImpl<>(List.of(tierEntity)));

        // Act
        Page<CustomerTierResponse> result = customerTierService.listPaginated(pageable, true);

        // Assert
        assertEquals(1, result.getTotalElements());
    }

    @Test
    @DisplayName("testUpdate_Success")
    void testUpdate_Success() {
        // Arrange
        CustomerTierUpdateRequest request = new CustomerTierUpdateRequest("Platinum", 2);
        when(persistencePort.findTierById(tierId)).thenReturn(Optional.of(tierEntity));
        when(persistencePort.existsActiveTierWithNameAndEcommerce(ecommerceId, "Platinum")).thenReturn(false);
        when(persistencePort.saveTier(any(CustomerTierEntity.class))).thenReturn(tierEntity);

        // Act
        CustomerTierResponse result = customerTierService.update(tierId, request);

        // Assert
        assertNotNull(result);
        verify(eventPort).publishTierUpdated(any(CustomerTierEntity.class), eq(ecommerceId));
    }

    @Test
    @DisplayName("testUpdate_DuplicateName_ThrowsBadRequest")
    void testUpdate_DuplicateName_ThrowsBadRequest() {
        // Arrange
        CustomerTierUpdateRequest request = new CustomerTierUpdateRequest("Silver", 2);
        when(persistencePort.findTierById(tierId)).thenReturn(Optional.of(tierEntity));
        when(persistencePort.existsActiveTierWithNameAndEcommerce(ecommerceId, "Silver")).thenReturn(true);

        // Act & Assert
        assertThrows(BadRequestException.class, () -> customerTierService.update(tierId, request));
    }

    @Test
    @DisplayName("testUpdate_NotFound_ThrowsResourceNotFoundException")
    void testUpdate_NotFound_ThrowsResourceNotFoundException() {
        // Arrange
        CustomerTierUpdateRequest request = new CustomerTierUpdateRequest("Platinum", 2);
        when(persistencePort.findTierById(tierId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(ResourceNotFoundException.class, () -> customerTierService.update(tierId, request));
    }

    @Test
    @DisplayName("testActivate_Success")
    void testActivate_Success() {
        // Arrange
        tierEntity.setIsActive(false);
        when(persistencePort.findTierById(tierId)).thenReturn(Optional.of(tierEntity));
        when(persistencePort.saveTier(any(CustomerTierEntity.class))).thenReturn(tierEntity);

        // Act
        CustomerTierResponse result = customerTierService.activate(tierId);

        // Assert
        assertNotNull(result);
        verify(eventPort).publishTierActivated(any(CustomerTierEntity.class), eq(ecommerceId));
    }

    @Test
    @DisplayName("testActivate_NotFound_ThrowsResourceNotFoundException")
    void testActivate_NotFound_ThrowsResourceNotFoundException() {
        // Arrange
        when(persistencePort.findTierById(tierId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(ResourceNotFoundException.class, () -> customerTierService.activate(tierId));
    }
}
