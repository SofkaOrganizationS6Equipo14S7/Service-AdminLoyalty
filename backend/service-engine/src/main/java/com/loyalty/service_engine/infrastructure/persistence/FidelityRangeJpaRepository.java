package com.loyalty.service_engine.infrastructure.persistence;

import com.loyalty.service_engine.application.dto.FidelityRangeDTO;
import com.loyalty.service_engine.domain.entity.FidelityRangeEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Data access layer for fidelity_ranges replica table in Engine Service.
 * All queries map to DTOs to avoid persistence coupling.
 */
@Repository
public interface FidelityRangeJpaRepository extends JpaRepository<FidelityRangeEntity, UUID> {

    /**
     * Fetch all active fidelity ranges, sorted by min_points ascending.
     * Used by FidelityRangeStartupLoader for cold start initialization.
     *
     * @return List of FidelityRangeDTO, sorted by min_points
     */
    @Query("""
        SELECT new com.loyalty.service_engine.application.dto.FidelityRangeDTO(
            fr.uid, fr.ecommerceId, fr.name, fr.minPoints, fr.maxPoints,
            fr.discountPercentage, fr.isActive, fr.createdAt, fr.updatedAt
        )
        FROM FidelityRangeEntity fr
        WHERE fr.isActive = true
        ORDER BY fr.minPoints ASC
        """)
    List<FidelityRangeDTO> findAllActiveRangesSorted();

    /**
     * Fetch active ranges for a specific ecommerce.
     *
     * @param ecommerceId Tenant identifier
     * @return List of FidelityRangeDTO
     */
    @Query("""
        SELECT new com.loyalty.service_engine.application.dto.FidelityRangeDTO(
            fr.uid, fr.ecommerceId, fr.name, fr.minPoints, fr.maxPoints,
            fr.discountPercentage, fr.isActive, fr.createdAt, fr.updatedAt
        )
        FROM FidelityRangeEntity fr
        WHERE fr.ecommerceId = :ecommerceId AND fr.isActive = true
        ORDER BY fr.minPoints ASC
        """)
    List<FidelityRangeDTO> findByEcommerceIdAndActiveSorted(UUID ecommerceId);
}
