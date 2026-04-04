package com.loyalty.service_admin.application.service;

import com.loyalty.service_admin.application.dto.rules.RuleAttributeValueDTO;
import com.loyalty.service_admin.application.dto.rules.RuleCreateRequest;
import com.loyalty.service_admin.application.dto.rules.RuleResponse;
import com.loyalty.service_admin.application.dto.rules.RuleResponseWithTiers;
import com.loyalty.service_admin.application.dto.rules.RuleCustomerTierDTO;
import com.loyalty.service_admin.domain.entity.*;
import com.loyalty.service_admin.domain.repository.*;
import com.loyalty.service_admin.infrastructure.exception.BadRequestException;
import com.loyalty.service_admin.infrastructure.exception.ResourceNotFoundException;
import com.loyalty.service_admin.domain.repository.DiscountLimitPriorityRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class RuleService {

    private final RuleRepository ruleRepository;
    private final RuleAttributeRepository ruleAttributeRepository;
    private final RuleAttributeValueRepository ruleAttributeValueRepository;
    private final DiscountLimitPriorityRepository discountLimitPriorityRepository;
    private final RuleCustomerTierRepository ruleCustomerTierRepository;
    private final CustomerTierRepository customerTierRepository;

    /**
     * Create a new rule with attributes
     */
    public RuleResponse createRule(UUID ecommerceId, RuleCreateRequest request) {
        log.debug("Creating rule for ecommerce: {}", ecommerceId);

        UUID priorityId = UUID.fromString(request.discountPriorityId());
        DiscountPriorityEntity priority = discountLimitPriorityRepository.findById(priorityId)
                .orElseThrow(() -> new ResourceNotFoundException("Discount priority not found: " + priorityId));

        // Create rule entity
        RuleEntity rule = RuleEntity.builder()
                .ecommerceId(ecommerceId)
                .discountPriorityId(priorityId)
                .name(request.name())
                .description(request.description())
                .discountPercentage(request.discountPercentage())
                .isActive(true)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        RuleEntity savedRule = ruleRepository.save(rule);
        log.info("Rule created with id: {}", savedRule.getId());

        // Save attribute values
        saveAttributeValues(savedRule.getId(), priority.getDiscountTypeId(), request.attributes());

        return toResponse(savedRule);
    }

    /**
     * Get rule by id with tenant isolation
     */
    public RuleResponse getRuleById(UUID ecommerceId, UUID ruleId) {
        RuleEntity rule = ruleRepository.findByIdAndEcommerceId(ruleId, ecommerceId)
                .orElseThrow(() -> new ResourceNotFoundException("Rule not found: " + ruleId));
        return toResponse(rule);
    }

    /**
     * List rules for an ecommerce with pagination
     */
    public Page<RuleResponse> listRules(UUID ecommerceId, Boolean isActive, Pageable pageable) {
        Page<RuleEntity> rules;
        if (isActive != null && isActive) {
            rules = ruleRepository.findByEcommerceIdAndIsActiveTrueOrderByCreatedAtDesc(ecommerceId, pageable);
        } else {
            rules = ruleRepository.findByEcommerceIdOrderByCreatedAtDesc(ecommerceId, pageable);
        }
        return rules.map(this::toResponse);
    }

    /**
     * Update rule
     */
    public RuleResponse updateRule(UUID ecommerceId, UUID ruleId, RuleCreateRequest request) {
        RuleEntity rule = ruleRepository.findByIdAndEcommerceId(ruleId, ecommerceId)
                .orElseThrow(() -> new ResourceNotFoundException("Rule not found: " + ruleId));

        rule.setName(request.name());
        rule.setDescription(request.description());
        rule.setDiscountPercentage(request.discountPercentage());
        rule.setUpdatedAt(Instant.now());

        RuleEntity updated = ruleRepository.save(rule);

        // Update attribute values
        ruleAttributeValueRepository.deleteByRuleId(ruleId);
        saveAttributeValues(ruleId, rule.getDiscountPriorityId(), request.attributes());

        return toResponse(updated);
    }

    /**
     * Delete rule (soft delete)
     */
    public void deleteRule(UUID ecommerceId, UUID ruleId) {
        RuleEntity rule = ruleRepository.findByIdAndEcommerceId(ruleId, ecommerceId)
                .orElseThrow(() -> new ResourceNotFoundException("Rule not found: " + ruleId));

        rule.setIsActive(false);
        rule.setUpdatedAt(Instant.now());
        ruleRepository.save(rule);
        log.info("Rule soft deleted: {}", ruleId);
    }

    /**
     * Save or update attribute values for a rule
     */
    private void saveAttributeValues(UUID ruleId, UUID discountTypeId, Map<String, String> attributes) {
        for (Map.Entry<String, String> entry : attributes.entrySet()) {
            String attributeName = entry.getKey();
            String value = entry.getValue();

            RuleAttributeEntity attribute = ruleAttributeRepository
                    .findByDiscountTypeIdAndAttributeName(discountTypeId, attributeName)
                    .orElseThrow(() -> new BadRequestException("Invalid attribute: " + attributeName));

            ruleAttributeValueRepository.findByRuleIdAndAttributeId(ruleId, attribute.getId())
                    .ifPresentOrElse(
                            attrValue -> {
                                attrValue.setValue(value);
                                attrValue.setUpdatedAt(Instant.now());
                                ruleAttributeValueRepository.save(attrValue);
                            },
                            () -> {
                                RuleAttributeValueEntity newValue = RuleAttributeValueEntity.builder()
                                        .ruleId(ruleId)
                                        .attributeId(attribute.getId())
                                        .value(value)
                                        .createdAt(Instant.now())
                                        .updatedAt(Instant.now())
                                        .build();
                                ruleAttributeValueRepository.save(newValue);
                            }
                    );
        }
    }

    /**
     * Assign customer tiers to a rule
     */
    public RuleResponseWithTiers assignCustomerTiersToRule(UUID ecommerceId, UUID ruleId, List<UUID> tierIds) {
        RuleEntity rule = ruleRepository.findByIdAndEcommerceId(ruleId, ecommerceId)
                .orElseThrow(() -> new ResourceNotFoundException("Rule not found: " + ruleId));

        // Delete existing tier assignments
        ruleCustomerTierRepository.deleteByRuleId(ruleId);

        // Add new tier assignments
        for (UUID tierId : tierIds) {
            CustomerTierEntity tier = customerTierRepository.findById(tierId)
                    .orElseThrow(() -> new ResourceNotFoundException("Customer tier not found: " + tierId));

            RuleCustomerTierEntity mapping = RuleCustomerTierEntity.builder()
                    .rule(rule)
                    .customerTier(tier)
                    .build();
            ruleCustomerTierRepository.save(mapping);
        }

        log.info("Assigned {} tiers to rule: {}", tierIds.size(), ruleId);
        return toResponseWithTiers(rule);
    }

    /**
     * Get assigned tiers for a rule
     */
    public List<RuleCustomerTierDTO> getRuleAssignedTiers(UUID ecommerceId, UUID ruleId) {
        RuleEntity rule = ruleRepository.findByIdAndEcommerceId(ruleId, ecommerceId)
                .orElseThrow(() -> new ResourceNotFoundException("Rule not found: " + ruleId));

        return ruleCustomerTierRepository.findByRuleId(ruleId).stream()
                .map(mapping -> new RuleCustomerTierDTO(
                        mapping.getCustomerTier().getId(),
                        mapping.getCustomerTier().getName()
                ))
                .collect(Collectors.toList());
    }

    /**
     * Convert entity to response DTO with tiers
     */
    private RuleResponseWithTiers toResponseWithTiers(RuleEntity rule) {
        List<RuleAttributeValueDTO> attributeDTOs = getAttributeDTOs(rule.getId());
        List<RuleCustomerTierDTO> tierDTOs = ruleCustomerTierRepository.findByRuleId(rule.getId()).stream()
                .map(mapping -> new RuleCustomerTierDTO(
                        mapping.getCustomerTier().getId(),
                        mapping.getCustomerTier().getName()
                ))
                .collect(Collectors.toList());

        return new RuleResponseWithTiers(
                rule.getId(),
                rule.getEcommerceId(),
                rule.getDiscountPriorityId(),
                rule.getName(),
                rule.getDescription(),
                rule.getDiscountPercentage(),
                rule.getIsActive(),
                attributeDTOs,
                tierDTOs,
                rule.getCreatedAt(),
                rule.getUpdatedAt()
        );
    }

    /**
     * Convert entity to response DTO
     */
    private RuleResponse toResponse(RuleEntity rule) {
        List<RuleAttributeValueDTO> attributeDTOs = getAttributeDTOs(rule.getId());

        return new RuleResponse(
                rule.getId(),
                rule.getEcommerceId(),
                rule.getDiscountPriorityId(),
                rule.getName(),
                rule.getDescription(),
                rule.getDiscountPercentage(),
                rule.getIsActive(),
                attributeDTOs,
                rule.getCreatedAt(),
                rule.getUpdatedAt()
        );
    }

    /**
     * Helper: Extract attribute DTOs for a rule
     */
    private List<RuleAttributeValueDTO> getAttributeDTOs(UUID ruleId) {
        return ruleAttributeValueRepository.findByRuleIdOrderByCreatedAtAsc(ruleId).stream()
                .map(v -> {
                    RuleAttributeEntity attr = ruleAttributeRepository.findById(v.getAttributeId())
                            .orElse(null);
                    return new RuleAttributeValueDTO(
                            v.getAttributeId(),
                            attr != null ? attr.getAttributeName() : "unknown",
                            v.getValue()
                    );
                })
                .collect(Collectors.toList());
    }
}
