package com.loyalty.service_admin.application.service;

import com.loyalty.service_admin.application.dto.EcommerceCreateRequest;
import com.loyalty.service_admin.application.dto.EcommerceResponse;
import com.loyalty.service_admin.application.dto.EcommerceUpdateStatusRequest;
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

/**
 * Servicio de gestión de ecommerces (multi-tenant).
 * 
 * SPEC-001: Registro y Gestión de Ecommerces
 * 
 * Responsabilidades:
 * - CRUD de ecommerces con validaciones de negocio
 * - Validación de slug único
 * - Cambio de estado (ACTIVE ↔ INACTIVE)
 * - Publicación de eventos RabbitMQ para cascada de desactivación
 * - Conversión entre entidades y DTOs
 * 
 * Implementa:
 * - HU-13.1: Registro exitoso de un nuevo ecommerce
 * - HU-13.2: Listar y obtener ecommerces
 * - HU-13.3: Actualizar estado de un ecommerce
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EcommerceService {
    
    private final EcommerceRepository ecommerceRepository;
    private final EcommerceEventPublisher eventPublisher;
    
    /**
     * Crea un nuevo ecommerce.
     * 
     * CRITERIO-1.1: Registro exitoso con datos válidos
     * CRITERIO-1.2: Rechazo por slug duplicado
     * CRITERIO-1.3: Rechazo por datos incompletos
     * 
     * Validaciones:
     * - Slug debe ser único
     * - Name es obligatorio
     * - UUID se genera automáticamente
     * - Status inicialmente ACTIVE
     * 
     * @param request EcommerceCreateRequest (name, slug)
     * @return EcommerceResponse con ecommerce creado
     * @throws ConflictException si slug ya existe
     * @throws BadRequestException si datos inválidos
     */
    @Transactional
    public EcommerceResponse createEcommerce(EcommerceCreateRequest request) {
        log.info("Creando ecommerce: name={}, slug={}", request.name(), request.slug());
        
        // Validación: Slug duplicado
        if (ecommerceRepository.existsBySlug(request.slug())) {
            log.warn("Intento de crear ecommerce con slug duplicado: {}", request.slug());
            throw new ConflictException(
                String.format("El slug '%s' ya está en uso. Elige uno diferente.", request.slug())
            );
        }
        
        // Crear entidad (UUID y status se generan en @PrePersist)
        EcommerceEntity entity = EcommerceEntity.builder()
                .name(request.name())
                .slug(request.slug())
                // Status y timestamps se generan automáticamente en @PrePersist
                .build();
        
        EcommerceEntity saved = ecommerceRepository.save(entity);
        log.info("Ecommerce creado exitosamente: uid={}, slug={}, status={}", 
                saved.getUid(), saved.getSlug(), saved.getStatus());
        
        return toResponse(saved);
    }
    
    /**
     * Lista ecommerces con paginación y filtrado por status.
     * 
     * CRITERIO-2.1: Listar todos los ecommerces
     * 
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
                // Usar Specification para filtrado dinámico
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
     * Obtiene un ecommerce por UUID.
     * 
     * CRITERIO-2.2: Obtener ecommerce por uid
     * 
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
     * Actualiza el estado de un ecommerce (ACTIVE ↔ INACTIVE).
     * 
     * CRITERIO-3.1: Desactivación exitosa
     * CRITERIO-3.2: Reactivación
     * CRITERIO-3.3: Status inválido
     * CRITERIO-3.4: No se pueden actualizar otros campos
     * 
     * Cascada en desactivación:
     * - Emitir evento EcommerceStatusChangedEvent a RabbitMQ (Fanout Exchange)
     * - service-admin invalida JWT de usuarios del ecommerce
     * - service-engine invalida API Keys en caché Caffeine
     * 
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
        
        // Parsear y validar nuevo status
        EcommerceStatus newStatus;
        try {
            newStatus = EcommerceStatus.valueOf(request.status().toUpperCase());
        } catch (IllegalArgumentException e) {
            log.warn("Status inválido: {}", request.status());
            throw new BadRequestException(
                "El status debe ser 'ACTIVE' o 'INACTIVE'."
            );
        }
        
        EcommerceStatus oldStatus = entity.getStatus();
        
        // Si no hay cambio de status, retornar sin hacer nada
        if (oldStatus == newStatus) {
            log.info("Status no cambió: uid={}, status={}", uid, oldStatus);
            return toResponse(entity);
        }
        
        // Actualizar status
        entity.setStatus(newStatus);
        EcommerceEntity updated = ecommerceRepository.save(entity);
        
        // Publicar evento para cascada (RabbitMQ Fanout Exchange)
        try {
            eventPublisher.publishEcommerceStatusChanged(entity.getUid(), newStatus);
            log.info("Evento de cambio de status publicado: uid={}, newStatus={}", uid, newStatus);
        } catch (Exception e) {
            log.error("Error al publicar evento RabbitMQ: uid={}, newStatus={}", uid, newStatus, e);
            // Reviertir cambio si el publish falla (transacción se revierte automáticamente)
            throw new RuntimeException("No se pudo publicar evento de cambio de status", e);
        }
        
        log.info("Ecommerce actualizado exitosamente: uid={}, oldStatus={}, newStatus={}", 
                uid, oldStatus, newStatus);
        
        return toResponse(updated);
    }
    
    /**
     * Valida que un ecommerce existe (para uso de otros servicios como UserService).
     * 
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
     * Convierte una entidad EcommerceEntity a DTO EcommerceResponse.
     * 
     * @param entity entidad JPA
     * @return DTO para respuesta HTTP
     */
    private EcommerceResponse toResponse(EcommerceEntity entity) {
        return new EcommerceResponse(
                entity.getUid().toString(),
                entity.getName(),
                entity.getSlug(),
                entity.getStatus().name(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }
}
