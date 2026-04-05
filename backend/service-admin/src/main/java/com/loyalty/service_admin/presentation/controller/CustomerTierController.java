package com.loyalty.service_admin.presentation.controller;

import com.loyalty.service_admin.application.dto.customertier.CustomerTierCreateRequest;
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

/**
 * REST Controller para gestionar customer tiers.
 * Implementa los criterios de aceptación HU-07 (CRUD básico).
 */
@RestController
@RequestMapping("/api/v1/customer-tiers")
@RequiredArgsConstructor
@Slf4j
public class CustomerTierController {

    private final CustomerTierService customerTierService;

    /**
     * CRITERIO-7.1: Crear customer tier
     * POST /api/v1/customer-tiers
     * @return HTTP 201 Created con CustomerTierResponse
     */
    @PostMapping
    public ResponseEntity<CustomerTierResponse> createCustomerTier(
            @Valid @RequestBody CustomerTierCreateRequest request
    ) {
        log.info("POST /api/v1/customer-tiers - Creating tier: name={}, hierarchyLevel={}", 
            request.name(), request.hierarchyLevel());
        
        CustomerTierResponse response = customerTierService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * CRITERIO-7.5: Listar customer tiers con paginación
     * GET /api/v1/customer-tiers?page=0&size=20
     * @return HTTP 200 OK con Page<CustomerTierResponse>
     */
    @GetMapping
    public ResponseEntity<Page<CustomerTierResponse>> listCustomerTiers(Pageable pageable) {
        log.info("GET /api/v1/customer-tiers - Listing tiers with pagination: page={}, size={}", 
            pageable.getPageNumber(), pageable.getPageSize());
        
        Page<CustomerTierResponse> response = customerTierService.listPaginated(pageable);
        return ResponseEntity.ok(response);
    }

    /**
     * CRITERIO-7.6: Obtener detalles de tier
     * GET /api/v1/customer-tiers/{tierId}
     * @return HTTP 200 OK con CustomerTierResponse
     */
    @GetMapping("/{tierId}")
    public ResponseEntity<CustomerTierResponse> getCustomerTierDetails(
            @PathVariable UUID tierId
    ) {
        log.info("GET /api/v1/customer-tiers/{} - Obtaining tier details", tierId);
        
        CustomerTierResponse response = customerTierService.getById(tierId);
        return ResponseEntity.ok(response);
    }

    /**
     * CRITERIO-7.8: Eliminar tier (soft delete con isActive=false)
     * DELETE /api/v1/customer-tiers/{tierId}
     * @return HTTP 204 No Content
     */
    @DeleteMapping("/{tierId}")
    public ResponseEntity<Void> deleteCustomerTier(
            @PathVariable UUID tierId
    ) {
        log.info("DELETE /api/v1/customer-tiers/{} - Soft deleting tier", tierId);
        
        customerTierService.delete(tierId);
        return ResponseEntity.noContent().build();
    }
}
