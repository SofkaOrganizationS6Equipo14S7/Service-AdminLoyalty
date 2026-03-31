package com.loyalty.service_engine.infrastructure.cache;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.loyalty.service_engine.domain.entity.SeasonalRuleEntity;
import com.loyalty.service_engine.domain.repository.SeasonalRuleRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * Caffeine Cache Manager for Seasonal Rules (Service-Engine)
 * 
 * Purpose:
 * - Cache active seasonal rules in memory for fast evaluation (<100ms)
 * - Key: ecommerceId (UUID)
 * - Value: List<SeasonalRuleEntity> of active rules
 * - TTL: 60 minutes (with proactive invalidation on RabbitMQ events)
 * 
 * Cold Start:
 * - On application startup, loads all active rules from database into cache
 * - Triggered by ApplicationPostgresStartupListener
 * 
 * Invalidation Patterns:
 * - Proactive: RabbitMQ consumer calls invalidate() immediately when rule changes
 * - TTL-based: Cache expires after 60 minutes of inactivity
 * 
 * Fallback:
 * - If cache miss: query database for active rules
 * - Ensures consistency even if cache is cleared unexpectedly
 */
@Component
@Slf4j
public class SeasonalRulesCacheManager {
    
    // Main cache: ecommerceId -> List<SeasonalRuleEntity>
    private final Cache<UUID, List<SeasonalRuleEntity>> rulesCache;
    
    // Repository for fallback queries
    private final SeasonalRuleRepository seasonalRuleRepository;

    /**
     * Initialize Caffeine cache with 60-minute TTL
     */
    public SeasonalRulesCacheManager(SeasonalRuleRepository seasonalRuleRepository) {
        this.seasonalRuleRepository = seasonalRuleRepository;
        this.rulesCache = Caffeine.newBuilder()
            .maximumSize(1000)  // Max 1000 ecommerce IDs
            .expireAfterWrite(60, TimeUnit.MINUTES)
            .recordStats()
            .build();
        
        log.info("SeasonalRulesCacheManager initialized with 60-minute TTL");
    }

    /**
     * Get active seasonal rules for an ecommerce
     * 
     * Returns cached rules if available, otherwise queries database and caches result
     * 
     * @param ecommerceId the ecommerce UUID
     * @return list of active seasonal rules (empty list if none)
     */
    public List<SeasonalRuleEntity> get(UUID ecommerceId) {
        return rulesCache.get(ecommerceId, key -> {
            log.debug("Cache miss for ecommerce: {} - loading from database", ecommerceId);
            return seasonalRuleRepository.findByEcommerceIdAndIsActiveTrue(ecommerceId);
        });
    }

    /**
     * Check if a rule is active for a given ecommerce now
     * 
     * @param ecommerceId the ecommerce UUID
     * @param now the current timestamp
     * @return true if at least one rule is active now
     */
    public boolean hasActiveRuleNow(UUID ecommerceId, Instant now) {
        List<SeasonalRuleEntity> rules = get(ecommerceId);
        return rules.stream().anyMatch(rule -> 
            rule.getIsActive() && 
            now.compareTo(rule.getStartDate()) >= 0 && 
            now.compareTo(rule.getEndDate()) < 0
        );
    }

    /**
     * Get applicable seasonal rules for a specific timestamp
     * 
     * @param ecommerceId the ecommerce UUID
     * @param timestamp the timestamp to check
     * @return list of rules applicable at that moment
     */
    public List<SeasonalRuleEntity> getApplicableRules(UUID ecommerceId, Instant timestamp) {
        List<SeasonalRuleEntity> rules = get(ecommerceId);
        return rules.stream()
            .filter(rule -> 
                rule.getIsActive() &&
                timestamp.compareTo(rule.getStartDate()) >= 0 &&
                timestamp.compareTo(rule.getEndDate()) < 0
            )
            .toList();
    }

    /**
     * Invalidate cache for a specific ecommerce
     * Called by RabbitMQ consumer when a rule changes (created, updated, deleted)
     * 
     * @param ecommerceId the ecommerce UUID
     */
    public void invalidate(UUID ecommerceId) {
        rulesCache.invalidate(ecommerceId);
        log.debug("Cache invalidated for ecommerce: {}", ecommerceId);
    }

    /**
     * Invalidate entire cache
     * Used for testing or maintenance
     */
    public void invalidateAll() {
        rulesCache.invalidateAll();
        log.info("All seasonal rules cache invalidated");
    }

    /**
     * Load all active rules into cache from database
     * Called during application startup (cold start)
     * 
     * @return number of ecommerce IDs cached
     */
    public int loadFromDatabase() {
        log.info("Loading seasonal rules from database (cold start)...");
        
        List<SeasonalRuleEntity> allRules = seasonalRuleRepository.findByIsActiveTrue();
        
        if (allRules.isEmpty()) {
            log.info("No active seasonal rules found in database");
            return 0;
        }
        
        // Group by ecommerce ID and cache
        var groupedByEcommerce = allRules.stream()
            .collect(java.util.stream.Collectors.groupingBy(SeasonalRuleEntity::getEcommerceId));
        
        groupedByEcommerce.forEach((ecommerceId, rules) -> {
            rulesCache.put(ecommerceId, rules);
            log.debug("Cached {} seasonal rules for ecommerce: {}", rules.size(), ecommerceId);
        });
        
        log.info("Cold start: {} ecommerce IDs loaded with {} total seasonal rules",
            groupedByEcommerce.size(), allRules.size());
        
        return groupedByEcommerce.size();
    }

    /**
     * Get cache statistics for monitoring
     * 
     * @return cache stats string
     */
    public String getStats() {
        return rulesCache.stats().toString();
    }

    /**
     * Get current cache size
     */
    public long getCacheSize() {
        return rulesCache.estimatedSize();
    }
}
