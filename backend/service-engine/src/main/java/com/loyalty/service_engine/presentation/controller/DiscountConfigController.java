package com.loyalty.service_engine.presentation.controller;

import com.loyalty.service_engine.application.dto.DiscountCalculateRequest;
import com.loyalty.service_engine.application.dto.DiscountCalculateResponse;
import com.loyalty.service_engine.application.dto.DiscountConfigCreateRequest;
import com.loyalty.service_engine.application.dto.DiscountConfigResponse;
import com.loyalty.service_engine.application.dto.DiscountPriorityRequest;
import com.loyalty.service_engine.application.dto.DiscountPriorityResponse;
import com.loyalty.service_engine.application.service.DiscountCalculationEngine;
import com.loyalty.service_engine.application.service.DiscountConfigService;
import com.loyalty.service_engine.application.service.DiscountPriorityService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * Controlador para gestionar la configuración de descuentos y cálculos.
 * 
 * Endpoints:
 * - POST /api/v1/discount-config → Configurar límite máximo de descuentos
 * - GET /api/v1/discount-config → Obtener configuración vigente
 * - POST /api/v1/discount-priority → Configurar prioridad de tipos de descuento
 * - GET /api/v1/discount-priority → Obtener prioridades vigentes
 * - POST /api/v1/discount-calculate → Calcular descuentos respetando límite y prioridad
 */
@RestController
@RequestMapping("/api/v1/discount")
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
     * Configura el límite máximo de descuentos.
     * Solo administradores pueden llamar este endpoint (requiere JWT con ROLE_ADMIN).
     * 
     * @param request Datos de configuración (maxDiscountLimit, currencyCode)
     * @param authentication Authentication extraído del JWT
     * @return 201 Created con la nueva configuración
     */
    @PostMapping("/config")
    public ResponseEntity<DiscountConfigResponse> updateDiscountConfig(
        @Valid @RequestBody DiscountConfigCreateRequest request,
        Authentication authentication
    ) {
        // Extraer username del JWT (está en authentication.getName())
        String username = authentication.getName();
        
        // Convertir username a UUID determinístico
        UUID userId = UUID.nameUUIDFromBytes(username.getBytes());
        
        log.info("Updating discount config. User: {} (ID: {})", username, userId);
        DiscountConfigResponse response = discountConfigService.updateConfig(request, userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
    
    /**
     * Obtiene la configuración vigente de límite máximo de descuentos.
     * 
     * @return 200 OK con la configuración activa
     */
    @GetMapping("/config")
    public ResponseEntity<DiscountConfigResponse> getActiveDiscountConfig() {
        log.info("Fetching active discount config");
        DiscountConfigResponse response = discountConfigService.getActiveConfig();
        return ResponseEntity.ok(response);
    }
    
    /**
     * Configura las prioridades de tipos de descuento.
     * Los descuentos se aplican en orden de prioridad (1 = máxima).
     * Solo administradores pueden llamar este endpoint.
     * 
     * @param request Datos de prioridades (discountConfigId, lista de tipos con prioridad)
     * @return 201 Created con las nuevas prioridades
     */
    @PostMapping("/priority")
    public ResponseEntity<DiscountPriorityResponse> saveDiscountPriorities(
        @Valid @RequestBody DiscountPriorityRequest request
    ) {
        log.info("Saving discount priorities for config: {}", request.discountConfigId());
        DiscountPriorityResponse response = discountPriorityService.savePriorities(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
    
    /**
     * Obtiene las prioridades vigentes de tipos de descuento.
     * 
     * @return 200 OK con las prioridades activas
     */
    @GetMapping("/priority")
    public ResponseEntity<DiscountPriorityResponse> getActiveDiscountPriorities() {
        log.info("Fetching active discount priorities");
        DiscountPriorityResponse response = discountPriorityService.getActivePriorities();
        return ResponseEntity.ok(response);
    }
    
    /**
     * Calcula el total de descuentos aplicables a una transacción.
     * Respeta el límite máximo configurado y la prioridad de tipos.
     * 
     * Algoritmo:
     * 1. Obtiene límite máximo de descuentos vigente
     * 2. Obtiene prioridades vigentes de tipos de descuento
     * 3. Ordena los descuentos por prioridad (1 = primero)
     * 4. Acumula hasta alcanzar el límite máximo
     * 5. Retorna desglose de original vs aplicado
     * 
     * @param request Datos de transacción (transactionId, lista de descuentos por cantidad)
     * @return 200 OK con cálculo completo
     */
    @PostMapping("/calculate")
    public ResponseEntity<DiscountCalculateResponse> calculateDiscounts(
        @Valid @RequestBody DiscountCalculateRequest request
    ) {
        log.info("Calculating discounts for transaction: {}", request.transactionId());
        DiscountCalculateResponse response = discountCalculationEngine.calculateDiscounts(request);
        return ResponseEntity.ok(response);
    }
}
