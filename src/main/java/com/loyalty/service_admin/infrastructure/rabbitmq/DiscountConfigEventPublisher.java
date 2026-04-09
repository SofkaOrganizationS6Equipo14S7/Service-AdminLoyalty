package com.loyalty.service_admin.infrastructure.rabbitmq;

import com.loyalty.service_admin.domain.entity.DiscountSettingsEntity;
import com.loyalty.service_admin.domain.entity.DiscountPriorityEntity;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

/**
 * Publicador de eventos de cambios en configuración de descuentos.
 * Env ía eventos al Engine para mantener sincronización.
 */
@Component
@Slf4j
public class DiscountConfigEventPublisher {

    private static final String EXCHANGE = "discount-exchange";
    private static final String ROUTING_KEY_CONFIG = "discount.config.updated";
    private static final String ROUTING_KEY_PRIORITY = "discount.priority.updated";

    private final RabbitTemplate rabbitTemplate;

    public DiscountConfigEventPublisher(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    /**
     * Publica evento cuando se actualiza la configuración de límite.
     */
    public void publishDiscountConfigUpdated(DiscountSettingsEntity config) {
        DiscountConfigUpdatedEvent event = new DiscountConfigUpdatedEvent(
            config.getId().toString(),
            config.getEcommerceId().toString(),
            config.getMaxDiscountCap().toPlainString(),
            config.getCurrencyCode(),
            config.getIsActive(),
            config.getUpdatedAt().toString()
        );

        try {
            rabbitTemplate.convertAndSend(EXCHANGE, ROUTING_KEY_CONFIG, event);
            log.info("DiscountConfigUpdated event published for config: {}", config.getId());
        } catch (Exception e) {
            log.warn("Failed to publish DiscountConfigUpdated event: {}", e.getMessage());
            // No lanzar excepción, permitir fallback a lectura de DB
        }
    }

    /**
     * Publica evento cuando se actualizan las prioridades.
     */
    public void publishDiscountPriorityUpdated(UUID configId, List<DiscountPriorityEntity> priorities) {
        DiscountPriorityUpdatedEvent event = new DiscountPriorityUpdatedEvent(
            configId.toString(),
            priorities.size(),
            System.currentTimeMillis()
        );

        try {
            rabbitTemplate.convertAndSend(EXCHANGE, ROUTING_KEY_PRIORITY, event);
            log.info("DiscountPriorityUpdated event published for config: {}", configId);
        } catch (Exception e) {
            log.warn("Failed to publish DiscountPriorityUpdated event: {}", e.getMessage());
            // No lanzar excepción, permitir fallback a lectura de DB
        }
    }

    /**
     * DTO del evento de configuración actualizada.
     */
    public record DiscountConfigUpdatedEvent(
        String configUid,
        String ecommerceId,
        String maxDiscountLimit,
        String currencyCode,
        Boolean isActive,
        String timestamp
    ) {}

    /**
     * DTO del evento de prioridades actualizadas.
     */
    public record DiscountPriorityUpdatedEvent(
        String configId,
        Integer priorityCount,
        Long timestamp
    ) {}
}
