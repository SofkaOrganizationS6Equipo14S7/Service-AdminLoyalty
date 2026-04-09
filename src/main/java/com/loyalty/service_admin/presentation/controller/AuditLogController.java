package com.loyalty.service_admin.presentation.controller;

import com.loyalty.service_admin.application.dto.auditlog.AuditLogResponse;
import com.loyalty.service_admin.application.service.AuditLogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * Controller read-only para auditoría de cambios en sistema (HU-09).
 * 
 * CRITERIO-9.4: No tiene endpoints PUT/DELETE — retorna HTTP 405 Method Not Allowed.
 */
@RestController
@RequestMapping("/api/v1/audit-logs")
@RequiredArgsConstructor
@Slf4j
@PreAuthorize("hasRole('SUPER_ADMIN')")
public class AuditLogController {
    
    private final AuditLogService auditLogService;
    
    /**
     * CRITERIO-9.1: Lista audit logs con filtros por entityName, ecommerceId.
     * 
     * @param entityName filtro opcional (ej. "app_user", "ecommerce")
     * @param ecommerceId filtro opcional por tenant
     * @param page número de página (default: 0)
     * @param size tamaño de página (default: 50)
     * @return HTTP 200 OK con Page<AuditLogResponse>
     */
    @GetMapping
    public ResponseEntity<Page<AuditLogResponse>> listAuditLogs(
            @RequestParam(name = "entityName", required = false) String entityName,
            @RequestParam(name = "ecommerceId", required = false) UUID ecommerceId,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "50") int size
    ) {
        log.info("GET /api/v1/audit-logs: entityName={}, ecommerceId={}, page={}, size={}", 
                entityName, ecommerceId, page, size);
        
        Page<AuditLogResponse> logs = auditLogService.listAuditLogs(entityName, ecommerceId, page, size);
        return ResponseEntity.ok(logs);
    }
    
    /**
     * CRITERIO-9.2: Obtiene detalles de un audit log específico.
     * 
     * @param logId UUID del log
     * @return HTTP 200 OK con AuditLogResponse (incluye oldValue y newValue como JSON)
     */
    @GetMapping("/{logId}")
    public ResponseEntity<AuditLogResponse> getAuditLogById(@PathVariable UUID logId) {
        log.info("GET /api/v1/audit-logs/{}: logId={}", logId, logId);
        
        AuditLogResponse log = auditLogService.getAuditLogById(logId);
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
