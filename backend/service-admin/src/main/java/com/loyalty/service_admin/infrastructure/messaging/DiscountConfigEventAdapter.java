package com.loyalty.service_admin.infrastructure.messaging;

import com.loyalty.service_admin.application.port.out.DiscountConfigEventPort;
import com.loyalty.service_admin.domain.entity.DiscountSettingsEntity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * DiscountConfigEventAdapter - Adapter de eventos para DiscountConfigService.
 *
 * Implementa DiscountConfigEventPort publicando eventos a RabbitMQ.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DiscountConfigEventAdapter implements DiscountConfigEventPort {

    private final RabbitTemplate rabbitTemplate;

    @Override
    public void publishConfigUpdated(DiscountSettingsEntity config, UUID ecommerceId) {
        try {
            Map<String, Object> eventData = new HashMap<>();
            eventData.put("configId", config.getId());
            eventData.put("ecommerceId", ecommerceId);
            eventData.put("maxDiscountCap", config.getMaxDiscountCap());
            eventData.put("currencyCode", config.getCurrencyCode());
            eventData.put("allowStacking", config.getAllowStacking());
            eventData.put("roundingRule", config.getRoundingRule());
            eventData.put("eventType", "DISCOUNT_CONFIG_UPDATED");
            eventData.put("timestamp", Instant.now());

            rabbitTemplate.convertAndSend("discount-exchange", "discount.config.updated", eventData);
            log.info("Published DISCOUNT_CONFIG_UPDATED event: configId={}, ecommerceId={}", config.getId(), ecommerceId);
        } catch (Exception ex) {
            log.error("Failed to publish DISCOUNT_CONFIG_UPDATED event: configId={}, ecommerceId={}", config.getId(), ecommerceId, ex);
        }
    }

    @Override
    public void publishConfigDeleted(UUID configId, UUID ecommerceId) {
        try {
            Map<String, Object> eventData = new HashMap<>();
            eventData.put("configId", configId);
            eventData.put("ecommerceId", ecommerceId);
            eventData.put("eventType", "DISCOUNT_CONFIG_DELETED");
            eventData.put("timestamp", Instant.now());

            rabbitTemplate.convertAndSend("discount-exchange", "discount.config.updated", eventData);
            log.info("Published DISCOUNT_CONFIG_DELETED event: configId={}, ecommerceId={}", configId, ecommerceId);
        } catch (Exception ex) {
            log.error("Failed to publish DISCOUNT_CONFIG_DELETED event: configId={}, ecommerceId={}", configId, ecommerceId, ex);
        }
    }
}
