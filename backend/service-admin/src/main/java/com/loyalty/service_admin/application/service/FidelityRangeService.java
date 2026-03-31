package com.loyalty.service_admin.application.service;

import com.loyalty.service_admin.application.dto.CreateFidelityRangeRequest;
import com.loyalty.service_admin.application.dto.FidelityRangeResponse;
import com.loyalty.service_admin.application.dto.UpdateFidelityRangeRequest;
import com.loyalty.service_admin.application.validation.FidelityRangeOverlapValidator;
import com.loyalty.service_admin.application.validation.FidelityRangeProgressionValidator;
import com.loyalty.service_admin.domain.entity.FidelityRangeEntity;
import com.loyalty.service_admin.domain.repository.FidelityRangeRepository;
import com.loyalty.service_admin.infrastructure.exception.BadRequestException;
import com.loyalty.service_admin.infrastructure.exception.ResourceNotFoundException;
import com.loyalty.service_admin.infrastructure.rabbitmq.FidelityRangeEventPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Service layer for Fidelity Range management
 * 
 * Business Logic:
 * 1. Validate no overlap (ESTRICTA) — 400 if ranges overlap
 * 2. Allow gaps (FLEXIBLE) — Admin can create huecos; Engine handles fallthrough
 * 3. Validate progression — minPoints must be in ascending order
 * 4. Publish RabbitMQ events for Engine Service sync
 * 5. Tenant isolation — user only manages own ecommerce ranges
 */
@Service
@Slf4j
@RequiredArgsConstructor
@Transactional
public class FidelityRangeService {

    private final FidelityRangeRepository repository;
    private final FidelityRangeOverlapValidator overlapValidator;
    private final FidelityRangeProgressionValidator progressionValidator;
    private final FidelityRangeEventPublisher eventPublisher;

    /**
     * Create a new fidelity range
     * 
     * Validations:
     * - minPoints < maxPoints
     * - No overlap with existing ranges
     * - minPoints progression
     * 
     * @param request create request
     * @param ecommerceId tenant ID
     * @return created range response
     * @throws BadRequestException if validation fails
     */
    public FidelityRangeResponse create(CreateFidelityRangeRequest request, UUID ecommerceId) {
        log.info("Creating fidelity range for ecommerce={} name={}", ecommerceId, request.name());

        // Validate minPoints < maxPoints
        overlapValidator.validateMinMaxOrder(request.minPoints(), request.maxPoints());

        // Fetch existing active ranges
        List<FidelityRangeEntity> existingRanges = 
            repository.findByEcommerceIdAndIsActiveTrueOrderByMinPointsAsc(ecommerceId);

        // Validate no overlap
        overlapValidator.validateNoOverlap(request.minPoints(), request.maxPoints(), existingRanges);

        // Validate progression
        progressionValidator.validateProgression(request.minPoints(), existingRanges);

        // Create entity
        FidelityRangeEntity entity = new FidelityRangeEntity(
            ecommerceId,
            request.name(),
            request.minPoints(),
            request.maxPoints(),
            request.discountPercentage()
        );

        // Save to database
        FidelityRangeEntity saved = repository.save(entity);
        log.info("Fidelity range created: uid={} ecommerce={} name={}", saved.getUid(), ecommerceId, saved.getName());

        // Publish event for Engine Service sync
        eventPublisher.publishFidelityRangeCreated(saved);

        return mapToResponse(saved);
    }

    /**
     * Get all active ranges for ecommerce (paginated)
     * 
     * @param ecommerceId tenant ID
     * @param pageable pagination info
     * @return paginated list of ranges
     */
    public Page<FidelityRangeResponse> listByEcommerce(UUID ecommerceId, Pageable pageable) {
        log.info("Listing fidelity ranges for ecommerce={} page={} size={}", 
            ecommerceId, pageable.getPageNumber(), pageable.getPageSize());

        Page<FidelityRangeEntity> page = 
            repository.findByEcommerceIdAndIsActiveTrueOrderByMinPointsAsc(ecommerceId, pageable);

        List<FidelityRangeResponse> responses = page.getContent().stream()
            .map(this::mapToResponse)
            .toList();

        return new PageImpl<>(responses, pageable, page.getTotalElements());
    }

    /**
     * Get a specific range by uid
     * 
     * @param uid range UID
     * @param ecommerceId tenant ID
     * @return range response
     * @throws ResourceNotFoundException if not found or belongs to another tenant
     */
    public FidelityRangeResponse getById(UUID uid, UUID ecommerceId) {
        log.info("Getting fidelity range uid={} ecommerce={}", uid, ecommerceId);

        FidelityRangeEntity entity = repository.findByUidAndEcommerceId(uid, ecommerceId)
            .orElseThrow(() -> {
                log.warn("Fidelity range not found: uid={} ecommerce={}", uid, ecommerceId);
                return new ResourceNotFoundException("Rango de fidelidad no encontrado");
            });

        return mapToResponse(entity);
    }

    /**
     * Update an existing range
     * 
     * Validations: Same as create
     * 
     * @param uid range UID
     * @param request update request (fields are optional)
     * @param ecommerceId tenant ID
     * @return updated range response
     * @throws ResourceNotFoundException if not found
     * @throws BadRequestException if validation fails
     */
    public FidelityRangeResponse update(UUID uid, UpdateFidelityRangeRequest request, UUID ecommerceId) {
        log.info("Updating fidelity range uid={} ecommerce={}", uid, ecommerceId);

        FidelityRangeEntity entity = repository.findByUidAndEcommerceId(uid, ecommerceId)
            .orElseThrow(() -> {
                log.warn("Fidelity range not found: uid={} ecommerce={}", uid, ecommerceId);
                return new ResourceNotFoundException("Rango de fidelidad no encontrado");
            });

        // Update only provided fields
        boolean changed = false;

        if (request.name() != null && !request.name().equals(entity.getName())) {
            entity.setName(request.name());
            changed = true;
        }

        if (request.minPoints() != null && !request.minPoints().equals(entity.getMinPoints())) {
            entity.setMinPoints(request.minPoints());
            changed = true;
        }

        if (request.maxPoints() != null && !request.maxPoints().equals(entity.getMaxPoints())) {
            entity.setMaxPoints(request.maxPoints());
            changed = true;
        }

        if (request.discountPercentage() != null && !request.discountPercentage().equals(entity.getDiscountPercentage())) {
            entity.setDiscountPercentage(request.discountPercentage());
            changed = true;
        }

        if (!changed) {
            log.info("No changes detected for range uid={}", uid);
            return mapToResponse(entity);
        }

        // Re-validate after update
        List<FidelityRangeEntity> existingRanges = 
            repository.findByEcommerceIdAndIsActiveTrueOrderByMinPointsAsc(ecommerceId);
        
        // Remove current entity from validation (to exclude itself)
        existingRanges.removeIf(r -> r.getUid().equals(uid));

        overlapValidator.validateMinMaxOrder(entity.getMinPoints(), entity.getMaxPoints());
        overlapValidator.validateNoOverlap(entity.getMinPoints(), entity.getMaxPoints(), existingRanges);
        progressionValidator.validateProgression(entity.getMinPoints(), existingRanges);

        // Update timestamp
        entity.setUpdatedAt(Instant.now());

        // Save
        FidelityRangeEntity saved = repository.save(entity);
        log.info("Fidelity range updated: uid={} ecommerce={}", uid, ecommerceId);

        // Publish event
        eventPublisher.publishFidelityRangeUpdated(saved);

        return mapToResponse(saved);
    }

    /**
     * Delete (soft-delete) a range
     * 
     * @param uid range UID
     * @param ecommerceId tenant ID
     * @throws ResourceNotFoundException if not found
     */
    public void delete(UUID uid, UUID ecommerceId) {
        log.info("Deleting fidelity range uid={} ecommerce={}", uid, ecommerceId);

        FidelityRangeEntity entity = repository.findByUidAndEcommerceId(uid, ecommerceId)
            .orElseThrow(() -> {
                log.warn("Fidelity range not found: uid={} ecommerce={}", uid, ecommerceId);
                return new ResourceNotFoundException("Rango de fidelidad no encontrado");
            });

        // Soft delete
        entity.setIsActive(false);
        entity.setUpdatedAt(Instant.now());

        repository.save(entity);
        log.info("Fidelity range soft-deleted: uid={} ecommerce={}", uid, ecommerceId);

        // Publish event
        eventPublisher.publishFidelityRangeDeleted(entity);
    }

    /**
     * Map entity to response DTO
     */
    private FidelityRangeResponse mapToResponse(FidelityRangeEntity entity) {
        return new FidelityRangeResponse(
            entity.getUid(),
            entity.getName(),
            entity.getMinPoints(),
            entity.getMaxPoints(),
            entity.getDiscountPercentage(),
            entity.getIsActive(),
            entity.getCreatedAt(),
            entity.getUpdatedAt()
        );
    }
}
