package com.loyalty.service_admin.presentation.controller;

import com.loyalty.service_admin.application.dto.*;
import com.loyalty.service_admin.application.service.DiscountCalculationEngine;
import com.loyalty.service_admin.application.service.DiscountConfigService;
import com.loyalty.service_admin.application.service.DiscountPriorityService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * Controlador para endpoints de configuración de límite y prioridad de descuentos.
 * Endpoints:
 * - POST /api/v1/discount-config → Crear/actualizar configuración de límite
 * - GET /api/v1/discount-config → Obtener configuración vigente
 * - POST /api/v1/discount-priority → Crear/actualizar prioridades
 * - GET /api/v1/discount-priority → Obtener prioridades vigentes
 * - POST /api/v1/discount-calculate → Calcular descuentos aplicables
 */
@RestController
@RequestMapping("/api/v1")
@Slf4j
public class DiscountConfigController {
    
    private final DiscountConfigService discountConfigService;
    private final DiscountPriorityService discountPriorityService;
    private final DiscountCalculationEngine discountCalculationEngine;
    
    public DiscountConfigController(
        DiscountConfigService discountConfigService,
        DiscountPriorityService discountPriorityService,
        DiscountCalculationEngine discountCalculationEngine
    ) {
        this.discountConfigService = discountConfigService;
        this.discountPriorityService = discountPriorityService;
        this.discountCalculationEngine = discountCalculationEngine;
    }
    
    /**
     * POST /api/v1/discount-config
     * Crea o actualiza la configuración de tope máximo de descuentos.
     * 
     * @param request Datos de tope máximo y moneda
     * @return HTTP 201 Created (nueva) o 200 OK (actualización)
     */
    @PostMapping("/discount-config")
    public ResponseEntity<DiscountConfigResponse> updateDiscountConfig(
        @RequestBody DiscountConfigCreateRequest request
    ) {
        log.info("Updating discount config with max limit: {}", request.maxDiscountLimit());
        
        // Generar UUID para el usuario (en producción, obtener del token JWT)
        UUID userId = UUID.randomUUID();
        DiscountConfigResponse response = discountConfigService.updateConfig(request, userId);
        
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
    
    /**
     * GET /api/v1/discount-config
     * Obtiene la configuración vigente de tope máximo de descuentos.
     * 
     * @return HTTP 200 OK con DiscountConfigResponse
     * @throws ResourceNotFoundException si no existe configuración
     */
    @GetMapping("/discount-config")
    public ResponseEntity<DiscountConfigResponse> getDiscountConfig() {
        log.info("Retrieving active discount config");
        
        DiscountConfigResponse response = discountConfigService.getActiveConfig();
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * POST /api/v1/discount-priority
     * Define el orden de prioridad para múltiples descuentos.
     * 
     * @param request Datos de prioridades (config ID + array de prioridades)
     * @return HTTP 201 Created (nueva) o 200 OK (actualización)
     * @throws BadRequestException si prioridades son inválidas
     * @throws ResourceNotFoundException si configuración de límite no existe
     */
    @PostMapping("/discount-priority")
    public ResponseEntity<DiscountPriorityResponse> saveDiscountPriority(
        @RequestBody DiscountPriorityRequest request
    ) {
        log.info("Saving discount priorities for config: {}", request.discountConfigId());
        
        DiscountPriorityResponse response = discountPriorityService.savePriorities(request);
        
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
    
    /**
     * GET /api/v1/discount-priority
     * Obtiene la prioridad vigente de descuentos.
     * 
     * @return HTTP 200 OK con DiscountPriorityResponse
     * @throws ResourceNotFoundException si no existe configuración de prioridades
     */
    @GetMapping("/discount-priority")
    public ResponseEntity<DiscountPriorityResponse> getDiscountPriority() {
        log.info("Retrieving active discount priorities");
        
        DiscountPriorityResponse response = discountPriorityService.getActivePriorities();
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * POST /api/v1/discount-calculate
     * Calcula el total de descuentos respetando prioridad y límite máximo.
     * 
     * @param request Datos de transacción y descuentos a aplicar
     * @return HTTP 200 OK con DiscountCalculateResponse
     * @throws ResourceNotFoundException si no existe configuración
     */
    @PostMapping("/discount-calculate")
    public ResponseEntity<DiscountCalculateResponse> calculateDiscounts(
        @RequestBody DiscountCalculateRequest request
    ) {
        log.info("Calculating discounts for transaction: {}", request.transactionId());
        
        DiscountCalculateResponse response = discountCalculationEngine.calculateDiscounts(request);
        
        return ResponseEntity.ok(response);
    }
}
