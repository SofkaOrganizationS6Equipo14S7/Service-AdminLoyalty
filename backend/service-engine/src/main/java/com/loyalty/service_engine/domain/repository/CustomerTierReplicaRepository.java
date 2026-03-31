package com.loyalty.service_engine.domain.repository;

import com.loyalty.service_engine.domain.entity.CustomerTierReplicaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for CustomerTierReplicaEntity.
 * Read-only access to replicated tier data from service-admin.
 * Used for Cold Start and caché population.
 */
@Repository
public interface CustomerTierReplicaRepository extends JpaRepository<CustomerTierReplicaEntity, UUID> {

    /**
     * Find all active tiers, ordered by level ascending.
     */
    List<CustomerTierReplicaEntity> findByIsActiveTrueOrderByLevelAsc();

    /**
     * Find a tier by name and active status.
     */
    Optional<CustomerTierReplicaEntity> findByNameAndIsActiveTrue(String name);

    /**
     * Find all tiers (active and inactive) ordered by level.
     */
    List<CustomerTierReplicaEntity> findAllByOrderByLevelAsc();

    /**
     * Check if a tier with a given name exists.
     */
    boolean existsByNameAndIsActiveTrue(String name);
}
