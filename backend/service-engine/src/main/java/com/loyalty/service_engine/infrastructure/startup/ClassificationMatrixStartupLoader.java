package com.loyalty.service_engine.infrastructure.startup;

import com.loyalty.service_engine.application.service.ClassificationMatrixCaffeineCacheService;
import com.loyalty.service_engine.domain.entity.ClassificationRuleReplicaEntity;
import com.loyalty.service_engine.domain.entity.CustomerTierReplicaEntity;
import com.loyalty.service_engine.domain.repository.ClassificationRuleReplicaRepository;
import com.loyalty.service_engine.domain.repository.CustomerTierReplicaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Cold Start Loader for Classification Matrix.
 *
 * Responsibility:
 * - Executes on application startup (ApplicationReadyEvent)
 * - Loads all active tiers and rules from replica database tables
 * - Populates Caffeine in-memory cache (10-minute TTL)
 * - Guarantees Engine autonomy even if RabbitMQ events haven't arrived yet
 *
 * Why this matters:
 * - Without this loader, Engine would fail on first /classify request after restart
 * - With this loader, classification works immediately after startup
 * - Replica tables are the fallback data source (populated by Admin via events)
 *
 * Algorithm:
 * 1. Query all active tiers (isActive=true) ordered by level
 * 2. Query all active rules
 * 3. Put both into Caffeine cache
 * 4. Log completion with counts
 *
 * Execution Order:
 * - @EventListener(ApplicationReadyEvent.class) executes AFTER all @PostConstruct hooks
 * - Ensures all Spring beans are fully initialized before we populate the cache
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ClassificationMatrixStartupLoader {

    private final CustomerTierReplicaRepository tierReplicaRepository;
    private final ClassificationRuleReplicaRepository ruleReplicaRepository;
    private final ClassificationMatrixCaffeineCacheService cacheService;

    @EventListener(ApplicationReadyEvent.class)
    public void loadClassificationMatrixOnStartup() {
        try {
            log.info("=== ClassificationMatrix Cold Start Loader ===");
            log.info("Loading customer tiers from replica database...");

            // Load active tiers ordered by level (Bronce=1 → Platino=4)
            List<CustomerTierReplicaEntity> tiers = tierReplicaRepository.findByIsActiveTrueOrderByLevelAsc();
            log.info("Loaded {} customer tiers", tiers.size());

            // Load active rules for cache
            List<ClassificationRuleReplicaEntity> rules = ruleReplicaRepository.findAllActiveRulesForCache();
            log.info("Loaded {} classification rules", rules.size());

            // Populate Caffeine cache (both must succeed or cache remains empty)
            if (!tiers.isEmpty()) {
                cacheService.putTiers(tiers);
                log.info("✓ Tiers populated in Caffeine cache (10-min TTL)");
            } else {
                log.warn("⚠ No active tiers found in replica database");
            }

            if (!rules.isEmpty()) {
                cacheService.putRules(rules);
                log.info("✓ Rules populated in Caffeine cache (10-min TTL)");
            } else {
                log.warn("⚠ No active rules found in replica database");
            }

            log.info("ClassificationMatrix Cold Start complete: {} tiers, {} rules ready for classification",
                tiers.size(), rules.size());

        } catch (Exception e) {
            log.error("❌ CRITICAL: ClassificationMatrix startup loader failed — cache empty", e);
            // Don't rethrow: Engine will use lazy-load fallback on first request
            // This allows graceful degradation if DB is temporarily unavailable
        }
    }
}
