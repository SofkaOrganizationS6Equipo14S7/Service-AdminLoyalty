package com.loyalty.service_admin.infrastructure.rabbitmq;

import com.loyalty.service_admin.application.dto.ApiKeyEventPayload;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
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
    
    public ApiKeyEventPublisher(RabbitTemplate rabbitTemplate, ObjectMapper objectMapper) {
        this.rabbitTemplate = rabbitTemplate;
        this.objectMapper = objectMapper;
    }
    
    /**
     * Publica evento de creación de API Key.
     */
    public void publishApiKeyCreated(ApiKeyEventPayload event) {
        publishEvent(event);
        log.info("API Key CREATED event published: keyId={}, ecommerceId={}", 
            event.keyId(), event.ecommerceId());
    }
    
    /**
     * Publica evento de eliminación de API Key.
     */
    public void publishApiKeyDeleted(ApiKeyEventPayload event) {
        publishEvent(event);
        log.info("API Key DELETED event published: keyId={}, ecommerceId={}", 
            event.keyId(), event.ecommerceId());
    }
    
    /**
     * Publica un evento en el exchange de configuración.
     */
    private void publishEvent(ApiKeyEventPayload event) {
        try {
            String payload = objectMapper.writeValueAsString(event);
            rabbitTemplate.convertAndSend(exchangeName, "", payload);
        } catch (Exception e) {
            log.error("Error publishing API Key event", e);
            throw new RuntimeException("Error publishing API Key event", e);
        }
    }
}
