package com.loyalty.service_admin.presentation.controller;

import com.loyalty.service_admin.application.dto.ClassificationRuleCreateRequest;
import com.loyalty.service_admin.application.dto.ClassificationRuleResponse;
import com.loyalty.service_admin.application.dto.ClassificationRuleUpdateRequest;
import com.loyalty.service_admin.application.dto.CustomerTierCreateRequest;
import com.loyalty.service_admin.application.dto.CustomerTierResponse;
import com.loyalty.service_admin.application.service.ClassificationRuleService;
import com.loyalty.service_admin.application.service.CustomerTierService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * REST Controller for Classification Admin Operations.
 *
 * Manages customer tiers and classification rules (source of truth).
 * All endpoints require SUPER_ADMIN role via JWT token.
 *
 * Endpoints:
 * - POST /api/v1/admin/tiers: Create a new tier
 * - GET /api/v1/admin/tiers: List active tiers (ordered by level)
 * - DELETE /api/v1/admin/tiers/{uid}: Soft-delete a tier
 *
 * - POST /api/v1/admin/classification-rules: Create a rule
 * - GET /api/v1/admin/classification-rules: List active rules
 * - GET /api/v1/admin/classification-rules/tier/{tierUid}: List rules for a specific tier
 * - PUT /api/v1/admin/classification-rules/{uid}: Update a rule (partial)
 * - DELETE /api/v1/admin/classification-rules/{uid}: Soft-delete a rule
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('SUPER_ADMIN')")
public class ClassificationAdminController {

    private final CustomerTierService tierService;
    private final ClassificationRuleService ruleService;

    // ============== CUSTOMER TIER ENDPOINTS ==============

    @PostMapping("/tiers")
    public ResponseEntity<CustomerTierResponse> createTier(
            @Valid @RequestBody CustomerTierCreateRequest request) {
        log.info("Creating new customer tier: {}", request.name());
        CustomerTierResponse response = tierService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/tiers")
    public ResponseEntity<List<CustomerTierResponse>> listActiveTiers() {
        log.info("Listing active customer tiers");
        List<CustomerTierResponse> tiers = tierService.listActive();
        return ResponseEntity.ok(tiers);
    }

    @DeleteMapping("/tiers/{uid}")
    public ResponseEntity<Void> deleteTier(@PathVariable UUID uid) {
        log.info("Deleting customer tier: {}", uid);
        tierService.delete(uid);
        return ResponseEntity.noContent().build();
    }

    // ============== CLASSIFICATION RULE ENDPOINTS ==============

    @PostMapping("/classification-rules")
    public ResponseEntity<ClassificationRuleResponse> createRule(
            @Valid @RequestBody ClassificationRuleCreateRequest request) {
        log.info("Creating classification rule for tier: {}", request.tierUid());
        ClassificationRuleResponse response = ruleService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/classification-rules")
    public ResponseEntity<List<ClassificationRuleResponse>> listActiveRules() {
        log.info("Listing active classification rules");
        List<ClassificationRuleResponse> rules = ruleService.listActive();
        return ResponseEntity.ok(rules);
    }

    @GetMapping("/classification-rules/tier/{tierUid}")
    public ResponseEntity<List<ClassificationRuleResponse>> listRulesByTier(
            @PathVariable UUID tierUid) {
        log.info("Listing classification rules for tier: {}", tierUid);
        List<ClassificationRuleResponse> rules = ruleService.listByTier(tierUid);
        return ResponseEntity.ok(rules);
    }

    @PutMapping("/classification-rules/{uid}")
    public ResponseEntity<ClassificationRuleResponse> updateRule(
            @PathVariable UUID uid,
            @Valid @RequestBody ClassificationRuleUpdateRequest request) {
        log.info("Updating classification rule: {}", uid);
        ClassificationRuleResponse response = ruleService.update(uid, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/classification-rules/{uid}")
    public ResponseEntity<Void> deleteRule(@PathVariable UUID uid) {
        log.info("Deleting classification rule: {}", uid);
        ruleService.delete(uid);
        return ResponseEntity.noContent().build();
    }
}
