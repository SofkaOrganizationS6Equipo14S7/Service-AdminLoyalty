package com.loyalty.service_admin.presentation.controller;

import com.loyalty.service_admin.application.dto.ecommerce.EcommerceCreateRequest;
import com.loyalty.service_admin.application.dto.ecommerce.EcommerceResponse;
import com.loyalty.service_admin.application.dto.ecommerce.EcommerceUpdateStatusRequest;
import com.loyalty.service_admin.application.port.in.EcommerceUseCase;
import com.loyalty.service_admin.infrastructure.exception.AuthorizationException;
import com.loyalty.service_admin.infrastructure.security.SecurityContextHelper;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/ecommerces")
@RequiredArgsConstructor
@Slf4j
@PreAuthorize("isAuthenticated()")
public class EcommerceController {
    
    private final EcommerceUseCase ecommerceUseCase;
    private final SecurityContextHelper securityContextHelper;
    
    /**
     * @param request ecommerce data (name, slug)
     * @return HTTP 201 Created with EcommerceResponse
     */
    @PostMapping
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<EcommerceResponse> createEcommerce(
            @Valid @RequestBody EcommerceCreateRequest request) {
        EcommerceResponse response = ecommerceUseCase.createEcommerce(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
    
    /**
     * @param status status filter (optional)
     * @param page page number (default: 0)
     * @param size page size (default: 50)
     * @return HTTP 200 OK with paginated list of EcommerceResponse
     */
    @GetMapping
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<Page<EcommerceResponse>> listEcommerces(
            @RequestParam(name = "status", required = false) String status,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "50") int size) {
        Page<EcommerceResponse> response = ecommerceUseCase.listEcommerces(status, page, size);
        return ResponseEntity.ok(response);
    }
    
    /**
     * @param uid ecommerce identifier
     * @return HTTP 200 OK with EcommerceResponse
     */
    @GetMapping("/{uid}")
    public ResponseEntity<EcommerceResponse> getEcommerceById(
            @PathVariable UUID uid) {
        String currentRole = securityContextHelper.getCurrentUserRole();
        
        if (!"SUPER_ADMIN".equals(currentRole)) {
            UUID userEcommerceId = securityContextHelper.getCurrentUserEcommerceId();
            if (userEcommerceId == null || !userEcommerceId.equals(uid)) {
                throw new AuthorizationException(
                    "No tienes permiso para acceder a este ecommerce"
                );
            }
        }
        
        EcommerceResponse response = ecommerceUseCase.getEcommerceById(uid);
        return ResponseEntity.ok(response);
    }
    
    /**
     * @param uid ecommerce identifier
     * @param request status update data
     * @return HTTP 200 OK with updated EcommerceResponse
     */
    @PutMapping("/{uid}/status")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<EcommerceResponse> updateEcommerceStatus(
            @PathVariable UUID uid,
            @Valid @RequestBody EcommerceUpdateStatusRequest request) {
        EcommerceResponse response = ecommerceUseCase.updateEcommerceStatus(uid, request);
        return ResponseEntity.ok(response);
    }
}
