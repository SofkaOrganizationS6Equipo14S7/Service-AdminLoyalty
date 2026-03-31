package com.loyalty.service_engine.application.service;

import com.loyalty.service_engine.application.dto.DiscountConfigResponse;
import com.loyalty.service_engine.domain.entity.DiscountConfigEntity;
import com.loyalty.service_engine.domain.repository.DiscountConfigRepository;
import com.loyalty.service_engine.infrastructure.exception.ResourceNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Servicio READ-ONLY para acceso a la configuración de descuentos desde BD réplica (loyalty_engine).
 * 
 * IMPORTANTE: Este servicio SOLO LEE de la réplica. Las escrituras se hacen en Service-Admin.
 * Se utiliza por el Controller y también por otros servicios (ej: DiscountLimitService para aplicar cálculos).
 */
@Service
@Slf4j
public class DiscountConfigService {
    
    private final DiscountConfigRepository discountConfigRepository;
    
    public DiscountConfigService(DiscountConfigRepository discountConfigRepository) {
        this.discountConfigRepository = discountConfigRepository;
    }
    
    /**
     * GET /api/v1/discount-config?ecommerceId=...
     * 
     * Obtiene la configuración activa de límite de descuentos desde la BD réplica.
     * Se usa para auditoría/debugging (lectura pura).
     * 
     * @param ecommerceId UUID del ecommerce para el cual obtener config
     * @return DiscountConfigResponse con configuración activa de la réplica
     * @throws ResourceNotFoundException si no existe config activa para ese ecommerce
     */
    @Transactional(readOnly = true)
    public DiscountConfigResponse getActiveConfig(String ecommerceId) {
        try {
            UUID ecommerceUuid = UUID.fromString(ecommerceId);
            DiscountConfigEntity config = discountConfigRepository
                .findByEcommerceIdAndIsActiveTrue(ecommerceUuid)
                .orElseThrow(() -> {
                    log.warn("No active discount config found in replica for ecommerce: {}", ecommerceId);
                    return new ResourceNotFoundException("discount_config_not_found");
                });
            
            log.info("Fetching discount config from replica for ecommerce: {}", ecommerceId);
            return toDiscountConfigResponse(config);
        } catch (IllegalArgumentException e) {
            log.error("Invalid ecommerceId format: {}", ecommerceId);
            throw new ResourceNotFoundException("invalid_ecommerce_id");
        }
    }
    
    /**
     * Obtiene la configuración activa de forma INTERNA (sin parámetro).
     * Usado por otros servicios (ej: DiscountLimitService, RabbitMQ consumer).
     * 
     * @return DiscountConfigEntity activa o null si no existe
     */
    @Transactional(readOnly = true)
    public DiscountConfigEntity getActiveConfigEntity() {
        return discountConfigRepository.findByIsActiveTrue().orElse(null);
    }
    
    /**
     * Obtiene config por ecommerce ID de forma interna (para servicios internos).
     * 
     * @param ecommerceId UUID del ecommerce
     * @return DiscountConfigEntity o null si no existe config activa
     */
    @Transactional(readOnly = true)
    public DiscountConfigEntity getActiveConfigEntityByEcommerce(UUID ecommerceId) {
        return discountConfigRepository.findByEcommerceIdAndIsActiveTrue(ecommerceId).orElse(null);
    }
    
    /**
     * Convierte entidad a DTO
     */
    private DiscountConfigResponse toDiscountConfigResponse(DiscountConfigEntity entity) {
        return new DiscountConfigResponse(
            entity.getUid().toString(),
            entity.getEcommerceId().toString(),
            entity.getMaxDiscountLimit().toPlainString(),
            entity.getCurrencyCode(),
            entity.getIsActive(),
            entity.getCreatedAt(),
            entity.getUpdatedAt()
        );
    }
}
