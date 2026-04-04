package com.loyalty.service_admin.domain.repository;

import com.loyalty.service_admin.domain.entity.ClassificationRuleEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

// @Repository - DEPRECATED: Migrated to generic Rule architecture
@org.springframework.data.repository.NoRepositoryBean
public interface ClassificationRuleRepository extends JpaRepository<ClassificationRuleEntity, UUID> {

    List<ClassificationRuleEntity> findByCustomerTierIdAndIsActiveTrueOrderByPriorityAsc(UUID customerTierId);

    List<ClassificationRuleEntity> findByIsActiveTrueOrderByPriorityAsc();

    List<ClassificationRuleEntity> findByCustomerTierIdOrderByPriorityAsc(UUID customerTierId);

    Optional<ClassificationRuleEntity> findByIdAndIsActiveTrue(UUID id);

    List<ClassificationRuleEntity> findByMetricTypeAndIsActiveTrueOrderByPriorityAsc(String metricType);

    @Query("SELECT CASE WHEN COUNT(r) > 0 THEN true ELSE false END " +
           "FROM ClassificationRuleEntity r " +
           "WHERE r.customerTierId = :customerTierId AND r.priority = :priority AND r.isActive = true")
    boolean existsByTierAndPriority(@Param("customerTierId") UUID customerTierId, @Param("priority") Integer priority);
}