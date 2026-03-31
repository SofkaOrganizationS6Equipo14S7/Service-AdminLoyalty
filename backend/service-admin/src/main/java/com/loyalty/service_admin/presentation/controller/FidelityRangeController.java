package com.loyalty.service_admin.presentation.controller;

import com.loyalty.service_admin.application.dto.CreateFidelityRangeRequest;
import com.loyalty.service_admin.application.dto.FidelityRangeResponse;
import com.loyalty.service_admin.application.dto.UpdateFidelityRangeRequest;
import com.loyalty.service_admin.application.service.FidelityRangeService;
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
 * REST Controller for Fidelity Range management
 * 
 * Endpoints:
 * - POST /api/v1/fidelity-ranges → Create new range (201)
 * - GET /api/v1/fidelity-ranges → List ranges paginated (200)
 * - GET /api/v1/fidelity-ranges/{uid} → Get specific range (200)
 * - PUT /api/v1/fidelity-ranges/{uid} → Update range (200)
 * - DELETE /api/v1/fidelity-ranges/{uid} → Delete range (204)
 * 
 * Authentication: JWT required
 * Authorization: Admin role + fidelity:read / fidelity:write permissions
 */
@RestController
@RequestMapping("/api/v1/fidelity-ranges")
@RequiredArgsConstructor
@Slf4j
public class FidelityRangeController {
    
    private final FidelityRangeService fidelityRangeService;
    
    /**
     * POST /api/v1/fidelity-ranges
     * Create a new fidelity range
     * 
     * @param request the create request (validated via @Valid)
     * @param auth authentication (injected by Spring Security with JWT)
     * @return 201 Created with the range response
     */
    @PostMapping
    @PreAuthorize("hasRole('ADMIN') and hasAuthority('fidelity:write')")
    public ResponseEntity<FidelityRangeResponse> createFidelityRange(
        @Valid @RequestBody CreateFidelityRangeRequest request,
        Authentication auth
    ) {
        // Extract ecommerce_id from JWT token
        UUID ecommerceId = extractEcommerceId(auth);
        
        log.info("POST /api/v1/fidelity-ranges ecommerce={} name={}", ecommerceId, request.name());
        
        FidelityRangeResponse response = fidelityRangeService.create(request, ecommerceId);
        
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
    
    /**
     * GET /api/v1/fidelity-ranges
     * List all active fidelity ranges for the authenticated ecommerce (paginated)
     * 
     * @param page page number (default: 0)
     * @param size page size (default: 10, max: 100)
     * @param auth authentication (injected by Spring Security with JWT)
     * @return 200 OK with paginated list of ranges
     */
    @GetMapping
    @PreAuthorize("hasRole('ADMIN') and hasAuthority('fidelity:read')")
    public ResponseEntity<Page<FidelityRangeResponse>> getFidelityRanges(
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "10") int size,
        Authentication auth
    ) {
        // Extract ecommerce_id from JWT token
        UUID ecommerceId = extractEcommerceId(auth);
        
        // Validate page size
        if (size > 100) {
            size = 100;
        }
        
        Pageable pageable = PageRequest.of(page, size);
        
        log.info("GET /api/v1/fidelity-ranges ecommerce={} page={} size={}", ecommerceId, page, size);
        
        Page<FidelityRangeResponse> response = fidelityRangeService.listByEcommerce(ecommerceId, pageable);
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * GET /api/v1/fidelity-ranges/{uid}
     * Get a specific fidelity range by UID
     * 
     * @param uid the range UID
     * @param auth authentication (injected by Spring Security with JWT)
     * @return 200 OK with the range details
     */
    @GetMapping("/{uid}")
    @PreAuthorize("hasRole('ADMIN') and hasAuthority('fidelity:read')")
    public ResponseEntity<FidelityRangeResponse> getFidelityRange(
        @PathVariable UUID uid,
        Authentication auth
    ) {
        // Extract ecommerce_id from JWT token
        UUID ecommerceId = extractEcommerceId(auth);
        
        log.info("GET /api/v1/fidelity-ranges/{} ecommerce={}", uid, ecommerceId);
        
        FidelityRangeResponse response = fidelityRangeService.getById(uid, ecommerceId);
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * PUT /api/v1/fidelity-ranges/{uid}
     * Update an existing fidelity range
     * 
     * @param uid the range UID
     * @param request the update request (partial fields allowed)
     * @param auth authentication (injected by Spring Security with JWT)
     * @return 200 OK with the updated range
     */
    @PutMapping("/{uid}")
    @PreAuthorize("hasRole('ADMIN') and hasAuthority('fidelity:write')")
    public ResponseEntity<FidelityRangeResponse> updateFidelityRange(
        @PathVariable UUID uid,
        @Valid @RequestBody UpdateFidelityRangeRequest request,
        Authentication auth
    ) {
        // Extract ecommerce_id from JWT token
        UUID ecommerceId = extractEcommerceId(auth);
        
        log.info("PUT /api/v1/fidelity-ranges/{} ecommerce={}", uid, ecommerceId);
        
        FidelityRangeResponse response = fidelityRangeService.update(uid, request, ecommerceId);
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * DELETE /api/v1/fidelity-ranges/{uid}
     * Delete (soft delete) a fidelity range
     * 
     * @param uid the range UID
     * @param auth authentication (injected by Spring Security with JWT)
     * @return 204 No Content
     */
    @DeleteMapping("/{uid}")
    @PreAuthorize("hasRole('ADMIN') and hasAuthority('fidelity:write')")
    public ResponseEntity<Void> deleteFidelityRange(
        @PathVariable UUID uid,
        Authentication auth
    ) {
        // Extract ecommerce_id from JWT token
        UUID ecommerceId = extractEcommerceId(auth);
        
        log.info("DELETE /api/v1/fidelity-ranges/{} ecommerce={}", uid, ecommerceId);
        
        fidelityRangeService.delete(uid, ecommerceId);
        
        return ResponseEntity.noContent().build();
    }
    
    /**
     * Extract ecommerce_id from JWT authentication token
     * 
     * Assumes the JWT contains "ecommerce_id" claim or is passed via request attribute
     * (This is typically set by TenantInterceptor or similar security configuration)
     * 
     * @param auth Spring Security Authentication object
     * @return ecommerce UUID
     * @throws IllegalArgumentException if ecommerce_id not found in token
     */
    private UUID extractEcommerceId(Authentication auth) {
        // In a real implementation, this would extract from JWT claims
        // For now, placeholder implementation
        // This will be handled by TenantInterceptor or custom JWT processor
        
        if (auth == null || auth.getDetails() == null) {
            throw new IllegalArgumentException("ecommerce_id not found in JWT token");
        }
        
        String ecommerceIdStr = (String) auth.getDetails();
        if (ecommerceIdStr != null) {
            try {
                return UUID.fromString(ecommerceIdStr);
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("Invalid ecommerce_id in JWT: " + ecommerceIdStr);
            }
        }
        
        throw new IllegalArgumentException("ecommerce_id not found in JWT token");
    }
}
