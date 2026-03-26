package com.loyalty.service_engine.infrastructure.rabbitmq;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;

/**
 * Consumidor de eventos RabbitMQ para invalidar caché Caffeine.
 * 
 * Cuando service-admin publica "DiscountConfigUpdated", este consumidor:
 * 1. Recibe el evento
 * 2. Invalida los cachés de discount_config y discount_priority
 * 3. Los siguientes accesos recargarán de BD con configuración actual
 * 
 * Colas:
 * - discount.config.queue → consumidor escucha actualizaciones de configuración
 */
@Service
@Slf4j
public class DiscountConfigConsumer {
    
    private final CacheManager cacheManager;
    private final ObjectMapper objectMapper;
    
    public DiscountConfigConsumer(
        CacheManager cacheManager,
        ObjectMapper objectMapper
    ) {
        this.cacheManager = cacheManager;
        this.objectMapper = objectMapper;
    }
    
    /**
     * Escucha eventos de actualización de configuración de descuentos.
     * Invalida caché para forzar re-fetch de BD en próximo acceso.
     * 
     * @param eventJson JSON del evento publicado por service-admin
     */
    @RabbitListener(queues = "discount.config.queue")
    public void handleDiscountConfigUpdated(String eventJson) {
        try {
            DiscountConfigUpdatedEvent event = objectMapper.readValue(
                eventJson,
                DiscountConfigUpdatedEvent.class
            );
            
            log.info("Received DiscountConfigUpdated event. ConfigId: {}, Timestamp: {}",
                event.configId, event.timestamp);
            
            // Invalidar cachés para forzar reload desde BD
            invalidateCaches();
            
            log.info("Discount caches invalidated. Next access will reload from database");
        } catch (Exception e) {
            log.error("Failed to process DiscountConfigUpdated event", e);
            // Invalidar de todas formas para ser seguro
            invalidateCaches();
        }
    }
    
    /**
     * Invalida todos los cachés relacionados con descuentos.
     */
    private void invalidateCaches() {
        try {
            var discountConfigCache = cacheManager.getCache("discount_config");
            var discountPriorityCache = cacheManager.getCache("discount_priority");
            
            if (discountConfigCache != null) {
                discountConfigCache.clear();
                log.debug("Cleared discount_config cache");
            }
            
            if (discountPriorityCache != null) {
                discountPriorityCache.clear();
                log.debug("Cleared discount_priority cache");
            }
        } catch (Exception e) {
            log.error("Error clearing caches", e);
        }
    }
    
    /**
     * DTO del evento recibido.
     */
    public record DiscountConfigUpdatedEvent(
        String configId,
        String maxDiscountLimit,
        String currencyCode,
        Boolean isActive,
        Long timestamp
    ) {}
}
