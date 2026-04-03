package com.loyalty.service_admin.application.service;

import com.loyalty.service_admin.application.dto.customertier.CustomerTierCreateRequest;
import com.loyalty.service_admin.application.dto.customertier.CustomerTierResponse;
import com.loyalty.service_admin.domain.entity.CustomerTierEntity;
import com.loyalty.service_admin.domain.repository.CustomerTierRepository;
import com.loyalty.service_admin.infrastructure.exception.BadRequestException;
import com.loyalty.service_admin.infrastructure.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
            .discountTypeId(request.discountTypeId())
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

    private CustomerTierResponse mapToResponse(CustomerTierEntity entity) {
        return new CustomerTierResponse(
            entity.getId(),
            entity.getEcommerceId(),
            entity.getDiscountTypeId(),
            entity.getName(),
            entity.getDiscountPercentage(),
            entity.getHierarchyLevel(),
            entity.getIsActive(),
            entity.getCreatedAt(),
            entity.getUpdatedAt()
        );
    }
}