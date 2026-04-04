package com.loyalty.service_admin.application.service;

import com.loyalty.service_admin.application.dto.ecommerce.EcommerceCreateRequest;
import com.loyalty.service_admin.application.dto.ecommerce.EcommerceResponse;
import com.loyalty.service_admin.application.dto.ecommerce.EcommerceUpdateStatusRequest;
import com.loyalty.service_admin.domain.entity.EcommerceEntity;
import com.loyalty.service_admin.domain.model.ecommerce.EcommerceStatus;
import com.loyalty.service_admin.domain.repository.EcommerceRepository;
import com.loyalty.service_admin.infrastructure.exception.BadRequestException;
import com.loyalty.service_admin.infrastructure.exception.ConflictException;
import com.loyalty.service_admin.infrastructure.exception.EcommerceNotFoundException;
import com.loyalty.service_admin.infrastructure.rabbitmq.EcommerceEventPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class EcommerceService {
    
    private final EcommerceRepository ecommerceRepository;
    private final EcommerceEventPublisher eventPublisher;
    
    /**
     * @param request EcommerceCreateRequest (name, slug)
     * @return EcommerceResponse con ecommerce creado
     * @throws ConflictException si slug ya existe
     * @throws BadRequestException si datos inválidos
     */
    @Transactional
    public EcommerceResponse createEcommerce(EcommerceCreateRequest request) {
        log.info("Creando ecommerce: name={}, slug={}", request.name(), request.slug());
        
        if (ecommerceRepository.existsBySlug(request.slug())) {
            log.warn("Intento de crear ecommerce con slug duplicado: {}", request.slug());
            throw new ConflictException(
                String.format("El slug '%s' ya está en uso. Elige uno diferente.", request.slug())
            );
        }

        EcommerceEntity entity = EcommerceEntity.builder()
                .name(request.name())
                .slug(request.slug())
                .build();
        
        EcommerceEntity saved = ecommerceRepository.save(entity);
        log.info("Ecommerce creado exitosamente: uid={}, slug={}, status={}", 
                saved.getId(), saved.getSlug(), saved.getStatus());
        
        return toResponse(saved);
    }
    
    /**
     * @param status filtro opcional (ACTIVE/INACTIVE); null = todos
     * @param page número de página (0-indexed)
     * @param size tamaño de la página
     * @return Page<EcommerceResponse> con ecommerces
     */
    @Transactional(readOnly = true)
    public Page<EcommerceResponse> listEcommerces(String status, int page, int size) {
        log.info("Listando ecommerces: status={}, page={}, size={}", status, page, size);
        
        Pageable pageable = PageRequest.of(page, size);
        Page<EcommerceEntity> entities;
        
        if (status != null && !status.isBlank()) {
            try {
                EcommerceStatus statusEnum = EcommerceStatus.valueOf(status.toUpperCase());
                entities = ecommerceRepository.findAll(
                    (root, query, cb) -> cb.equal(root.get("status"), statusEnum),
                    pageable
                );
            } catch (IllegalArgumentException e) {
                throw new BadRequestException("Status inválido: " + status);
            }
        } else {
            entities = ecommerceRepository.findAll(pageable);
        }

        return entities.map(this::toResponse);
    }
    
    /**
     * @param uid identificador único del ecommerce
     * @return EcommerceResponse
     * @throws EcommerceNotFoundException si no existe
     */
    @Transactional(readOnly = true)
    public EcommerceResponse getEcommerceById(UUID uid) {
        log.info("Obteniendo ecommerce por uid: {}", uid);
        
        EcommerceEntity entity = ecommerceRepository.findById(uid)
                .orElseThrow(() -> {
                    log.warn("Ecommerce no encontrado: uid={}", uid);
                    return new EcommerceNotFoundException(
                        String.format("El ecommerce con uid '%s' no existe.", uid)
                    );
                });
        
        return toResponse(entity);
    }
    
    /**
     * @param uid identificador único del ecommerce
     * @param request EcommerceUpdateStatusRequest (status)
     * @return EcommerceResponse actualizado
     * @throws EcommerceNotFoundException si no existe
     * @throws BadRequestException si status inválido
     */
    @Transactional
    public EcommerceResponse updateEcommerceStatus(UUID uid, EcommerceUpdateStatusRequest request) {
        log.info("Actualizando status de ecommerce: uid={}, newStatus={}", uid, request.status());
        
        EcommerceEntity entity = ecommerceRepository.findById(uid)
                .orElseThrow(() -> {
                    log.warn("Ecommerce no encontrado para actualización: uid={}", uid);
                    return new EcommerceNotFoundException(
                        String.format("El ecommerce con uid '%s' no existe.", uid)
                    );
                });
        
        EcommerceStatus newStatus;

        try {
            newStatus = EcommerceStatus.valueOf(request.status());
        } catch (IllegalArgumentException e) {
            log.warn("Status inválido para ecommerce: {}", request.status());
            throw new BadRequestException("Status inválido: " + request.status());
        }
        
        EcommerceStatus oldStatus = entity.getStatus();
        
        if (oldStatus == newStatus) {
            log.info("Status no cambió: uid={}, status={}", uid, oldStatus);
            return toResponse(entity);
        }
        
        entity.setStatus(newStatus);
        EcommerceEntity updated = ecommerceRepository.save(entity);
        
        try {
            eventPublisher.publishEcommerceStatusChanged(entity.getId(), newStatus);
            log.info("Evento de cambio de status publicado: uid={}, newStatus={}", uid, newStatus);
        } catch (Exception e) {
            log.error("Error al publicar evento RabbitMQ: uid={}, newStatus={}", uid, newStatus, e);
            throw new RuntimeException("No se pudo publicar evento de cambio de status", e);
        }
        
        log.info("Ecommerce actualizado exitosamente: uid={}, oldStatus={}, newStatus={}", 
                uid, oldStatus, newStatus);
        
        return toResponse(updated);
    }
    
    /**
     * @param ecommerceId UUID del ecommerce
     * @throws EcommerceNotFoundException si no existe o es null
     */
    @Transactional(readOnly = true)
    public void validateEcommerceExists(UUID ecommerceId) {
        if (ecommerceId == null) {
            throw new EcommerceNotFoundException("Ecommerce ID no puede ser null");
        }
        
        boolean exists = ecommerceRepository.existsById(ecommerceId);
        if (!exists) {
            log.warn("Validación fallida: ecommerce no existe: {}", ecommerceId);
            throw new EcommerceNotFoundException(
                String.format("El ecommerce con uid '%s' no existe.", ecommerceId)
            );
        }
    }
    
    /**
     * @param entity entidad JPA
     * @return DTO para respuesta HTTP
     */
    private EcommerceResponse toResponse(EcommerceEntity entity) {
        return new EcommerceResponse(
                entity.getId(),
                entity.getName(),
                entity.getSlug(),
                entity.getStatus().name(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }
}
