package com.loyalty.service_admin.infrastructure.messaging;

import com.loyalty.service_admin.application.port.out.DiscountLimitPriorityEventPort;
import com.loyalty.service_admin.domain.entity.DiscountPriorityEntity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * DiscountLimitPriorityEventAdapter - Adapter de eventos para DiscountLimitPriorityService.
 *
 * Implementa DiscountLimitPriorityEventPort publicando eventos a RabbitMQ.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DiscountLimitPriorityEventAdapter implements DiscountLimitPriorityEventPort {

    private final RabbitTemplate rabbitTemplate;

    @Override
    public void publishPriorityUpdated(DiscountPriorityEntity priority, UUID ecommerceId) {
        try {
            Map<String, Object> eventData = new HashMap<>();
            eventData.put("priorityId", priority.getId());
            eventData.put("ecommerceId", ecommerceId);
            eventData.put("priorityName", priority.getName());
            eventData.put("level", priority.getLevel());
            eventData.put("eventType", "DISCOUNT_PRIORITY_UPDATED");
            eventData.put("timestamp", Instant.now());

            rabbitTemplate.convertAndSend("discount-exchange", "discount.config.updated", eventData);
            log.info("Published DISCOUNT_PRIORITY_UPDATED event: priorityId={}, ecommerceId={}", priority.getId(), ecommerceId);
        } catch (Exception ex) {
            log.error("Failed to publish DISCOUNT_PRIORITY_UPDATED event: priorityId={}, ecommerceId={}", priority.getId(), ecommerceId, ex);
        }
    }

    @Override
    public void publishPriorityDeleted(UUID priorityId, UUID ecommerceId) {
        try {
            Map<String, Object> eventData = new HashMap<>();
            eventData.put("priorityId", priorityId);
            eventData.put("ecommerceId", ecommerceId);
            eventData.put("eventType", "DISCOUNT_PRIORITY_DELETED");
            eventData.put("timestamp", Instant.now());

            rabbitTemplate.convertAndSend("discount-exchange", "discount.config.updated", eventData);
            log.info("Published DISCOUNT_PRIORITY_DELETED event: priorityId={}, ecommerceId={}", priorityId, ecommerceId);
        } catch (Exception ex) {
            log.error("Failed to publish DISCOUNT_PRIORITY_DELETED event: priorityId={}, ecommerceId={}", priorityId, ecommerceId, ex);
        }
    }
}
