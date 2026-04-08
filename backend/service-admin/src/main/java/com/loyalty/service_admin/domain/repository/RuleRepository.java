package com.loyalty.service_admin.domain.repository;

import com.loyalty.service_admin.domain.entity.RuleEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface RuleRepository extends JpaRepository<RuleEntity, UUID> {

    /**
     * Find all active rules for an ecommerce
     */
    Page<RuleEntity> findByEcommerceIdAndIsActiveTrueOrderByCreatedAtDesc(UUID ecommerceId, Pageable pageable);

    /**
     * Find all rules (active and inactive) for an ecommerce
     */
    Page<RuleEntity> findByEcommerceIdOrderByCreatedAtDesc(UUID ecommerceId, Pageable pageable);

    /**
     * Find rule by id and ecommerce (tenant isolation)
     */
    Optional<RuleEntity> findByIdAndEcommerceId(UUID id, UUID ecommerceId);

    /**
     * Find all active rules by discount priority
     */
    List<RuleEntity> findByDiscountPriorityIdAndIsActiveTrueOrderByCreatedAtDesc(UUID discountPriorityId);

    /**
     * Count active rules for an ecommerce
     */
    long countByEcommerceIdAndIsActiveTrue(UUID ecommerceId);

    /**
     * HU-08: Find active classification rules for ecommerce
     * 
     * Retorna todas las reglas activas del tipo CLASSIFICATION para un ecommerce.
     * Usado por los validadores de continuidad, jerarquía y unicidad.
     * 
     * @param ecommerceId UUID del ecommerce
     * @return Lista de RuleEntity filtradas
     */
    @Query("""
        SELECT r FROM RuleEntity r 
        JOIN DiscountPriorityEntity dp ON r.discountPriorityId = dp.id 
        JOIN DiscountTypeEntity dt ON dp.discountTypeId = dt.id 
        WHERE r.ecommerceId = :ecommerceId 
        AND r.isActive = true 
        AND dt.code = 'CLASSIFICATION'
        ORDER BY r.createdAt ASC
    """)
    List<RuleEntity> findActiveClassificationRulesByEcommerce(@Param("ecommerceId") UUID ecommerceId);
}
