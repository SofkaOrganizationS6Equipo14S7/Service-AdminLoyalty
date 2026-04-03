package com.loyalty.service_admin.presentation.controller;

import com.loyalty.service_admin.application.dto.rules.product.ProductRuleCreateRequest;
import com.loyalty.service_admin.application.dto.rules.product.ProductRuleResponse;
import com.loyalty.service_admin.application.dto.rules.product.ProductRuleUpdateRequest;
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

@RestController
@RequestMapping("/api/v1/product-rules")
@Slf4j
public class ProductRuleController {
    
    private final ProductRuleService productRuleService;
    
    public ProductRuleController(ProductRuleService productRuleService) {
        this.productRuleService = productRuleService;
    }
    
    /**
     * @param request rule creation data
     * @param auth authentication (injected by Spring Security)
     * @return HTTP 201 Created with ProductRuleResponse
     */
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ProductRuleResponse> createProductRule(
        @Valid @RequestBody ProductRuleCreateRequest request,
        Authentication auth
    ) {
        UUID ecommerceId = extractEcommerceId(auth);
        ProductRuleResponse response = productRuleService.createProductRule(request, ecommerceId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
    
    /**
     * @param page page number (default: 0)
     * @param size page size (default: 20, max: 100)
     * @param active filter by active status (optional)
     * @param auth authentication (injected by Spring Security)
     * @return HTTP 200 OK with paginated list
     */
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Page<ProductRuleResponse>> getProductRules(
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size,
        @RequestParam(required = false) Boolean active,
        Authentication auth
    ) {
        UUID ecommerceId = extractEcommerceId(auth);
        
        if (size > 100) {
            size = 100;
        }
        
        Pageable pageable = PageRequest.of(page, size);
        Page<ProductRuleResponse> response = productRuleService.getProductRules(ecommerceId, pageable, active);
        return ResponseEntity.ok(response);
    }
    
    /**
     * @param uid rule identifier
     * @param auth authentication (injected by Spring Security)
     * @return HTTP 200 OK with rule details
     */
    @GetMapping("/{uid}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ProductRuleResponse> getProductRule(
        @PathVariable UUID uid,
        Authentication auth
    ) {
        UUID ecommerceId = extractEcommerceId(auth);
        ProductRuleResponse response = productRuleService.getProductRule(uid, ecommerceId);
        return ResponseEntity.ok(response);
    }
    
    /**
     * @param uid rule identifier
     * @param request partial update data
     * @param auth authentication (injected by Spring Security)
     * @return HTTP 200 OK with updated rule
     */
    @PutMapping("/{uid}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ProductRuleResponse> updateProductRule(
        @PathVariable UUID uid,
        @Valid @RequestBody ProductRuleUpdateRequest request,
        Authentication auth
    ) {
        UUID ecommerceId = extractEcommerceId(auth);
        ProductRuleResponse response = productRuleService.updateProductRule(uid, request, ecommerceId);
        return ResponseEntity.ok(response);
    }
    
    /**
     * @param uid rule identifier
     * @param auth authentication (injected by Spring Security)
     * @return HTTP 204 No Content
     */
    @DeleteMapping("/{uid}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteProductRule(
        @PathVariable UUID uid,
        Authentication auth
    ) {
        UUID ecommerceId = extractEcommerceId(auth);
        productRuleService.deleteProductRule(uid, ecommerceId);
        return ResponseEntity.noContent().build();
    }
    
    /**
     * @param auth Spring Security Authentication object
     * @return ecommerce UUID
     * @throws IllegalArgumentException if ecommerce_id not found in token
     */
    private UUID extractEcommerceId(Authentication auth) {
        Object principal = auth.getPrincipal();
        
        if (principal instanceof String) {
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
