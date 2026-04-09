package com.loyalty.service_admin.infrastructure.persistence.jpa;

import com.loyalty.service_admin.application.port.out.RulePersistencePort;
import com.loyalty.service_admin.domain.entity.RuleEntity;
import com.loyalty.service_admin.domain.entity.RuleAttributeEntity;
import com.loyalty.service_admin.domain.entity.RuleAttributeValueEntity;
import com.loyalty.service_admin.domain.repository.RuleRepository;
import com.loyalty.service_admin.domain.repository.RuleAttributeRepository;
import com.loyalty.service_admin.domain.repository.RuleAttributeValueRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * JpaRuleAdapter - Adapter de persistencia para RuleService.
 *
 * Implementa RulePersistencePort delegando a los repositorios JPA.
 * Este adapter inyecta los repositorios concretos, desacoplando RuleService de JPA.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class JpaRuleAdapter implements RulePersistencePort {

    private final RuleRepository ruleRepository;
    private final RuleAttributeRepository ruleAttributeRepository;
    private final RuleAttributeValueRepository ruleAttributeValueRepository;

    @Override
    public RuleEntity saveRule(RuleEntity rule) {
        log.debug("Saving rule: {}", rule.getId());
        return ruleRepository.save(rule);
    }

    @Override
    public Optional<RuleEntity> findRuleById(UUID ruleId) {
        log.debug("Finding rule by id: {}", ruleId);
        return ruleRepository.findById(ruleId);
    }

    @Override
    public Optional<RuleEntity> findRuleByIdAndEcommerce(UUID ruleId, UUID ecommerceId) {
        log.debug("Finding rule by id and ecommerce: {} - {}", ruleId, ecommerceId);
        return ruleRepository.findByIdAndEcommerceId(ruleId, ecommerceId);
    }

    @Override
    public Page<RuleEntity> findRulesByEcommerce(UUID ecommerceId, Pageable pageable) {
        log.debug("Finding rules by ecommerce: {}", ecommerceId);
        return ruleRepository.findByEcommerceIdOrderByCreatedAtDesc(ecommerceId, pageable);
    }

    @Override
    public Page<RuleEntity> findActiveRulesByEcommerce(UUID ecommerceId, Pageable pageable) {
        log.debug("Finding active rules by ecommerce: {}", ecommerceId);
        return ruleRepository.findByEcommerceIdAndIsActiveTrueOrderByCreatedAtDesc(ecommerceId, pageable);
    }

    @Override
    public List<RuleEntity> findRulesByStatus(UUID ecommerceId, Boolean isActive) {
        log.debug("Finding rules by status: {} - {}", ecommerceId, isActive);
        Page<RuleEntity> page = isActive 
            ? ruleRepository.findByEcommerceIdAndIsActiveTrueOrderByCreatedAtDesc(
                ecommerceId, 
                org.springframework.data.domain.PageRequest.of(0, Integer.MAX_VALUE))
            : ruleRepository.findByEcommerceIdOrderByCreatedAtDesc(
                ecommerceId, 
                org.springframework.data.domain.PageRequest.of(0, Integer.MAX_VALUE));
        return page.getContent();
    }

    @Override
    public void deleteRule(RuleEntity rule) {
        log.debug("Soft-deleting rule: {}", rule.getId());
        rule.setIsActive(false);
        ruleRepository.save(rule);
    }

    @Override
    public boolean existsRule(UUID ruleId) {
        log.debug("Checking if rule exists: {}", ruleId);
        return ruleRepository.existsById(ruleId);
    }

    @Override
    public boolean existsActiveRuleWithAttribute(UUID ecommerceId, String attributeName, String attributeValue) {
        log.debug("Checking if active rule exists with attribute: {} = {}", attributeName, attributeValue);
        
        Page<RuleEntity> activeRules = ruleRepository.findByEcommerceIdAndIsActiveTrueOrderByCreatedAtDesc(
            ecommerceId, 
            org.springframework.data.domain.PageRequest.of(0, Integer.MAX_VALUE)
        );
        
        for (RuleEntity rule : activeRules) {
            List<RuleAttributeValueEntity> attrs = ruleAttributeValueRepository.findByRuleIdOrderByCreatedAtAsc(rule.getId());
            for (RuleAttributeValueEntity attr : attrs) {
                RuleAttributeEntity attrDef = ruleAttributeRepository.findById(attr.getAttributeId()).orElse(null);
                if (attrDef != null && attributeName.equalsIgnoreCase(attrDef.getAttributeName())) {
                    if (attributeValue.equals(attr.getValue())) {
                        return true;
                    }
                }
            }
        }
        return false;
    }
}
