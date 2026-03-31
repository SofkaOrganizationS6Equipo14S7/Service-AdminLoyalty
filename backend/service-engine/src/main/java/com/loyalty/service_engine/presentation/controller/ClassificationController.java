package com.loyalty.service_engine.presentation.controller;

import com.loyalty.service_engine.application.dto.ClassifyRequestV1;
import com.loyalty.service_engine.application.dto.ClassifyResponseV1;
import com.loyalty.service_engine.application.service.ClassificationEngine;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST Controller for Customer Classification Execution.
 *
 * Exposes the classification endpoint to e-commerce services.
 * Uses API Key authentication (validated by SecurityContext).
 *
 * Endpoints:
 * - POST /api/v1/customers/classify: Classify a customer based on metrics
 *
 * The classification engine:
 * 1. Loads the customer tier matrix from Caffeine cache (or database on cold start)
 * 2. Evaluates classification rules against the provided metrics
 * 3. Returns the highest matching tier or null if no tier matches
 *
 * Response includes classification reason for debugging and audit purposes.
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/customers")
@RequiredArgsConstructor
public class ClassificationController {

    private final ClassificationEngine classificationEngine;

    @PostMapping("/classify")
    public ResponseEntity<ClassifyResponseV1> classify(
            @Valid @RequestBody ClassifyRequestV1 request) {
        log.info("Classifying customer: totalSpent={}, orderCount={}, loyaltyPoints={}",
                request.totalSpent(), request.orderCount(), request.loyaltyPoints());
        
        ClassifyResponseV1 response = classificationEngine.classify(request);
        
        log.info("Classification result: tierUid={}, tierName={}, tierLevel={}, reason={}",
                response.tierUid(), response.tierName(), response.tierLevel(), response.classificationReason());
        
        return ResponseEntity.ok(response);
    }
}
