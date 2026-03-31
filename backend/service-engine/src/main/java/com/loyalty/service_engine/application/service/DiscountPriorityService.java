package com.loyalty.service_engine.application.service;

import com.loyalty.service_engine.application.dto.DiscountPriorityResponse;
import com.loyalty.service_engine.domain.entity.DiscountConfigEntity;
import com.loyalty.service_engine.domain.entity.DiscountPriorityEntity;
import com.loyalty.service_engine.domain.repository.DiscountConfigRepository;
import com.loyalty.service_engine.domain.repository.DiscountPriorityRepository;
import com.loyalty.service_engine.infrastructure.exception.ResourceNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Servicio READ-ONLY para acceso a prioridades de descuentos desde BD réplica (loyalty_engine).
 * 
 * IMPORTANTE: Este servicio SOLO LEE de la réplica. Las escrituras se hacen en Service-Admin.
 */
@Service
@Slf4j
public class DiscountPriorityService {
    
    private final DiscountConfigRepository discountConfigRepository;
    private final DiscountPriorityRepository discountPriorityRepository;
    
    public DiscountPriorityService(
        DiscountConfigRepository discountConfigRepository,
        DiscountPriorityRepository discountPriorityRepository
    ) {
        this.discountConfigRepository = discountConfigRepository;
        this.discountPriorityRepository = discountPriorityRepository;
    }
    
    /**
     * GET /api/v1/discount-priority?configId=...
     * 
     * Obtiene las prioridades vigentes desde la BD réplica.
     * Se usa para auditoría/debugging (lectura pura).
     * 
     * @param configId UUID de la configuración
     * @return DiscountPriorityResponse con las prioridades de la réplica
     * @throws ResourceNotFoundException si no existen prioridades para esa config
     */
    @Transactional(readOnly = true)
    public DiscountPriorityResponse getPriorities(String configId) {
        try {
            UUID configUuid = UUID.fromString(configId);
            
            // Validar que la configuración existe
            DiscountConfigEntity config = discountConfigRepository.findById(configUuid)
                .orElseThrow(() -> {
                    log.warn("Discount config not found in replica: {}", configId);
                    return new ResourceNotFoundException("discount_config_not_found");
                });
            
            // Obtener prioridades ordenadas
            List<DiscountPriorityEntity> priorities = discountPriorityRepository
                .findByDiscountConfigIdOrderByPriorityLevel(configUuid);
            
            if (priorities.isEmpty()) {
                log.warn("No discount priorities found in replica for config: {}", configId);
                throw new ResourceNotFoundException("discount_priority_not_found");
            }
            
            log.info("Fetching discount priorities from replica for config: {}", configId);
            return toDiscountPriorityResponse(config, priorities);
        } catch (IllegalArgumentException e) {
            log.error("Invalid configId format: {}", configId);
            throw new ResourceNotFoundException("invalid_config_id");
        }
    }
    
    /**
     * Obtiene las prioridades vigentes de forma INTERNA (sin parámetro).
     * Usado por otros servicios (ej: DiscountLimitService, RabbitMQ consumer).
     * 
     * @return Lista de prioridades ordenadas o lista vacía
     */
    @Transactional(readOnly = true)
    public List<DiscountPriorityEntity> getActivePrioritiesEntity() {
        DiscountConfigEntity config = discountConfigRepository.findByIsActiveTrue().orElse(null);
        if (config == null) {
            return List.of();
        }
        return discountPriorityRepository.findByDiscountConfigIdOrderByPriorityLevel(config.getUid());
    }
    
    /**
     * Obtiene las prioridades para una config específica de forma INTERNA.
     * Usado por otros servicios (ej: DiscountLimitService).
     * 
     * @param configUid UUID de la configuración
     * @return Lista de prioridades ordenadas o lista vacía
     */
    @Transactional(readOnly = true)
    public List<DiscountPriorityEntity> getPrioritiesEntityByConfigId(UUID configUid) {
        return discountPriorityRepository.findByDiscountConfigIdOrderByPriorityLevel(configUid);
    }
    
    /**
     * Convierte entidades a DTO
     */
    private DiscountPriorityResponse toDiscountPriorityResponse(
        DiscountConfigEntity config,
        List<DiscountPriorityEntity> entities
    ) {
        List<DiscountPriorityResponse.PriorityEntry> priorities = entities.stream()
            .map(entity -> new DiscountPriorityResponse.PriorityEntry(
                entity.getDiscountType(),
                entity.getPriorityLevel(),
                entity.getCreatedAt()
            ))
            .collect(Collectors.toList());
        
        OffsetDateTime createdAt = entities.isEmpty() ? null : entities.get(0).getCreatedAt();
        
        return new DiscountPriorityResponse(
            UUID.randomUUID().toString(),
            config.getUid().toString(),
            priorities,
            createdAt
        );
    }
}
