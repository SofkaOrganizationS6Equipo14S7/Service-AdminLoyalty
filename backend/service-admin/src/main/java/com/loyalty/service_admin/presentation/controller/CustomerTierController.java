package com.loyalty.service_admin.presentation.controller;

import com.loyalty.service_admin.application.dto.customertier.CustomerTierCreateRequest;
import com.loyalty.service_admin.application.dto.customertier.CustomerTierUpdateRequest;
import com.loyalty.service_admin.application.dto.customertier.CustomerTierResponse;
import com.loyalty.service_admin.application.service.CustomerTierService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/customer-tiers")
@RequiredArgsConstructor
@Slf4j
public class CustomerTierController {

    private final CustomerTierService customerTierService;

    @PostMapping
    public ResponseEntity<CustomerTierResponse> createCustomerTier(
            @Valid @RequestBody CustomerTierCreateRequest request
    ) {
        log.info("POST /api/v1/customer-tiers - Creating tier: name={}, hierarchyLevel={}", 
            request.name(), request.hierarchyLevel());
        
        CustomerTierResponse response = customerTierService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<Page<CustomerTierResponse>> listCustomerTiers(
            Pageable pageable,
            @RequestParam(required = false) Boolean isActive
    ) {
        log.info("GET /api/v1/customer-tiers - Listing tiers with pagination: page={}, size={}, isActive={}", 
            pageable.getPageNumber(), pageable.getPageSize(), isActive);
        
        Page<CustomerTierResponse> response = customerTierService.listPaginated(pageable, isActive);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{tierId}")
    public ResponseEntity<CustomerTierResponse> getCustomerTierDetails(
            @PathVariable UUID tierId
    ) {
        log.info("GET /api/v1/customer-tiers/{} - Obtaining tier details", tierId);
        
        CustomerTierResponse response = customerTierService.getById(tierId);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{tierId}")
    public ResponseEntity<CustomerTierResponse> updateCustomerTier(
            @PathVariable UUID tierId,
            @Valid @RequestBody CustomerTierUpdateRequest request
    ) {
        log.info("PUT /api/v1/customer-tiers/{} - Updating tier", tierId);
        
        CustomerTierResponse response = customerTierService.update(tierId, request);
        return ResponseEntity.ok(response);
    }


    @DeleteMapping("/{tierId}")
    public ResponseEntity<Void> deleteCustomerTier(
            @PathVariable UUID tierId
    ) {
        log.info("DELETE /api/v1/customer-tiers/{} - Soft deleting tier", tierId);
        
        customerTierService.delete(tierId);
        return ResponseEntity.noContent().build();
    }
}
