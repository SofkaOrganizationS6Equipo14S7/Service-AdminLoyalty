package com.loyalty.service_admin.infrastructure.rabbitmq;

import com.loyalty.service_admin.application.dto.FidelityRangeCreatedEvent;
import com.loyalty.service_admin.application.dto.FidelityRangeDeletedEvent;
import com.loyalty.service_admin.application.dto.FidelityRangeUpdatedEvent;
import com.loyalty.service_admin.domain.entity.FidelityRangeEntity;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Instant;

/**
 * Event Publisher for Fidelity Range events
 * Publishes to RabbitMQ for Engine Service consumption
 */
@Component
@Slf4j
public class FidelityRangeEventPublisher {
    
    private final RabbitTemplate rabbitTemplate;
    private final ObjectMapper objectMapper;
    
    @Value("${rabbitmq.exchange.fidelity-ranges:fidelity-ranges.exchange}")
    private String exchangeName;

    @Value("${rabbitmq.routing.fidelity-ranges:fidelity.ranges.*}")
    private String routingKey;

    // Dead Letter Exchange for failed messages
    @Value("${rabbitmq.exchange.fidelity-ranges-dlx:fidelity-ranges.dlx}")
    private String deadLetterExchange;

    @Value("${rabbitmq.routing.fidelity-ranges-dlq:fidelity.ranges.dlq}")
    private String deadLetterRoutingKey;
    
    public FidelityRangeEventPublisher(RabbitTemplate rabbitTemplate, ObjectMapper objectMapper) {
        this.rabbitTemplate = rabbitTemplate;
        this.objectMapper = objectMapper;
    }
    
    /**
     * Publish when a fidelity range is created
     */
    public void publishFidelityRangeCreated(FidelityRangeEntity entity) {
        FidelityRangeCreatedEvent event = new FidelityRangeCreatedEvent(
            "FIDELITY_RANGE_CREATED",
            entity.getUid(),
            entity.getEcommerceId(),
            entity.getName(),
            entity.getMinPoints(),
            entity.getMaxPoints(),
            entity.getDiscountPercentage(),
            Instant.now()
        );
        publishEvent(event, "fidelity.range.created");
        log.info("Fidelity range created event published: uid={} ecommerce={} name={}", 
            entity.getUid(), entity.getEcommerceId(), entity.getName());
    }
    
    /**
     * Publish when a fidelity range is updated
     */
    public void publishFidelityRangeUpdated(FidelityRangeEntity entity) {
        FidelityRangeUpdatedEvent event = new FidelityRangeUpdatedEvent(
            "FIDELITY_RANGE_UPDATED",
            entity.getUid(),
            entity.getEcommerceId(),
            "Fidelity range updated",
            Instant.now()
        );
        publishEvent(event, "fidelity.range.updated");
        log.info("Fidelity range updated event published: uid={} ecommerce={}", 
            entity.getUid(), entity.getEcommerceId());
    }
    
    /**
     * Publish when a fidelity range is deleted (soft-delete)
     */
    public void publishFidelityRangeDeleted(FidelityRangeEntity entity) {
        FidelityRangeDeletedEvent event = new FidelityRangeDeletedEvent(
            "FIDELITY_RANGE_DELETED",
            entity.getUid(),
            entity.getEcommerceId(),
            Instant.now()
        );
        publishEvent(event, "fidelity.range.deleted");
        log.info("Fidelity range deleted event published: uid={} ecommerce={}", 
            entity.getUid(), entity.getEcommerceId());
    }
    
    /**
     * Generic event publisher with error handling
     */
    private void publishEvent(Object event, String specificRoutingKey) {
        String eventId = event.getClass().getSimpleName() + ":" + Instant.now().toEpochMilli();
        try {
            String payload = objectMapper.writeValueAsString(event);
            rabbitTemplate.convertAndSend(exchangeName, specificRoutingKey, payload);
            log.debug("Event published to RabbitMQ: eventId={} exchange={} routingKey={}", 
                eventId, exchangeName, specificRoutingKey);
        } catch (Exception e) {
            log.error("Failed to publish event to RabbitMQ: eventId={} exception={}", eventId, e.getMessage(), e);
            // Send to DLX (Dead Letter Exchange)
            try {
                String payload = objectMapper.writeValueAsString(event);
                rabbitTemplate.convertAndSend(deadLetterExchange, deadLetterRoutingKey, payload);
                log.warn("Event sent to DLX: eventId={} dlxExchange={}", eventId, deadLetterExchange);
            } catch (Exception dlxException) {
                log.error("Failed to send event to DLX: eventId={} exception={}", eventId, dlxException.getMessage(), dlxException);
            }
        }
    }
}
