package com.loyalty.service_engine.infrastructure.event;

import com.loyalty.service_engine.application.dto.SeasonalRuleCreatedEvent;
import com.loyalty.service_engine.application.dto.SeasonalRuleDeletedEvent;
import com.loyalty.service_engine.application.dto.SeasonalRuleUpdatedEvent;
import com.loyalty.service_engine.domain.entity.SeasonalRuleEntity;
import com.loyalty.service_engine.domain.repository.SeasonalRuleRepository;
import com.loyalty.service_engine.infrastructure.cache.SeasonalRulesCacheManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

/**
 * RabbitMQ Consumer for Seasonal Rule events
 * 
 * Listens to three events from Service-Admin:
 * 1. SeasonalRuleCreated → Insert new rule + invalidate cache
 * 2. SeasonalRuleUpdated → Upsert (update if exists, insert if new) + invalidate cache
 * 3. SeasonalRuleDeleted → Soft delete (is_active=false) + invalidate cache
 * 
 * Implementation Pattern: UPSERT (INSERT OR UPDATE)
 * - Handles message retries idempotently
 * - At-least-once delivery + idempotent processing = safe system
 * - Manual ACK after successful save
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class SeasonalRuleEventConsumer {
    
    private final SeasonalRuleRepository seasonalRuleRepository;
    private final SeasonalRulesCacheManager cacheManager;
    
    /**
     * Consume SeasonalRuleCreated event
     * Insert a new rule or update if it already exists (idempotent)
     */
    @RabbitListener(queues = "${rabbitmq.queue.seasonal-rules:loyalty.seasonal.rules.queue}")
    @Transactional
    public void consumeSeasonalRuleCreated(
        @Payload SeasonalRuleCreatedEvent event,
        @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag
    ) {
        try {
            log.info("Consuming SeasonalRuleCreated event: ruleUid={} ecommerce={} eventTime={}",
                event.ruleUid(), event.ecommerceId(), event.timestamp());
            
            // UPSERT Pattern: find or create new entity
            var existing = seasonalRuleRepository.findByUidAndEcommerceId(event.ruleUid(), event.ecommerceId());
            SeasonalRuleEntity entity = existing.orElseGet(SeasonalRuleEntity::new);
            
            // Update all fields from event
            entity.setUid(event.ruleUid());
            entity.setEcommerceId(event.ecommerceId());
            entity.setName(event.name());
            entity.setDescription(event.description());
            entity.setDiscountPercentage(event.discountPercentage());
            entity.setDiscountType(event.discountType());
            entity.setStartDate(event.startDate());
            entity.setEndDate(event.endDate());
            entity.setIsActive(true);
            entity.setCreatedAt(existing.isEmpty() ? Instant.now() : entity.getCreatedAt());
            entity.setUpdatedAt(Instant.now());
            
            // Save (INSERT or UPDATE)
            seasonalRuleRepository.save(entity);
            
            // Invalidate cache immediately (don't wait for TTL)
            cacheManager.invalidate(event.ecommerceId());
            
            log.info("event=seasonal_rule_created_processed ruleUid={} ecommerce={} action={}",
                event.ruleUid(), event.ecommerceId(), existing.isEmpty() ? "INSERT" : "UPDATE");
            
        } catch (Exception ex) {
            log.error("event=seasonal_rule_created_failed ruleUid={} ecommerce={} error={}",
                event.ruleUid(), event.ecommerceId(), ex.getMessage(), ex);
            throw new RuntimeException(ex);  // NACK + requeue
        }
    }
    
    /**
     * Consume SeasonalRuleUpdated event
     * Update existing rule using UPSERT pattern (insert if doesn't exist)
     */
    @RabbitListener(queues = "${rabbitmq.queue.seasonal-rules:loyalty.seasonal.rules.queue}")
    @Transactional
    public void consumeSeasonalRuleUpdated(
        @Payload SeasonalRuleUpdatedEvent event,
        @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag
    ) {
        try {
            log.info("Consuming SeasonalRuleUpdated event: ruleUid={} ecommerce={} eventTime={}",
                event.ruleUid(), event.ecommerceId(), event.timestamp());
            
            // UPSERT Pattern: find or create new entity
            var existing = seasonalRuleRepository.findByUidAndEcommerceId(event.ruleUid(), event.ecommerceId());
            SeasonalRuleEntity entity = existing.orElseGet(SeasonalRuleEntity::new);
            
            // Update all fields from event
            entity.setUid(event.ruleUid());
            entity.setEcommerceId(event.ecommerceId());
            entity.setName(event.name());
            entity.setDescription(event.description());
            entity.setDiscountPercentage(event.discountPercentage());
            entity.setDiscountType(event.discountType());
            entity.setStartDate(event.startDate());
            entity.setEndDate(event.endDate());
            entity.setIsActive(true);
            entity.setCreatedAt(existing.isEmpty() ? Instant.now() : entity.getCreatedAt());
            entity.setUpdatedAt(Instant.now());
            
            // Save (INSERT or UPDATE)
            seasonalRuleRepository.save(entity);
            
            // Invalidate cache immediately
            cacheManager.invalidate(event.ecommerceId());
            
            log.info("event=seasonal_rule_updated_processed ruleUid={} ecommerce={} action={}",
                event.ruleUid(), event.ecommerceId(), existing.isEmpty() ? "INSERT" : "UPDATE");
            
        } catch (Exception ex) {
            log.error("event=seasonal_rule_updated_failed ruleUid={} ecommerce={} error={}",
                event.ruleUid(), event.ecommerceId(), ex.getMessage(), ex);
            throw new RuntimeException(ex);  // NACK + requeue
        }
    }
    
    /**
     * Consume SeasonalRuleDeleted event
     * Soft delete: mark rule as inactive
     */
    @RabbitListener(queues = "${rabbitmq.queue.seasonal-rules:loyalty.seasonal.rules.queue}")
    @Transactional
    public void consumeSeasonalRuleDeleted(
        @Payload SeasonalRuleDeletedEvent event,
        @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag
    ) {
        try {
            log.info("Consuming SeasonalRuleDeleted event: ruleUid={} ecommerce={} eventTime={}",
                event.ruleUid(), event.ecommerceId(), event.timestamp());
            
            // Find the rule
            var existing = seasonalRuleRepository.findByUidAndEcommerceId(event.ruleUid(), event.ecommerceId());
            
            if (existing.isPresent()) {
                SeasonalRuleEntity entity = existing.get();
                // Soft delete: mark as inactive
                entity.setIsActive(false);
                entity.setUpdatedAt(Instant.now());
                seasonalRuleRepository.save(entity);
                
                log.info("event=seasonal_rule_deleted_processed ruleUid={} ecommerce={}",
                    event.ruleUid(), event.ecommerceId());
            } else {
                // Rule doesn't exist - idempotent: treat as already deleted
                log.debug("Seasonal rule already deleted or never existed: ruleUid={} ecommerce={}",
                    event.ruleUid(), event.ecommerceId());
            }
            
            // Invalidate cache in any case
            cacheManager.invalidate(event.ecommerceId());
            
        } catch (Exception ex) {
            log.error("event=seasonal_rule_deleted_failed ruleUid={} ecommerce={} error={}",
                event.ruleUid(), event.ecommerceId(), ex.getMessage(), ex);
            throw new RuntimeException(ex);  // NACK + requeue
        }
    }
}
