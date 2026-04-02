package com.loyalty.service_admin.presentation.controller;

import com.loyalty.service_admin.application.dto.EcommerceCreateRequest;
import com.loyalty.service_admin.application.dto.EcommerceResponse;
import com.loyalty.service_admin.application.dto.EcommerceUpdateStatusRequest;
import com.loyalty.service_admin.application.service.EcommerceService;
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
    
    private final EcommerceService ecommerceService;
    private final SecurityContextHelper securityContextHelper;
    
    /**
     * @param request EcommerceCreateRequest (name, slug)
     * @return ResponseEntity<EcommerceResponse> 201 Created
     */
    @PostMapping
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<EcommerceResponse> createEcommerce(
            @Valid @RequestBody EcommerceCreateRequest request) {
        log.info("POST /api/v1/ecommerces - Creando ecommerce: slug={}", request.slug());
        
        EcommerceResponse response = ecommerceService.createEcommerce(request);
        
        log.info("Ecommerce creado exitosamente: uid={}", response.uid());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
    
    /**
     * @param status filtro por estado (opcional)
     * @param page número de página (default: 0)
     * @param size tamaño de página (default: 50)
     * @return ResponseEntity<Page<EcommerceResponse>> 200 OK
     */
    @GetMapping
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<Page<EcommerceResponse>> listEcommerces(
            @RequestParam(name = "status", required = false) String status,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "50") int size) {
        log.info("GET /api/v1/ecommerces - Listando ecommerces: status={}, page={}, size={}", 
                status, page, size);
        
        Page<EcommerceResponse> response = ecommerceService.listEcommerces(status, page, size);
        
        log.info("Listado completado: {} elementos", response.getTotalElements());
        return ResponseEntity.ok(response);
    }
    
    /**
     * GET /api/v1/ecommerces/{uid}
     * @param uid identificador único del ecommerce
     * @return ResponseEntity<EcommerceResponse> 200 OK
     */
    @GetMapping("/{uid}")
    public ResponseEntity<EcommerceResponse> getEcommerceById(
            @PathVariable UUID uid) {
        log.info("GET /api/v1/ecommerces/{} - Obteniendo ecommerce", uid);
        
        String currentRole = securityContextHelper.getCurrentUserRole();
        
        if (!"SUPER_ADMIN".equals(currentRole)) {
            UUID userEcommerceId = securityContextHelper.getCurrentUserEcommerceId();
            if (userEcommerceId == null || !userEcommerceId.equals(uid)) {
                log.warn("Intento de acceso a ecommerce no autorizado: role={}, requested_uid={}, user_ecommerce={}", 
                        currentRole, uid, userEcommerceId);
                throw new AuthorizationException(
                    "No tienes permiso para acceder a este ecommerce"
                );
            }
        }
        
        EcommerceResponse response = ecommerceService.getEcommerceById(uid);
        
        log.info("Ecommerce obtenido: {}", uid);
        return ResponseEntity.ok(response);
    }
    
    /**
     * PUT /api/v1/ecommerces/{uid}
     * @param uid identificador único del ecommerce
     * @param request EcommerceUpdateStatusRequest (status)
     * @return ResponseEntity<EcommerceResponse> 200 OK
     */
    @PutMapping("/{uid}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<EcommerceResponse> updateEcommerceStatus(
            @PathVariable UUID uid,
            @Valid @RequestBody EcommerceUpdateStatusRequest request) {
        log.info("PUT /api/v1/ecommerces/{} - Actualizando status: newStatus={}", uid, request.status());
        
        EcommerceResponse response = ecommerceService.updateEcommerceStatus(uid, request);
        
        log.info("Ecommerce actualizado exitosamente: uid={}, newStatus={}", uid, response.status());
        return ResponseEntity.ok(response);
    }
}
