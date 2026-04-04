package com.loyalty.service_admin.presentation.controller;

import com.loyalty.service_admin.application.dto.rules.AssignCustomerTiersRequest;
import com.loyalty.service_admin.application.dto.rules.RuleCreateRequest;
import com.loyalty.service_admin.application.dto.rules.RuleCustomerTierDTO;
import com.loyalty.service_admin.application.dto.rules.RuleResponse;
import com.loyalty.service_admin.application.dto.rules.RuleResponseWithTiers;
import com.loyalty.service_admin.application.dto.rules.RuleAttributeMetadataDTO;
import com.loyalty.service_admin.application.service.RuleService;
import com.loyalty.service_admin.infrastructure.security.SecurityContextHelper;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Generic Rule Controller
 * 
 * HU-06, HU-07, HU-08: Unified endpoint for managing all types of rules:
 * - Product Rules (discount by product type)
 * - Classification Rules (discount by customer tier metrics)
 * - Fidelity Range Rules (loyalty tier segmentation)
 * 
 * Uses normalized architecture:
 * - rules table: generic rule data
 * - rule_attributes: metadata catalog per discount type
 * - rule_attribute_values: extended attributes per rule
 * - rule_customer_tiers: flexible tier assignment (many-to-many)
 * 
 * Single endpoint simplifies CRUD and allows reuse across all rule types.
 */
@RestController
@RequestMapping("/api/v1/rules")
@RequiredArgsConstructor
@Slf4j
public class RuleController {

    private final RuleService ruleService;
    private final SecurityContextHelper securityContextHelper;

    /**
     * Create a new rule (any type)
     * POST /api/v1/rules
     * 
     * Payload: { name, description, discountPercentage, discountPriorityId, attributes }
     * Mapping type determined by discountPriorityId (FIDELITY, SEASONAL, PRODUCT, CLASSIFICATION)
     */
    @PostMapping
    @PreAuthorize("hasRole('STORE_ADMIN')")
    public ResponseEntity<RuleResponse> createRule(@Valid @RequestBody RuleCreateRequest request) {
        UUID ecommerceId = securityContextHelper.getCurrentUserEcommerceId();
        log.info("Creating rule for ecommerce: {}", ecommerceId);

        RuleResponse response = ruleService.createRule(ecommerceId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * List rules with pagination and filtering
     * GET /api/v1/rules?page=0&size=20&active=true
     * 
     * Supports filtering by active status and pagination.
     */
    @GetMapping
    @PreAuthorize("hasRole('STORE_ADMIN')")
    public ResponseEntity<Page<RuleResponse>> listRules(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) Boolean active
    ) {
        UUID ecommerceId = securityContextHelper.getCurrentUserEcommerceId();
        Pageable pageable = PageRequest.of(page, size);

        Page<RuleResponse> response = ruleService.listRules(ecommerceId, active, pageable);
        return ResponseEntity.ok(response);
    }

    /**
     * Get rule details with all attributes and assigned tiers
     * GET /api/v1/rules/{ruleId}
     */
    @GetMapping("/{ruleId}")
    @PreAuthorize("hasRole('STORE_ADMIN')")
    public ResponseEntity<RuleResponseWithTiers> getRuleById(@PathVariable UUID ruleId) {
        UUID ecommerceId = securityContextHelper.getCurrentUserEcommerceId();
        log.info("Fetching rule: {}", ruleId);

        RuleResponse baseResponse = ruleService.getRuleById(ecommerceId, ruleId);
        List<RuleCustomerTierDTO> tiers = ruleService.getRuleAssignedTiers(ecommerceId, ruleId);

        RuleResponseWithTiers response = new RuleResponseWithTiers(
                baseResponse.id(),
                baseResponse.ecommerceId(),
                baseResponse.discountPriorityId(),
                baseResponse.name(),
                baseResponse.description(),
                baseResponse.discountPercentage(),
                baseResponse.isActive(),
                baseResponse.attributes(),
                tiers,
                baseResponse.createdAt(),
                baseResponse.updatedAt()
        );
        return ResponseEntity.ok(response);
    }

    /**
     * Update rule (name, description, discountPercentage, attributes)
     * PUT /api/v1/rules/{ruleId}
     */
    @PutMapping("/{ruleId}")
    @PreAuthorize("hasRole('STORE_ADMIN')")
    public ResponseEntity<RuleResponse> updateRule(
            @PathVariable UUID ruleId,
            @Valid @RequestBody RuleCreateRequest request
    ) {
        UUID ecommerceId = securityContextHelper.getCurrentUserEcommerceId();
        log.info("Updating rule: {}", ruleId);

        RuleResponse response = ruleService.updateRule(ecommerceId, ruleId, request);
        return ResponseEntity.ok(response);
    }

    /**
     * Delete rule (soft delete: isActive = false)
     * DELETE /api/v1/rules/{ruleId}
     */
    @DeleteMapping("/{ruleId}")
    @PreAuthorize("hasRole('STORE_ADMIN')")
    public ResponseEntity<Void> deleteRule(@PathVariable UUID ruleId) {
        UUID ecommerceId = securityContextHelper.getCurrentUserEcommerceId();
        log.info("Deleting rule: {}", ruleId);

        ruleService.deleteRule(ecommerceId, ruleId);
        return ResponseEntity.noContent().build();
    }

    /**
     * Assign customer tiers to a rule
     * POST /api/v1/rules/{ruleId}/tiers
     * 
     * Payload: { customerTierIds: [uuid1, uuid2, ...] }
     * Links a rule to specific customer tiers for flexible application.
     */
    @PostMapping("/{ruleId}/tiers")
    @PreAuthorize("hasRole('STORE_ADMIN')")
    public ResponseEntity<RuleResponseWithTiers> assignCustomerTiersToRule(
            @PathVariable UUID ruleId,
            @Valid @RequestBody AssignCustomerTiersRequest request
    ) {
        UUID ecommerceId = securityContextHelper.getCurrentUserEcommerceId();
        log.info("Assigning {} tiers to rule: {}", request.customerTierIds().size(), ruleId);

        RuleResponseWithTiers response = ruleService.assignCustomerTiersToRule(ecommerceId, ruleId, request.customerTierIds());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Get assigned tiers for a rule
     * GET /api/v1/rules/{ruleId}/tiers
     */
    @GetMapping("/{ruleId}/tiers")
    @PreAuthorize("hasRole('STORE_ADMIN')")
    public ResponseEntity<List<RuleCustomerTierDTO>> getRuleAssignedTiers(@PathVariable UUID ruleId) {
        UUID ecommerceId = securityContextHelper.getCurrentUserEcommerceId();
        log.info("Fetching assigned tiers for rule: {}", ruleId);

        List<RuleCustomerTierDTO> tiers = ruleService.getRuleAssignedTiers(ecommerceId, ruleId);
        return ResponseEntity.ok(tiers);
    }

    /**
     * Helper Endpoint: Get available attributes for a discount type
     * GET /api/v1/rules/attributes?discountTypeId={uuid}
     * 
     * Use this to know what attributes to send in the 'attributes' field
     * when creating a rule POST /api/v1/rules
     * 
     * Example response:
     * [
     *   { "id": "uuid", "attributeName": "product_type", "attributeType": "VARCHAR", "isRequired": true },
     *   { "id": "uuid", "attributeName": "min_purchase", "attributeType": "NUMERIC", "isRequired": false }
     * ]
     */
    @GetMapping("/attributes")
    @PreAuthorize("hasRole('STORE_ADMIN')")
    public ResponseEntity<List<RuleAttributeMetadataDTO>> getAvailableAttributes(
            @RequestParam UUID discountTypeId
    ) {
        log.info("Fetching available attributes for discount type: {}", discountTypeId);
        List<RuleAttributeMetadataDTO> attributes = ruleService.getAvailableAttributesForDiscountType(discountTypeId);
        return ResponseEntity.ok(attributes);
    }
}
