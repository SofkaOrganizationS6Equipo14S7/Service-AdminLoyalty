package com.loyalty.service_admin.domain.repository;

import com.loyalty.service_admin.domain.entity.FidelityRangeEntity;
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
public interface FidelityRangeRepository extends JpaRepository<FidelityRangeEntity, UUID> {

    /**
     * Find all active fidelity ranges for an ecommerce, ordered by min_points ascending
     */
    List<FidelityRangeEntity> findByEcommerceIdAndIsActiveTrueOrderByMinPointsAsc(UUID ecommerceId);

    /**
     * Find all fidelity ranges for an ecommerce (active and inactive)
     */
    List<FidelityRangeEntity> findByEcommerceIdOrderByMinPointsAsc(UUID ecommerceId);

    /**
     * Find a specific range by uid and ecommerce_id (tenant isolation)
     */
    Optional<FidelityRangeEntity> findByUidAndEcommerceId(UUID uid, UUID ecommerceId);

    /**
     * Check if a range with the given name and ecommerce exists and is active
     */
    boolean existsByEcommerceIdAndNameAndIsActiveTrue(UUID ecommerceId, String name);

    /**
     * Find overlapping ranges: where minPoints <= searchMaxPoints AND maxPoints >= searchMinPoints
     */
    @Query("SELECT fr FROM FidelityRangeEntity fr WHERE fr.ecommerceId = :ecommerceId " +
           "AND fr.isActive = true " +
           "AND fr.minPoints <= :maxPoints AND fr.maxPoints >= :minPoints " +
           "AND (:excludeUid IS NULL OR fr.uid != :excludeUid)")
    List<FidelityRangeEntity> findOverlappingRanges(
        @Param("ecommerceId") UUID ecommerceId,
        @Param("minPoints") Integer minPoints,
        @Param("maxPoints") Integer maxPoints,
        @Param("excludeUid") UUID excludeUid
    );

    /**
     * Get paginated list of active ranges for an ecommerce
     */
    Page<FidelityRangeEntity> findByEcommerceIdAndIsActiveTrueOrderByMinPointsAsc(UUID ecommerceId, Pageable pageable);
}
