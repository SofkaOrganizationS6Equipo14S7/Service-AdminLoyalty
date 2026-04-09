package com.loyalty.service_admin.application.service;

import com.loyalty.service_admin.application.dto.auditlog.AuditLogResponse;
import com.loyalty.service_admin.domain.entity.AuditLogEntity;
import com.loyalty.service_admin.domain.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Servicio de consulta de audit logs (SOLO LECTURA).
 * Implementa CRITERIO-9.1 y CRITERIO-9.2.
 * 
 * CRITERIO-9.4: No permite modificación (sin PUT/DELETE endpoints).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AuditLogService {
    
    private final AuditLogRepository auditLogRepository;
    
    /**
     * CRITERIO-9.1: Lista audit logs con filtros por entityName, ecommerceId y paginación.
     * 
     * @param entityName filtro opcional (ej. "app_user", "ecommerce")
     * @param ecommerceId filtro obligatorio para STORE_ADMIN
     * @param page número de página (0-indexed)
     * @param size tamaño de página
     * @return Page<AuditLogResponse> con logs filtrados
     */
    @Transactional(readOnly = true)
    public Page<AuditLogResponse> listAuditLogs(String entityName, UUID ecommerceId, int page, int size) {
        log.info("Listando audit logs: entityName={}, ecommerceId={}, page={}, size={}", 
                entityName, ecommerceId, page, size);
        
        Pageable pageable = PageRequest.of(page, size);
        
        Page<AuditLogEntity> logs;
        if (entityName != null && !entityName.isBlank()) {
            if (ecommerceId != null) {
                logs = auditLogRepository.findByEntityNameAndEcommerceId(entityName, ecommerceId, pageable);
            } else {
                logs = auditLogRepository.findByEntityName(entityName, pageable);
            }
        } else {
            if (ecommerceId != null) {
                logs = auditLogRepository.findByEcommerceId(ecommerceId, pageable);
            } else {
                logs = auditLogRepository.findAll(pageable);
            }
        }
        
        return logs.map(this::toResponse);
    }
    
    /**
     * CRITERIO-9.2: Obtiene detalles de un registro de auditoría específico.
     * 
     * @param logId UUID del log a recuperar
     * @return AuditLogResponse completa (incluye oldValue y newValue como JSON)
     */
    @Transactional(readOnly = true)
    public AuditLogResponse getAuditLogById(UUID logId) {
        log.info("Obteniendo audit log: logId={}", logId);
        
        AuditLogEntity entity = auditLogRepository.findById(logId)
                .orElseThrow(() -> {
                    log.warn("Audit log no encontrado: logId={}", logId);
                    return new com.loyalty.service_admin.infrastructure.exception.ResourceNotFoundException(
                        "Audit log no encontrado"
                    );
                });
        
        return toResponse(entity);
    }
    
    /**
     * Convierte AuditLogEntity a AuditLogResponse.
     */
    private AuditLogResponse toResponse(AuditLogEntity entity) {
        return new AuditLogResponse(
            entity.getId(),
            entity.getUserId(),
            entity.getEcommerceId(),
            entity.getAction(),
            entity.getEntityName(),
            entity.getEntityId(),
            entity.getOldValue(),
            entity.getNewValue(),
            entity.getCreatedAt()
        );
    }
}
