package com.loyalty.service_engine.presentation.controller;

import com.loyalty.service_engine.application.dto.DiscountConfigResponse;
import com.loyalty.service_engine.application.dto.DiscountPriorityResponse;
import com.loyalty.service_engine.application.service.DiscountConfigService;
import com.loyalty.service_engine.application.service.DiscountPriorityService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Controlador READ-ONLY para acceso a configuración de descuentos desde la réplica.
 * 
 * IMPORTANTE: Este controller es SOLO LECTURA desde BD réplica (loyalty_engine).
 * Las escrituras de configuración se hacen SOLO en Service-Admin (8081).
 * 
 * Endpoints (HU-09 SPEC-compliant):
 * - GET /api/v1/discount-config?ecommerceId=... → Obtener config activa (audit/debug)
 * - GET /api/v1/discount-priority?configId=... → Obtener prioridades (audit/debug)
 * 
 * Nota: applyDiscountLimit() es un servicio INTERNO para SPEC-011 (engine-calculate).
 *       No está expuesto como endpoint HTTP.
 */
@RestController
@RequestMapping("/api/v1")
@Slf4j
public class DiscountConfigController {
    
    private final DiscountConfigService discountConfigService;
    private final DiscountPriorityService discountPriorityService;
    
    public DiscountConfigController(
        DiscountConfigService discountConfigService,
        DiscountPriorityService discountPriorityService
    ) {
        this.discountConfigService = discountConfigService;
        this.discountPriorityService = discountPriorityService;
    }
    
    /**
     * GET /api/v1/discount-config?ecommerceId=...
     * Obtiene la configuración activa de límite de descuentos desde la réplica.
     * 
     * IMPORTANTE: Esta es una lectura desde BD réplica (loyalty_engine) para auditoría/debugging.
     * Para modificar configuración, USE Service-Admin POST /api/v1/discount-config.
     * 
     * @param ecommerceId UUID del ecommerce
     * @return 200 OK con configuración activa de la réplica
     * @return 404 Not Found si no existe configuración activa en la réplica
     */
    @GetMapping("/discount-config")
    public ResponseEntity<DiscountConfigResponse> getDiscountConfig(
            @RequestParam String ecommerceId
    ) {
        log.info("Fetching discount config from replica for ecommerce: {}", ecommerceId);
        DiscountConfigResponse response = discountConfigService.getActiveConfig(ecommerceId);
        return ResponseEntity.ok(response);
    }
    
    /**
     * GET /api/v1/discount-priority?configId=...
     * Obtiene las prioridades vigentes desde la réplica.
     * 
     * IMPORTANTE: Lectura desde BD réplica (loyalty_engine).
     * Para modificar prioridades, USE Service-Admin POST /api/v1/discount-priority.
     * 
     * @param configId UUID de la configuración
     * @return 200 OK con prioridades de la réplica
     * @return 404 Not Found si no existe prioridad en la réplica
     */
    @GetMapping("/discount-priority")
    public ResponseEntity<DiscountPriorityResponse> getDiscountPriority(
            @RequestParam String configId
    ) {
        log.info("Fetching discount priorities from replica for config: {}", configId);
        DiscountPriorityResponse response = discountPriorityService.getPriorities(configId);
        return ResponseEntity.ok(response);
    }
}
