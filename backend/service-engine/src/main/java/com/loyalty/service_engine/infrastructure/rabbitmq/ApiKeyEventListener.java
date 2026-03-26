package com.loyalty.service_engine.infrastructure.rabbitmq;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.loyalty.service_engine.application.dto.ApiKeyEventPayload;
import com.loyalty.service_engine.infrastructure.cache.ApiKeyCache;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * Listener para eventos de API Key desde Admin Service.
 * Consume desde queue "engine-api-keys-queue" vinculada al exchange "loyalty.config.exchange".
 */
@Component
@Slf4j
public class ApiKeyEventListener {
    
    private final ApiKeyCache apiKeyCache;
    private final ObjectMapper objectMapper;
    
    public ApiKeyEventListener(ApiKeyCache apiKeyCache, ObjectMapper objectMapper) {
        this.apiKeyCache = apiKeyCache;
        this.objectMapper = objectMapper;
    }
    
    /**
     * Consume eventos de API Key del queue de Engine.
     * @param payload String JSON con el evento
     */
    @RabbitListener(queues = "${rabbitmq.queue.api-keys:engine-api-keys-queue}")
    public void onApiKeyEvent(String payload) {
        try {
            log.debug("Received API Key event: {}", payload);
            
            // Parsear JSON a DTO
            ApiKeyEventPayload event = objectMapper.readValue(payload, ApiKeyEventPayload.class);
            
            // Procesar según tipo de evento
            switch (event.eventType()) {
                case "API_KEY_CREATED":
                    handleApiKeyCreated(event);
                    break;
                case "API_KEY_DELETED":
                    handleApiKeyDeleted(event);
                    break;
                default:
                    log.warn("Unknown API Key event type: {}", event.eventType());
            }
        } catch (Exception e) {
            log.error("Error processing API Key event", e);
            // No relanzar excepción — permitir que el listener continúe funcionando
        }
    }
    
    /**
     * Procesa evento de creación de API Key.
     */
    private void handleApiKeyCreated(ApiKeyEventPayload event) {
        try {
            apiKeyCache.addKey(event.keyString(), event.ecommerceId());
            log.info("API Key created in cache: ecommerceId={}", event.ecommerceId());
        } catch (Exception e) {
            log.error("Error handling API_KEY_CREATED event", e);
        }
    }
    
    /**
     * Procesa evento de eliminación de API Key.
     */
    private void handleApiKeyDeleted(ApiKeyEventPayload event) {
        try {
            apiKeyCache.removeKey(event.keyString());
            log.info("API Key removed from cache: ecommerceId={}", event.ecommerceId());
        } catch (Exception e) {
            log.error("Error handling API_KEY_DELETED event", e);
        }
    }
}
