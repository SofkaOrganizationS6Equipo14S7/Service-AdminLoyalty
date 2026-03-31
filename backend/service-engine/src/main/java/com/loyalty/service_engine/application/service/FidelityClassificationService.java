package com.loyalty.service_engine.application.service;

import com.loyalty.service_engine.application.dto.ClassificationResult;
import com.loyalty.service_engine.application.dto.FidelityRangeDTO;
import com.loyalty.service_engine.infrastructure.cache.FidelityRangeCache;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Classifies clients to fidelity levels based on accumulated points.
 *
 * Classification Logic (3-path):
 * 1. EXACT MATCH: Client points fall within a range [min, max] → Return that level
 * 2. FALLTHROUGH: Client in gap (e.g., 1001-4999 between Bronce[0-999] and Oro[5000-9999])
 *    → Assign the nearest lower level (the level with highest max_points < client_points)
 * 3. NONE: Client < minimum entry threshold → No level (not auto-assigned to minimum)
 *
 * Example:
 * Ranges: Bronce[0-999], Plata[1000-4999], Oro[5000-9999], Platino[10000+]
 *
 * Client A (2500 pts) → EXACT MATCH → Plata
 * Client B (3000 pts in gap [1001-4999]) → FALLTHROUGH → Bronce (max=999 < 3000)
 * Client C (500 pts < 1000) → NONE (below entry threshold, not auto-assigned)
 * Client D (15000 pts) → EXACT MATCH → Platino
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FidelityClassificationService {
    private final FidelityRangeCache cache;

    /**
     * Classify a client to a fidelity level based on accumulated points.
     *
     * @param ecommerceId Tenant identifier
     * @param clientPoints Accumulated fidelity points
     * @return ClassificationResult with exact match, fallthrough, or NONE
     */
    public ClassificationResult classify(UUID ecommerceId, Integer clientPoints) {
        if (ecommerceId == null || clientPoints == null || clientPoints < 0) {
            log.warn("Invalid classification request: ecommerceId={}, points={}", ecommerceId, clientPoints);
            return ClassificationResult.NONE;
        }

        // Fetch ranges from cache (already sorted by min_points)
        List<FidelityRangeDTO> ranges = cache.getRangesByEcommerce(ecommerceId);

        if (ranges == null || ranges.isEmpty()) {
            log.debug("No fidelity ranges configured for ecommerce: {}", ecommerceId);
            return ClassificationResult.NONE;
        }

        // PATH 1: Exact match - client falls exactly within a range
        for (FidelityRangeDTO range : ranges) {
            if (clientPoints >= range.minPoints() && clientPoints <= range.maxPoints()) {
                log.debug("Exact match: ecommerce={}, points={}, level={}, range=[{}-{}]",
                    ecommerceId, clientPoints, range.name(), range.minPoints(), range.maxPoints());
                return ClassificationResult.of(range);
            }
        }

        // PATH 2: Fallthrough - client in gap, find nearest lower level
        // Get the range with highest max_points that is still < client_points
        Optional<FidelityRangeDTO> fallback = ranges.stream()
            .filter(r -> r.maxPoints() < clientPoints)
            .max(Comparator.comparingInt(FidelityRangeDTO::maxPoints));

        if (fallback.isPresent()) {
            FidelityRangeDTO fallbackRange = fallback.get();
            log.debug("Fallthrough: ecommerce={}, points={}, assigned to={}, range=[{}-{}] " +
                "(gap tolerance)",
                ecommerceId, clientPoints, fallbackRange.name(),
                fallbackRange.minPoints(), fallbackRange.maxPoints());
            return ClassificationResult.of(fallbackRange);
        }

        // PATH 3: No qualification - client points below minimum entry level
        // This means client < minPoints of the lowest level
        // IMPORTANT: Do NOT auto-assign to minimum level; return NONE instead
        Integer minEntryPoints = ranges.stream()
            .mapToInt(FidelityRangeDTO::minPoints)
            .min()
            .orElse(Integer.MAX_VALUE);

        if (clientPoints < minEntryPoints) {
            log.debug("No qualification: ecommerce={}, points={}, below entry threshold={}",
                ecommerceId, clientPoints, minEntryPoints);
            return ClassificationResult.NONE;
        }

        // This should rarely happen unless cache is inconsistent
        log.warn("Unexpected classification state: ecommerce={}, points={}, ranges available={}",
            ecommerceId, clientPoints, ranges.size());
        return ClassificationResult.NONE;
    }

    /**
     * Classify multiple clients in batch.
     * Useful for bulk operations or testing.
     */
    public List<ClassificationResult> classifyBatch(UUID ecommerceId, List<Integer> clientPointsList) {
        return clientPointsList.stream()
            .map(points -> classify(ecommerceId, points))
            .toList();
    }
}
