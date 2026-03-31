package com.loyalty.service_admin.application.service;

import com.loyalty.service_admin.application.dto.CustomerTierCreateRequest;
import com.loyalty.service_admin.application.dto.CustomerTierResponse;
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

/**
 * Service for Customer Tier management (source of truth in Admin).
 * Handles CRUD operations for loyalty tiers (Bronze, Silver, Gold, Platinum).
 */
@Service
@Slf4j
@RequiredArgsConstructor
@Transactional
public class CustomerTierService {

    private final CustomerTierRepository repository;

    public CustomerTierResponse create(CustomerTierCreateRequest request) {
        log.info("Creating customer tier: name={}, level={}", request.name(), request.level());

        if (repository.existsByName(request.name())) {
            throw new BadRequestException("Tier with name '" + request.name() + "' already exists");
        }

        CustomerTierEntity entity = new CustomerTierEntity(request.name(), request.level());
        CustomerTierEntity saved = repository.save(entity);
        log.info("Customer tier created: uid={}", saved.getUid());

        return mapToResponse(saved);
    }

    public CustomerTierResponse getById(UUID uid) {
        CustomerTierEntity entity = repository.findById(uid)
            .orElseThrow(() -> new ResourceNotFoundException("Customer tier not found: " + uid));
        return mapToResponse(entity);
    }

    public List<CustomerTierResponse> listActive() {
        return repository.findByIsActiveTrueOrderByLevelAsc().stream()
            .map(this::mapToResponse)
            .toList();
    }

    public List<CustomerTierResponse> listAll() {
        return repository.findAllByOrderByLevelAsc().stream()
            .map(this::mapToResponse)
            .toList();
    }

    public void delete(UUID uid) {
        CustomerTierEntity entity = repository.findById(uid)
            .orElseThrow(() -> new ResourceNotFoundException("Customer tier not found: " + uid));
        entity.setIsActive(false);
        repository.save(entity);
        log.info("Customer tier soft-deleted: uid={}", uid);
    }

    private CustomerTierResponse mapToResponse(CustomerTierEntity entity) {
        return new CustomerTierResponse(
            entity.getUid(),
            entity.getName(),
            entity.getLevel(),
            entity.getIsActive(),
            entity.getCreatedAt(),
            entity.getUpdatedAt()
        );
    }
}
