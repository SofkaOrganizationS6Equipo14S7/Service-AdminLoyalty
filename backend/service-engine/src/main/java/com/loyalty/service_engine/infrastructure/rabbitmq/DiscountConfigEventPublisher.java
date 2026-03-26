package com.loyalty.service_engine.infrastructure.rabbitmq;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * Publica eventos de cambios en configuración de descuentos.
 * 
 * Evento: DiscountConfigUpdated
 * Intercambiador: discount-exchange
 * Clave de ruta: discount.config.updated
 * 
 * Payload: {
 *   "configId": "uuid",
 *   "maxDiscountLimit": "12345.67",
 *   "currencyCode": "USD",
 *   "isActive": true,
 *   "timestamp": "2024-01-01T12:00:00Z"
 * }
 * 
 * Nota: Puede ser consumido por otros pods de service-engine para invalidar caché
 * o por otros servicios interesados en cambios de configuración.
 */
@Service
@Slf4j
public class DiscountConfigEventPublisher {
    
    private final RabbitTemplate rabbitTemplate;
    private final ObjectMapper objectMapper;
    
    public DiscountConfigEventPublisher(
        RabbitTemplate rabbitTemplate,
        ObjectMapper objectMapper
    ) {
        this.rabbitTemplate = rabbitTemplate;
        this.objectMapper = objectMapper;
    }
    
    /**
     * Publica evento de actualización de configuración de descuentos.
     * 
     * @param configId ID de la configuración actualizada
     * @param maxDiscountLimit Nuevo límite máximo
     * @param currencyCode Moneda del límite
     * @param isActive Si la configuración está activa
     */
    public void publishDiscountConfigUpdated(
        UUID configId,
        String maxDiscountLimit,
        String currencyCode,
        Boolean isActive
    ) {
        try {
            DiscountConfigUpdatedEvent event = new DiscountConfigUpdatedEvent(
                configId.toString(),
                maxDiscountLimit,
                currencyCode,
                isActive,
                System.currentTimeMillis()
            );
            
            String eventJson = objectMapper.writeValueAsString(event);
            
            // Publicar a exchange discount-exchange con routing key discount.config.updated
            rabbitTemplate.convertAndSend(
                "discount-exchange",
                "discount.config.updated",
                eventJson
            );
            
            log.info("Published DiscountConfigUpdated event. ConfigId: {}, MaxLimit: {}, Currency: {}",
                configId, maxDiscountLimit, currencyCode);
        } catch (Exception e) {
            log.error("Failed to publish DiscountConfigUpdated event. ConfigId: {}", configId, e);
            // No lanzar excepción - el cambio se persiste en BD de todas formas
            // RabbitMQ es para optimizar caché, no para funcionalidad crítica
        }
    }
    
    /**
     * DTO del evento de cambio de configuración.
     */
    public record DiscountConfigUpdatedEvent(
        String configId,
        String maxDiscountLimit,
        String currencyCode,
        Boolean isActive,
        Long timestamp
    ) {}
}
