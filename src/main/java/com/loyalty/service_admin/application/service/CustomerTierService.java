package com.loyalty.service_admin.application.service;

import com.loyalty.service_admin.application.dto.customertier.CustomerTierCreateRequest;
import com.loyalty.service_admin.application.dto.customertier.CustomerTierUpdateRequest;
import com.loyalty.service_admin.application.dto.customertier.CustomerTierResponse;
import com.loyalty.service_admin.application.port.in.CustomerTierUseCase;
import com.loyalty.service_admin.application.port.out.CustomerTierPersistencePort;
import com.loyalty.service_admin.application.port.out.CustomerTierEventPort;
import com.loyalty.service_admin.domain.entity.CustomerTierEntity;
import com.loyalty.service_admin.infrastructure.exception.BadRequestException;
import com.loyalty.service_admin.infrastructure.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * CustomerTierService - Implementación del UseCase para operaciones sobre customer tiers.
 * Implementa CustomerTierUseCase e inyecta puertos (interfaces) en lugar de implementaciones concretas.
 */
@Service
@Slf4j
@RequiredArgsConstructor
@Transactional
public class CustomerTierService implements CustomerTierUseCase {

    private final CustomerTierPersistencePort persistencePort;
    private final CustomerTierEventPort eventPort;

    @Override
    public CustomerTierResponse create(CustomerTierCreateRequest request) {
        log.info("Creating customer tier: ecommerceId={}, name={}, hierarchyLevel={}", 
            request.ecommerceId(), request.name(), request.hierarchyLevel());

        // Validar unicidad solo contra tiers ACTIVOS
        if (persistencePort.existsActiveTierWithNameAndEcommerce(request.ecommerceId(), request.name())) {
            throw new BadRequestException("Tier with name '" + request.name() + "' already exists for this ecommerce");
        }

        CustomerTierEntity entity = CustomerTierEntity.builder()
            .ecommerceId(request.ecommerceId())
            .name(request.name())
            .hierarchyLevel(request.hierarchyLevel())
            .isActive(true)
            .build();

        CustomerTierEntity saved = persistencePort.saveTier(entity);
        log.info("Customer tier created: id={}", saved.getId());

        // Publicar evento
        eventPort.publishTierCreated(saved, request.ecommerceId());

        return mapToResponse(saved);
    }

    @Override
    public CustomerTierResponse getById(UUID id) {
        CustomerTierEntity entity = persistencePort.findTierById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Customer tier not found: " + id));
        return mapToResponse(entity);
    }

    @Override
    public List<CustomerTierResponse> listActive() {
        return persistencePort.findActivetiersOrderedByHierarchy().stream()
            .map(this::mapToResponse)
            .toList();
    }

    @Override
    public List<CustomerTierResponse> listAll() {
        return persistencePort.findAllTiersOrderedByHierarchy().stream()
            .map(this::mapToResponse)
            .toList();
    }

    @Override
    public void delete(UUID id) {
        CustomerTierEntity entity = persistencePort.findTierById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Customer tier not found: " + id));
        persistencePort.deleteTier(entity);
        log.info("Customer tier soft-deleted: id={}", id);
        
        // Publicar evento
        eventPort.publishTierDeleted(id, entity.getEcommerceId());
    }

    /**
     * CRITERIO-7.5: Listar customer tiers con paginación
     * Opcional: filtrar por isActive si se especifica
     */
    @Override
    @Transactional(readOnly = true)
    public Page<CustomerTierResponse> listPaginated(Pageable pageable, Boolean isActive) {
        Page<CustomerTierEntity> page = persistencePort.findTiersPaginated(pageable, isActive);
        return page.map(this::mapToResponse);
    }

    /**
     * Actualizar un tier (name, hierarchyLevel)
     * No permite cambiar ecommerceId ni isActive
     * Descuentos se definen en las rules, no en el tier
     */
    @Override
    @Transactional
    public CustomerTierResponse update(UUID id, CustomerTierUpdateRequest request) {
        CustomerTierEntity entity = persistencePort.findTierById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Customer tier not found: " + id));

        // Validar unicidad de nombre si cambió (solo contra tiers ACTIVOS del mismo ecommerce)
        if (!entity.getName().equals(request.name()) && 
            persistencePort.existsActiveTierWithNameAndEcommerce(entity.getEcommerceId(), request.name())) {
            throw new BadRequestException("Tier with name '" + request.name() + "' already exists for this ecommerce");
        }

        entity.setName(request.name());
        entity.setHierarchyLevel(request.hierarchyLevel());

        CustomerTierEntity updated = persistencePort.saveTier(entity);
        log.info("Customer tier updated: id={}", id);

        // Publicar evento
        eventPort.publishTierUpdated(updated, entity.getEcommerceId());

        return mapToResponse(updated);
    }

    @Override
    @Transactional
    public CustomerTierResponse activate(UUID id) {
        CustomerTierEntity entity = persistencePort.findTierById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Customer tier not found: " + id));

        entity.setIsActive(true);
        CustomerTierEntity updated = persistencePort.saveTier(entity);
        log.info("Customer tier activated: id={}", id);

        // Publicar evento
        eventPort.publishTierActivated(updated, entity.getEcommerceId());

        return mapToResponse(updated);
    }

    private CustomerTierResponse mapToResponse(CustomerTierEntity entity) {
        return new CustomerTierResponse(
            entity.getId(),
            entity.getEcommerceId(),
            entity.getName(),
            entity.getHierarchyLevel(),
            entity.getIsActive(),
            entity.getCreatedAt(),
            entity.getUpdatedAt()
        );
    }
}