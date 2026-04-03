package com.loyalty.service_admin.presentation.controller;

import com.loyalty.service_admin.application.dto.rules.classification.ClassificationRuleCreateRequest;
import com.loyalty.service_admin.application.dto.rules.classification.ClassificationRuleResponse;
import com.loyalty.service_admin.application.dto.rules.classification.ClassificationRuleUpdateRequest;
import com.loyalty.service_admin.application.dto.customertier.CustomerTierCreateRequest;
import com.loyalty.service_admin.application.dto.customertier.CustomerTierResponse;
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

@Slf4j
@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('SUPER_ADMIN')")
public class ClassificationAdminController {

    private final CustomerTierService tierService;
    private final ClassificationRuleService ruleService;

    /**
     * @param request tier creation data
     * @return HTTP 201 Created with CustomerTierResponse
     */
    @PostMapping("/tiers")
    public ResponseEntity<CustomerTierResponse> createTier(
            @Valid @RequestBody CustomerTierCreateRequest request) {
        CustomerTierResponse response = tierService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * @return HTTP 200 OK with list of active tiers ordered by level
     */
    @GetMapping("/tiers")
    public ResponseEntity<List<CustomerTierResponse>> listActiveTiers() {
        List<CustomerTierResponse> tiers = tierService.listActive();
        return ResponseEntity.ok(tiers);
    }

    /**
     * @param uid tier identifier
     * @return HTTP 204 No Content
     */
    @DeleteMapping("/tiers/{uid}")
    public ResponseEntity<Void> deleteTier(@PathVariable UUID uid) {
        tierService.delete(uid);
        return ResponseEntity.noContent().build();
    }

    /**
     * @param request rule creation data
     * @return HTTP 201 Created with ClassificationRuleResponse
     */
    @PostMapping("/classification-rules")
    public ResponseEntity<ClassificationRuleResponse> createRule(
            @Valid @RequestBody ClassificationRuleCreateRequest request) {
        ClassificationRuleResponse response = ruleService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * @return HTTP 200 OK with list of active classification rules
     */
    @GetMapping("/classification-rules")
    public ResponseEntity<List<ClassificationRuleResponse>> listActiveRules() {
        List<ClassificationRuleResponse> rules = ruleService.listActive();
        return ResponseEntity.ok(rules);
    }

    /**
     * @param tierUid tier identifier
     * @return HTTP 200 OK with list of classification rules for the tier
     */
    @GetMapping("/classification-rules/tier/{tierUid}")
    public ResponseEntity<List<ClassificationRuleResponse>> listRulesByTier(
            @PathVariable UUID tierUid) {
        List<ClassificationRuleResponse> rules = ruleService.listByTier(tierUid);
        return ResponseEntity.ok(rules);
    }

    /**
     * @param uid rule identifier
     * @param request partial rule data to update
     * @return HTTP 200 OK with updated ClassificationRuleResponse
     */
    @PutMapping("/classification-rules/{uid}")
    public ResponseEntity<ClassificationRuleResponse> updateRule(
            @PathVariable UUID uid,
            @Valid @RequestBody ClassificationRuleUpdateRequest request) {
        ClassificationRuleResponse response = ruleService.update(uid, request);
        return ResponseEntity.ok(response);
    }

    /**
     * @param uid rule identifier
     * @return HTTP 204 No Content
     */
    @DeleteMapping("/classification-rules/{uid}")
    public ResponseEntity<Void> deleteRule(@PathVariable UUID uid) {
        ruleService.delete(uid);
        return ResponseEntity.noContent().build();
    }
}
