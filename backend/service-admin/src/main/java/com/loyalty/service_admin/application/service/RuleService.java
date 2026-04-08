package com.loyalty.service_admin.application.service;

import com.loyalty.service_admin.application.dto.rules.RuleAttributeValueDTO;
import com.loyalty.service_admin.application.dto.rules.RuleCreateRequest;
import com.loyalty.service_admin.application.dto.rules.RuleResponse;
import com.loyalty.service_admin.application.dto.rules.RuleResponseWithTiers;
import com.loyalty.service_admin.application.dto.rules.RuleCustomerTierDTO;
import com.loyalty.service_admin.application.dto.rules.RuleAttributeMetadataDTO;
import com.loyalty.service_admin.application.dto.discount.DiscountTypeDTO;
import com.loyalty.service_admin.application.dto.discount.DiscountPriorityDTO;
import com.loyalty.service_admin.application.dto.classificationrule.ClassificationRuleResponse;
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

        // Resolver tipo de descuento
        DiscountTypeEntity discountType = discountTypeRepository.findById(priority.getDiscountTypeId())
                .orElseThrow(() -> new ResourceNotFoundException("Discount type not found"));
        
        String typeCode = discountType.getCode(); // PRODUCT, SEASONAL, CLASSIFICATION

        // ========== HU-07: PRODUCT RULES UNIQUENESS VALIDATION ==========
        if ("PRODUCT".equals(typeCode)) {
            String productType = request.attributes().get("product_type");
            
            // Validar que product_type existe
            if (productType == null || productType.isBlank()) {
                throw new BadRequestException("product_type attribute is required for PRODUCT rules");
            }
            
            // Validar que no existe otra regla activa con el mismo product_type para este ecommerce
            List<RuleEntity> activeRules = ruleRepository.findByEcommerceIdAndIsActiveTrueOrderByCreatedAtDesc(ecommerceId, org.springframework.data.domain.PageRequest.of(0, Integer.MAX_VALUE))
                    .getContent();
            
            for (RuleEntity activeRule : activeRules) {
                List<RuleAttributeValueEntity> attrs = ruleAttributeValueRepository.findByRuleIdOrderByCreatedAtAsc(activeRule.getId());
                for (RuleAttributeValueEntity attr : attrs) {
                    RuleAttributeEntity attrDef = ruleAttributeRepository.findById(attr.getAttributeId()).orElse(null);
                    if (attrDef != null && "product_type".equalsIgnoreCase(attrDef.getAttributeName())) {
                        if (productType.equals(attr.getValue())) {
                            throw new ConflictException("A rule with product_type '" + productType + "' already exists for this ecommerce. Only one active rule per product type is allowed.");
                        }
                    }
                }
            }
        }

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

        // Resolver tipo de descuento
        DiscountTypeEntity discountType = discountTypeRepository.findById(priority.getDiscountTypeId())
                .orElseThrow(() -> new ResourceNotFoundException("Discount type not found"));
        
        String typeCode = discountType.getCode();

        // ========== HU-07: PRODUCT RULES UNIQUENESS VALIDATION ON UPDATE ==========
        if ("PRODUCT".equals(typeCode)) {
            String productType = request.attributes().get("product_type");
            
            // Validar que product_type existe
            if (productType == null || productType.isBlank()) {
                throw new BadRequestException("product_type attribute is required for PRODUCT rules");
            }
            
            // Obtener product_type actual
            List<RuleAttributeValueEntity> currentAttrs = ruleAttributeValueRepository.findByRuleIdOrderByCreatedAtAsc(ruleId);
            String currentProductType = null;
            
            for (RuleAttributeValueEntity attr : currentAttrs) {
                RuleAttributeEntity attrDef = ruleAttributeRepository.findById(attr.getAttributeId()).orElse(null);
                if (attrDef != null && "product_type".equalsIgnoreCase(attrDef.getAttributeName())) {
                    currentProductType = attr.getValue();
                    break;
                }
            }
            
            // Solo validar duplicidad si el product_type cambió
            if (!productType.equals(currentProductType)) {
                List<RuleEntity> activeRules = ruleRepository.findByEcommerceIdAndIsActiveTrueOrderByCreatedAtDesc(ecommerceId, org.springframework.data.domain.PageRequest.of(0, Integer.MAX_VALUE))
                        .getContent();
                
                for (RuleEntity activeRule : activeRules) {
                    if (activeRule.getId().equals(ruleId)) continue;  // Saltar la regla actual
                    
                    List<RuleAttributeValueEntity> attrs = ruleAttributeValueRepository.findByRuleIdOrderByCreatedAtAsc(activeRule.getId());
                    for (RuleAttributeValueEntity attr : attrs) {
                        RuleAttributeEntity attrDef = ruleAttributeRepository.findById(attr.getAttributeId()).orElse(null);
                        if (attrDef != null && "product_type".equalsIgnoreCase(attrDef.getAttributeName())) {
                            if (productType.equals(attr.getValue())) {
                                throw new ConflictException("A rule with product_type '" + productType + "' already exists for this ecommerce. Only one active rule per product type is allowed.");
                            }
                        }
                    }
                }
            }
        }

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
                "Discount amount $" + discountPercentage + 
                " exceeds maximum allowed: $" + settings.getMaxDiscountCap()
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
     * Delete a specific customer tier from a rule (HU-07 CRITERIO-7.3)
     */
    public void deleteCustomerTierFromRule(UUID ecommerceId, UUID ruleId, UUID tierId) {
        RuleEntity rule = ruleRepository.findByIdAndEcommerceId(ruleId, ecommerceId)
                .orElseThrow(() -> new ResourceNotFoundException("Rule not found: " + ruleId));

        // Verify tier exists
        if (!ruleCustomerTierRepository.existsByRuleIdAndCustomerTierId(ruleId, tierId)) {
            throw new ResourceNotFoundException(
                "Customer tier " + tierId + " is not assigned to rule " + ruleId
            );
        }

        ruleCustomerTierRepository.deleteByRuleIdAndCustomerTierId(ruleId, tierId);
        log.info("Deleted tier {} from rule {}", tierId, ruleId);
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

    // ========== HU-07: CLASSIFICATION RULES (NESTED) ==========

    /**
     * HU-07 CRITERIO-7.3, 7.4: Crear classification_rule para un customer_tier
     * 
     * Internamente:
     * 1. Resuelve discount_priority por tipo CLASSIFICATION
     * 2. Valida metricType enum y minValue < maxValue
     * 3. Crea RuleEntity con type=CLASSIFICATION
     * 4. Inserta record en rule_customer_tiers
     * 5. Guarda atributos (metricType, minValue, maxValue, priority) en rule_attributes
     */
    @Transactional
    public ClassificationRuleResponse createClassificationRuleForTier(
            UUID ecommerceId, 
            UUID tierId, 
            com.loyalty.service_admin.application.dto.classificationrule.ClassificationRuleCreateRequest request
    ) {
        log.info("Creating classification rule for tier: {}, ecommerce: {}", tierId, ecommerceId);

        // Validar que el tier existe
        CustomerTierEntity tier = customerTierRepository.findById(tierId)
                .orElseThrow(() -> new ResourceNotFoundException("Customer tier not found: " + tierId));

        // Obtener discount_priority desde request
        UUID priorityId = UUID.fromString(request.discountPriorityId());
        DiscountPriorityEntity priority = discountLimitPriorityRepository.findById(priorityId)
                .orElseThrow(() -> new ResourceNotFoundException("Discount priority not found: " + priorityId));

        // Resolver tipo de la prioridad
        DiscountTypeEntity discountType = discountTypeRepository.findById(priority.getDiscountTypeId())
                .orElseThrow(() -> new ResourceNotFoundException("Discount type not found"));
        
        // La classification rule DEBE tener type=CLASSIFICATION
        if (!"CLASSIFICATION".equals(discountType.getCode())) {
            throw new BadRequestException("Discount priority must be of type CLASSIFICATION for this endpoint");
        }

        // Validaciones específicas para CLASSIFICATION (CRITERIO-7.3, 7.4)
        if (request.minValue().compareTo(request.maxValue()) >= 0) {
            throw new BadRequestException("minValue must be less than maxValue (CRITERIO-7.3)");
        }

        // Validar enum metricType
        String metricTypeUpper = request.metricType().toUpperCase();
        if (!("TOTAL_SPENT".equals(metricTypeUpper) || "ORDER_COUNT".equals(metricTypeUpper) || 
              "LOYALTY_POINTS".equals(metricTypeUpper) || "CUSTOM".equals(metricTypeUpper))) {
            throw new BadRequestException("metricType must be one of: total_spent, order_count, loyalty_points, custom (CRITERIO-7.4)");
        }

        // Crear RuleEntity
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
        log.info("Classification rule created with id: {}", savedRule.getId());

        // Guardar atributos de clasificación
        Map<String, String> attributes = new HashMap<>();
        attributes.put("metricType", request.metricType());
        attributes.put("minValue", request.minValue().toString());
        attributes.put("maxValue", request.maxValue().toString());
        attributes.put("priority", request.priority().toString());
        
        saveAttributeValues(savedRule.getId(), priority.getDiscountTypeId(), attributes);

        // Vincular rule a tier en rule_customer_tiers
        RuleCustomerTierEntity ruleCustomerTier = RuleCustomerTierEntity.builder()
                .rule(savedRule)
                .customerTier(tier)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
        
        ruleCustomerTierRepository.save(ruleCustomerTier);
        log.info("Classification rule linked to tier: ruleId={}, tierId={}", savedRule.getId(), tierId);

        return mapToClassificationRuleResponse(savedRule);
    }

    /**
     * HU-07 CRITERIO-7.6: Listar classification_rules de un tier
     */
    @Transactional(readOnly = true)
    public List<ClassificationRuleResponse> listClassificationRulesForTier(UUID ecommerceId, UUID tierId) {
        log.info("Listing classification rules for tier: {}, ecommerce: {}", tierId, ecommerceId);

        // Validar que el tier existe
        CustomerTierEntity tier = customerTierRepository.findById(tierId)
                .orElseThrow(() -> new ResourceNotFoundException("Customer tier not found: " + tierId));

        // Obtener todas las relaciones rule_customer_tiers para este tier
        List<RuleCustomerTierEntity> ruleLinks = ruleCustomerTierRepository.findByCustomerTierId(tierId);

        // Convertir a responses
        return ruleLinks.stream()
                .filter(link -> link.getRule().getIsActive())  // Solo rules activas
                .map(link -> mapToClassificationRuleResponse(link.getRule()))
                .collect(Collectors.toList());
    }

    /**
     * HU-07 CRITERIO-7.7: Actualizar classification_rule
     */
    @Transactional
    public ClassificationRuleResponse updateClassificationRuleForTier(
            UUID ecommerceId, 
            UUID tierId, 
            UUID ruleId,
            com.loyalty.service_admin.application.dto.classificationrule.ClassificationRuleUpdateRequest request
    ) {
        log.info("Updating classification rule: ruleId={}, tierId={}, ecommerce={}", ruleId, tierId, ecommerceId);

        // Validar que tier existe
        CustomerTierEntity tier = customerTierRepository.findById(tierId)
                .orElseThrow(() -> new ResourceNotFoundException("Customer tier not found: " + tierId));

        // Validar que rule existe y está vinculada a este tier
        RuleEntity rule = ruleRepository.findByIdAndEcommerceId(ruleId, ecommerceId)
                .orElseThrow(() -> new ResourceNotFoundException("Rule not found: " + ruleId));

        RuleCustomerTierEntity link = ruleCustomerTierRepository.findByRuleIdAndCustomerTierId(ruleId, tierId)
                .orElseThrow(() -> new BadRequestException("Rule is not linked to this tier"));

        // Resolvr priority para validaciones
        DiscountPriorityEntity priority = discountLimitPriorityRepository.findById(rule.getDiscountPriorityId())
                .orElseThrow(() -> new ResourceNotFoundException("Discount priority not found"));

        // Aplicar updates (solo los que se proporcionan)
        if (request.name() != null) rule.setName(request.name());
        if (request.description() != null) rule.setDescription(request.description());
        if (request.discountPercentage() != null) rule.setDiscountPercentage(request.discountPercentage());

        rule.setUpdatedAt(Instant.now());
        RuleEntity updated = ruleRepository.save(rule);

        // Actualizar atributos si se proporcionan
        if (request.metricType() != null || request.minValue() != null || request.maxValue() != null || request.priority() != null) {
            // Obtener atributos actuales
            List<RuleAttributeValueEntity> currentAttrs = ruleAttributeValueRepository.findByRuleIdOrderByCreatedAtAsc(ruleId);
            Map<String, String> newAttributes = new HashMap<>();
            
            for (RuleAttributeValueEntity attr : currentAttrs) {
                RuleAttributeEntity attrDef = ruleAttributeRepository.findById(attr.getAttributeId()).orElse(null);
                if (attrDef != null) {
                    newAttributes.put(attrDef.getAttributeName(), attr.getValue());
                }
            }

            // Override con valores nuevos
            if (request.metricType() != null) newAttributes.put("metricType", request.metricType());
            if (request.minValue() != null) newAttributes.put("minValue", request.minValue().toString());
            if (request.maxValue() != null) newAttributes.put("maxValue", request.maxValue().toString());
            if (request.priority() != null) newAttributes.put("priority", request.priority().toString());

            // Validar si se actualizaron minValue/maxValue
            if (request.minValue() != null && request.maxValue() != null) {
                if (request.minValue().compareTo(request.maxValue()) >= 0) {
                    throw new BadRequestException("minValue must be less than maxValue");
                }
            }

            // Guardar atributos actualizados
            ruleAttributeValueRepository.deleteByRuleId(ruleId);
            saveAttributeValues(ruleId, priority.getDiscountTypeId(), newAttributes);
        }

        return mapToClassificationRuleResponse(updated);
    }

    /**
     * HU-07 CRITERIO-7.8: Soft delete classification_rule
     */
    @Transactional
    public void deleteClassificationRuleForTier(UUID ecommerceId, UUID tierId, UUID ruleId) {
        log.info("Soft deleting classification rule: ruleId={}, tierId={}, ecommerce={}", ruleId, tierId, ecommerceId);

        // Validar que tier existe
        CustomerTierEntity tier = customerTierRepository.findById(tierId)
                .orElseThrow(() -> new ResourceNotFoundException("Customer tier not found: " + tierId));

        // Validar que rule existe y está vinculada a este tier
        RuleEntity rule = ruleRepository.findByIdAndEcommerceId(ruleId, ecommerceId)
                .orElseThrow(() -> new ResourceNotFoundException("Rule not found: " + ruleId));

        ruleCustomerTierRepository.findByRuleIdAndCustomerTierId(ruleId, tierId)
                .orElseThrow(() -> new BadRequestException("Rule is not linked to this tier"));

        // Soft delete
        rule.setIsActive(false);
        rule.setUpdatedAt(Instant.now());
        ruleRepository.save(rule);

        log.info("Classification rule soft deleted: {}", ruleId);
    }

    /**
     * Helper: Map RuleEntity to ClassificationRuleResponse
     */
    private ClassificationRuleResponse mapToClassificationRuleResponse(RuleEntity rule) {
        List<RuleAttributeValueDTO> attrs = getAttributeDTOs(rule.getId());
        
        // Extraer atributos específicos
        String metricType = attrs.stream()
                .filter(a -> "metricType".equals(a.attributeName()))
                .findFirst()
                .map(RuleAttributeValueDTO::value)
                .orElse("");
        
        BigDecimal minValue = attrs.stream()
                .filter(a -> "minValue".equals(a.attributeName()))
                .findFirst()
                .map(a -> new BigDecimal(a.value()))
                .orElse(BigDecimal.ZERO);
        
        BigDecimal maxValue = attrs.stream()
                .filter(a -> "maxValue".equals(a.attributeName()))
                .findFirst()
                .map(a -> new BigDecimal(a.value()))
                .orElse(BigDecimal.ZERO);
        
        Integer priority = attrs.stream()
                .filter(a -> "priority".equals(a.attributeName()))
                .findFirst()
                .map(a -> Integer.parseInt(a.value()))
                .orElse(0);

        return new ClassificationRuleResponse(
                rule.getId(),
                rule.getName(),
                rule.getDescription(),
                rule.getDiscountPercentage(),
                metricType,
                minValue,
                maxValue,
                priority,
                rule.getIsActive(),
                rule.getCreatedAt(),
                rule.getUpdatedAt()
        );
    }

    /**
     * Get available attributes for a discount type
     */
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
