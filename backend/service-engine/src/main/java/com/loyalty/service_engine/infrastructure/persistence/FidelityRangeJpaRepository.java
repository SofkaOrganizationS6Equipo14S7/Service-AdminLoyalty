package com.loyalty.service_engine.infrastructure.persistence;

import com.loyalty.service_engine.application.dto.FidelityRangeDTO;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Data access layer for fidelity_ranges replica table in Engine Service.
 * All queries map to DTOs to avoid persistence coupling.
 */
@Repository
public interface FidelityRangeJpaRepository extends JpaRepository<Object, UUID> {

    /**
     * Fetch all active fidelity ranges, sorted by min_points ascending.
     * Used by FidelityRangeStartupLoader for cold start initialization.
     *
     * @return List of FidelityRangeDTO, sorted by min_points
     */
    @Query("""
        SELECT new com.loyalty.service_engine.application.dto.FidelityRangeDTO(
            fr.uid, fr.ecommerce_id, fr.name, fr.min_points, fr.max_points,
            fr.discount_percentage, fr.is_active, fr.created_at, fr.updated_at
        )
        FROM fidelity_ranges fr
        WHERE fr.is_active = true
        ORDER BY fr.min_points ASC
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
            fr.uid, fr.ecommerce_id, fr.name, fr.min_points, fr.max_points,
            fr.discount_percentage, fr.is_active, fr.created_at, fr.updated_at
        )
        FROM fidelity_ranges fr
        WHERE fr.ecommerce_id = :ecommerceId AND fr.is_active = true
        ORDER BY fr.min_points ASC
        """)
    List<FidelityRangeDTO> findByEcommerceIdAndActiveSorted(UUID ecommerceId);
}
