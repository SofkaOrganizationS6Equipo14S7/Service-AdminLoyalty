package com.loyalty.service_admin.presentation.controller;

import com.loyalty.service_admin.application.dto.rules.AssignCustomerTiersRequest;
import com.loyalty.service_admin.application.dto.rules.RuleCreateRequest;
import com.loyalty.service_admin.application.dto.rules.RuleCustomerTierDTO;
import com.loyalty.service_admin.application.dto.rules.RuleResponse;
import com.loyalty.service_admin.application.dto.rules.RuleResponseWithTiers;
import com.loyalty.service_admin.application.dto.rules.RuleAttributeMetadataDTO;
import com.loyalty.service_admin.presentation.dto.rules.RuleStatusUpdateRequest;
import com.loyalty.service_admin.application.dto.discount.DiscountTypeDTO;
import com.loyalty.service_admin.application.dto.discount.DiscountPriorityDTO;
import com.loyalty.service_admin.application.dto.classificationrule.ClassificationRuleCreateRequest;
import com.loyalty.service_admin.application.dto.classificationrule.ClassificationRuleUpdateRequest;
import com.loyalty.service_admin.application.dto.classificationrule.ClassificationRuleResponse;
import com.loyalty.service_admin.application.port.in.RuleUseCase;
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

    private final RuleUseCase ruleUseCase;
    private final SecurityContextHelper securityContextHelper;

    @GetMapping("/discount-types")
    public ResponseEntity<List<DiscountTypeDTO>> listDiscountTypes() {
        log.info("Fetching all discount types");
        List<DiscountTypeDTO> types = ruleUseCase.getAllDiscountTypes();
        return ResponseEntity.ok(types);
    }

    @GetMapping("/attributes")
    public ResponseEntity<List<RuleAttributeMetadataDTO>> getAvailableAttributes(
            @RequestParam UUID discountTypeId
    ) {
        log.info("Fetching available attributes for discount type: {}", discountTypeId);
        List<RuleAttributeMetadataDTO> attributes = ruleUseCase.getAvailableAttributesForDiscountType(discountTypeId);
        return ResponseEntity.ok(attributes);
    }

    @GetMapping("/discount-priorities")
    public ResponseEntity<List<DiscountPriorityDTO>> getDiscountPrioritiesByType(
            @RequestParam UUID discountTypeId
    ) {
        log.info("Fetching discount priorities for type: {}", discountTypeId);
        List<DiscountPriorityDTO> priorities = ruleUseCase.getDiscountPrioritiesByType(discountTypeId);
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

        RuleResponse response = ruleUseCase.createRule(ecommerceId, request);
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

        Page<RuleResponse> response = ruleUseCase.listRules(ecommerceId, active, pageable);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{ruleId}")
    public ResponseEntity<RuleResponseWithTiers> getRuleById(@PathVariable UUID ruleId) {
        UUID ecommerceId = securityContextHelper.getCurrentUserEcommerceId();
        log.info("Fetching rule: {}", ruleId);

        RuleResponse baseResponse = ruleUseCase.getRuleById(ecommerceId, ruleId);
        List<RuleCustomerTierDTO> tiers = ruleUseCase.getRuleAssignedTiers(ecommerceId, ruleId);

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

        RuleResponse response = ruleUseCase.updateRule(ecommerceId, ruleId, request);
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

        ruleUseCase.deleteRule(ecommerceId, ruleId);
        return ResponseEntity.noContent().build();
    }

    /**
     * SPEC-008: HU-14 - Change rule status (active/inactive) via PATCH endpoint
     * 
     * PATCH /api/v1/rules/{ruleId}/status
     * Body: { "active": true | false }
     */
    @PatchMapping("/{ruleId}/status")
    public ResponseEntity<RuleResponse> updateRuleStatus(
            @PathVariable UUID ruleId,
            @Valid @RequestBody RuleStatusUpdateRequest request
    ) {
        UUID ecommerceId = securityContextHelper.getCurrentUserEcommerceId();
        log.info("Updating rule status: ruleId={}, newStatus={}", ruleId, request.active());

        if (ecommerceId == null) {
            throw new AuthorizationException(
                "El Usuario no puede actualizar el status de reglas porque no tiene un ecommerceId asignado."
            );
        }

        RuleResponse response = ruleUseCase.updateRuleStatus(ecommerceId, ruleId, request.active());
        return ResponseEntity.ok(response);
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

        RuleResponseWithTiers response = ruleUseCase.assignCustomerTiersToRule(ecommerceId, ruleId, request.customerTierIds());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{ruleId}/tiers")
    public ResponseEntity<List<RuleCustomerTierDTO>> getRuleAssignedTiers(@PathVariable UUID ruleId) {
        UUID ecommerceId = securityContextHelper.getCurrentUserEcommerceId();
        log.info("Fetching assigned tiers for rule: {}", ruleId);

        List<RuleCustomerTierDTO> tiers = ruleUseCase.getRuleAssignedTiers(ecommerceId, ruleId);
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

        ruleUseCase.deleteCustomerTierFromRule(ecommerceId, ruleId, tierId);
        return ResponseEntity.noContent().build();
    }

    // ========== HU-07: NESTED ENDPOINTS FOR CLASSIFICATION RULES ==========

    /**
     * HU-07 CRITERIO-7.3, 7.4: Crear classification_rule para un customer_tier
     * POST /api/v1/rules/customer-tiers/{tierId}
     * 
     * Request incluye: metricType, minValue, maxValue, priority
     * Internamente crea un RuleEntity con type=CLASSIFICATION
     */
    @PostMapping("/customer-tiers/{tierId}")
    public ResponseEntity<ClassificationRuleResponse> createClassificationRuleForTier(
            @PathVariable UUID tierId,
            @Valid @RequestBody ClassificationRuleCreateRequest request
    ) {
        UUID ecommerceId = securityContextHelper.getCurrentUserEcommerceId();
        log.info("POST /api/v1/rules/customer-tiers/{} - Creating classification rule. metricType={}", 
            tierId, request.metricType());

        if (ecommerceId == null) {
            throw new AuthorizationException(
                "El Usuario no puede crear reglas porque no tiene un ecommerceId asignado."
            );
        }

        ClassificationRuleResponse response = ruleUseCase.createClassificationRuleForTier(ecommerceId, tierId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * HU-07 CRITERIO-7.6: Listar classification_rules de un tier
     * GET /api/v1/rules/customer-tiers/{tierId}
     */
    @GetMapping("/customer-tiers/{tierId}")
    public ResponseEntity<List<ClassificationRuleResponse>> listClassificationRulesForTier(
            @PathVariable UUID tierId
    ) {
        UUID ecommerceId = securityContextHelper.getCurrentUserEcommerceId();
        log.info("GET /api/v1/rules/customer-tiers/{} - Listing classification rules", tierId);

        if (ecommerceId == null) {
            throw new AuthorizationException(
                "El Usuario no puede listar reglas porque no tiene un ecommerceId asignado."
            );
        }

        List<ClassificationRuleResponse> response = ruleUseCase.listClassificationRulesForTier(ecommerceId, tierId);
        return ResponseEntity.ok(response);
    }

    /**
     * HU-07 CRITERIO-7.7: Actualizar classification_rule
     * PUT /api/v1/rules/customer-tiers/{tierId}/{ruleId}
     */
    @PutMapping("/customer-tiers/{tierId}/{ruleId}")
    public ResponseEntity<ClassificationRuleResponse> updateClassificationRuleForTier(
            @PathVariable UUID tierId,
            @PathVariable UUID ruleId,
            @Valid @RequestBody ClassificationRuleUpdateRequest request
    ) {
        UUID ecommerceId = securityContextHelper.getCurrentUserEcommerceId();
        log.info("PUT /api/v1/rules/customer-tiers/{}/{} - Updating classification rule", tierId, ruleId);

        if (ecommerceId == null) {
            throw new AuthorizationException(
                "El Usuario no puede actualizar reglas porque no tiene un ecommerceId asignado."
            );
        }

        ClassificationRuleResponse response = ruleUseCase.updateClassificationRuleForTier(ecommerceId, tierId, ruleId, request);
        return ResponseEntity.ok(response);
    }

    /**
     * HU-07 CRITERIO-7.8: Eliminar classification_rule (soft delete)
     * DELETE /api/v1/rules/customer-tiers/{tierId}/{ruleId}
     */
    @DeleteMapping("/customer-tiers/{tierId}/{ruleId}")
    public ResponseEntity<Void> deleteClassificationRuleForTier(
            @PathVariable UUID tierId,
            @PathVariable UUID ruleId
    ) {
        UUID ecommerceId = securityContextHelper.getCurrentUserEcommerceId();
        log.info("DELETE /api/v1/rules/customer-tiers/{}/{} - Soft deleting classification rule", tierId, ruleId);

        if (ecommerceId == null) {
            throw new AuthorizationException(
                "El Usuario no puede eliminar reglas porque no tiene un ecommerceId asignado."
            );
        }

        ruleUseCase.deleteClassificationRuleForTier(ecommerceId, tierId, ruleId);
        return ResponseEntity.noContent().build();
    }
}

