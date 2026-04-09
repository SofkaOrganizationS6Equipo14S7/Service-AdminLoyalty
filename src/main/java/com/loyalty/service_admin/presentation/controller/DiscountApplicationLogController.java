package com.loyalty.service_admin.presentation.controller;

import com.loyalty.service_admin.application.dto.discountlog.DiscountApplicationLogResponse;
import com.loyalty.service_admin.application.service.DiscountApplicationLogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * Controller read-only para logs de descuentos aplicados (HU-09).
 * 
 * CRITERIO-9.4: No tiene endpoints PUT/DELETE — retorna HTTP 405 Method Not Allowed.
 */
@RestController
@RequestMapping("/api/v1/discount-logs")
@RequiredArgsConstructor
@Slf4j
@PreAuthorize("isAuthenticated()")
public class DiscountApplicationLogController {
    
    private final DiscountApplicationLogService discountLogService;
    
    /**
     * CRITERIO-9.3: Lista registros de descuentos aplicados con filtros.
     * 
     * @param ecommerceId filtro opcional por tenant
     * @param externalOrderId filtro opcional (ej. #ORDER-12345)
     * @param page número de página (default: 0)
     * @param size tamaño de página (default: 50)
     * @return HTTP 200 OK con Page<DiscountApplicationLogResponse>
     */
    @GetMapping
    public ResponseEntity<Page<DiscountApplicationLogResponse>> listDiscountLogs(
            @RequestParam(name = "ecommerceId", required = false) UUID ecommerceId,
            @RequestParam(name = "externalOrderId", required = false) String externalOrderId,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "50") int size
    ) {
        log.info("GET /api/v1/discount-logs: ecommerceId={}, externalOrderId={}, page={}, size={}", 
                ecommerceId, externalOrderId, page, size);
        
        Page<DiscountApplicationLogResponse> logs = discountLogService.listDiscountLogs(
            ecommerceId, externalOrderId, page, size);
        return ResponseEntity.ok(logs);
    }
    
    /**
     * Obtiene detalles de un registro de descuento específico.
     * 
     * @param logId UUID del registro
     * @return HTTP 200 OK con DiscountApplicationLogResponse
     */
    @GetMapping("/{logId}")
    public ResponseEntity<DiscountApplicationLogResponse> getDiscountLogById(@PathVariable UUID logId) {
        log.info("GET /api/v1/discount-logs/{}: logId={}", logId, logId);
        
        DiscountApplicationLogResponse log = discountLogService.getDiscountLogById(logId);
        return ResponseEntity.ok(log);
    }
    
    /**
     * CRITERIO-9.4: No permite PUT en logs (immutable).
     */
    @PutMapping
    public ResponseEntity<Void> methodNotAllowed() {
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED).build();
    }
    
    /**
     * CRITERIO-9.4: No permite DELETE en logs (immutable).
     */
    @DeleteMapping
    public ResponseEntity<Void> methodNotAllowedDelete() {
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED).build();
    }
}
