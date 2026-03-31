package com.loyalty.service_engine.infrastructure.cache;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.loyalty.service_engine.application.dto.FidelityRangeDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * In-memory cache for fidelity ranges using Caffeine.
 * Organized by ecommerce to support multi-tenancy.
 * Pre-loaded at startup from Engine's local replica table.
 * Updated via RabbitMQ listener when Admin publishes events.
 *
 * Cache structure: Map<UUID(ecommerceId), List<FidelityRangeDTO>>
 */
@Slf4j
@Component
public class FidelityRangeCache {
    private static final Duration CACHE_EXPIRATION = Duration.ofHours(24);
    private static final int INITIAL_CAPACITY = 1000;

    // Per-tenant cache of ranges
    // Key: ecommerce_id, Value: sorted list of active ranges by min_points
    private final Map<UUID, List<FidelityRangeDTO>> rangesByEcommerce = new ConcurrentHashMap<>();

    // Optional: Caffeine cache for specific range lookups by uid
    private final Cache<UUID, FidelityRangeDTO> rangesByUid = Caffeine.newBuilder()
        .expireAfterWrite(CACHE_EXPIRATION)
        .initialCapacity(INITIAL_CAPACITY)
        .build();

    /**
     * Add or update a fidelity range in cache.
     * Maintains sorted order by min_points for each ecommerce.
     */
    public synchronized void addOrUpdate(FidelityRangeDTO range) {
        if (range == null || !range.isActive()) {
            return;
        }

        UUID ecommerceId = range.ecommerceId();
        
        // Update individual range cache
        rangesByUid.put(range.uid(), range);

        // Update ecommerce ranges (replace old version if exists)
        List<FidelityRangeDTO> ranges = rangesByEcommerce.computeIfAbsent(
            ecommerceId,
            k -> new ArrayList<>()
        );

        // Remove old version if exists
        ranges.removeIf(r -> r.uid().equals(range.uid()));

        // Add new version and sort by min_points
        ranges.add(range);
        ranges.sort(Comparator.comparingInt(FidelityRangeDTO::minPoints));

        log.debug("Added/updated fidelity range to cache: uid={}, ecommerce={}, level={}, range=[{}-{}]",
            range.uid(), ecommerceId, range.name(), range.minPoints(), range.maxPoints());
    }

    /**
     * Remove a fidelity range from cache (soft-delete).
     */
    public synchronized void remove(UUID rangeUid, UUID ecommerceId) {
        // Remove from individual range cache
        rangesByUid.invalidate(rangeUid);

        // Remove from ecommerce ranges
        List<FidelityRangeDTO> ranges = rangesByEcommerce.get(ecommerceId);
        if (ranges != null) {
            ranges.removeIf(r -> r.uid().equals(rangeUid));
            log.debug("Removed fidelity range from cache: uid={}, ecommerce={}", rangeUid, ecommerceId);
        }
    }

    /**
     * Get all active ranges for an ecommerce, sorted by min_points ascending.
     * Returns empty list if ecommerce not found or no ranges.
     */
    public List<FidelityRangeDTO> getRangesByEcommerce(UUID ecommerceId) {
        List<FidelityRangeDTO> ranges = rangesByEcommerce.getOrDefault(ecommerceId, Collections.emptyList());
        return Collections.unmodifiableList(ranges);
    }

    /**
     * Get a specific range by uid.
     * Returns empty Optional if not found.
     */
    public Optional<FidelityRangeDTO> getRange(UUID uid) {
        return Optional.ofNullable(rangesByUid.getIfPresent(uid));
    }

    /**
     * Pre-load cache from initial data set (used at startup).
     * Clears existing cache first to ensure consistency.
     */
    public synchronized void loadAll(List<FidelityRangeDTO> ranges) {
        rangesByEcommerce.clear();
        rangesByUid.invalidateAll();

        for (FidelityRangeDTO range : ranges) {
            if (range.isActive()) {
                addOrUpdate(range);
            }
        }

        long totalRanges = ranges.stream().filter(FidelityRangeDTO::isActive).count();
        long ecommerceCount = rangesByEcommerce.keySet().size();
        log.info("Pre-loaded fidelity ranges cache: {} ranges across {} ecommerces",
            totalRanges, ecommerceCount);
    }

    /**
     * Get cache statistics.
     */
    public Map<String, Object> getStats() {
        return Map.of(
            "totalEcommerces", rangesByEcommerce.size(),
            "totalRanges", rangesByUid.asMap().size(),
            "cacheExpiration", CACHE_EXPIRATION.toString()
        );
    }

    /**
     * Clear all caches (useful for testing).
     */
    public synchronized void clear() {
        rangesByEcommerce.clear();
        rangesByUid.invalidateAll();
        log.debug("Cleared all fidelity ranges from cache");
    }
}
