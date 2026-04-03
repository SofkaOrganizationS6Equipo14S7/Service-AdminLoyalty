package com.loyalty.service_admin.presentation.controller;

import com.loyalty.service_admin.application.dto.rules.seasonal.SeasonalRuleCreateRequest;
import com.loyalty.service_admin.application.dto.rules.seasonal.SeasonalRuleResponse;
import com.loyalty.service_admin.application.dto.rules.seasonal.SeasonalRuleUpdateRequest;
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

@RestController
@RequestMapping("/api/v1/seasonal-rules")
@RequiredArgsConstructor
@Slf4j
public class SeasonalRuleController {
    
    private final SeasonalRuleService seasonalRuleService;
    
    /**
     * @param request rule creation data
     * @param auth authentication (injected by Spring Security)
     * @return HTTP 201 Created with SeasonalRuleResponse
     */
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<SeasonalRuleResponse> createSeasonalRule(
        @Valid @RequestBody SeasonalRuleCreateRequest request,
        Authentication auth
    ) {
        UUID ecommerceId = extractEcommerceId(auth);
        SeasonalRuleResponse response = seasonalRuleService.createSeasonalRule(request, ecommerceId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
    
    /**
     * @param page page number (default: 0)
     * @param size page size (default: 20, max: 100)
     * @param auth authentication (injected by Spring Security)
     * @return HTTP 200 OK with paginated list
     */
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Page<SeasonalRuleResponse>> getSeasonalRules(
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size,
        Authentication auth
    ) {
        UUID ecommerceId = extractEcommerceId(auth);
        
        if (size > 100) {
            size = 100;
        }
        
        Pageable pageable = PageRequest.of(page, size);
        Page<SeasonalRuleResponse> response = seasonalRuleService.getSeasonalRules(ecommerceId, pageable);
        return ResponseEntity.ok(response);
    }
    
    /**
     * @param uid rule identifier
     * @param auth authentication (injected by Spring Security)
     * @return HTTP 200 OK with rule details
     */
    @GetMapping("/{uid}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<SeasonalRuleResponse> getSeasonalRule(
        @PathVariable UUID uid,
        Authentication auth
    ) {
        UUID ecommerceId = extractEcommerceId(auth);
        SeasonalRuleResponse response = seasonalRuleService.getSeasonalRule(uid, ecommerceId);
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
    public ResponseEntity<SeasonalRuleResponse> updateSeasonalRule(
        @PathVariable UUID uid,
        @Valid @RequestBody SeasonalRuleUpdateRequest request,
        Authentication auth
    ) {
        UUID ecommerceId = extractEcommerceId(auth);
        SeasonalRuleResponse response = seasonalRuleService.updateSeasonalRule(uid, request, ecommerceId);
        return ResponseEntity.ok(response);
    }
    
    /**
     * @param uid rule identifier
     * @param auth authentication (injected by Spring Security)
     * @return HTTP 204 No Content
     */
    @DeleteMapping("/{uid}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteSeasonalRule(
        @PathVariable UUID uid,
        Authentication auth
    ) {
        UUID ecommerceId = extractEcommerceId(auth);
        seasonalRuleService.deleteSeasonalRule(uid, ecommerceId);
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
