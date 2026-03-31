package com.loyalty.service_admin.presentation.controller;

import com.loyalty.service_admin.application.dto.DiscountConfigCreateRequest;
import com.loyalty.service_admin.application.dto.DiscountConfigResponse;
import com.loyalty.service_admin.application.dto.DiscountLimitPriorityRequest;
import com.loyalty.service_admin.application.dto.DiscountLimitPriorityResponse;
import com.loyalty.service_admin.application.service.DiscountConfigService;
import com.loyalty.service_admin.application.service.DiscountLimitPriorityService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Controlador REST para gestión de configuración de límite y prioridad de descuentos.
 * ADMINISTRADOR ONLY.
 */
@RestController
@RequestMapping("/api/v1")
@Slf4j
public class DiscountConfigController {

    private final DiscountConfigService discountConfigService;
    private final DiscountLimitPriorityService priorityService;

    public DiscountConfigController(
            DiscountConfigService discountConfigService,
            DiscountLimitPriorityService priorityService
    ) {
        this.discountConfigService = discountConfigService;
        this.priorityService = priorityService;
    }

    /**
     * POST /api/v1/discount-config
     * Crea o actualiza la configuración de límite de descuentos.
     * Solo una configuración activa por ecommerce.
     */
    @PostMapping("/discount-config")
    public ResponseEntity<DiscountConfigResponse> updateDiscountConfig(
            @RequestBody DiscountConfigCreateRequest request
    ) {
        log.info("Updating discount config for ecommerce: {}", request.ecommerceId());
        DiscountConfigResponse response = discountConfigService.updateConfig(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * GET /api/v1/discount-config?ecommerceId=...
     * Obtiene la configuración activa de descuentos.
     */
    @GetMapping("/discount-config")
    public ResponseEntity<DiscountConfigResponse> getDiscountConfig(
            @RequestParam String ecommerceId
    ) {
        log.info("Fetching discount config for ecommerce: {}", ecommerceId);
        DiscountConfigResponse response = discountConfigService.getActiveConfig(ecommerceId);
        return ResponseEntity.ok(response);
    }

    /**
     * POST /api/v1/discount-priority
     * Define el orden de prioridad de tipos de descuento.
     */
    @PostMapping("/discount-priority")
    public ResponseEntity<DiscountLimitPriorityResponse> savePriorities(
            @RequestBody DiscountLimitPriorityRequest request
    ) {
        log.info("Saving priorities for config: {}", request.discountConfigId());
        DiscountLimitPriorityResponse response = priorityService.savePriorities(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * GET /api/v1/discount-priority?configId=...
     * Obtiene las prioridades vigentes.
     */
    @GetMapping("/discount-priority")
    public ResponseEntity<DiscountLimitPriorityResponse> getPriorities(
            @RequestParam String configId
    ) {
        log.info("Fetching priorities for config: {}", configId);
        DiscountLimitPriorityResponse response = priorityService.getPriorities(configId);
        return ResponseEntity.ok(response);
    }
}
