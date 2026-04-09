package com.loyalty.service_admin.infrastructure.rabbitmq;

import com.loyalty.service_admin.application.dto.ecommerce.EcommerceStatusChangedEvent;
import com.loyalty.service_admin.application.dto.ecommerce.EcommerceCreatedEvent;
import com.loyalty.service_admin.domain.model.ecommerce.EcommerceStatus;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.UUID;

@Component
@Slf4j
public class EcommerceEventPublisher {
    
    private final RabbitTemplate rabbitTemplate;
    private final ObjectMapper objectMapper;
    
    @Value("${rabbitmq.exchange.events:loyalty.events}")
    private String exchangeName;
    
    public EcommerceEventPublisher(RabbitTemplate rabbitTemplate, ObjectMapper objectMapper) {
        this.rabbitTemplate = rabbitTemplate;
        this.objectMapper = objectMapper;
    }
    
    /**
     * Publica evento cuando se crea un nuevo ecommerce.
     * 
     * @param ecommerceId UUID del nuevo ecommerce
     * @param name nombre del ecommerce
     * @param slug slug único del ecommerce
     * @throws RuntimeException si falla la publicación (revierte transacción)
     */
    public void publishEcommerceCreated(UUID ecommerceId, String name, String slug) {
        log.info("Publicando evento de creación de ecommerce: ecommerceId={}, name={}, slug={}", 
                ecommerceId, name, slug);
        
        EcommerceCreatedEvent event = new EcommerceCreatedEvent(
                "ECOMMERCE_CREATED",
                ecommerceId.toString(),
                name,
                slug,
                "ACTIVE",
                Instant.now()
        );
        
        publishEvent(event);
        log.info("Evento de creación de ecommerce publicado exitosamente: ecommerceId={}", ecommerceId);
    }
     
    /**
     * @param ecommerceId    UUID del ecommerce
     * @param newStatus      nuevo estado (ACTIVE/INACTIVE)
     * @throws RuntimeException si falla la publicación (revierte transacción)
     */
    public void publishEcommerceStatusChanged(UUID ecommerceId, EcommerceStatus newStatus) {
        log.info("Publicando evento de cambio de status: ecommerceId={}, newStatus={}", 
                ecommerceId, newStatus);
        
        EcommerceStatusChangedEvent event = new EcommerceStatusChangedEvent(
                "ECOMMERCE_STATUS_CHANGED",
                ecommerceId.toString(),
                newStatus.name(),
                null,
                Instant.now()
        );
        
        publishEvent(event);
        log.info("Evento de cambio de status publicado exitosamente: ecommerceId={}, newStatus={}", 
                ecommerceId, newStatus);
    }
    
    /**
     * @param event payload del evento (puede ser EcommerceCreatedEvent, EcommerceStatusChangedEvent, etc.)
     * @throws RuntimeException si falla la serialización o el envío
     */
    private void publishEvent(Object event) {
        try {
            String payload = objectMapper.writeValueAsString(event);
            rabbitTemplate.convertAndSend(exchangeName, "", payload);
            log.debug("Evento serializado y enviado: payload={}", payload);
        } catch (Exception e) {
            log.error("Error publicando evento de ecommerce: event={}", event, e);
            throw new RuntimeException("Error publicando evento de ecommerce", e);
        }
    }
}
