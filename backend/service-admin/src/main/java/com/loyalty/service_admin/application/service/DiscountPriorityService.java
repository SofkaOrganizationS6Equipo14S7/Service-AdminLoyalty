package com.loyalty.service_admin.application.service;

import com.loyalty.service_admin.application.dto.DiscountPriorityRequest;
import com.loyalty.service_admin.application.dto.DiscountPriorityResponse;
import com.loyalty.service_admin.domain.entity.DiscountConfigEntity;
import com.loyalty.service_admin.domain.entity.DiscountPriorityEntity;
import com.loyalty.service_admin.domain.repository.DiscountConfigRepository;
import com.loyalty.service_admin.domain.repository.DiscountPriorityRepository;
import com.loyalty.service_admin.infrastructure.exception.ResourceNotFoundException;
import com.loyalty.service_admin.infrastructure.exception.BadRequestException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Servicio para gestionar la configuración de prioridad de descuentos.
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
     * Obtiene la configuración vigente de prioridad de descuentos.
     * @return DiscountPriorityResponse con las prioridades activas
     * @throws ResourceNotFoundException si no existe configuración de prioridades
     */
    @Transactional(readOnly = true)
    public DiscountPriorityResponse getActivePriorities() {
        DiscountConfigEntity config = discountConfigRepository.findByIsActiveTrue()
            .orElseThrow(() -> {
                log.warn("No active discount config found when retrieving priorities");
                return new ResourceNotFoundException("discount_config_not_found");
            });
        
        List<DiscountPriorityEntity> priorities = discountPriorityRepository
            .findByDiscountConfigIdOrderByPriorityLevel(config.getId());
        
        if (priorities.isEmpty()) {
            log.warn("No discount priorities found for config: {}", config.getId());
            throw new ResourceNotFoundException("discount_priority_not_found");
        }
        
        return toDiscountPriorityResponse(config, priorities);
    }
    
    /**
     * Guarda la configuración de prioridades de descuentos.
     * Valida que las prioridades sean secuenciales (1..N) y únicas.
     * 
     * @param request Datos de las prioridades a guardar
     * @return DiscountPriorityResponse con la configuración guardada
     * @throws ResourceNotFoundException si la configuración de límite no existe
     * @throws BadRequestException si las prioridades son inválidas
     */
    @Transactional
    public DiscountPriorityResponse savePriorities(DiscountPriorityRequest request) {
        // Validar que exista la configuración de límite máximo
        UUID configId = UUID.fromString(request.discountConfigId());
        DiscountConfigEntity config = discountConfigRepository.findById(configId)
            .orElseThrow(() -> {
                log.warn("Discount config not found: {}", configId);
                return new ResourceNotFoundException("discount_config_not_found");
            });
        
        // Validar que las prioridades sean válidas
        validatePriorities(request.priorities());
        
        // Eliminar prioridades antiguas
        discountPriorityRepository.deleteByDiscountConfigId(configId);
        log.debug("Deleted existing priorities for config: {}", configId);
        
        // Guardar nuevas prioridades
        List<DiscountPriorityEntity> prioritiesToSave = request.priorities().stream()
            .map(item -> {
                DiscountPriorityEntity entity = new DiscountPriorityEntity();
                entity.setDiscountConfigId(configId);
                entity.setDiscountType(item.discountType());
                entity.setPriorityLevel(item.priorityLevel());
                return entity;
            })
            .collect(Collectors.toList());
        
        List<DiscountPriorityEntity> saved = discountPriorityRepository.saveAll(prioritiesToSave);
        log.info("Discount priorities saved for config: {}. Count: {}", configId, saved.size());
        
        return toDiscountPriorityResponse(config, saved);
    }
    
    /**
     * Obtiene las prioridades vigentes de forma interna (sin lanzar excepción).
     * @return Lista de prioridades o lista vacía si no existen
     */
    @Transactional(readOnly = true)
    public List<DiscountPriorityEntity> getActivePrioritiesEntity() {
        DiscountConfigEntity config = discountConfigRepository.findByIsActiveTrue().orElse(null);
        if (config == null) {
            return List.of();
        }
        return discountPriorityRepository.findByDiscountConfigIdOrderByPriorityLevel(config.getId());
    }
    
    /**
     * Valida que las prioridades sean secuenciales comenzando desde 1, sin duplicados.
     * @param priorities Lista de prioridades a validar
     * @throws BadRequestException si las prioridades son inválidas
     */
    private void validatePriorities(List<DiscountPriorityRequest.DiscountPriorityItem> priorities) {
        if (priorities == null || priorities.isEmpty()) {
            throw new BadRequestException("priorities_empty: at least one discount type must be configured");
        }
        
        // Validar que no haya duplicados en tipos de descuento
        boolean hasDuplicateTypes = priorities.stream()
            .map(DiscountPriorityRequest.DiscountPriorityItem::discountType)
            .distinct()
            .count() != priorities.size();
        
        if (hasDuplicateTypes) {
            throw new BadRequestException("duplicate_discount_type: each discount type must appear only once");
        }
        
        // Validar que no haya duplicados en prioridades
        boolean hasDuplicateLevels = priorities.stream()
            .map(DiscountPriorityRequest.DiscountPriorityItem::priorityLevel)
            .distinct()
            .count() != priorities.size();
        
        if (hasDuplicateLevels) {
            throw new BadRequestException("duplicate_priority_level: each discount type must have unique priority");
        }
        
        // Validar que las prioridades sean secuenciales (1..N)
        List<Integer> levels = priorities.stream()
            .map(DiscountPriorityRequest.DiscountPriorityItem::priorityLevel)
            .sorted()
            .collect(Collectors.toList());
        
        for (int i = 0; i < levels.size(); i++) {
            if (levels.get(i) != i + 1) {
                throw new BadRequestException("priority_sequence_invalid: must be sequential starting from 1");
            }
        }
    }
    
    /**
     * Convierte entidades a DTO.
     */
    private DiscountPriorityResponse toDiscountPriorityResponse(
        DiscountConfigEntity config,
        List<DiscountPriorityEntity> entities
    ) {
        List<DiscountPriorityResponse.DiscountPriority> priorities = entities.stream()
            .map(entity -> new DiscountPriorityResponse.DiscountPriority(
                entity.getDiscountType(),
                entity.getPriorityLevel()
            ))
            .collect(Collectors.toList());
        
        return new DiscountPriorityResponse(
            UUID.randomUUID().toString(),
            config.getId().toString(),
            entities.isEmpty() ? null : entities.get(0).getCreatedAt(),
            priorities
        );
    }
}
