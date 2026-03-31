package com.loyalty.service_admin.domain.repository;

import com.loyalty.service_admin.domain.entity.ClassificationRuleEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for ClassificationRuleEntity.
 * Manages classification rules that determine tier assignment.
 */
@Repository
public interface ClassificationRuleRepository extends JpaRepository<ClassificationRuleEntity, UUID> {

    /**
     * Find all active rules for a specific tier, ordered by priority ascending.
     */
    List<ClassificationRuleEntity> findByCustomerTierUidAndIsActiveTrueOrderByPriorityAsc(UUID tierUid);

    /**
     * Find all active rules, ordered by priority.
     */
    List<ClassificationRuleEntity> findByIsActiveTrueOrderByPriorityAsc();

    /**
     * Find all rules for a specific tier (active and inactive).
     */
    List<ClassificationRuleEntity> findByCustomerTierUidOrderByPriorityAsc(UUID tierUid);

    /**
     * Find a rule by uid and check if it's active.
     */
    Optional<ClassificationRuleEntity> findByUidAndIsActiveTrue(UUID uid);

    /**
     * Find all rules by metric type and active status.
     */
    List<ClassificationRuleEntity> findByMetricTypeAndIsActiveTrueOrderByPriorityAsc(String metricType);

    /**
     * Check if a priority already exists for a tier (to prevent duplicates).
     */
    @Query("SELECT CASE WHEN COUNT(r) > 0 THEN true ELSE false END " +
           "FROM ClassificationRuleEntity r " +
           "WHERE r.customerTierUid = :tierUid AND r.priority = :priority AND r.isActive = true")
    boolean existsByTierAndPriority(@Param("tierUid") UUID tierUid, @Param("priority") Integer priority);
}
