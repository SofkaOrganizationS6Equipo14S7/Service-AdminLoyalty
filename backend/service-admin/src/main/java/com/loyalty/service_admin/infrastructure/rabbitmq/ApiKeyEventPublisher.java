package com.loyalty.service_admin.infrastructure.rabbitmq;

import com.loyalty.service_admin.application.dto.apikey.ApiKeyEventPayload;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class ApiKeyEventPublisher {
    
    private final RabbitTemplate rabbitTemplate;
    private final ObjectMapper objectMapper;
    
    @Value("${rabbitmq.exchange.config:loyalty.config.exchange}")
    private String exchangeName;

    @Value("${rabbitmq.routing.api-keys:}")
    private String routingKey;

    @Value("${rabbitmq.exchange.config-dlx:loyalty.config.dlx}")
    private String deadLetterExchange;

    @Value("${rabbitmq.routing.api-keys-dlq:api.keys.dlq}")
    private String deadLetterRoutingKey;
    
    public ApiKeyEventPublisher(RabbitTemplate rabbitTemplate, ObjectMapper objectMapper) {
        this.rabbitTemplate = rabbitTemplate;
        this.objectMapper = objectMapper;
    }
    
    /**
     * Publica evento de creación de API Key.
     */
    public void publishApiKeyCreated(ApiKeyEventPayload event) {
        publishEvent(event);
        log.info("event=api_key_created_published keyId={} ecommerceId={}",
            event.keyId(), event.ecommerceId());
    }
    
    /**
     * Publica evento de eliminación de API Key.
     */
    public void publishApiKeyDeleted(ApiKeyEventPayload event) {
        publishEvent(event);
        log.info("event=api_key_deleted_published keyId={} ecommerceId={}",
            event.keyId(), event.ecommerceId());
    }
    
    /**
     * Publica un evento en el exchange de configuración.
     */
    private void publishEvent(ApiKeyEventPayload event) {
        String eventId = event.eventType() + ":" + event.keyId() + ":" + event.timestamp();
        try {
            String payload = objectMapper.writeValueAsString(event);
            rabbitTemplate.convertAndSend(exchangeName, routingKey, payload, message -> withHeaders(message, event, eventId));
            log.info("event=api_key_event_published eventId={} type={} keyId={} ecommerceId={} exchange={} routingKey={}",
                    eventId, event.eventType(), event.keyId(), event.ecommerceId(), exchangeName, routingKey);
        } catch (Exception e) {
            log.error("event=api_key_event_publish_failed eventId={} type={} keyId={} ecommerceId={}",
                    eventId, event.eventType(), event.keyId(), event.ecommerceId(), e);
            rabbitTemplate.convertAndSend(deadLetterExchange, deadLetterRoutingKey, event,
                    message -> withHeaders(message, event, eventId));
        }
    }

    private Message withHeaders(Message message, ApiKeyEventPayload event, String eventId) {
        message.getMessageProperties().setHeader("x-event-id", eventId);
        message.getMessageProperties().setHeader("x-event-type", event.eventType());
        message.getMessageProperties().setHeader("x-key-id", event.keyId());
        message.getMessageProperties().setHeader("x-ecommerce-id", event.ecommerceId());
        return message;
    }
}
