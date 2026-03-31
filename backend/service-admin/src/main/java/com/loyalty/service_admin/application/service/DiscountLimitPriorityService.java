package com.loyalty.service_admin.application.service;

import com.loyalty.service_admin.application.dto.DiscountLimitPriorityRequest;
import com.loyalty.service_admin.application.dto.DiscountLimitPriorityResponse;
import com.loyalty.service_admin.application.validation.DiscountPriorityValidator;
import com.loyalty.service_admin.domain.entity.DiscountLimitPriorityEntity;
import com.loyalty.service_admin.domain.model.DiscountType;
import com.loyalty.service_admin.domain.repository.DiscountLimitPriorityRepository;
import com.loyalty.service_admin.infrastructure.exception.BadRequestException;
import com.loyalty.service_admin.infrastructure.exception.ResourceNotFoundException;
import com.loyalty.service_admin.infrastructure.rabbitmq.DiscountConfigEventPublisher;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
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
    private final DiscountPriorityValidator priorityValidator;
    private final DiscountConfigEventPublisher eventPublisher;

    public DiscountLimitPriorityService(
            DiscountLimitPriorityRepository priorityRepository,
            DiscountPriorityValidator priorityValidator,
            DiscountConfigEventPublisher eventPublisher
    ) {
        this.priorityRepository = priorityRepository;
        this.priorityValidator = priorityValidator;
        this.eventPublisher = eventPublisher;
    }

    /**
     * Guarda o actualiza las prioridades para una configuración.
     * Reemplaza las prioridades anteriores.
     */
    @Transactional
    public DiscountLimitPriorityResponse savePriorities(DiscountLimitPriorityRequest request) {
        // Validar request
        priorityValidator.validatePriorities(request);

        UUID configId = UUID.fromString(request.discountConfigId());

        // Eliminar prioridades anteriores
        priorityRepository.deleteByDiscountConfigId(configId);
        log.info("Cleared previous priorities for config: {}", configId);

        // Insertar nuevas prioridades
        List<DiscountLimitPriorityEntity> entities = request.priorities()
            .stream()
            .map(entry -> {
                DiscountLimitPriorityEntity entity = new DiscountLimitPriorityEntity();
                entity.setDiscountConfigId(configId);
                entity.setDiscountType(DiscountType.valueOf(entry.discountType()));
                entity.setPriorityLevel(entry.priorityLevel());
                return entity;
            })
            .toList();

        List<DiscountLimitPriorityEntity> saved = priorityRepository.saveAll(entities);
        log.info("Saved {} priorities for config: {}", saved.size(), configId);

        // Publicar evento
        eventPublisher.publishDiscountPriorityUpdated(configId, saved);

        return toResponse(configId, saved);
    }

    /**
     * Obtiene las prioridades para una configuración, ordenadas por nivel.
     */
    @Transactional(readOnly = true)
    public DiscountLimitPriorityResponse getPriorities(String configId) {
        UUID configUuid = UUID.fromString(configId);

        List<DiscountLimitPriorityEntity> priorities = priorityRepository
            .findByDiscountConfigIdOrderByPriorityLevel(configUuid);

        if (priorities.isEmpty()) {
            throw new ResourceNotFoundException(
                "No existen prioridades configuradas para: " + configId
            );
        }

        return toResponse(configUuid, priorities);
    }

    /**
     * Convierte entidades a DTO response.
     */
    private DiscountLimitPriorityResponse toResponse(UUID configId, List<DiscountLimitPriorityEntity> entities) {
        List<DiscountLimitPriorityResponse.PriorityEntry> entries = entities
            .stream()
            .map(entity -> new DiscountLimitPriorityResponse.PriorityEntry(
                entity.getDiscountType().name(),
                entity.getPriorityLevel(),
                entity.getCreatedAt()
            ))
            .collect(Collectors.toList());

        return new DiscountLimitPriorityResponse(
            UUID.randomUUID().toString(), // ID de agrupación
            configId.toString(),
            entries,
            OffsetDateTime.now()
        );
    }
}
