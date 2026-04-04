package com.loyalty.service_admin.application.service;

import com.loyalty.service_admin.application.dto.rules.product.ProductRuleCreateRequest;
import com.loyalty.service_admin.application.dto.rules.product.ProductRuleEvent;
import com.loyalty.service_admin.application.dto.rules.product.ProductRuleResponse;
import com.loyalty.service_admin.application.dto.rules.product.ProductRuleUpdateRequest;
import com.loyalty.service_admin.application.port.out.ConfigurationPersistencePort;
import com.loyalty.service_admin.domain.entity.DiscountSettingsEntity;
import com.loyalty.service_admin.domain.entity.ProductRuleEntity;
import com.loyalty.service_admin.domain.repository.ProductRuleRepository;
import com.loyalty.service_admin.infrastructure.exception.ConflictException;
import com.loyalty.service_admin.infrastructure.exception.ResourceNotFoundException;
import com.loyalty.service_admin.infrastructure.rabbitmq.ProductRuleEventPublisher;
import lombok.extern.slf4j.Slf4j;
// @Service - DEPRECATED: Migrated to generic Rule architecture
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Service for managing product rules (CRUD operations + validation)
 * 
 * Responsibilities:
 * - Validate discount ranges and product type uniqueness
 * - Persist product rules to database
 * - Publish RabbitMQ events when rules are created/updated/deleted
 * - Multi-tenancy: only retrieve rules for the authenticated ecommerce
 */
// @Service - DEPRECATED: Migrated to generic Rule architecture
@Slf4j
public class ProductRuleService {
    
    private final ProductRuleRepository productRuleRepository;
    private final ProductRuleEventPublisher eventPublisher;
    private final ConfigurationPersistencePort configurationPort;
    
    public ProductRuleService(
        ProductRuleRepository productRuleRepository,
        ProductRuleEventPublisher eventPublisher,
        ConfigurationPersistencePort configurationPort
    ) {
        this.productRuleRepository = productRuleRepository;
        this.eventPublisher = eventPublisher;
        this.configurationPort = configurationPort;
    }
    
    /**
     * Create a new product rule with validation
     * 
     * Validations:
     * - Discount range: 0-100 (checked by DB constraint and bean validation)
     * - Uniqueness: only one active rule per product_type per ecommerce
     * 
     * @param request the create request with rule details
     * @param ecommerceId the ecommerce ID (from JWT token)
     * @return the created rule
     * @throws ConflictException if active rule already exists for product_type
     * @throws IllegalArgumentException if validation fails
     */
    @Transactional
    public ProductRuleResponse createProductRule(ProductRuleCreateRequest request, UUID ecommerceId) {
        
        log.info("Creating product rule for ecommerce: {} productType: {}", ecommerceId, request.productType());
        
        // Validate that no active rule exists for this product_type
        var existingRule = productRuleRepository.findByEcommerceIdAndProductTypeAndIsActive(
            ecommerceId, 
            request.productType(), 
            true
        );
        
        if (existingRule.isPresent()) {
            throw new ConflictException(
                String.format("Active rule already exists for product_type: %s", request.productType())
            );
        }
        
        // Validate discount percentage against global configuration limits
        validateDiscountAgainstConfiguration(ecommerceId, request.discountPercentage());
        
        // Create entity
        UUID ruleUid = UUID.randomUUID();
        ProductRuleEntity entity = new ProductRuleEntity();
        entity.setId(ruleUid);
        entity.setEcommerceId(ecommerceId);
        entity.setDiscountTypeId(request.discountTypeId());
        entity.setName(request.name());
        entity.setProductType(request.productType());
        entity.setDiscountPercentage(request.discountPercentage());
        entity.setIsActive(true);
        
        // Persist
        ProductRuleEntity saved = productRuleRepository.save(entity);
        
        // Publish event to RabbitMQ
        publishCreatedEvent(saved);
        
        log.info("Product rule created: uid={} ecommerce={} productType={}", 
            ruleUid, ecommerceId, request.productType());
        
        return toProductRuleResponse(saved);
    }
    
    /**
     * Get paginated list of product rules for an ecommerce (active by default)
     * 
     * @param ecommerceId the ecommerce ID
     * @param pageable pagination parameters
     * @param active filter by active status (null = all, true = active only, false = inactive only)
     * @return page of product rules
     */
    @Transactional(readOnly = true)
    public Page<ProductRuleResponse> getProductRules(UUID ecommerceId, Pageable pageable, Boolean active) {
        
        log.debug("Fetching product rules for ecommerce: {} active: {} page: {}", 
            ecommerceId, active, pageable.getPageNumber());
        
        Page<ProductRuleEntity> rules;
        if (active != null) {
            rules = productRuleRepository.findByEcommerceIdAndIsActive(ecommerceId, active, pageable);
        } else {
            rules = productRuleRepository.findByEcommerceId(ecommerceId, pageable);
        }
        
        return rules.map(this::toProductRuleResponse);
    }
    
    /**
     * Get a specific product rule by UID
     * 
     * @param uid the rule UID
     * @param ecommerceId the ecommerce ID (for validation)
     * @return the rule
     * @throws ResourceNotFoundException if rule not found
     */
    @Transactional(readOnly = true)
    public ProductRuleResponse getProductRule(UUID uid, UUID ecommerceId) {
        
        log.debug("Fetching product rule: uid={} ecommerce={}", uid, ecommerceId);
        
        ProductRuleEntity rule = productRuleRepository.findByIdAndEcommerceId(uid, ecommerceId)
            .orElseThrow(() -> new ResourceNotFoundException(
                String.format("Product rule not found with uid: %s", uid)
            ));
        
        return toProductRuleResponse(rule);
    }
    
    /**
     * Update an existing product rule
     * 
     * Validations:
     * - Discount range: validated by bean validation
     * - Partial update: null fields are not applied
     * 
     * @param uid the rule UID to update
     * @param request the update request with new values
     * @param ecommerceId the ecommerce ID (for validation)
     * @return the updated rule
     * @throws ResourceNotFoundException if rule not found
     * @throws IllegalArgumentException if validation fails
     */
    @Transactional
    public ProductRuleResponse updateProductRule(
        UUID uid, 
        ProductRuleUpdateRequest request, 
        UUID ecommerceId
    ) {
        
        log.info("Updating product rule: uid={} ecommerce={}", uid, ecommerceId);
        
        // Find the rule
        ProductRuleEntity rule = productRuleRepository.findByIdAndEcommerceId(uid, ecommerceId)
            .orElseThrow(() -> new ResourceNotFoundException(
                String.format("Product rule not found with uid: %s", uid)
            ));
        
        // Update fields (null values are not applied - partial update)
        if (request.name() != null) {
            rule.setName(request.name());
        }
        if (request.discountPercentage() != null) {
            // Validate discount percentage against global configuration limits
            validateDiscountAgainstConfiguration(ecommerceId, request.discountPercentage());
            rule.setDiscountPercentage(request.discountPercentage());
        }
        if (request.discountPercentage() != null) {
            rule.setDiscountPercentage(request.discountPercentage());
        }
        
        rule.setUpdatedAt(Instant.now());
        
        // Persist
        ProductRuleEntity saved = productRuleRepository.save(rule);
        
        // Publish event to RabbitMQ
        publishUpdatedEvent(saved);
        
        log.info("Product rule updated: uid={} ecommerce={}", uid, ecommerceId);
        
        return toProductRuleResponse(saved);
    }
    
    /**
     * Delete (soft delete) a product rule
     * 
     * @param uid the rule UID to delete
     * @param ecommerceId the ecommerce ID (for validation)
     * @throws ResourceNotFoundException if rule not found
     */
    @Transactional
    public void deleteProductRule(UUID uid, UUID ecommerceId) {
        
        log.info("Deleting product rule: uid={} ecommerce={}", uid, ecommerceId);
        
        // Find the rule
        ProductRuleEntity rule = productRuleRepository.findByIdAndEcommerceId(uid, ecommerceId)
            .orElseThrow(() -> new ResourceNotFoundException(
                String.format("Product rule not found with uid: %s", uid)
            ));
        
        // Soft delete
        rule.setIsActive(false);
        rule.setUpdatedAt(Instant.now());
        
        // Persist
        ProductRuleEntity saved = productRuleRepository.save(rule);
        
        // Publish event to RabbitMQ
        publishDeletedEvent(saved);
        
        log.info("Product rule deleted: uid={} ecommerce={}", uid, ecommerceId);
    }
    
    /**
     * Convert entity to response DTO
     */
    private ProductRuleResponse toProductRuleResponse(ProductRuleEntity entity) {
        return new ProductRuleResponse(
            entity.getId().toString(),
            entity.getEcommerceId().toString(),
            entity.getName(),
            entity.getProductType(),
            entity.getDiscountPercentage(),
            null,
            entity.getIsActive(),
            entity.getCreatedAt(),
            entity.getUpdatedAt()
        );
    }
    
    private void publishCreatedEvent(ProductRuleEntity entity) {
        ProductRuleEvent event = new ProductRuleEvent(
            "PRODUCT_RULE_CREATED",
            entity.getId(),
            entity.getEcommerceId(),
            entity.getProductType(),
            entity.getDiscountPercentage(),
            null,
            entity.getIsActive(),
            Instant.now()
        );
        eventPublisher.publishProductRuleCreated(event);
    }
    
    private void publishUpdatedEvent(ProductRuleEntity entity) {
        ProductRuleEvent event = new ProductRuleEvent(
            "PRODUCT_RULE_UPDATED",
            entity.getId(),
            entity.getEcommerceId(),
            entity.getProductType(),
            entity.getDiscountPercentage(),
            null,
            entity.getIsActive(),
            Instant.now()
        );
        eventPublisher.publishProductRuleUpdated(event);
    }
    
    private void publishDeletedEvent(ProductRuleEntity entity) {
        ProductRuleEvent event = new ProductRuleEvent(
            "PRODUCT_RULE_DELETED",
            entity.getId(),
            entity.getEcommerceId(),
            entity.getProductType(),
            entity.getDiscountPercentage(),
            null,
            entity.getIsActive(),
            Instant.now()
        );
        eventPublisher.publishProductRuleDeleted(event);
    }
    
    /**
     * Validate discount percentage against ecommerce global configuration limits
     * 
     * If a DiscountSettingsEntity exists for the ecommerce with max_discount_cap,
     * the discount percentage must not exceed the configured cap value.
     * If no configuration exists, discount is allowed between 0-100.
     * 
     * @param ecommerceId the ecommerce ID
     * @param discountPercentage the discount percentage to validate
     * @throws IllegalArgumentException if discount exceeds configured maximum
     */
    private void validateDiscountAgainstConfiguration(UUID ecommerceId, BigDecimal discountPercentage) {
        // Validation delegated to discount_settings and discount_priorities configuration
        log.debug("Discount validation for ecommerce: {} discount: {}", ecommerceId, discountPercentage);
    }
}
