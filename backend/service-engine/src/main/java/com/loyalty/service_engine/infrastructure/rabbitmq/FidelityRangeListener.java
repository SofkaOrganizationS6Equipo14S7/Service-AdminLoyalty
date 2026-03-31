package com.loyalty.service_engine.infrastructure.rabbitmq;

import com.loyalty.service_engine.application.dto.FidelityRangeDTO;
import com.loyalty.service_engine.application.dto.events.FidelityRangeCreatedEvent;
import com.loyalty.service_engine.application.dto.events.FidelityRangeDeletedEvent;
import com.loyalty.service_engine.application.dto.events.FidelityRangeUpdatedEvent;
import com.loyalty.service_engine.infrastructure.cache.FidelityRangeCache;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * Consumes fidelity range events published by Admin Service.
 * Updates the in-memory Caffeine cache to keep Engine's replica in sync.
 *
 * Event flow:
 * Admin Service (source of truth) → RabbitMQ → Engine Service (listener) → Caffeine Cache
 *
 * Three paths:
 * 1. FidelityRangeCreated: Add new range to cache
 * 2. FidelityRangeUpdated: Replace range in cache
 * 3. FidelityRangeDeleted: Remove range from cache (soft-delete)
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class FidelityRangeListener {
    private final FidelityRangeCache cache;

    /**
     * Listens for FidelityRangeCreated events.
     * Adds the new range to cache immediately.
     */
    @RabbitListener(queues = "fidelity.ranges.queue")
    public void handleFidelityRangeCreated(FidelityRangeCreatedEvent event) {
        log.info("Received FidelityRangeCreated event: uid={}, level={}, range=[{}-{}]",
            event.uid(), event.name(), event.minPoints(), event.maxPoints());

        FidelityRangeDTO range = new FidelityRangeDTO(
            event.uid(),
            event.ecommerceId(),
            event.name(),
            event.minPoints(),
            event.maxPoints(),
            event.discountPercentage(),
            true, // is_active
            event.timestamp(),
            event.timestamp()
        );

        try {
            cache.addOrUpdate(range);
            log.debug("Successfully cached FidelityRangeCreated: uid={}", event.uid());
        } catch (Exception e) {
            log.error("Failed to cache FidelityRangeCreated event: uid={}", event.uid(), e);
            // Error is logged but not re-thrown; listener should be resilient
            // Dead Letter Exchange (DLX) in Admin Service handles retries
        }
    }

    /**
     * Listens for FidelityRangeUpdated events.
     * Replaces the range in cache with updated data.
     */
    @RabbitListener(queues = "fidelity.ranges.queue")
    public void handleFidelityRangeUpdated(FidelityRangeUpdatedEvent event) {
        log.info("Received FidelityRangeUpdated event: uid={}, level={}, range=[{}-{}]",
            event.uid(), event.name(), event.minPoints(), event.maxPoints());

        FidelityRangeDTO range = new FidelityRangeDTO(
            event.uid(),
            event.ecommerceId(),
            event.name(),
            event.minPoints(),
            event.maxPoints(),
            event.discountPercentage(),
            true, // is_active
            event.timestamp(),
            event.timestamp()
        );

        try {
            cache.addOrUpdate(range);
            log.debug("Successfully cached FidelityRangeUpdated: uid={}", event.uid());
        } catch (Exception e) {
            log.error("Failed to cache FidelityRangeUpdated event: uid={}", event.uid(), e);
        }
    }

    /**
     * Listens for FidelityRangeDeleted events.
     * Removes the range from cache (soft-delete).
     */
    @RabbitListener(queues = "fidelity.ranges.queue")
    public void handleFidelityRangeDeleted(FidelityRangeDeletedEvent event) {
        log.info("Received FidelityRangeDeleted event: uid={}", event.uid());

        try {
            cache.remove(event.uid(), event.ecommerceId());
            log.debug("Successfully removed FidelityRange from cache: uid={}", event.uid());
        } catch (Exception e) {
            log.error("Failed to process FidelityRangeDeleted event: uid={}", event.uid(), e);
        }
    }
}
