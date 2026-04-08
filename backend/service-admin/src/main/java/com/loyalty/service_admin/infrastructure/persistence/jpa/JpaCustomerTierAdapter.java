package com.loyalty.service_admin.infrastructure.persistence.jpa;

import com.loyalty.service_admin.application.port.out.CustomerTierPersistencePort;
import com.loyalty.service_admin.domain.entity.CustomerTierEntity;
import com.loyalty.service_admin.domain.repository.CustomerTierRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * JpaCustomerTierAdapter - Adapter de persistencia para CustomerTierService.
 *
 * Implementa CustomerTierPersistencePort delegando a CustomerTierRepository.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class JpaCustomerTierAdapter implements CustomerTierPersistencePort {

    private final CustomerTierRepository repository;

    @Override
    public CustomerTierEntity saveTier(CustomerTierEntity tier) {
        log.debug("Saving customer tier: {}", tier.getId());
        return repository.save(tier);
    }

    @Override
    public Optional<CustomerTierEntity> findTierById(UUID tierId) {
        log.debug("Finding customer tier by id: {}", tierId);
        return repository.findById(tierId);
    }

    @Override
    public List<CustomerTierEntity> findActivetiersOrderedByHierarchy() {
        log.debug("Finding active customer tiers ordered by hierarchy");
        return repository.findByIsActiveTrueOrderByHierarchyLevelAsc();
    }

    @Override
    public List<CustomerTierEntity> findAllTiersOrderedByHierarchy() {
        log.debug("Finding all customer tiers ordered by hierarchy");
        return repository.findAllByOrderByHierarchyLevelAsc();
    }

    @Override
    public Page<CustomerTierEntity> findTiersPaginated(Pageable pageable, Boolean isActive) {
        log.debug("Finding customer tiers paginated - isActive: {}", isActive);
        if (isActive == null) {
            return repository.findAll(pageable);
        }
        return isActive 
            ? repository.findByIsActiveTrueOrderByHierarchyLevelAsc(pageable)
            : repository.findByIsActiveFalseOrderByHierarchyLevelAsc(pageable);
    }

    @Override
    public boolean existsActiveTierWithNameAndEcommerce(UUID ecommerceId, String name) {
        log.debug("Checking if active tier exists with name: {} in ecommerce: {}", name, ecommerceId);
        return repository.existsByEcommerceIdAndNameAndIsActiveTrue(ecommerceId, name);
    }

    @Override
    public boolean existsTierWithName(UUID ecommerceId, String name) {
        log.debug("Checking if tier exists with name: {} in ecommerce: {}", name, ecommerceId);
        return repository.existsByEcommerceIdAndName(ecommerceId, name);
    }

    @Override
    public void deleteTier(CustomerTierEntity tier) {
        log.debug("Soft-deleting customer tier: {}", tier.getId());
        tier.setIsActive(false);
        repository.save(tier);
    }
}
