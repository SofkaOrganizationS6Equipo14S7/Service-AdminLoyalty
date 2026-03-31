package com.loyalty.service_admin.infrastructure.rabbitmq;

import com.loyalty.service_admin.application.dto.ClassificationRuleCreatedEvent;
import com.loyalty.service_admin.application.dto.ClassificationRuleDeletedEvent;
import com.loyalty.service_admin.application.dto.ClassificationRuleUpdatedEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Event Publisher for Classification Rule events.
 * Publishes to RabbitMQ for Engine Service consumption.
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class ClassificationEventPublisher {

    private final RabbitTemplate rabbitTemplate;
    private final ObjectMapper objectMapper;

    @Value("${rabbitmq.exchange.classification:classification.exchange}")
    private String exchangeName;

    public void publishRuleCreated(ClassificationRuleCreatedEvent event) {
        publish(event, "classification.rule.created");
    }

    public void publishRuleUpdated(ClassificationRuleUpdatedEvent event) {
        publish(event, "classification.rule.updated");
    }

    public void publishRuleDeleted(ClassificationRuleDeletedEvent event) {
        publish(event, "classification.rule.deleted");
    }

    private void publish(Object event, String routingKey) {
        try {
            String json = objectMapper.writeValueAsString(event);
            rabbitTemplate.convertAndSend(exchangeName, routingKey, json);
            log.info("Published event: {} to {}", event.getClass().getSimpleName(), exchangeName);
        } catch (Exception e) {
            log.error("Failed to publish event: {}", event.getClass().getSimpleName(), e);
            throw new RuntimeException("Event publishing failed", e);
        }
    }
}
