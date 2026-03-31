package com.loyalty.service_engine.domain.repository;

import com.loyalty.service_engine.domain.entity.ClassificationRuleReplicaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Repository for ClassificationRuleReplicaEntity.
 * Read-only access to replicated rules from service-admin.
 * Used for Cold Start and caché population.
 */
@Repository
public interface ClassificationRuleReplicaRepository extends JpaRepository<ClassificationRuleReplicaEntity, UUID> {

    /**
     * Find all active rules for a specific tier, ordered by priority ascending.
     */
    List<ClassificationRuleReplicaEntity> findByTierUidAndIsActiveTrueOrderByPriorityAsc(UUID tierUid);

    /**
     * Find all active rules, ordered by priority.
     */
    List<ClassificationRuleReplicaEntity> findByIsActiveTrueOrderByPriorityAsc();

    /**
     * Find all rules for a specific metric type and active status, ordered by priority.
     */
    List<ClassificationRuleReplicaEntity> findByMetricTypeAndIsActiveTrueOrderByPriorityAsc(String metricType);

    /**
     * Find all rules grouped by tier for bulk loading into caché.
     */
    @Query("SELECT r FROM ClassificationRuleReplicaEntity r " +
           "WHERE r.isActive = true " +
           "ORDER BY r.tierUid ASC, r.priority ASC")
    List<ClassificationRuleReplicaEntity> findAllActiveRulesForCache();
}
