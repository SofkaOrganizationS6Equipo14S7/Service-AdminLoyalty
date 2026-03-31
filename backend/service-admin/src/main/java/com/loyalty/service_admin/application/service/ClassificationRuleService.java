package com.loyalty.service_admin.application.service;

import com.loyalty.service_admin.application.dto.ClassificationRuleCreateRequest;
import com.loyalty.service_admin.application.dto.ClassificationRuleCreatedEvent;
import com.loyalty.service_admin.application.dto.ClassificationRuleDeletedEvent;
import com.loyalty.service_admin.application.dto.ClassificationRuleResponse;
import com.loyalty.service_admin.application.dto.ClassificationRuleUpdateRequest;
import com.loyalty.service_admin.application.dto.ClassificationRuleUpdatedEvent;
import com.loyalty.service_admin.domain.entity.ClassificationRuleEntity;
import com.loyalty.service_admin.domain.repository.ClassificationRuleRepository;
import com.loyalty.service_admin.infrastructure.exception.BadRequestException;
import com.loyalty.service_admin.infrastructure.exception.ResourceNotFoundException;
import com.loyalty.service_admin.infrastructure.rabbitmq.ClassificationEventPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Service for Classification Rule management (source of truth in Admin).
 * Handles CRUD operations and publishes RabbitMQ events for Engine sync.
 */
@Service
@Slf4j
@RequiredArgsConstructor
@Transactional
public class ClassificationRuleService {

    private final ClassificationRuleRepository repository;
    private final ClassificationEventPublisher eventPublisher;

    public ClassificationRuleResponse create(ClassificationRuleCreateRequest request) {
        log.info("Creating classification rule: tierUid={}, metricType={}, priority={}",
            request.tierUid(), request.metricType(), request.priority());

        // Validate max >= min
        if (request.maxValue() != null && request.maxValue().compareTo(request.minValue()) < 0) {
            throw new BadRequestException("maxValue must be >= minValue");
        }

        // Check priority uniqueness for tier (not duplicate)
        if (repository.existsByTierAndPriority(request.tierUid(), request.priority())) {
            throw new BadRequestException("Priority " + request.priority() + " already exists for this tier");
        }

        ClassificationRuleEntity entity = new ClassificationRuleEntity(
            request.tierUid(),
            request.metricType(),
            request.minValue(),
            request.maxValue(),
            request.priority()
        );

        ClassificationRuleEntity saved = repository.save(entity);
        log.info("Classification rule created: uid={}", saved.getUid());

        // Publish event
        eventPublisher.publishRuleCreated(
            new ClassificationRuleCreatedEvent(
                "CLASSIFICATION_RULE_CREATED",
                saved.getUid(),
                saved.getCustomerTierUid(),
                saved.getMetricType(),
                saved.getMinValue(),
                saved.getMaxValue(),
                saved.getPriority(),
                saved.getCreatedAt()
            )
        );

        return mapToResponse(saved);
    }

    public ClassificationRuleResponse getById(UUID uid) {
        ClassificationRuleEntity entity = repository.findById(uid)
            .orElseThrow(() -> new ResourceNotFoundException("Classification rule not found: " + uid));
        return mapToResponse(entity);
    }

    public List<ClassificationRuleResponse> listActive() {
        return repository.findByIsActiveTrueOrderByPriorityAsc().stream()
            .map(this::mapToResponse)
            .toList();
    }

    public List<ClassificationRuleResponse> listByTier(UUID tierUid) {
        return repository.findByCustomerTierUidOrderByPriorityAsc(tierUid).stream()
            .map(this::mapToResponse)
            .toList();
    }

    public ClassificationRuleResponse update(UUID uid, ClassificationRuleUpdateRequest request) {
        ClassificationRuleEntity entity = repository.findById(uid)
            .orElseThrow(() -> new ResourceNotFoundException("Classification rule not found: " + uid));

        if (request.minValue() != null) {
            entity.setMinValue(request.minValue());
        }
        if (request.maxValue() != null) {
            entity.setMaxValue(request.maxValue());
        }
        if (request.priority() != null) {
            entity.setPriority(request.priority());
        }
        if (request.isActive() != null) {
            entity.setIsActive(request.isActive());
        }

        ClassificationRuleEntity saved = repository.save(entity);
        log.info("Classification rule updated: uid={}", uid);

        // Publish event
        eventPublisher.publishRuleUpdated(
            new ClassificationRuleUpdatedEvent(
                "CLASSIFICATION_RULE_UPDATED",
                saved.getUid(),
                saved.getCustomerTierUid(),
                saved.getMetricType(),
                saved.getMinValue(),
                saved.getMaxValue(),
                saved.getPriority(),
                saved.getIsActive(),
                saved.getUpdatedAt()
            )
        );

        return mapToResponse(saved);
    }

    public void delete(UUID uid) {
        ClassificationRuleEntity entity = repository.findById(uid)
            .orElseThrow(() -> new ResourceNotFoundException("Classification rule not found: " + uid));

        entity.setIsActive(false);
        repository.save(entity);
        log.info("Classification rule soft-deleted: uid={}", uid);

        // Publish event
        eventPublisher.publishRuleDeleted(
            new ClassificationRuleDeletedEvent(
                "CLASSIFICATION_RULE_DELETED",
                entity.getUid(),
                entity.getCustomerTierUid(),
                Instant.now()
            )
        );
    }

    private ClassificationRuleResponse mapToResponse(ClassificationRuleEntity entity) {
        return new ClassificationRuleResponse(
            entity.getUid(),
            entity.getCustomerTierUid(),
            entity.getMetricType(),
            entity.getMinValue(),
            entity.getMaxValue(),
            entity.getPriority(),
            entity.getIsActive(),
            entity.getCreatedAt(),
            entity.getUpdatedAt()
        );
    }
}
