package com.loyalty.service_admin.domain.repository;

import com.loyalty.service_admin.domain.entity.RuleAttributeEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface RuleAttributeRepository extends JpaRepository<RuleAttributeEntity, UUID> {

    /**
     * Find all attributes for a discount type
     */
    List<RuleAttributeEntity> findByDiscountTypeIdOrderByAttributeNameAsc(UUID discountTypeId);

    /**
     * Find attribute by name and discount type (case-insensitive)
     */
    @Query("SELECT ra FROM RuleAttributeEntity ra WHERE ra.discountTypeId = :discountTypeId AND LOWER(ra.attributeName) = LOWER(:attributeName)")
    Optional<RuleAttributeEntity> findByDiscountTypeIdAndAttributeName(@Param("discountTypeId") UUID discountTypeId, @Param("attributeName") String attributeName);

    /**
     * Check if attribute exists (case-insensitive)
     */
    @Query("SELECT CASE WHEN COUNT(ra) > 0 THEN true ELSE false END FROM RuleAttributeEntity ra WHERE ra.discountTypeId = :discountTypeId AND LOWER(ra.attributeName) = LOWER(:attributeName)")
    boolean existsByDiscountTypeIdAndAttributeName(@Param("discountTypeId") UUID discountTypeId, @Param("attributeName") String attributeName);
}
