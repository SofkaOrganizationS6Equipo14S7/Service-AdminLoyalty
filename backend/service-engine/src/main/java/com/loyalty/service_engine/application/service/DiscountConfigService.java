package com.loyalty.service_engine.application.service;

import com.loyalty.service_engine.application.dto.DiscountConfigCreateRequest;
import com.loyalty.service_engine.application.dto.DiscountConfigResponse;
import com.loyalty.service_engine.domain.entity.DiscountConfigEntity;
import com.loyalty.service_engine.domain.repository.DiscountConfigRepository;
import com.loyalty.service_engine.infrastructure.exception.ResourceNotFoundException;
import com.loyalty.service_engine.infrastructure.rabbitmq.DiscountConfigEventPublisher;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Servicio para gestionar la configuración de tope máximo de descuentos en el engine.
 */
@Service
@Slf4j
public class DiscountConfigService {
    
    private final DiscountConfigRepository discountConfigRepository;
    private final DiscountConfigEventPublisher eventPublisher;
    
    public DiscountConfigService(
        DiscountConfigRepository discountConfigRepository,
        DiscountConfigEventPublisher eventPublisher
    ) {
        this.discountConfigRepository = discountConfigRepository;
        this.eventPublisher = eventPublisher;
    }
    
    /**
     * Obtiene la configuración vigente de tope máximo de descuentos.
     * @return DiscountConfigResponse con la configuración activa
     * @throws ResourceNotFoundException si no existe configuración activa
     */
    @Transactional(readOnly = true)
    public DiscountConfigResponse getActiveConfig() {
        DiscountConfigEntity config = discountConfigRepository.findByIsActiveTrue()
            .orElseThrow(() -> {
                log.warn("No active discount config found");
                return new ResourceNotFoundException("discount_config_not_found");
            });
        
        return toDiscountConfigResponse(config);
    }
    
    /**
     * Crea o actualiza la configuración de tope máximo de descuentos.
     * Si ya existe una configuración activa, la desactiva y crea una nueva.
     * 
     * @param request Datos de la nueva configuración
     * @param userId ID del usuario que realiza la operación
     * @return DiscountConfigResponse con la nueva configuración
     * @throws IllegalArgumentException si maxDiscountLimit <= 0
     */
    @Transactional
    public DiscountConfigResponse updateConfig(DiscountConfigCreateRequest request, UUID userId) {
        // Validar que el tope máximo sea positivo
        validateMaxDiscountLimit(request.maxDiscountLimit());
        
        // Obtener configuración actual activa si existe
        DiscountConfigEntity existingConfig = discountConfigRepository.findByIsActiveTrue().orElse(null);
        
        // Si existe una configuración activa, desactivarla
        if (existingConfig != null) {
            existingConfig.setIsActive(false);
            discountConfigRepository.save(existingConfig);
            log.info("Previous discount config deactivated. Config ID: {}", existingConfig.getId());
        }
        
        // Crear nueva configuración activa
        DiscountConfigEntity newConfig = new DiscountConfigEntity();
        newConfig.setMaxDiscountLimit(request.maxDiscountLimit());
        newConfig.setCurrencyCode(request.currencyCode());
        newConfig.setIsActive(true);
        newConfig.setCreatedByUserId(userId);
        
        // Persistir
        DiscountConfigEntity saved = discountConfigRepository.save(newConfig);
        log.info("Discount config created/updated. Config ID: {}, Max Limit: {}, Created by: {}", 
            saved.getId(), saved.getMaxDiscountLimit(), userId);
        
        // Publicar evento para invalidar caché
        eventPublisher.publishDiscountConfigUpdated(
            saved.getId(),
            saved.getMaxDiscountLimit().toPlainString(),
            saved.getCurrencyCode(),
            saved.getIsActive()
        );
        
        return toDiscountConfigResponse(saved);
    }
    
    /**
     * Obtiene la configuración vigente de forma interna (sin lanzar excepción).
     * @return DiscountConfigEntity o null si no existe
     */
    @Transactional(readOnly = true)
    public DiscountConfigEntity getActiveConfigEntity() {
        return discountConfigRepository.findByIsActiveTrue().orElse(null);
    }
    
    /**
     * Valida que el tope máximo sea un valor positivo.
     * @param maxLimit Valor a validar
     * @throws IllegalArgumentException si maxLimit <= 0
     */
    private void validateMaxDiscountLimit(BigDecimal maxLimit) {
        if (maxLimit == null || maxLimit.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("max_discount_limit must be greater than 0");
        }
    }
    
    /**
     * Convierte entidad a DTO.
     */
    private DiscountConfigResponse toDiscountConfigResponse(DiscountConfigEntity entity) {
        return new DiscountConfigResponse(
            entity.getId().toString(),
            entity.getMaxDiscountLimit(),
            entity.getCurrencyCode(),
            entity.getIsActive(),
            entity.getCreatedAt(),
            entity.getUpdatedAt()
        );
    }
}
