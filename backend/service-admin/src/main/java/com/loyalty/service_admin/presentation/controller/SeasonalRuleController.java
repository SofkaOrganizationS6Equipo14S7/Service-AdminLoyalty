package com.loyalty.service_admin.presentation.controller;

import com.loyalty.service_admin.application.dto.SeasonalRuleCreateRequest;
import com.loyalty.service_admin.application.dto.SeasonalRuleResponse;
import com.loyalty.service_admin.application.dto.SeasonalRuleUpdateRequest;
import com.loyalty.service_admin.application.service.SeasonalRuleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * REST Controller for Seasonal Rules management
 * 
 * Endpoints:
 * - POST /api/v1/seasonal-rules → Create new rule (201)
 * - GET /api/v1/seasonal-rules → List rules paginated (200)
 * - GET /api/v1/seasonal-rules/{uid} → Get specific rule (200)
 * - PUT /api/v1/seasonal-rules/{uid} → Update rule (200)
 * - DELETE /api/v1/seasonal-rules/{uid} → Delete rule (204)
 * 
 * Authentication: JWT required (via TenantInterceptor)
 * Authorization: Admin role required
 */
@RestController
@RequestMapping("/api/v1/seasonal-rules")
@RequiredArgsConstructor
@Slf4j
public class SeasonalRuleController {
    
    private final SeasonalRuleService seasonalRuleService;
    
    /**
     * POST /api/v1/seasonal-rules
     * Create a new seasonal rule
     * 
     * @param request the create request (validated via @Valid)
     * @param auth authentication (injected by Spring Security with JWT)
     * @return 201 Created with the rule response
     */
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<SeasonalRuleResponse> createSeasonalRule(
        @Valid @RequestBody SeasonalRuleCreateRequest request,
        Authentication auth
    ) {
        // Extract ecommerce_id from JWT token (via Principal/Claims)
        UUID ecommerceId = extractEcommerceId(auth);
        
        log.info("POST /api/v1/seasonal-rules ecommerce={} name={}", ecommerceId, request.name());
        
        SeasonalRuleResponse response = seasonalRuleService.createSeasonalRule(request, ecommerceId);
        
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
    
    /**
     * GET /api/v1/seasonal-rules
     * List all active seasonal rules for the authenticated ecommerce (paginated)
     * 
     * @param page page number (default: 0)
     * @param size page size (default: 20, max: 100)
     * @param auth authentication (injected by Spring Security with JWT)
     * @return 200 OK with paginated list of rules
     */
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Page<SeasonalRuleResponse>> getSeasonalRules(
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size,
        Authentication auth
    ) {
        // Extract ecommerce_id from JWT token
        UUID ecommerceId = extractEcommerceId(auth);
        
        // Validate page size
        if (size > 100) {
            size = 100;
        }
        
        Pageable pageable = PageRequest.of(page, size);
        
        log.info("GET /api/v1/seasonal-rules ecommerce={} page={} size={}", ecommerceId, page, size);
        
        Page<SeasonalRuleResponse> response = seasonalRuleService.getSeasonalRules(ecommerceId, pageable);
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * GET /api/v1/seasonal-rules/{uid}
     * Get a specific seasonal rule by UID
     * 
     * @param uid the rule UID
     * @param auth authentication (injected by Spring Security with JWT)
     * @return 200 OK with the rule details
     */
    @GetMapping("/{uid}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<SeasonalRuleResponse> getSeasonalRule(
        @PathVariable UUID uid,
        Authentication auth
    ) {
        // Extract ecommerce_id from JWT token
        UUID ecommerceId = extractEcommerceId(auth);
        
        log.info("GET /api/v1/seasonal-rules/{} ecommerce={}", uid, ecommerceId);
        
        SeasonalRuleResponse response = seasonalRuleService.getSeasonalRule(uid, ecommerceId);
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * PUT /api/v1/seasonal-rules/{uid}
     * Update an existing seasonal rule
     * 
     * @param uid the rule UID
     * @param request the update request (partial fields allowed)
     * @param auth authentication (injected by Spring Security with JWT)
     * @return 200 OK with the updated rule
     */
    @PutMapping("/{uid}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<SeasonalRuleResponse> updateSeasonalRule(
        @PathVariable UUID uid,
        @Valid @RequestBody SeasonalRuleUpdateRequest request,
        Authentication auth
    ) {
        // Extract ecommerce_id from JWT token
        UUID ecommerceId = extractEcommerceId(auth);
        
        log.info("PUT /api/v1/seasonal-rules/{} ecommerce={}", uid, ecommerceId);
        
        SeasonalRuleResponse response = seasonalRuleService.updateSeasonalRule(uid, request, ecommerceId);
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * DELETE /api/v1/seasonal-rules/{uid}
     * Delete (soft delete) a seasonal rule
     * 
     * @param uid the rule UID
     * @param auth authentication (injected by Spring Security with JWT)
     * @return 204 No Content
     */
    @DeleteMapping("/{uid}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteSeasonalRule(
        @PathVariable UUID uid,
        Authentication auth
    ) {
        // Extract ecommerce_id from JWT token
        UUID ecommerceId = extractEcommerceId(auth);
        
        log.info("DELETE /api/v1/seasonal-rules/{} ecommerce={}", uid, ecommerceId);
        
        seasonalRuleService.deleteSeasonalRule(uid, ecommerceId);
        
        return ResponseEntity.noContent().build();
    }
    
    /**
     * Extract ecommerce_id from JWT authentication token
     * 
     * Assumes the JWT contains "ecommerce_id" claim
     * (This is set by TenantInterceptor or similar security configuration)
     * 
     * @param auth Spring Security Authentication object
     * @return ecommerce UUID
     * @throws IllegalArgumentException if ecommerce_id not found in token
     */
    private UUID extractEcommerceId(Authentication auth) {
        // Get the principal (typically a JWT token or custom UserPrincipal)
        Object principal = auth.getPrincipal();
        
        // If using Spring Security's OAuth2/JWT, extract from JWT claims
        // For now, we assume it's passed as a principal attribute
        // This will be handled by TenantInterceptor or custom JWT processor
        
        // Placeholder: In real implementation, this would extract from JWT
        // For testing, you might need to use @RequestAttribute("ecommerceId")
        // or extract from the JWT token properties
        
        if (principal instanceof String) {
            // If principal is just the subject (user ID), we need another way to get ecommerce_id
            // This is typically in the JWT claims
            String ecommerceIdStr = (String) auth.getDetails();
            if (ecommerceIdStr != null) {
                try {
                    return UUID.fromString(ecommerceIdStr);
                } catch (IllegalArgumentException e) {
                    throw new IllegalArgumentException("Invalid ecommerce_id in JWT: " + ecommerceIdStr);
                }
            }
        }
        
        throw new IllegalArgumentException("ecommerce_id not found in JWT token");
    }
}
