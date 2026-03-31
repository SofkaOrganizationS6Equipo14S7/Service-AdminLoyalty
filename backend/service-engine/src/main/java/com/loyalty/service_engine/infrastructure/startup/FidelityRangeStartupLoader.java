package com.loyalty.service_engine.infrastructure.startup;

import com.loyalty.service_engine.application.dto.FidelityRangeDTO;
import com.loyalty.service_engine.infrastructure.cache.FidelityRangeCache;
import com.loyalty.service_engine.infrastructure.persistence.FidelityRangeJpaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Loads fidelity ranges from Engine's local replica table into Caffeine cache at startup.
 *
 * Purpose: Cold Start Autonomy
 * - Engine Service is autonomous and does NOT depend on Admin Service for initial data
 * - At startup, populates cache from local PostgreSQL replica table (V14 migration)
 * - Cache remains valid even if Admin Service is offline
 * - RabbitMQ listener keeps cache in sync when Admin publishes events
 *
 * Lifecycle:
 * 1. ApplicationReadyEvent fires (after Spring beans are initialized)
 * 2. Fetch all active ranges from fidelity_ranges table
 * 3. Load into FidelityRangeCache
 * 4. Log statistics and confirm readiness
 *
 * Thread-safety: Uses FidelityRangeCache.loadAll() which is synchronized
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class FidelityRangeStartupLoader {
    private final FidelityRangeCache cache;
    private final FidelityRangeJpaRepository repository;

    /**
     * Triggered after Spring context is fully initialized and ready.
     * Pre-loads all active fidelity ranges into memory.
     */
    @EventListener(ApplicationReadyEvent.class)
    public void loadFidelityRangesAtStartup() {
        log.info("Starting fidelity ranges cold start loader...");

        try {
            // Fetch all active ranges from Engine's local replica table
            List<FidelityRangeDTO> ranges = repository.findAllActiveRangesSorted();

            if (ranges.isEmpty()) {
                log.warn("No fidelity ranges found in database. Cache is empty. " +
                    "When Admin Service publishes events, cache will be populated.");
            } else {
                // Load into cache
                cache.loadAll(ranges);
                log.info("Successfully pre-loaded {} fidelity ranges into cache " +
                    "from Engine's local replica table", ranges.size());
                log.debug("Cache stats: {}", cache.getStats());
            }

            log.info("FidelityRangeStartupLoader completed successfully. " +
                "Engine is autonomous and ready to classify clients.");

        } catch (Exception e) {
            log.error("Failed to load fidelity ranges at startup. " +
                "Engine will attempt to populate cache from RabbitMQ events.", e);
            // Intentionally not throwing; Engine stays operational even if DB load fails
            // RabbitMQ listener will synchronize when Admin publishes events
        }
    }
}
