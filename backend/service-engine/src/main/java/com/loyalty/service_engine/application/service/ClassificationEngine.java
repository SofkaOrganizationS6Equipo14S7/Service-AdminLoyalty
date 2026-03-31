package com.loyalty.service_engine.application.service;

import com.loyalty.service_engine.application.dto.ClassifyRequestV1;
import com.loyalty.service_engine.application.dto.ClassifyResponseV1;
import com.loyalty.service_engine.domain.entity.ClassificationRuleReplicaEntity;
import com.loyalty.service_engine.domain.entity.CustomerTierReplicaEntity;
import com.loyalty.service_engine.domain.repository.ClassificationRuleReplicaRepository;
import com.loyalty.service_engine.domain.repository.CustomerTierReplicaRepository;
import com.loyalty.service_engine.infrastructure.exception.ServiceUnavailableException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Classification Engine — DETERMINISTIC classification logic.
 * 
 * Algorithm (3 paths):
 * 1. Evaluate ALL active rules against payload metrics
 * 2. Find ALL matching rules (metric value within [min, max])
 * 3. If multiple tiers match → select HIGHEST tier level (Platino > Oro > Plata > Bronce)
 * 4. Return single tier OR null (no match)
 * 
 * Heavy lifting: in-memory Caffeine cache. Fallback: read from replica tables.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class ClassificationEngine {

    private final ClassificationMatrixCaffeineCacheService cacheService;
    private final CustomerTierReplicaRepository tierReplicaRepo;
    private final ClassificationRuleReplicaRepository ruleReplicaRepo;

    /**
     * Classify a customer based on their metrics.
     * 
     * @param request contains total_spent, order_count, loyalty_points
     * @return ClassifyResponseV1 with matched tier or null
     * @throws ServiceUnavailableException if matrix unavailable
     */
    public ClassifyResponseV1 classify(ClassifyRequestV1 request) {
        log.info("Classifying customer: totalSpent={}, orderCount={}, loyaltyPoints={}",
            request.totalSpent(), request.orderCount(), request.loyaltyPoints());

        // Try to load matrix from cache
        var tiersOptional = cacheService.getTiers();
        var rulesOptional = cacheService.getRules();

        // Fallback to database if cache empty
        if (tiersOptional.isEmpty() || rulesOptional.isEmpty()) {
            loadMatrixFromDatabase();
            tiersOptional = cacheService.getTiers();
            rulesOptional = cacheService.getRules();
        }

        if (tiersOptional.isEmpty() || rulesOptional.isEmpty()) {
            throw new ServiceUnavailableException("Classification matrix not available");
        }

        List<CustomerTierReplicaEntity> tiers = tiersOptional.get();
        Map<UUID, List<ClassificationRuleReplicaEntity>> rulesByTier = rulesOptional.get();

        // Build payload map for flexible metric evaluation
        Map<String, Object> payload = buildPayload(request);

        // Find ALL matching rules
        Optional<CustomerTierReplicaEntity> matchedTier = Optional.empty();
        Integer highestLevel = -1;

        for (CustomerTierReplicaEntity tier : tiers) {
            if (!tier.getIsActive()) continue;

            List<ClassificationRuleReplicaEntity> tierRules = rulesByTier.getOrDefault(tier.getUid(), List.of());
            
            // Check if ALL rules for this tier match the payload
            boolean allRulesMatch = true;
            for (ClassificationRuleReplicaEntity rule : tierRules) {
                if (!rule.getIsActive()) continue;
                
                Object metricValue = payload.get(rule.getMetricType());
                if (metricValue == null || !ruleMatches(rule, metricValue)) {
                    allRulesMatch = false;
                    break;
                }
            }

            // If all rules match and this tier has higher level, select it
            if (allRulesMatch && tier.getLevel() > highestLevel) {
                matchedTier = Optional.of(tier);
                highestLevel = tier.getLevel();
                log.debug("Tier {} matches with level {}", tier.getName(), tier.getLevel());
            }
        }

        if (matchedTier.isPresent()) {
            CustomerTierReplicaEntity tier = matchedTier.get();
            log.info("Customer classified as: tier={}, level={}", tier.getName(), tier.getLevel());
            return new ClassifyResponseV1(
                tier.getUid(),
                tier.getName(),
                tier.getLevel(),
                "Qualified for " + tier.getName(),
                Instant.now()
            );
        } else {
            log.info("Customer does not qualify for any tier");
            return new ClassifyResponseV1(null, null, null, "No tier match", Instant.now());
        }
    }

    /**
     * Build payload map from request for flexible metric evaluation.
     */
    private Map<String, Object> buildPayload(ClassifyRequestV1 request) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("total_spent", request.totalSpent());
        payload.put("order_count", request.orderCount());
        if (request.loyaltyPoints() != null && request.loyaltyPoints() >= 0) {
            payload.put("loyalty_points", request.loyaltyPoints());
        }
        return payload;
    }

    /**
     * Check if a metric value matches a rule (deterministic).
     */
    @SuppressWarnings("unchecked")
    private boolean ruleMatches(ClassificationRuleReplicaEntity rule, Object metricValue) {
        BigDecimal value;

        // Convert metric to BigDecimal for comparison
        if (metricValue instanceof BigDecimal bd) {
            value = bd;
        } else if (metricValue instanceof Integer i) {
            value = BigDecimal.valueOf(i);
        } else if (metricValue instanceof Long l) {
            value = BigDecimal.valueOf(l);
        } else if (metricValue instanceof String s) {
            try {
                value = new BigDecimal(s);
            } catch (NumberFormatException e) {
                log.warn("Invalid metric value format: {}", s);
                return false;
            }
        } else {
            return false;
        }

        // Check min <= value <= max (NULL max = no upper limit)
        if (value.compareTo(rule.getMinValue()) < 0) {
            return false;
        }
        if (rule.getMaxValue() != null && value.compareTo(rule.getMaxValue()) > 0) {
            return false;
        }

        return true;
    }

    /**
     * Load matrix from replica tables (for Cold Start).
     */
    private void loadMatrixFromDatabase() {
        log.info("Loading classification matrix from replica database...");
        List<CustomerTierReplicaEntity> tiers = tierReplicaRepo.findByIsActiveTrueOrderByLevelAsc();
        List<ClassificationRuleReplicaEntity> rules = ruleReplicaRepo.findAllActiveRulesForCache();

        cacheService.putTiers(tiers);
        cacheService.putRules(rules);
        log.info("Classification matrix loaded: {} tiers, {} rules", tiers.size(), rules.size());
    }
}
