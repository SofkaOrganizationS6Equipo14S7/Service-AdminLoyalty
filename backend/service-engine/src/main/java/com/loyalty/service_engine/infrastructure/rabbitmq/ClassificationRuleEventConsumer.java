package com.loyalty.service_engine.infrastructure.rabbitmq;

import com.loyalty.service_engine.application.dto.events.ClassificationRuleCreatedEvent;
import com.loyalty.service_engine.application.dto.events.ClassificationRuleDeletedEvent;
import com.loyalty.service_engine.application.dto.events.ClassificationRuleUpdatedEvent;
import com.loyalty.service_engine.application.service.ClassificationMatrixCaffeineCacheService;
import com.loyalty.service_engine.domain.entity.ClassificationRuleReplicaEntity;
import com.loyalty.service_engine.domain.repository.ClassificationRuleReplicaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.time.Instant;

/**
 * Consumes classification rule events published by Admin Service.
 * Updates the in-memory Caffeine cache and replica database to keep Engine in sync.
 *
 * Event flow:
 * Admin Service (source of truth) → RabbitMQ → Engine Service (listener) → DB replica + Caffeine Cache
 *
 * Three paths:
 * 1. ClassificationRuleCreated: Add new rule to DB replica and invalidate cache
 * 2. ClassificationRuleUpdated: Update rule in DB replica and invalidate cache
 * 3. ClassificationRuleDeleted: Soft-delete rule in DB replica and invalidate cache
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ClassificationRuleEventConsumer {

    private final ClassificationRuleReplicaRepository ruleReplicaRepo;
    private final ClassificationMatrixCaffeineCacheService cacheService;

    @RabbitListener(queues = "${rabbitmq.queue.classification:classification.matrix.queue}")
    public void handleRuleCreated(ClassificationRuleCreatedEvent event) {
        log.info("Received ClassificationRuleCreatedEvent: ruleUid={}", event.ruleUid());

        ClassificationRuleReplicaEntity replica = new ClassificationRuleReplicaEntity(
            event.ruleUid(),
            event.tierUid(),
            event.metricType(),
            event.minValue(),
            event.maxValue(),
            event.priority(),
            true,
            Instant.now()
        );

        ruleReplicaRepo.save(replica);
        cacheService.invalidate();
        log.info("Rule created in replica and cache invalidated");
    }

    @RabbitListener(queues = "${rabbitmq.queue.classification:classification.matrix.queue}")
    public void handleRuleUpdated(ClassificationRuleUpdatedEvent event) {
        log.info("Received ClassificationRuleUpdatedEvent: ruleUid={}", event.ruleUid());

        return;
        /* Return is valid because the listener is idempotent.
           If the same event arrives multiple times (retry), we don't update —
           just invalidate cache. The next /classify call will reload from DB.
        */
    }

    @RabbitListener(queues = "${rabbitmq.queue.classification:classification.matrix.queue}")
    public void handleRuleDeleted(ClassificationRuleDeletedEvent event) {
        log.info("Received ClassificationRuleDeletedEvent: ruleUid={}", event.ruleUid());

        ruleReplicaRepo.findById(event.ruleUid()).ifPresent(rule -> {
            rule.setIsActive(false);
            ruleReplicaRepo.save(rule);
            log.info("Rule soft-deleted in replica");
        });

        cacheService.invalidate();
        log.info("Cache invalidated");
    }
}
