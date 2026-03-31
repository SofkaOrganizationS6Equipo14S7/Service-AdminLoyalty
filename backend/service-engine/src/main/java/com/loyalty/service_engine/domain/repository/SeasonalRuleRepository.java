package com.loyalty.service_engine.domain.repository;

import com.loyalty.service_engine.domain.entity.SeasonalRuleEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for SeasonalRuleEntity in Service-Engine
 * 
 * Used for:
 * - Cold start: loading all active rules into Caffeine cache
 * - Fallback: if cache miss, query database
 * - Event sync: updating/deleting replicated rules
 */
@Repository
public interface SeasonalRuleRepository extends JpaRepository<SeasonalRuleEntity, UUID> {

    /**
     * Find all active seasonal rules for an ecommerce
     * Used during cold start and cache fallback
     */
    List<SeasonalRuleEntity> findByEcommerceIdAndIsActiveTrue(UUID ecommerceId);

    /**
     * Find a specific rule by UID and ecommerce ID
     * Used during cache lookups
     */
    Optional<SeasonalRuleEntity> findByUidAndEcommerceId(UUID uid, UUID ecommerceId);

    /**
     * Find all active rules (across all ecommerces)
     * Used during full cache reload
     */
    List<SeasonalRuleEntity> findByIsActiveTrue();
}
