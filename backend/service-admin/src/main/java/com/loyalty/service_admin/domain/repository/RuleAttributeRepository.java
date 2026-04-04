package com.loyalty.service_admin.domain.repository;

import com.loyalty.service_admin.domain.entity.RuleAttributeEntity;
import org.springframework.data.jpa.repository.JpaRepository;
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
     * Find attribute by name and discount type
     */
    Optional<RuleAttributeEntity> findByDiscountTypeIdAndAttributeName(UUID discountTypeId, String attributeName);

    /**
     * Check if attribute exists
     */
    boolean existsByDiscountTypeIdAndAttributeName(UUID discountTypeId, String attributeName);
}
