package com.loyalty.service_admin.domain.repository;

import com.loyalty.service_admin.domain.entity.RuleCustomerTierEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface RuleCustomerTierRepository extends JpaRepository<RuleCustomerTierEntity, UUID> {

    /**
     * Find all tiers for a rule
     */
    List<RuleCustomerTierEntity> findByRuleId(UUID ruleId);

    /**
     * Find all rules for a customer tier
     */
    List<RuleCustomerTierEntity> findByCustomerTierId(UUID customerTierId);

    /**
     * Find specific rule-tier relationship
     */
    Optional<RuleCustomerTierEntity> findByRuleIdAndCustomerTierId(UUID ruleId, UUID customerTierId);

    /**
     * Check if rule is linked to tier
     */
    boolean existsByRuleIdAndCustomerTierId(UUID ruleId, UUID customerTierId);

    /**
     * Delete all tier links for a rule
     */
    void deleteByRuleId(UUID ruleId);

    /**
     * Delete specific rule-tier relationship
     */
    void deleteByRuleIdAndCustomerTierId(UUID ruleId, UUID customerTierId);
}
