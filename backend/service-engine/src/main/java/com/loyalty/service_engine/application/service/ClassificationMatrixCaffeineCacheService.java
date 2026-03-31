package com.loyalty.service_engine.application.service;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.loyalty.service_engine.domain.entity.ClassificationRuleReplicaEntity;
import com.loyalty.service_engine.domain.entity.CustomerTierReplicaEntity;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * In-memory Caffeine cache for Classification Matrix.
 * Stores tiers and rules for O(1) lookup during /classify endpoint.
 * TTL: 10 minutes. Invalidated via RabbitMQ events.
 */
@Service
@Slf4j
public class ClassificationMatrixCaffeineCacheService {

    private static final String TIERS_CACHE_KEY = "CUSTOMER_TIERS";
    private static final String RULES_CACHE_KEY = "CLASSIFICATION_RULES";
    private static final int TTL_MINUTES = 10;

    private final Cache<String, Object> cache;

    public ClassificationMatrixCaffeineCacheService() {
        this.cache = Caffeine.newBuilder()
            .expireAfterWrite(TTL_MINUTES, TimeUnit.MINUTES)
            .maximumSize(10000)
            .recordStats()
            .build();
        log.info("ClassificationMatrixCaffeineCacheService initialized with TTL={}min", TTL_MINUTES);
    }

    /**
     * Store tiers in cache.
     */
    public void putTiers(List<CustomerTierReplicaEntity> tiers) {
        cache.put(TIERS_CACHE_KEY, tiers);
        log.info("Cached {} customer tiers", tiers.size());
    }

    /**
     * Retrieve tiers from cache.
     */
    @SuppressWarnings("unchecked")
    public Optional<List<CustomerTierReplicaEntity>> getTiers() {
        Object cached = cache.getIfPresent(TIERS_CACHE_KEY);
        return Optional.ofNullable((List<CustomerTierReplicaEntity>) cached);
    }

    /**
     * Store rules in cache (grouped by tier for quick access).
     */
    public void putRules(List<ClassificationRuleReplicaEntity> rules) {
        Map<UUID, List<ClassificationRuleReplicaEntity>> rulesByTier = new HashMap<>();
        for (ClassificationRuleReplicaEntity rule : rules) {
            rulesByTier.computeIfAbsent(rule.getTierUid(), key -> new java.util.ArrayList<>()).add(rule);
        }
        cache.put(RULES_CACHE_KEY, rulesByTier);
        log.info("Cached {} classification rules grouped by tier", rules.size());
    }

    /**
     * Retrieve rules from cache.
     */
    @SuppressWarnings("unchecked")
    public Optional<Map<UUID, List<ClassificationRuleReplicaEntity>>> getRules() {
        Object cached = cache.getIfPresent(RULES_CACHE_KEY);
        return Optional.ofNullable((Map<UUID, List<ClassificationRuleReplicaEntity>>) cached);
    }

    /**
     * Invalidate entire cache (call when matrix updated via RabbitMQ).
     */
    public void invalidate() {
        cache.invalidateAll();
        log.info("Classification matrix cache invalidated");
    }

    /**
     * Check if cache is empty.
     */
    public boolean isEmpty() {
        return getTiers().isEmpty() || getRules().isEmpty();
    }

    /**
     * Get cache stats for monitoring.
     */
    public com.github.benmanes.caffeine.cache.stats.CacheStats getStats() {
        return cache.stats();
    }
}
