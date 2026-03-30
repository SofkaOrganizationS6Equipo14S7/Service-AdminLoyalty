package com.loyalty.service_engine.infrastructure.startup;

import com.loyalty.service_engine.infrastructure.cache.SeasonalRulesCacheManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Startup event listener for Service-Engine
 * 
 * Executes after Spring Boot application is fully initialized.
 * Performs cold start initialization:
 * - Loads all active seasonal rules into Caffeine cache
 * 
 * This ensures that the first `/calculate` request has rules
 * already cached (avoids cold cache misses on first requests).
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ServiceEngineStartupListener {
    
    private final SeasonalRulesCacheManager seasonalRulesCacheManager;
    
    /**
     * Called when the application is ready (all beans initialized)
     */
    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        log.info("=== Service-Engine Startup: Initializing caches ===");
        
        // Load seasonal rules into cache
        int ecommercesLoaded = seasonalRulesCacheManager.loadFromDatabase();
        log.info("Seasonal rules cache: {} ecommerce IDs loaded with active rules", ecommercesLoaded);
        
        log.info("=== Service-Engine Startup: Initialization complete ===");
    }
}
