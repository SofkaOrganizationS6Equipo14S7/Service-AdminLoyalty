package com.loyalty.service_admin.infrastructure.event;

import com.loyalty.service_admin.application.dto.SeasonalRuleCreatedEvent;
import com.loyalty.service_admin.application.dto.SeasonalRuleDeletedEvent;
import com.loyalty.service_admin.application.dto.SeasonalRuleUpdatedEvent;
import com.loyalty.service_admin.domain.entity.SeasonalRuleEntity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Instant;

/**
 * Publisher for Seasonal Rule events to RabbitMQ
 * 
 * Publishes:
 * - SeasonalRuleCreated: when a new rule is created
 * - SeasonalRuleUpdated: when a rule is updated
 * - SeasonalRuleDeleted: when a rule is deleted (soft delete)
 * 
 * Events are consumed by Service-Engine to:
 * - Invalidate Caffeine cache
 * - Replicate data to Engine's database
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class SeasonalRuleEventPublisher {
    
    private final RabbitTemplate rabbitTemplate;
    
    @Value("${rabbitmq.exchange.seasonal:loyalty.seasonal.exchange}")
    private String exchange;
    
    @Value("${rabbitmq.routing.seasonal-created:seasonal.rule.created}")
    private String routingKeyCreated;
    
    @Value("${rabbitmq.routing.seasonal-updated:seasonal.rule.updated}")
    private String routingKeyUpdated;
    
    @Value("${rabbitmq.routing.seasonal-deleted:seasonal.rule.deleted}")
    private String routingKeyDeleted;
    
    @Value("${rabbitmq.exchange.seasonal-dlx:loyalty.seasonal.dlx}")
    private String deadLetterExchange;
    
    @Value("${rabbitmq.routing.seasonal-dlq:seasonal.rule.dlq}")
    private String deadLetterRoutingKey;
    
    /**
     * Publish SeasonalRuleCreated event
     */
    public void publishSeasonalRuleCreated(SeasonalRuleEntity entity) {
        SeasonalRuleCreatedEvent event = new SeasonalRuleCreatedEvent(
            "SEASONAL_RULE_CREATED",
            entity.getUid(),
            entity.getEcommerceId(),
            entity.getName(),
            entity.getDescription(),
            entity.getDiscountPercentage(),
            entity.getDiscountType(),
            entity.getStartDate(),
            entity.getEndDate(),
            Instant.now()
        );
        
        String eventId = entity.getUid() + ":" + System.currentTimeMillis();
        
        try {
            rabbitTemplate.convertAndSend(exchange, routingKeyCreated, event,
                message -> withStandardHeaders(message, event, eventId));
            
            log.info("event=seasonal_rule_created_published eventId={} ruleUid={} ecommerceId={} exchange={} routingKey={}",
                eventId, entity.getUid(), entity.getEcommerceId(), exchange, routingKeyCreated);
        } catch (Exception ex) {
            log.error("event=seasonal_rule_created_publish_failed eventId={} ruleUid={} ecommerceId={}",
                eventId, entity.getUid(), entity.getEcommerceId(), ex);
            publishToDeadLetter(event, eventId);
        }
    }
    
    /**
     * Publish SeasonalRuleUpdated event
     */
    public void publishSeasonalRuleUpdated(SeasonalRuleEntity entity) {
        SeasonalRuleUpdatedEvent event = new SeasonalRuleUpdatedEvent(
            "SEASONAL_RULE_UPDATED",
            entity.getUid(),
            entity.getEcommerceId(),
            entity.getName(),
            entity.getDescription(),
            entity.getDiscountPercentage(),
            entity.getDiscountType(),
            entity.getStartDate(),
            entity.getEndDate(),
            Instant.now()
        );
        
        String eventId = entity.getUid() + ":" + System.currentTimeMillis();
        
        try {
            rabbitTemplate.convertAndSend(exchange, routingKeyUpdated, event,
                message -> withStandardHeaders(message, event, eventId));
            
            log.info("event=seasonal_rule_updated_published eventId={} ruleUid={} ecommerceId={} exchange={} routingKey={}",
                eventId, entity.getUid(), entity.getEcommerceId(), exchange, routingKeyUpdated);
        } catch (Exception ex) {
            log.error("event=seasonal_rule_updated_publish_failed eventId={} ruleUid={} ecommerceId={}",
                eventId, entity.getUid(), entity.getEcommerceId(), ex);
            publishToDeadLetter(event, eventId);
        }
    }
    
    /**
     * Publish SeasonalRuleDeleted event
     */
    public void publishSeasonalRuleDeleted(SeasonalRuleEntity entity) {
        SeasonalRuleDeletedEvent event = new SeasonalRuleDeletedEvent(
            "SEASONAL_RULE_DELETED",
            entity.getUid(),
            entity.getEcommerceId(),
            Instant.now()
        );
        
        String eventId = entity.getUid() + ":" + System.currentTimeMillis();
        
        try {
            rabbitTemplate.convertAndSend(exchange, routingKeyDeleted, event,
                message -> withStandardHeaders(message, event, eventId));
            
            log.info("event=seasonal_rule_deleted_published eventId={} ruleUid={} ecommerceId={} exchange={} routingKey={}",
                eventId, entity.getUid(), entity.getEcommerceId(), exchange, routingKeyDeleted);
        } catch (Exception ex) {
            log.error("event=seasonal_rule_deleted_publish_failed eventId={} ruleUid={} ecommerceId={}",
                eventId, entity.getUid(), entity.getEcommerceId(), ex);
            publishToDeadLetter(event, eventId);
        }
    }
    
    /**
     * Add standard headers to RabbitMQ message
     */
    private <T> Message withStandardHeaders(Message message, T event, String eventId) {
        message.getMessageProperties().setHeader("x-event-id", eventId);
        String eventType;
        if (event instanceof SeasonalRuleCreatedEvent) {
            eventType = "SEASONAL_RULE_CREATED";
        } else if (event instanceof SeasonalRuleUpdatedEvent) {
            eventType = "SEASONAL_RULE_UPDATED";
        } else if (event instanceof SeasonalRuleDeletedEvent) {
            eventType = "SEASONAL_RULE_DELETED";
        } else {
            eventType = "UNKNOWN";
        }
        message.getMessageProperties().setHeader("x-event-type", eventType);
        return message;
    }
    
    /**
     * Publish to Dead Letter Exchange for retry
     */
    private void publishToDeadLetter(Object event, String eventId) {
        try {
            rabbitTemplate.convertAndSend(deadLetterExchange, deadLetterRoutingKey, event);
            log.debug("event=seasonal_rule_dlq_published eventId={}", eventId);
        } catch (Exception ex) {
            log.error("event=seasonal_rule_dlq_publish_failed eventId={}", eventId, ex);
        }
    }
}
