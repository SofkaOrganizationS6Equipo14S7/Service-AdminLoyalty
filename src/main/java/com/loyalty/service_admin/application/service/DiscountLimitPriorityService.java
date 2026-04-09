package com.loyalty.service_admin.application.service;

import com.loyalty.service_admin.application.dto.rules.discount.DiscountLimitPriorityRequest;
import com.loyalty.service_admin.application.dto.rules.discount.DiscountLimitPriorityResponse;
import com.loyalty.service_admin.application.port.in.DiscountLimitPriorityUseCase;
import com.loyalty.service_admin.application.port.out.DiscountLimitPriorityPersistencePort;
import com.loyalty.service_admin.application.port.out.DiscountLimitPriorityEventPort;
import com.loyalty.service_admin.application.validation.DiscountPriorityValidator;
import com.loyalty.service_admin.domain.entity.DiscountPriorityEntity;
import com.loyalty.service_admin.domain.entity.DiscountTypeEntity;
import com.loyalty.service_admin.domain.model.DiscountType;
import com.loyalty.service_admin.domain.repository.DiscountTypeRepository;
import com.loyalty.service_admin.infrastructure.exception.BadRequestException;
import com.loyalty.service_admin.infrastructure.exception.ResourceNotFoundException;
import lombok.extern.slf4j.Slf4j;
import lombok.RequiredArgsConstructor;
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
@RequiredArgsConstructor
@Transactional
public class DiscountLimitPriorityService implements DiscountLimitPriorityUseCase {

    private final DiscountLimitPriorityPersistencePort persistencePort;
    private final DiscountLimitPriorityEventPort eventPort;
    private final DiscountTypeRepository discountTypeRepository;
    private final DiscountPriorityValidator priorityValidator;

    @Override
    @Transactional
    public DiscountLimitPriorityResponse savePriorities(DiscountLimitPriorityRequest request) {
        // Validar request
        priorityValidator.validatePriorities(request);

        UUID configId = request.discountSettingId();

        // Obtener prioridades anteriores para borrarlas
        List<DiscountPriorityEntity> oldPriorities = persistencePort.findPrioritiesByConfig(configId);
        for (DiscountPriorityEntity old : oldPriorities) {
            persistencePort.deletePriority(old.getId());
        }
        log.info("Cleared previous priorities for config: {}", configId);

        List<DiscountPriorityEntity> saved = request.priorities()
            .stream()
            .map(dto -> {
                DiscountPriorityEntity entity = new DiscountPriorityEntity();
                entity.setDiscountSettingId(configId);
                // Validar que discountTypeId existe
                discountTypeRepository.findById(dto.discountTypeId())
                    .orElseThrow(() -> new BadRequestException("Tipo de descuento no encontrado: " + dto.discountTypeId()));
                entity.setDiscountTypeId(dto.discountTypeId());
                entity.setPriorityLevel(dto.priorityLevel());
                return persistencePort.savePriority(entity);
            })
            .collect(Collectors.toList());
        log.info("Saved {} priorities for config: {}", saved.size(), configId);

        // Publish event - need to get ecommerceId from config (use pragmatic default for now)
        if (!saved.isEmpty()) {
            try {
                UUID defaultEcommerceId = UUID.fromString("00000000-0000-0000-0000-000000000000");
                eventPort.publishPriorityUpdated(saved.get(0), defaultEcommerceId);
            } catch (Exception ex) {
                log.error("Failed to publish priority event: {}", ex.getMessage());
            }
        }

        return toResponse(configId, saved);
    }

    @Override
    @Transactional(readOnly = true)
    public DiscountLimitPriorityResponse getPriorities(UUID configId) {
        List<DiscountPriorityEntity> priorities = persistencePort.findPrioritiesByConfig(configId);

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
