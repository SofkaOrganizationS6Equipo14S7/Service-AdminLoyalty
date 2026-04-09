package com.loyalty.service_admin.infrastructure.persistence.jpa;

import com.loyalty.service_admin.domain.entity.CustomerTierEntity;
import com.loyalty.service_admin.domain.repository.CustomerTierRepository;
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

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("JpaCustomerTierAdapter Unit Tests")
class JpaCustomerTierAdapterTest {

    @Mock
    private CustomerTierRepository repository;

    @InjectMocks
    private JpaCustomerTierAdapter adapter;

    @Test
    void testSaveTier() {
        // Arrange
        CustomerTierEntity tier = new CustomerTierEntity();
        when(repository.save(tier)).thenReturn(tier);

        // Act
        CustomerTierEntity result = adapter.saveTier(tier);

        // Assert
        assertEquals(tier, result);
    }

    @Test
    void testFindTierById_found() {
        // Arrange
        UUID id = UUID.randomUUID();
        CustomerTierEntity tier = new CustomerTierEntity();
        when(repository.findById(id)).thenReturn(Optional.of(tier));

        // Act & Assert
        assertTrue(adapter.findTierById(id).isPresent());
    }

    @Test
    void testFindTierById_notFound() {
        // Arrange
        UUID id = UUID.randomUUID();
        when(repository.findById(id)).thenReturn(Optional.empty());

        // Act & Assert
        assertTrue(adapter.findTierById(id).isEmpty());
    }

    @Test
    void testFindActivetiersOrderedByHierarchy() {
        // Arrange
        List<CustomerTierEntity> tiers = List.of(new CustomerTierEntity());
        when(repository.findByIsActiveTrueOrderByHierarchyLevelAsc()).thenReturn(tiers);

        // Act
        List<CustomerTierEntity> result = adapter.findActivetiersOrderedByHierarchy();

        // Assert
        assertEquals(1, result.size());
    }

    @Test
    void testFindAllTiersOrderedByHierarchy() {
        // Arrange
        List<CustomerTierEntity> tiers = List.of(new CustomerTierEntity(), new CustomerTierEntity());
        when(repository.findAllByOrderByHierarchyLevelAsc()).thenReturn(tiers);

        // Act
        List<CustomerTierEntity> result = adapter.findAllTiersOrderedByHierarchy();

        // Assert
        assertEquals(2, result.size());
    }

    @Test
    void testFindTiersPaginated() {
        // Arrange
        Pageable pageable = PageRequest.of(0, 10);
        Page<CustomerTierEntity> page = new PageImpl<>(List.of(new CustomerTierEntity()));
        when(repository.findAll(pageable)).thenReturn(page);

        // Act
        Page<CustomerTierEntity> result = adapter.findTiersPaginated(pageable, true);

        // Assert
        assertEquals(1, result.getTotalElements());
    }

    @Test
    void testExistsActiveTierWithNameAndEcommerce() {
        // Arrange
        UUID ecommerceId = UUID.randomUUID();
        when(repository.existsByEcommerceIdAndNameAndIsActiveTrue(ecommerceId, "Gold")).thenReturn(true);

        // Act & Assert
        assertTrue(adapter.existsActiveTierWithNameAndEcommerce(ecommerceId, "Gold"));
    }

    @Test
    void testExistsTierWithName() {
        // Arrange
        UUID ecommerceId = UUID.randomUUID();
        when(repository.existsByEcommerceIdAndName(ecommerceId, "Silver")).thenReturn(false);

        // Act & Assert
        assertFalse(adapter.existsTierWithName(ecommerceId, "Silver"));
    }

    @Test
    void testDeleteTier_softDelete() {
        // Arrange
        CustomerTierEntity tier = new CustomerTierEntity();
        tier.setIsActive(true);
        when(repository.save(tier)).thenReturn(tier);

        // Act
        adapter.deleteTier(tier);

        // Assert
        assertFalse(tier.getIsActive());
        verify(repository).save(tier);
    }
}
