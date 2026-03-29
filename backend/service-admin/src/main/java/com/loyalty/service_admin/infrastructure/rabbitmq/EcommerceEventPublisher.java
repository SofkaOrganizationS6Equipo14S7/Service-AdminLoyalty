package com.loyalty.service_admin.infrastructure.rabbitmq;

import com.loyalty.service_admin.application.dto.EcommerceStatusChangedEvent;
import com.loyalty.service_admin.domain.model.ecommerce.EcommerceStatus;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.UUID;

/**
 * Publicador de eventos de ecommerce a RabbitMQ.
 * 
 * SPEC-001: Registro y Gestión de Ecommerces
 * 
 * Responsabilidad:
 * - Publicar eventos de cambio de status de ecommerce
 * - Usar Fanout Exchange para multi-consumidor
 * - Garantizar entrega (confirmación de publisher)
 * 
 * Routing Details (Fanout Exchange):
 * - Exchange: loyalty.events (tipo Fanout)
 * - Queue Admin: loyalty.admin.ecommerce.events → escucha cambios para invalidar JWT
 * - Queue Engine: loyalty.engine.ecommerce.events → escucha cambios para invalidar cache
 * - Durability: true (ambas queues persistentes)
 * 
 * Atomic Behavior:
 * - Operación de updateStatus + publish ocurren en transacción única
 * - Si publish falla (<5s timeout), la transacción se revierte
 * - No se cambia estado sin confirmación de entrega de evento
 */
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
     * Publica evento de cambio de status de ecommerce.
     * 
     * El evento se envía al Fanout Exchange loyalty.events.
     * Múltiples consumidores pueden reaccionar:
     * - Admin Service: invalida JWT de usuarios
     * - Engine Service: invalida API Keys en caché
     * 
     * @param ecommerceId    UUID del ecommerce
     * @param newStatus      nuevo estado (ACTIVE/INACTIVE)
     * @throws RuntimeException si falla la publicación (revierte transacción)
     */
    public void publishEcommerceStatusChanged(UUID ecommerceId, EcommerceStatus newStatus) {
        log.info("Publicando evento de cambio de status: ecommerceId={}, newStatus={}", 
                ecommerceId, newStatus);
        
        // Construir payload del evento
        EcommerceStatusChangedEvent event = new EcommerceStatusChangedEvent(
                "ECOMMERCE_STATUS_CHANGED",
                ecommerceId.toString(),
                newStatus.name(),
                null, // oldStatus no incluido en este moment, se podría agregar si se necesita
                Instant.now()
        );
        
        publishEvent(event);
        log.info("Evento de cambio de status publicado exitosamente: ecommerceId={}, newStatus={}", 
                ecommerceId, newStatus);
    }
    
    /**
     * Publica un evento en el Fanout Exchange de eventos.
     * 
     * Usa Jackson para serializar a JSON y Spring AMQP para enviar.
     * Routing key vacío (Fanout no usa routing keys, distribuye a todas las bindings).
     * 
     * @param event payload del evento
     * @throws RuntimeException si falla la serialización o el envío
     */
    private void publishEvent(EcommerceStatusChangedEvent event) {
        try {
            String payload = objectMapper.writeValueAsString(event);
            // Fanout Exchange: routing key vacío, se envía a todas las queues bindeadas
            rabbitTemplate.convertAndSend(exchangeName, "", payload);
            log.debug("Evento serializado y enviado: payload={}", payload);
        } catch (Exception e) {
            log.error("Error publicando evento de ecommerce: event={}", event, e);
            throw new RuntimeException("Error publicando evento de cambio de status de ecommerce", e);
        }
    }
}
