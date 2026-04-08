package com.loyalty.service_admin.application.service;

import com.loyalty.service_admin.application.dto.rules.discount.DiscountLimitPriorityRequest;
import com.loyalty.service_admin.application.dto.rules.discount.DiscountLimitPriorityResponse;
import com.loyalty.service_admin.application.validation.DiscountPriorityValidator;
import com.loyalty.service_admin.domain.entity.DiscountPriorityEntity;
import com.loyalty.service_admin.domain.entity.DiscountTypeEntity;
import com.loyalty.service_admin.domain.model.DiscountType;
import com.loyalty.service_admin.domain.repository.DiscountLimitPriorityRepository;
import com.loyalty.service_admin.domain.repository.DiscountTypeRepository;
import com.loyalty.service_admin.infrastructure.exception.BadRequestException;
import com.loyalty.service_admin.infrastructure.exception.ResourceNotFoundException;
import com.loyalty.service_admin.infrastructure.rabbitmq.DiscountConfigEventPublisher;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Servicio de lógica de negocio para prioridades de descuentos.
 * Valida y persiste el orden de aplicación de descuentos.
 */
@Service
@Slf4j
public class DiscountLimitPriorityService {

    private final DiscountLimitPriorityRepository priorityRepository;
    private final DiscountTypeRepository discountTypeRepository;
    private final DiscountPriorityValidator priorityValidator;
    private final DiscountConfigEventPublisher eventPublisher;

    public DiscountLimitPriorityService(
            DiscountLimitPriorityRepository priorityRepository,
            DiscountTypeRepository discountTypeRepository,
            DiscountPriorityValidator priorityValidator,
            DiscountConfigEventPublisher eventPublisher
    ) {
        this.priorityRepository = priorityRepository;
        this.discountTypeRepository = discountTypeRepository;
        this.priorityValidator = priorityValidator;
        this.eventPublisher = eventPublisher;
    }

    /**
     * Guarda o actualiza las prioridades para una configuración.
     * Reemplaza las prioridades anteriores.
     * CRITERIO-4.3: Valida discountTypeId y priorityLevel
     */
    @Transactional
    public DiscountLimitPriorityResponse savePriorities(DiscountLimitPriorityRequest request) {
        // Validar request
        priorityValidator.validatePriorities(request);

        UUID configId = request.discountSettingId();

        // Eliminar prioridades anteriores
        priorityRepository.deleteByDiscountSettingsId(configId);
        log.info("Cleared previous priorities for config: {}", configId);

        // Insert nuevas prioridades
        List<DiscountPriorityEntity> entities = request.priorities()
            .stream()
            .map(dto -> {
                DiscountPriorityEntity entity = new DiscountPriorityEntity();
                entity.setDiscountSettingId(configId);
                // Validar que discountTypeId existe
                discountTypeRepository.findById(dto.discountTypeId())
                    .orElseThrow(() -> new BadRequestException("Tipo de descuento no encontrado: " + dto.discountTypeId()));
                entity.setDiscountTypeId(dto.discountTypeId());
                entity.setPriorityLevel(dto.priorityLevel());
                return entity;
            })
            .toList();

        List<DiscountPriorityEntity> saved = priorityRepository.saveAll(entities);
        log.info("Saved {} priorities for config: {}", saved.size(), configId);

        // Publicar evento
        eventPublisher.publishDiscountPriorityUpdated(configId, saved);

        return toResponse(configId, saved);
    }

    /**
     * Obtiene las prioridades para una configuración, ordenadas por nivel.
     */
    @Transactional(readOnly = true)
    /**
     * Obtiene las prioridades guardadas para una configuración de descuentos.
     * 
     * @param configId identificador UUID de la configuración
     * @return response con lista ordenada de prioridades
     * @throws ResourceNotFoundException si no existen prioridades configuradas
     */
    public DiscountLimitPriorityResponse getPriorities(UUID configId) {
        List<DiscountPriorityEntity> priorities = priorityRepository
            .findByDiscountSettingsIdOrderByPriorityLevel(configId);

        if (priorities.isEmpty()) {
            throw new ResourceNotFoundException(
                "No existen prioridades configuradas para: " + configId
            );
        }

        return toResponse(configId, priorities);
    }

    /**
     * Convierte entidades a DTO response.
     */
    private DiscountLimitPriorityResponse toResponse(UUID configId, List<DiscountPriorityEntity> entities) {
        // Obtener el primer entity para uid (si existe), o generar un UUID único para el grupo
        UUID responseUid = entities.isEmpty() ? UUID.randomUUID() : entities.get(0).getId();
        Instant now = Instant.now();
        
        List<DiscountLimitPriorityResponse.PriorityEntry> entries = entities
            .stream()
            .map(entity -> new DiscountLimitPriorityResponse.PriorityEntry(
                entity.getDiscountTypeId(),
                entity.getPriorityLevel(),
                entity.getCreatedAt()
            ))
            .collect(Collectors.toList());

        return new DiscountLimitPriorityResponse(
            responseUid,
            configId,
            entries,
            now,
            now
        );
    }
}
