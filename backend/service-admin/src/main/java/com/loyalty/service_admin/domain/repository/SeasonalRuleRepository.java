package com.loyalty.service_admin.domain.repository;

import com.loyalty.service_admin.domain.entity.SeasonalRuleEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SeasonalRuleRepository extends JpaRepository<SeasonalRuleEntity, UUID> {

    /**
     * Find all active seasonal rules for an ecommerce (paginated)
     */
    Page<SeasonalRuleEntity> findByEcommerceIdAndIsActiveTrue(UUID ecommerceId, Pageable pageable);

    /**
     * Find all active seasonal rules for an ecommerce (non-paginated)
     */
    List<SeasonalRuleEntity> findByEcommerceIdAndIsActiveTrue(UUID ecommerceId);

    /**
     * Find a specific seasonal rule by uid and ecommerce_id
     */
    Optional<SeasonalRuleEntity> findByUidAndEcommerceId(UUID uid, UUID ecommerceId);

    /**
     * Check for overlapping date ranges (for validation)
     * Returns any active rules that overlap with the given date range
     * Optionally excludes a specific uid from the search
     */
    @Query("""
        SELECT sr FROM SeasonalRuleEntity sr
        WHERE sr.ecommerceId = :ecommerceId
        AND sr.isActive = true
        AND (sr.startDate, sr.endDate) OVERLAPS (CAST(:startDate AS java.time.Instant), CAST(:endDate AS java.time.Instant))
        AND (:excludeUid IS NULL OR sr.uid != :excludeUid)
    """)
    List<SeasonalRuleEntity> findOverlappingRules(
        @Param("ecommerceId") UUID ecommerceId,
        @Param("startDate") Instant startDate,
        @Param("endDate") Instant endDate,
        @Param("excludeUid") UUID excludeUid
    );
}
