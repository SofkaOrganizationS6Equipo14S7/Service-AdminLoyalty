package com.loyalty.service_admin.application.service;

import com.loyalty.service_admin.application.dto.discountlog.DiscountApplicationLogResponse;
import com.loyalty.service_admin.domain.entity.DiscountApplicationLogEntity;
import com.loyalty.service_admin.domain.repository.DiscountApplicationLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Servicio de consulta de discount application logs (SOLO LECTURA).
 * Implementa CRITERIO-9.3.
 * 
 * CRITERIO-9.4: No permite modificación (sin PUT/DELETE endpoints).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DiscountApplicationLogService {
    
    private final DiscountApplicationLogRepository discountLogRepository;
    
    /**
     * CRITERIO-9.3: Lista registros de descuentos aplicados con filtros por ecommerceId y externalOrderId.
     * 
     * @param ecommerceId filtro opcional
     * @param externalOrderId filtro opcional (ej. #ORDER-12345)
     * @param page número de página (0-indexed)
     * @param size tamaño de página
     * @return Page<DiscountApplicationLogResponse> con logs de descuentos
     */
    @Transactional(readOnly = true)
    public Page<DiscountApplicationLogResponse> listDiscountLogs(
            UUID ecommerceId, String externalOrderId, int page, int size) {
        
        log.info("Listando discount application logs: ecommerceId={}, externalOrderId={}, page={}, size={}", 
                ecommerceId, externalOrderId, page, size);
        
        Pageable pageable = PageRequest.of(page, size);
        
        Page<DiscountApplicationLogEntity> logs;
        if (ecommerceId != null && externalOrderId != null && !externalOrderId.isBlank()) {
            logs = discountLogRepository.findByEcommerceIdAndExternalOrderId(ecommerceId, externalOrderId, pageable);
        } else if (ecommerceId != null) {
            logs = discountLogRepository.findByEcommerceId(ecommerceId, pageable);
        } else if (externalOrderId != null && !externalOrderId.isBlank()) {
            logs = discountLogRepository.findByExternalOrderId(externalOrderId, pageable);
        } else {
            logs = discountLogRepository.findAll(pageable);
        }
        
        return logs.map(this::toResponse);
    }
    
    /**
     * Obtiene detalles de un registro específico de descuento aplicado.
     * 
     * @param logId UUID del registro
     * @return DiscountApplicationLogResponse completa
     */
    @Transactional(readOnly = true)
    public DiscountApplicationLogResponse getDiscountLogById(UUID logId) {
        log.info("Obteniendo discount application log: logId={}", logId);
        
        DiscountApplicationLogEntity entity = discountLogRepository.findById(logId)
                .orElseThrow(() -> {
                    log.warn("Discount log no encontrado: logId={}", logId);
                    return new com.loyalty.service_admin.infrastructure.exception.ResourceNotFoundException(
                        "Discount application log no encontrado"
                    );
                });
        
        return toResponse(entity);
    }
    
    /**
     * Convierte DiscountApplicationLogEntity a DiscountApplicationLogResponse.
     */
    private DiscountApplicationLogResponse toResponse(DiscountApplicationLogEntity entity) {
        return new DiscountApplicationLogResponse(
            entity.getId(),
            entity.getEcommerceId(),
            entity.getExternalOrderId(),
            entity.getOriginalAmount(),
            entity.getDiscountApplied(),
            entity.getFinalAmount(),
            entity.getAppliedRulesDetails(),
            entity.getCreatedAt()
        );
    }
}
