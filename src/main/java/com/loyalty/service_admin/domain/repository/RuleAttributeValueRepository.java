package com.loyalty.service_admin.domain.repository;

import com.loyalty.service_admin.domain.entity.RuleAttributeValueEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface RuleAttributeValueRepository extends JpaRepository<RuleAttributeValueEntity, UUID> {

    /**
     * Find all attribute values for a rule
     */
    List<RuleAttributeValueEntity> findByRuleIdOrderByCreatedAtAsc(UUID ruleId);

    /**
     * Find attribute value by rule and attribute
     */
    Optional<RuleAttributeValueEntity> findByRuleIdAndAttributeId(UUID ruleId, UUID attributeId);

    /**
     * Delete all values for a rule
     */
    void deleteByRuleId(UUID ruleId);

    /**
     * Count values for a rule
     */
    long countByRuleId(UUID ruleId);
}
