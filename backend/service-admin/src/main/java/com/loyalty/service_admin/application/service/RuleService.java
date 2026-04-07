package com.loyalty.service_admin.application.service;

import com.loyalty.service_admin.application.dto.rules.RuleAttributeValueDTO;
import com.loyalty.service_admin.application.dto.rules.RuleCreateRequest;
import com.loyalty.service_admin.application.dto.rules.RuleResponse;
import com.loyalty.service_admin.application.dto.rules.RuleResponseWithTiers;
import com.loyalty.service_admin.application.dto.rules.RuleCustomerTierDTO;
import com.loyalty.service_admin.application.dto.rules.RuleAttributeMetadataDTO;
import com.loyalty.service_admin.application.dto.discount.DiscountTypeDTO;
import com.loyalty.service_admin.application.dto.discount.DiscountPriorityDTO;
import com.loyalty.service_admin.domain.entity.*;
import com.loyalty.service_admin.domain.repository.*;
import com.loyalty.service_admin.infrastructure.exception.BadRequestException;
import com.loyalty.service_admin.infrastructure.exception.ConflictException;
import com.loyalty.service_admin.infrastructure.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
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
    private final DiscountTypeRepository discountTypeRepository;
    private final DiscountConfigRepository discountConfigRepository;

    public RuleResponse createRule(UUID ecommerceId, RuleCreateRequest request) {
        log.debug("Creating rule for ecommerce: {}", ecommerceId);

        UUID priorityId = UUID.fromString(request.discountPriorityId());
        DiscountPriorityEntity priority = discountLimitPriorityRepository.findById(priorityId)
                .orElseThrow(() -> new ResourceNotFoundException("Discount priority not found: " + priorityId));

        DiscountTypeEntity discountType = discountTypeRepository.findById(priority.getDiscountTypeId())
                .orElseThrow(() -> new ResourceNotFoundException("Discount type not found"));
        
        String typeCode = discountType.getCode(); // PRODUCT, SEASONAL, CLASSIFICATION

        // ========== HU-06 SEASONAL VALIDATION ==========
        if ("SEASONAL".equals(typeCode)) {
            LocalDate startDate = LocalDate.parse(request.attributes().get("start_date"));
            LocalDate endDate = LocalDate.parse(request.attributes().get("end_date"));
            validateSeasonalDateOverlap(ecommerceId, null, startDate, endDate);
        }

        // ========== HU-06/HU-07 DISCOUNT LIMITS VALIDATION (APPLIES TO ALL) ==========
        validateDiscountLimits(ecommerceId, request.discountPercentage());

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

        UUID priorityId = UUID.fromString(request.discountPriorityId());
        DiscountPriorityEntity priority = discountLimitPriorityRepository.findById(priorityId)
                .orElseThrow(() -> new ResourceNotFoundException("Discount priority not found: " + priorityId));

        DiscountTypeEntity discountType = discountTypeRepository.findById(priority.getDiscountTypeId())
                .orElseThrow(() -> new ResourceNotFoundException("Discount type not found"));
        
        String typeCode = discountType.getCode();

        // ========== HU-06 SEASONAL VALIDATION ==========
        if ("SEASONAL".equals(typeCode)) {
            LocalDate startDate = LocalDate.parse(request.attributes().get("start_date"));
            LocalDate endDate = LocalDate.parse(request.attributes().get("end_date"));
            validateSeasonalDateOverlap(ecommerceId, ruleId, startDate, endDate); // Pass ruleId to exclude from check
        }

        // ========== HU-06/HU-07 DISCOUNT LIMITS VALIDATION ==========
        validateDiscountLimits(ecommerceId, request.discountPercentage());

        rule.setName(request.name());
        rule.setDescription(request.description());
        rule.setDiscountPercentage(request.discountPercentage());
        rule.setDiscountPriorityId(priorityId);
        rule.setUpdatedAt(Instant.now());

        RuleEntity updated = ruleRepository.save(rule);

        // Update attribute values
        ruleAttributeValueRepository.deleteByRuleId(ruleId);
        saveAttributeValues(ruleId, priority.getDiscountTypeId(), request.attributes());

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
     * Validates that attribute names match exactly what's in the database
     */
    private void saveAttributeValues(UUID ruleId, UUID discountTypeId, Map<String, String> attributes) {
        // Get all valid attributes for this discount type
        List<RuleAttributeEntity> validAttributes = ruleAttributeRepository
                .findByDiscountTypeIdOrderByAttributeNameAsc(discountTypeId);

        // Validate all provided attribute names exist
        for (String attributeName : attributes.keySet()) {
            boolean attributeExists = validAttributes.stream()
                    .anyMatch(attr -> attr.getAttributeName().equalsIgnoreCase(attributeName));

            if (!attributeExists) {
                String validNames = validAttributes.stream()
                        .map(RuleAttributeEntity::getAttributeName)
                        .collect(Collectors.joining(", "));
                throw new BadRequestException(
                        String.format("Invalid attribute '%s' for discount type. Valid attributes: %s", 
                                attributeName, validNames)
                );
            }
        }

        // Save attribute values
        for (Map.Entry<String, String> entry : attributes.entrySet()) {
            String attributeName = entry.getKey();
            String value = entry.getValue();

            RuleAttributeEntity attribute = ruleAttributeRepository
                    .findByDiscountTypeIdAndAttributeName(discountTypeId, attributeName)
                    .orElseThrow(() -> new BadRequestException("Attribute not found: " + attributeName));

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

    // ============================================================
    // VALIDATION METHODS - Date Overlap & Discount Limits (HU-06/HU-07)
    // ============================================================

    /**
     * Validates that SEASONAL rule dates don't overlap with existing rules
     * HU-06: Validación de Superposición de Fechas
     * 
     * @param ecommerceId    Tenant ID
     * @param ruleId         Current rule ID (for update, null for create)
     * @param startDate      Season start date
     * @param endDate        Season end date
     * @throws BadRequestException if dates are invalid
     * @throws ConflictException if overlap detected
     */
    private void validateSeasonalDateOverlap(UUID ecommerceId, UUID ruleId, 
                                             LocalDate startDate, LocalDate endDate) {
        log.debug("Validating SEASONAL date overlap for ecommerce: {}", ecommerceId);
        
        // Validate date order
        if (startDate.isAfter(endDate)) {
            throw new BadRequestException("start_date must be before or equal to end_date");
        }
        
        // Query all active SEASONAL rules for this ecommerce
        List<RuleEntity> activeSeasonalRules = findActiveSeasonalRulesByEcommerce(ecommerceId);
        
        for (RuleEntity existingRule : activeSeasonalRules) {
            // Skip current rule if updating
            if (ruleId != null && existingRule.getId().equals(ruleId)) {
                continue;
            }
            
            // Extract dates from existing rule
            LocalDate existingStart = getAttributeDate(existingRule, "start_date");
            LocalDate existingEnd = getAttributeDate(existingRule, "end_date");
            
            // Check overlap
            if (datesOverlap(startDate, endDate, existingStart, existingEnd)) {
                log.warn("Date overlap detected with rule: {} (dates: {} to {})", 
                    existingRule.getId(), existingStart, existingEnd);
                throw new ConflictException(
                    "SEASONAL rule date overlap detected. Existing rule (" + 
                    existingRule.getId() + ") covers " + existingStart + 
                    " to " + existingEnd
                );
            }
        }
        
        log.debug("SEASONAL date overlap validation passed");
    }

    /**
     * Validates that discount percentage doesn't exceed configured limits
     * HU-06/HU-07: Validación de Límites de Descuento
     * 
     * @param ecommerceId        Tenant ID
     * @param discountPercentage Discount to validate
     * @throws BadRequestException if discount exceeds max limit
     * @throws ResourceNotFoundException if discount settings not found
     */
    private void validateDiscountLimits(UUID ecommerceId, BigDecimal discountPercentage) {
        log.debug("Validating discount limits for ecommerce: {}", ecommerceId);
        
        // Get discount settings for ecommerce
        DiscountSettingsEntity settings = discountConfigRepository
            .findActiveByEcommerceId(ecommerceId)
            .orElseThrow(() -> new ResourceNotFoundException(
                "Discount settings not found for ecommerce: " + ecommerceId
            ));
        
        // Validate against max_discount_cap
        if (discountPercentage.compareTo(settings.getMaxDiscountCap()) > 0) {
            log.warn("Discount {} exceeds max cap: {}", discountPercentage, settings.getMaxDiscountCap());
            throw new BadRequestException(
                "Discount percentage " + discountPercentage + 
                "% exceeds maximum allowed: " + settings.getMaxDiscountCap() + "%"
            );
        }
        
        log.debug("Discount limits validation passed");
    }

    /**
     * Extract date value from rule attributes by name
     * Helper for date validation
     * 
     * @param rule           Rule entity
     * @param attributeName  Attribute name (e.g., "start_date")
     * @return LocalDate parsed value
     * @throws BadRequestException if attribute not found or invalid format
     */
    private LocalDate getAttributeDate(RuleEntity rule, String attributeName) {
        List<RuleAttributeValueEntity> values = 
            ruleAttributeValueRepository.findByRuleIdOrderByCreatedAtAsc(rule.getId());
        
        for (RuleAttributeValueEntity value : values) {
            RuleAttributeEntity attr = ruleAttributeRepository
                .findById(value.getAttributeId())
                .orElse(null);
            
            if (attr != null && attr.getAttributeName().equalsIgnoreCase(attributeName)) {
                try {
                    // Expected format: YYYY-MM-DD (ISO 8601)
                    return LocalDate.parse(value.getValue(), DateTimeFormatter.ISO_LOCAL_DATE);
                } catch (DateTimeParseException e) {
                    throw new BadRequestException(
                        "Invalid " + attributeName + " format. Expected YYYY-MM-DD, got: " + value.getValue()
                    );
                }
            }
        }
        
        throw new BadRequestException(
            "Attribute '" + attributeName + "' not found for rule: " + rule.getId()
        );
    }

    /**
     * Check if two date ranges overlap
     * Logic: ranges overlap if end1 >= start2 AND end2 >= start1
     * 
     * @param s1 First range start
     * @param e1 First range end
     * @param s2 Second range start
     * @param e2 Second range end
     * @return true if overlap exists
     */
    private boolean datesOverlap(LocalDate s1, LocalDate e1, LocalDate s2, LocalDate e2) {
        return !e1.isBefore(s2) && !e2.isBefore(s1);
    }

    /**
     * Query all active SEASONAL rules for ecommerce
     * Helper for overlap validation
     * 
     * @param ecommerceId Tenant ID
     * @return List of active SEASONAL rules
     */
    private List<RuleEntity> findActiveSeasonalRulesByEcommerce(UUID ecommerceId) {
        // Get all active rules
        Page<RuleEntity> allRules = ruleRepository
            .findByEcommerceIdAndIsActiveTrueOrderByCreatedAtDesc(ecommerceId, 
                org.springframework.data.domain.PageRequest.of(0, Integer.MAX_VALUE));
        
        // Filter by SEASONAL type
        return allRules.stream()
            .filter(rule -> {
                try {
                    DiscountPriorityEntity priority = discountLimitPriorityRepository
                        .findById(rule.getDiscountPriorityId()).orElse(null);
                    if (priority == null) return false;
                    
                    DiscountTypeEntity type = discountTypeRepository
                        .findById(priority.getDiscountTypeId()).orElse(null);
                    
                    return type != null && "SEASONAL".equals(type.getCode());
                } catch (Exception e) {
                    log.error("Error filtering SEASONAL rule", e);
                    return false;
                }
            })
            .collect(Collectors.toList());
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

    public List<DiscountTypeDTO> getAllDiscountTypes() {
        log.debug("Fetching all discount types");
        return discountTypeRepository.findAll().stream()
                .map(dt -> new DiscountTypeDTO(
                        dt.getId(),
                        dt.getCode(),
                        dt.getDisplayName(),
                        dt.getCreatedAt()
                ))
                .collect(Collectors.toList());
    }

    /**
     * Get discount priorities for a specific discount type
     */
    public List<DiscountPriorityDTO> getDiscountPrioritiesByType(UUID discountTypeId) {
        log.debug("Fetching discount priorities for type: {}", discountTypeId);
        
        // Verify type exists
        discountTypeRepository.findById(discountTypeId)
                .orElseThrow(() -> new ResourceNotFoundException("Discount type not found: " + discountTypeId));
        
        // Get priorities for this type from discount_settings
        // Note: priorities are linked through discount_settings
        return discountLimitPriorityRepository.findAll().stream()
                .filter(priority -> priority.getDiscountTypeId().equals(discountTypeId))
                .map(priority -> new DiscountPriorityDTO(
                        priority.getId(),
                        priority.getDiscountTypeId(),
                        priority.getPriorityLevel(),
                        priority.getIsActive(),
                        priority.getCreatedAt(),
                        priority.getUpdatedAt()
                ))
                .collect(Collectors.toList());
    }

    /**
     * Get available attributes for a discount type
     */
    public List<RuleAttributeMetadataDTO> getAvailableAttributesForDiscountType(UUID discountTypeId) {
        log.debug("Fetching available attributes for discount type: {}", discountTypeId);
        return ruleAttributeRepository.findByDiscountTypeIdOrderByAttributeNameAsc(discountTypeId).stream()
                .map(attr -> new RuleAttributeMetadataDTO(
                        attr.getId(),
                        attr.getAttributeName(),
                        attr.getAttributeType(),
                        attr.getIsRequired(),
                        attr.getDescription()
                ))
                .collect(Collectors.toList());
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
