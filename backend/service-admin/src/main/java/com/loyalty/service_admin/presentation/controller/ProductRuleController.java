package com.loyalty.service_admin.presentation.controller;

import com.loyalty.service_admin.application.dto.ProductRuleCreateRequest;
import com.loyalty.service_admin.application.dto.ProductRuleResponse;
import com.loyalty.service_admin.application.dto.ProductRuleUpdateRequest;
import com.loyalty.service_admin.application.service.ProductRuleService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * REST Controller for Product Rules management
 * 
 * Endpoints:
 * - POST /api/v1/product-rules → Create new rule (201)
 * - GET /api/v1/product-rules → List rules paginated (200)
 * - GET /api/v1/product-rules/{uid} → Get specific rule (200)
 * - PUT /api/v1/product-rules/{uid} → Update rule (200)
 * - DELETE /api/v1/product-rules/{uid} → Delete rule (204)
 * 
 * Authentication: JWT required (via Security configuration)
 * Authorization: Admin role required
 */
@RestController
@RequestMapping("/api/v1/product-rules")
@Slf4j
public class ProductRuleController {
    
    private final ProductRuleService productRuleService;
    
    public ProductRuleController(ProductRuleService productRuleService) {
        this.productRuleService = productRuleService;
    }
    
    /**
     * POST /api/v1/product-rules
     * Create a new product rule
     * 
     * @param request the create request (validated via @Valid)
     * @param auth authentication (injected by Spring Security with JWT)
     * @return 201 Created with the rule response
     */
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ProductRuleResponse> createProductRule(
        @Valid @RequestBody ProductRuleCreateRequest request,
        Authentication auth
    ) {
        // Extract ecommerce_id from JWT token
        UUID ecommerceId = extractEcommerceId(auth);
        
        log.info("POST /api/v1/product-rules ecommerce={} productType={}", ecommerceId, request.productType());
        
        ProductRuleResponse response = productRuleService.createProductRule(request, ecommerceId);
        
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
    
    /**
     * GET /api/v1/product-rules
     * List all product rules for the authenticated ecommerce (paginated)
     * 
     * @param page page number (default: 0)
     * @param size page size (default: 20, max: 100)
     * @param active filter by active status (true=active only, false=inactive only, null=all)
     * @param auth authentication (injected by Spring Security with JWT)
     * @return 200 OK with paginated list of rules
     */
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Page<ProductRuleResponse>> getProductRules(
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size,
        @RequestParam(required = false) Boolean active,
        Authentication auth
    ) {
        // Extract ecommerce_id from JWT token
        UUID ecommerceId = extractEcommerceId(auth);
        
        // Validate page size
        if (size > 100) {
            size = 100;
        }
        
        Pageable pageable = PageRequest.of(page, size);
        
        log.info("GET /api/v1/product-rules ecommerce={} page={} size={} active={}", 
            ecommerceId, page, size, active);
        
        Page<ProductRuleResponse> response = productRuleService.getProductRules(ecommerceId, pageable, active);
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * GET /api/v1/product-rules/{uid}
     * Get a specific product rule by UID
     * 
     * @param uid the rule UID
     * @param auth authentication (injected by Spring Security with JWT)
     * @return 200 OK with the rule details
     */
    @GetMapping("/{uid}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ProductRuleResponse> getProductRule(
        @PathVariable UUID uid,
        Authentication auth
    ) {
        // Extract ecommerce_id from JWT token
        UUID ecommerceId = extractEcommerceId(auth);
        
        log.info("GET /api/v1/product-rules/{} ecommerce={}", uid, ecommerceId);
        
        ProductRuleResponse response = productRuleService.getProductRule(uid, ecommerceId);
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * PUT /api/v1/product-rules/{uid}
     * Update an existing product rule
     * 
     * @param uid the rule UID
     * @param request the update request (partial fields allowed)
     * @param auth authentication (injected by Spring Security with JWT)
     * @return 200 OK with the updated rule
     */
    @PutMapping("/{uid}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ProductRuleResponse> updateProductRule(
        @PathVariable UUID uid,
        @Valid @RequestBody ProductRuleUpdateRequest request,
        Authentication auth
    ) {
        // Extract ecommerce_id from JWT token
        UUID ecommerceId = extractEcommerceId(auth);
        
        log.info("PUT /api/v1/product-rules/{} ecommerce={}", uid, ecommerceId);
        
        ProductRuleResponse response = productRuleService.updateProductRule(uid, request, ecommerceId);
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * DELETE /api/v1/product-rules/{uid}
     * Delete (soft delete) a product rule
     * 
     * @param uid the rule UID
     * @param auth authentication (injected by Spring Security with JWT)
     * @return 204 No Content
     */
    @DeleteMapping("/{uid}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteProductRule(
        @PathVariable UUID uid,
        Authentication auth
    ) {
        // Extract ecommerce_id from JWT token
        UUID ecommerceId = extractEcommerceId(auth);
        
        log.info("DELETE /api/v1/product-rules/{} ecommerce={}", uid, ecommerceId);
        
        productRuleService.deleteProductRule(uid, ecommerceId);
        
        return ResponseEntity.noContent().build();
    }
    
    /**
     * Extract ecommerce_id from JWT authentication token
     * 
     * Assumes the JWT contains ecommerce_id in claims or as request attribute.
     * This is typically set by a custom JWT processor or security filter.
     * 
     * @param auth Spring Security Authentication object
     * @return ecommerce UUID
     * @throws IllegalArgumentException if ecommerce_id not found in token
     */
    private UUID extractEcommerceId(Authentication auth) {
        // Try to extract from principal details
        Object principal = auth.getPrincipal();
        
        if (principal instanceof String) {
            // If principal is just the subject (user ID), we need to extract ecommerce_id from details
            Object details = auth.getDetails();
            if (details instanceof String) {
                try {
                    return UUID.fromString((String) details);
                } catch (IllegalArgumentException e) {
                    throw new IllegalArgumentException("Invalid ecommerce_id in JWT: " + details);
                }
            }
        }
        
        throw new IllegalArgumentException("ecommerce_id not found in JWT token");
    }
}
