package com.loyalty.service_admin.domain.repository;

import com.loyalty.service_admin.domain.entity.CustomerTierEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for CustomerTierEntity.
 * Manages tier definitions (source of truth).
 */
@Repository
public interface CustomerTierRepository extends JpaRepository<CustomerTierEntity, UUID> {

    /**
     * Find all active tiers, ordered by level ascending.
     */
    List<CustomerTierEntity> findByIsActiveTrueOrderByLevelAsc();

    /**
     * Find a tier by name.
     */
    Optional<CustomerTierEntity> findByNameAndIsActiveTrue(String name);

    /**
     * Find all tiers (active and inactive) ordered by level.
     */
    List<CustomerTierEntity> findAllByOrderByLevelAsc();

    /**
     * Check if a tier with a given name already exists (case-sensitive).
     */
    boolean existsByName(String name);
}
