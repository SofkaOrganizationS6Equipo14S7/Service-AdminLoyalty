package com.loyalty.service_admin.application.service;

import com.loyalty.service_admin.application.dto.rules.seasonal.SeasonalRuleCreateRequest;
import com.loyalty.service_admin.application.dto.rules.seasonal.SeasonalRuleResponse;
import com.loyalty.service_admin.application.dto.rules.seasonal.SeasonalRuleUpdateRequest;
import com.loyalty.service_admin.application.mapper.SeasonalRuleMapper;
import com.loyalty.service_admin.domain.entity.SeasonalRuleEntity;
import com.loyalty.service_admin.domain.repository.SeasonalRuleRepository;
import com.loyalty.service_admin.infrastructure.event.SeasonalRuleEventPublisher;
import com.loyalty.service_admin.infrastructure.exception.ConflictException;
import com.loyalty.service_admin.infrastructure.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
// @Service - DEPRECATED: Migrated to generic Rule architecture
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Service for managing seasonal rules (CRUD operations + validation)
 * 
 * Responsibilities:
 * - Validate date overlaps and discount ranges
 * - Persist seasonal rules to database
 * - Publish RabbitMQ events when rules are created/updated/deleted
 */
// @Service - DEPRECATED: Migrated to generic Rule architecture
@Slf4j
@RequiredArgsConstructor
public class SeasonalRuleService {
    
    private final SeasonalRuleRepository seasonalRuleRepository;
    private final SeasonalRuleMapper seasonalRuleMapper;
    private final SeasonalRuleEventPublisher eventPublisher;
    
    /**
     * Create a new seasonal rule with validation
     * 
     * Validations:
     * - Date range: start_date < end_date
     * - No overlap with existing active rules for same ecommerce
     * - Discount range: validated against discount limits (if configured)
     * 
     * @param request the create request with rule details
     * @param ecommerceId the ecommerce ID (from JWT token)
     * @return the created rule
     * @throws ConflictException if date range overlaps with existing rule
     * @throws IllegalArgumentException if validation fails
     */
    @Transactional
    public SeasonalRuleResponse createSeasonalRule(SeasonalRuleCreateRequest request, UUID ecommerceId) {
        
        log.info("Creating seasonal rule for ecommerce: {} name: {}", ecommerceId, request.name());
        
        // Validate date range
        if (request.startDate().compareTo(request.endDate()) >= 0) {
            throw new IllegalArgumentException("start_date must be before end_date");
        }
        
        // Validate for overlapping dates
        validateDateOverlap(ecommerceId, request.startDate(), request.endDate(), null);
        
        // Create entity (let Hibernate generate UUID)
        SeasonalRuleEntity entity = new SeasonalRuleEntity();
        entity.setEcommerceId(ecommerceId);
        entity.setName(request.name());
        entity.setDescription(request.description());
        entity.setDiscountTypeId(request.discountTypeId());
        entity.setDiscountPercentage(request.discountPercentage());
        entity.setStartDate(request.startDate());
        entity.setEndDate(request.endDate());
        entity.setIsActive(true);
        entity.setCreatedAt(Instant.now());
        entity.setUpdatedAt(Instant.now());
        
        // Persist
        SeasonalRuleEntity saved = seasonalRuleRepository.save(entity);
        
        // Publish event to RabbitMQ
        eventPublisher.publishSeasonalRuleCreated(saved);
        
        log.info("Seasonal rule created: uid={} ecommerce={}", saved.getId(), ecommerceId);
        
        return seasonalRuleMapper.toResponse(saved);
    }
    
    /**
     * Get paginated list of active seasonal rules for an ecommerce
     * 
     * @param ecommerceId the ecommerce ID
     * @param pageable pagination parameters
     * @return page of seasonal rules
     */
    @Transactional(readOnly = true)
    public Page<SeasonalRuleResponse> getSeasonalRules(UUID ecommerceId, Pageable pageable) {
        
        log.info("Fetching seasonal rules for ecommerce: {} page: {}", ecommerceId, pageable.getPageNumber());
        
        Page<SeasonalRuleEntity> rules = seasonalRuleRepository.findByEcommerceIdAndIsActiveTrue(ecommerceId, pageable);
        
        return rules.map(seasonalRuleMapper::toResponse);
    }
    
    /**
     * Get a specific seasonal rule by UID
     * 
     * @param uid the rule UID
     * @param ecommerceId the ecommerce ID (for validation)
     * @return the rule
     * @throws ResourceNotFoundException if rule not found
     */
    @Transactional(readOnly = true)
    public SeasonalRuleResponse getSeasonalRule(UUID uid, UUID ecommerceId) {
        
        log.debug("Fetching seasonal rule: uid={} ecommerce={}", uid, ecommerceId);
        
        SeasonalRuleEntity rule = seasonalRuleRepository.findByIdAndEcommerceId(uid, ecommerceId)
            .orElseThrow(() -> new ResourceNotFoundException(
                String.format("Regla de temporada no encontrada para el identificador %s", uid)
            ));
        
        return seasonalRuleMapper.toResponse(rule);
    }
    
    /**
     * Update an existing seasonal rule
     * 
     * Validations:
     * - Date range: start_date < end_date (if both provided)
     * - No overlap with other rules (except the rule being updated)
     * 
     * @param uid the rule UID to update
     * @param request the update request with new values
     * @param ecommerceId the ecommerce ID (for validation)
     * @return the updated rule
     * @throws ResourceNotFoundException if rule not found
     * @throws ConflictException if new dates overlap with other rules
     */
    @Transactional
    public SeasonalRuleResponse updateSeasonalRule(UUID uid, SeasonalRuleUpdateRequest request, UUID ecommerceId) {
        
        log.info("Updating seasonal rule: uid={} ecommerce={}", uid, ecommerceId);
        
        // Find the rule
        SeasonalRuleEntity rule = seasonalRuleRepository.findByIdAndEcommerceId(uid, ecommerceId)
            .orElseThrow(() -> new ResourceNotFoundException(
                String.format("Regla de temporada no encontrada para el identificador %s", uid)
            ));
        
        // Update fields (null values are not applied - partial update)
        if (request.name() != null) {
            rule.setName(request.name());
        }
        if (request.description() != null) {
            rule.setDescription(request.description());
        }
        if (request.discountTypeId() != null) {
            rule.setDiscountTypeId(request.discountTypeId());
        }
        if (request.discountPercentage() != null) {
            rule.setDiscountPercentage(request.discountPercentage());
        }
        
        // If dates are updated, validate them
        if (request.startDate() != null || request.endDate() != null) {
            Instant startDate = request.startDate() != null ? request.startDate() : rule.getStartDate();
            Instant endDate = request.endDate() != null ? request.endDate() : rule.getEndDate();
            
            if (startDate.compareTo(endDate) >= 0) {
                throw new IllegalArgumentException("start_date must be before end_date");
            }
            
            // Check overlap (excluding this rule by UID)
            validateDateOverlap(ecommerceId, startDate, endDate, uid);
            
            rule.setStartDate(startDate);
            rule.setEndDate(endDate);
        }
        
        rule.setUpdatedAt(Instant.now());
        
        // Persist
        SeasonalRuleEntity saved = seasonalRuleRepository.save(rule);
        
        // Publish event to RabbitMQ
        eventPublisher.publishSeasonalRuleUpdated(saved);
        
        log.info("Seasonal rule updated: uid={} ecommerce={}", uid, ecommerceId);
        
        return seasonalRuleMapper.toResponse(saved);
    }
    
    /**
     * Delete (soft delete) a seasonal rule
     * 
     * Sets is_active = false, publishes event
     * 
     * @param uid the rule UID to delete
     * @param ecommerceId the ecommerce ID (for validation)
     * @throws ResourceNotFoundException if rule not found
     */
    @Transactional
    public void deleteSeasonalRule(UUID uid, UUID ecommerceId) {
        
        log.info("Deleting seasonal rule: uid={} ecommerce={}", uid, ecommerceId);
        
        // Find the rule
        SeasonalRuleEntity rule = seasonalRuleRepository.findByIdAndEcommerceId(uid, ecommerceId)
            .orElseThrow(() -> new ResourceNotFoundException(
                String.format("Regla de temporada no encontrada para el identificador %s", uid)
            ));
        
        // Soft delete: mark as inactive
        rule.setIsActive(false);
        rule.setUpdatedAt(Instant.now());
        
        // Persist
        SeasonalRuleEntity saved = seasonalRuleRepository.save(rule);
        
        // Publish event to RabbitMQ
        eventPublisher.publishSeasonalRuleDeleted(saved);
        
        log.info("Seasonal rule deleted: uid={} ecommerce={}", uid, ecommerceId);
    }
    
    /**
     * Validate that the given date range doesn't overlap with existing active rules
     * 
     * @param ecommerceId the ecommerce ID
     * @param startDate the start date to check
     * @param endDate the end date to check
     * @param excludeUid (optional) UID to exclude from the check (useful during updates)
     * @throws ConflictException if overlap found
     */
    private void validateDateOverlap(UUID ecommerceId, Instant startDate, Instant endDate, UUID excludeId) {
        
        List<SeasonalRuleEntity> overlapping = seasonalRuleRepository.findOverlappingRules(
            ecommerceId,
            startDate,
            endDate,
            excludeId
        );
        
        if (!overlapping.isEmpty()) {
            String message = String.format(
                "Existe una regla de temporada activa en el rango especificado (%s a %s)",
                startDate,
                endDate
            );
            throw new ConflictException(message);
        }
    }
}
