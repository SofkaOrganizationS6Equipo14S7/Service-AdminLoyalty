package com.loyalty.service_engine.infrastructure.rabbitmq;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.loyalty.service_engine.domain.entity.DiscountConfigEntity;
import com.loyalty.service_engine.domain.repository.DiscountConfigRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Consumidor de eventos RabbitMQ para sincronizar BD réplica.
 * 
 * Cuando service-admin publica "DiscountConfigUpdated", este consumidor:
 * 1. Recibe el evento con nueva configuración
 * 2. PERSISTE cambios en BD réplica (loyalty_engine.discount_config)
 * 3. Invalida caché Caffeine para forzar reload
 * 4. Logs para debugging
 * 
 * Garantiza eventual consistency entre Admin (master) y Engine (replica).
 */
@Service
@Slf4j
public class DiscountConfigConsumer {
    
    private final DiscountConfigRepository discountConfigRepository;
    private final CacheManager cacheManager;
    private final ObjectMapper objectMapper;
    
    public DiscountConfigConsumer(
        DiscountConfigRepository discountConfigRepository,
        CacheManager cacheManager,
        ObjectMapper objectMapper
    ) {
        this.discountConfigRepository = discountConfigRepository;
        this.cacheManager = cacheManager;
        this.objectMapper = objectMapper;
    }
    
    /**
     * Escucha eventos de actualización de configuración de descuentos desde Admin.
     * Persiste cambios en BD réplica e invalida caché.
     * 
     * Flujo:
     * 1. Deserializa evento JSON
     * 2. Obtiene config anterior (si existe) y la marca como inactiva
     * 3. Persiste nueva config activa
     * 4. Invalida cachés para reload en próximo acceso
     * 
     * @param eventJson JSON del evento publicado por service-admin
     */
    @RabbitListener(queues = "discount.config.queue")
    @Transactional
    public void handleDiscountConfigUpdated(String eventJson) {
        try {
            DiscountConfigUpdatedEvent event = objectMapper.readValue(
                eventJson,
                DiscountConfigUpdatedEvent.class
            );
            
            log.info("Received DiscountConfigUpdated event from Admin. EcommerceId: {}, Limit: {} {}",
                event.ecommerceId, event.maxDiscountLimit, event.currencyCode);
            
            // Deactivate previous config if it exists
            discountConfigRepository.findByEcommerceIdAndIsActiveTrue(UUID.fromString(event.ecommerceId))
                .ifPresent(existingConfig -> {
                    existingConfig.setIsActive(false);
                    discountConfigRepository.save(existingConfig);
                    log.debug("Deactivated previous config for ecommerce: {}", event.ecommerceId);
                });
            
            // Create/persist new config in replica
            DiscountConfigEntity newConfig = new DiscountConfigEntity();
            newConfig.setUid(UUID.fromString(event.configId));
            newConfig.setEcommerceId(UUID.fromString(event.ecommerceId));
            newConfig.setMaxDiscountLimit(new BigDecimal(event.maxDiscountLimit));
            newConfig.setCurrencyCode(event.currencyCode);
            newConfig.setIsActive(event.isActive);
            newConfig.setCreatedAt(OffsetDateTime.now());
            newConfig.setUpdatedAt(OffsetDateTime.now());
            
            discountConfigRepository.save(newConfig);
            log.info("Persisted new config to replica DB. ConfigId: {}", event.configId);
            
            // Invalidate caches to force next acces to reload from replica
            invalidateCaches();
            
            log.info("Event processed successfully. Replica updated + caches invalidated");
        } catch (Exception e) {
            log.error("Failed to process DiscountConfigUpdated event. Event content: {}", eventJson, e);
            // Try to clear caches anyway as failsafe
            try {
                invalidateCaches();
            } catch (Exception cacheError) {
                log.error("Failed to clear caches during error recovery", cacheError);
            }
        }
    }
    
    /**
     * Invalida todos los cachés relacionados con descuentos para forzar reload desde BD.
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
     * DTO del evento recibido de RabbitMQ (publicado por service-admin).
     * Contiene toda la información de la nueva config.
     */
    public record DiscountConfigUpdatedEvent(
        String configId,              // UUID de la nueva config
        String ecommerceId,           // UUID del ecommerce (para multi-tenancy)
        String maxDiscountLimit,      // Moneda en String (ej "100.00")
        String currencyCode,          // ISO 4217 (ej "COP")
        Boolean isActive,             // Flag de activación
        Long timestamp                // Timestamp del evento
    ) {}
}
