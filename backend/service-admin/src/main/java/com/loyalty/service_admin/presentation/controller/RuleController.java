package com.loyalty.service_admin.presentation.controller;

import com.loyalty.service_admin.application.dto.rules.AssignCustomerTiersRequest;
import com.loyalty.service_admin.application.dto.rules.RuleCreateRequest;
import com.loyalty.service_admin.application.dto.rules.RuleCustomerTierDTO;
import com.loyalty.service_admin.application.dto.rules.RuleResponse;
import com.loyalty.service_admin.application.dto.rules.RuleResponseWithTiers;
import com.loyalty.service_admin.application.dto.rules.RuleAttributeMetadataDTO;
import com.loyalty.service_admin.application.dto.discount.DiscountTypeDTO;
import com.loyalty.service_admin.application.dto.discount.DiscountPriorityDTO;
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

import com.loyalty.service_admin.infrastructure.exception.AuthorizationException;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/rules")
@Slf4j
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class RuleController {

    private final RuleService ruleService;
    private final SecurityContextHelper securityContextHelper;

    @GetMapping("/discount-types")
    public ResponseEntity<List<DiscountTypeDTO>> listDiscountTypes() {
        log.info("Fetching all discount types");
        List<DiscountTypeDTO> types = ruleService.getAllDiscountTypes();
        return ResponseEntity.ok(types);
    }

    @GetMapping("/attributes")
    public ResponseEntity<List<RuleAttributeMetadataDTO>> getAvailableAttributes(
            @RequestParam UUID discountTypeId
    ) {
        log.info("Fetching available attributes for discount type: {}", discountTypeId);
        List<RuleAttributeMetadataDTO> attributes = ruleService.getAvailableAttributesForDiscountType(discountTypeId);
        return ResponseEntity.ok(attributes);
    }

    @GetMapping("/discount-priorities")
    public ResponseEntity<List<DiscountPriorityDTO>> getDiscountPrioritiesByType(
            @RequestParam UUID discountTypeId
    ) {
        log.info("Fetching discount priorities for type: {}", discountTypeId);
        List<DiscountPriorityDTO> priorities = ruleService.getDiscountPrioritiesByType(discountTypeId);
        return ResponseEntity.ok(priorities);
    }


    @PostMapping
    public ResponseEntity<RuleResponse> createRule(@Valid @RequestBody RuleCreateRequest request) {
        UUID ecommerceId = securityContextHelper.getCurrentUserEcommerceId();
        log.info("Creating rule for ecommerce: {}", ecommerceId);

        if (ecommerceId == null) {
            throw new AuthorizationException(
                "El Usuario no puede crear reglas porque no tiene un ecommerceId asignado."
            );
        }

        RuleResponse response = ruleService.createRule(ecommerceId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
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

    @GetMapping("/{ruleId}")
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

    @PutMapping("/{ruleId}")
    public ResponseEntity<RuleResponse> updateRule(
            @PathVariable UUID ruleId,
            @Valid @RequestBody RuleCreateRequest request
    ) {
        UUID ecommerceId = securityContextHelper.getCurrentUserEcommerceId();
        log.info("Updating rule: {}", ruleId);

        if (ecommerceId == null) {
            throw new AuthorizationException(
                "El Usuario no puede actualizar reglas porque no tiene un ecommerceId asignado."
            );
        }

        RuleResponse response = ruleService.updateRule(ecommerceId, ruleId, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{ruleId}")
    public ResponseEntity<Void> deleteRule(@PathVariable UUID ruleId) {
        UUID ecommerceId = securityContextHelper.getCurrentUserEcommerceId();
        log.info("Deleting rule: {}", ruleId);

        if (ecommerceId == null) {
            throw new AuthorizationException(
                "El Usuario no puede eliminar reglas porque no tiene un ecommerceId asignado."
            );
        }

        ruleService.deleteRule(ecommerceId, ruleId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{ruleId}/tiers")
    public ResponseEntity<RuleResponseWithTiers> assignCustomerTiersToRule(
            @PathVariable UUID ruleId,
            @Valid @RequestBody AssignCustomerTiersRequest request
    ) {
        UUID ecommerceId = securityContextHelper.getCurrentUserEcommerceId();
        log.info("Assigning {} tiers to rule: {}", request.customerTierIds().size(), ruleId);

        if (ecommerceId == null) {
            throw new AuthorizationException(
                "El Usuario no puede crear tiers porque no tiene un ecommerceId asignado."
            );
        }

        RuleResponseWithTiers response = ruleService.assignCustomerTiersToRule(ecommerceId, ruleId, request.customerTierIds());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{ruleId}/tiers")
    public ResponseEntity<List<RuleCustomerTierDTO>> getRuleAssignedTiers(@PathVariable UUID ruleId) {
        UUID ecommerceId = securityContextHelper.getCurrentUserEcommerceId();
        log.info("Fetching assigned tiers for rule: {}", ruleId);

        List<RuleCustomerTierDTO> tiers = ruleService.getRuleAssignedTiers(ecommerceId, ruleId);
        return ResponseEntity.ok(tiers);
    }

    /**
     * HU-07 CRITERIO-7.3: Eliminar tier de una rule
     * DELETE /api/v1/rules/{ruleId}/tiers/{tierId}
     */
    @DeleteMapping("/{ruleId}/tiers/{tierId}")
    public ResponseEntity<Void> deleteCustomerTierFromRule(
            @PathVariable UUID ruleId,
            @PathVariable UUID tierId
    ) {
        UUID ecommerceId = securityContextHelper.getCurrentUserEcommerceId();
        log.info("Deleting tier {} from rule: {}", tierId, ruleId);

        if (ecommerceId == null) {
            throw new AuthorizationException(
                "El Usuario no puede eliminar tiers porque no tiene un ecommerceId asignado."
            );
        }

        ruleService.deleteCustomerTierFromRule(ecommerceId, ruleId, tierId);
        return ResponseEntity.noContent().build();
    }
}

