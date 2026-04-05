package com.loyalty.service_admin.application.service;

import com.loyalty.service_admin.application.dto.customertier.CustomerTierCreateRequest;
import com.loyalty.service_admin.application.dto.customertier.CustomerTierUpdateRequest;
import com.loyalty.service_admin.application.dto.customertier.CustomerTierResponse;
import com.loyalty.service_admin.domain.entity.CustomerTierEntity;
import com.loyalty.service_admin.domain.repository.CustomerTierRepository;
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

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional
public class CustomerTierService {

    private final CustomerTierRepository repository;

    public CustomerTierResponse create(CustomerTierCreateRequest request) {
        log.info("Creating customer tier: ecommerceId={}, name={}, hierarchyLevel={}", 
            request.ecommerceId(), request.name(), request.hierarchyLevel());

        if (repository.existsByEcommerceIdAndName(request.ecommerceId(), request.name())) {
            throw new BadRequestException("Tier with name '" + request.name() + "' already exists for this ecommerce");
        }

        CustomerTierEntity entity = CustomerTierEntity.builder()
            .ecommerceId(request.ecommerceId())
            .name(request.name())
            .discountPercentage(request.discountPercentage())
            .hierarchyLevel(request.hierarchyLevel())
            .isActive(true)
            .build();

        CustomerTierEntity saved = repository.save(entity);
        log.info("Customer tier created: id={}", saved.getId());

        return mapToResponse(saved);
    }

    public CustomerTierResponse getById(UUID id) {
        CustomerTierEntity entity = repository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Customer tier not found: " + id));
        return mapToResponse(entity);
    }

    public List<CustomerTierResponse> listActive() {
        return repository.findByIsActiveTrueOrderByHierarchyLevelAsc().stream()
            .map(this::mapToResponse)
            .toList();
    }

    public List<CustomerTierResponse> listAll() {
        return repository.findAllByOrderByHierarchyLevelAsc().stream()
            .map(this::mapToResponse)
            .toList();
    }

    public void delete(UUID id) {
        CustomerTierEntity entity = repository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Customer tier not found: " + id));
        entity.setIsActive(false);
        repository.save(entity);
        log.info("Customer tier soft-deleted: id={}", id);
    }

    /**
     * CRITERIO-7.5: Listar customer tiers con paginación
     * Opcional: filtrar por isActive si se especifica
     */
    @Transactional(readOnly = true)
    public Page<CustomerTierResponse> listPaginated(Pageable pageable, Boolean isActive) {
        Page<CustomerTierEntity> page = repository.findAll(pageable);
        
        if (isActive != null) {
            // Filter in memory  
            var filtered = page.getContent().stream()
                    .filter(tier -> tier.getIsActive().equals(isActive))
                    .map(this::mapToResponse)
                    .toList();
            return new org.springframework.data.domain.PageImpl<>(
                    filtered, 
                    pageable, 
                    page.getTotalElements()
            );
        }
        
        return page.map(this::mapToResponse);
    }

    /**
     * Actualizar un tier (name, discountPercentage, hierarchyLevel)
     * No permite cambiar ecommerceId ni isActive
     */
    @Transactional
    public CustomerTierResponse update(UUID id, CustomerTierUpdateRequest request) {
        CustomerTierEntity entity = repository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Customer tier not found: " + id));

        // Validar unicidad de nombre si cambió (para el mismo ecommerce)
        if (!entity.getName().equals(request.name()) && 
            repository.existsByEcommerceIdAndName(entity.getEcommerceId(), request.name())) {
            throw new BadRequestException("Tier with name '" + request.name() + "' already exists for this ecommerce");
        }

        entity.setName(request.name());
        entity.setDiscountPercentage(request.discountPercentage());
        entity.setHierarchyLevel(request.hierarchyLevel());
        entity.setIsActive(request.isActive());

        CustomerTierEntity updated = repository.save(entity);
        log.info("Customer tier updated: id={}", id);

        return mapToResponse(updated);
    }

    /**
     * Activar un tier (revertir soft delete)
     */
    @Transactional
    public CustomerTierResponse activate(UUID id) {
        CustomerTierEntity entity = repository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Customer tier not found: " + id));

        entity.setIsActive(true);
        CustomerTierEntity updated = repository.save(entity);
        log.info("Customer tier activated: id={}", id);

        return mapToResponse(updated);
    }

    private CustomerTierResponse mapToResponse(CustomerTierEntity entity) {
        return new CustomerTierResponse(
            entity.getId(),
            entity.getEcommerceId(),
            entity.getName(),
            entity.getDiscountPercentage(),
            entity.getHierarchyLevel(),
            entity.getIsActive(),
            entity.getCreatedAt(),
            entity.getUpdatedAt()
        );
    }
}