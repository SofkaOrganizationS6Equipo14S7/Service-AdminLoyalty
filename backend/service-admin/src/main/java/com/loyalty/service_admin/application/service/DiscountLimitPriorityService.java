package com.loyalty.service_admin.application.service;

import com.loyalty.service_admin.application.dto.DiscountLimitPriorityRequest;
import com.loyalty.service_admin.application.dto.DiscountLimitPriorityResponse;
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
     */
    @Transactional
    public DiscountLimitPriorityResponse savePriorities(DiscountLimitPriorityRequest request) {
        // Validar request
        priorityValidator.validatePriorities(request);

        UUID configId = UUID.fromString(request.discountSettingsId());

        // Eliminar prioridades anteriores
        priorityRepository.deleteByDiscountSettingsId(configId);
        log.info("Cleared previous priorities for config: {}", configId);

        // Insert nuevas prioridades
        List<DiscountPriorityEntity> entities = request.priorities()
            .stream()
            .map(dto -> {
                DiscountPriorityEntity entity = new DiscountPriorityEntity();
                entity.setDiscountSettingId(configId);
                // Buscar el UUID del discount type por código
                UUID discountTypeId = discountTypeRepository.findByCode(dto.discountType())
                    .map(DiscountTypeEntity::getId)
                    .orElseThrow(() -> new BadRequestException("Tipo de descuento no encontrado: " + dto.discountType()));
                entity.setDiscountTypeId(discountTypeId);
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
    public DiscountLimitPriorityResponse getPriorities(String configId) {
        UUID configUuid = UUID.fromString(configId);

        List<DiscountPriorityEntity> priorities = priorityRepository
            .findByDiscountSettingsIdOrderByPriorityLevel(configUuid);

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
    private DiscountLimitPriorityResponse toResponse(UUID configId, List<DiscountPriorityEntity> entities) {
        List<DiscountLimitPriorityResponse.PriorityEntry> entries = entities
            .stream()
            .map(entity -> {
                String discountTypeCode = discountTypeRepository.findById(entity.getDiscountTypeId())
                    .map(DiscountTypeEntity::getCode)
                    .orElse("UNKNOWN");
                return new DiscountLimitPriorityResponse.PriorityEntry(
                    discountTypeCode,
                    entity.getPriorityLevel(),
                    entity.getCreatedAt().atOffset(java.time.ZoneOffset.UTC)
                );
            })
            .collect(Collectors.toList());

        return new DiscountLimitPriorityResponse(
            UUID.randomUUID().toString(), // ID de agrupación
            configId.toString(),
            entries,
            OffsetDateTime.now()
        );
    }
}
