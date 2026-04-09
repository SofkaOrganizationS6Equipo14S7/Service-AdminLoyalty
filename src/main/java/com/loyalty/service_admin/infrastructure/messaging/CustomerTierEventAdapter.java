package com.loyalty.service_admin.infrastructure.messaging;

import com.loyalty.service_admin.application.port.out.CustomerTierEventPort;
import com.loyalty.service_admin.domain.entity.CustomerTierEntity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * CustomerTierEventAdapter - Adapter de eventos para CustomerTierService.
 *
 * Implementa CustomerTierEventPort publicando eventos a RabbitMQ.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class CustomerTierEventAdapter implements CustomerTierEventPort {

    private final RabbitTemplate rabbitTemplate;

    @Override
    public void publishTierCreated(CustomerTierEntity tier, UUID ecommerceId) {
        publishTierEvent("TIER_CREATED", tier, ecommerceId);
    }

    @Override
    public void publishTierUpdated(CustomerTierEntity tier, UUID ecommerceId) {
        publishTierEvent("TIER_UPDATED", tier, ecommerceId);
    }

    @Override
    public void publishTierDeleted(UUID tierId, UUID ecommerceId) {
        try {
            Map<String, Object> eventData = new HashMap<>();
            eventData.put("tierId", tierId);
            eventData.put("ecommerceId", ecommerceId);
            eventData.put("eventType", "TIER_DELETED");
            eventData.put("timestamp", Instant.now());

            rabbitTemplate.convertAndSend("loyalty.events", "tier.updated", eventData);
            log.info("Published TIER_DELETED event: tierId={}, ecommerceId={}", tierId, ecommerceId);
        } catch (Exception ex) {
            log.error("Failed to publish TIER_DELETED event: tierId={}, ecommerceId={}", tierId, ecommerceId, ex);
        }
    }

    @Override
    public void publishTierActivated(CustomerTierEntity tier, UUID ecommerceId) {
        publishTierEvent("TIER_ACTIVATED", tier, ecommerceId);
    }

    @Override
    public void publishTierDeactivated(CustomerTierEntity tier, UUID ecommerceId) {
        publishTierEvent("TIER_DEACTIVATED", tier, ecommerceId);
    }

    private void publishTierEvent(String eventType, CustomerTierEntity tier, UUID ecommerceId) {
        try {
            Map<String, Object> eventData = new HashMap<>();
            eventData.put("tierId", tier.getId());
            eventData.put("ecommerceId", ecommerceId);
            eventData.put("tierName", tier.getName());
            eventData.put("hierarchyLevel", tier.getHierarchyLevel());
            eventData.put("isActive", tier.getIsActive());
            eventData.put("eventType", eventType);
            eventData.put("timestamp", Instant.now());

            rabbitTemplate.convertAndSend("loyalty.events", "tier.updated", eventData);
            log.info("Published {} event: tierId={}, ecommerceId={}", eventType, tier.getId(), ecommerceId);
        } catch (Exception ex) {
            log.error("Failed to publish {} event: tierId={}, ecommerceId={}", eventType, tier.getId(), ecommerceId, ex);
        }
    }
}
