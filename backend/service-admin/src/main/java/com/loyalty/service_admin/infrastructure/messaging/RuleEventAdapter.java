package com.loyalty.service_admin.infrastructure.messaging;

import com.loyalty.service_admin.application.port.out.RuleEventPort;
import com.loyalty.service_admin.application.dto.events.RuleEvent;
import com.loyalty.service_admin.domain.entity.RuleEntity;
import com.loyalty.service_admin.domain.entity.DiscountPriorityEntity;
import com.loyalty.service_admin.domain.repository.DiscountLimitPriorityRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * RuleEventAdapter - Adapter de eventos para RuleService.
 *
 * Implementa RuleEventPort publicando eventos a RabbitMQ.
 * Desacopla RuleService de la implementación concreta de RabbitMQ.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class RuleEventAdapter implements RuleEventPort {

    private final RabbitTemplate rabbitTemplate;
    private final DiscountLimitPriorityRepository priorityRepository;

    @Override
    public void publishRuleCreated(
            RuleEntity rule,
            UUID ecommerceId,
            Map<String, String> attributes,
            String ruleType
    ) {
        publishRuleEvent("RULE_CREATED", rule, ecommerceId, attributes, ruleType);
    }

    @Override
    public void publishRuleUpdated(
            RuleEntity rule,
            UUID ecommerceId,
            Map<String, String> attributes,
            String ruleType
    ) {
        publishRuleEvent("RULE_UPDATED", rule, ecommerceId, attributes, ruleType);
    }

    @Override
    public void publishRuleDeleted(UUID ruleId, UUID ecommerceId, String ruleType) {
        try {
            Map<String, Object> eventData = new HashMap<>();
            eventData.put("ruleId", ruleId);
            eventData.put("ecommerceId", ecommerceId);
            eventData.put("ruleType", ruleType);
            eventData.put("eventType", "RULE_DELETED");
            eventData.put("timestamp", Instant.now());

            rabbitTemplate.convertAndSend("loyalty.events", "rule.updated", eventData);
            log.info("Published RULE_DELETED event: ruleId={}, ecommerceId={}", ruleId, ecommerceId);
        } catch (Exception ex) {
            log.error("Failed to publish RULE_DELETED event: ruleId={}, ecommerceId={}", ruleId, ecommerceId, ex);
        }
    }

    @Override
    public void publishRuleActivated(RuleEntity rule, UUID ecommerceId, String ruleType) {
        publishRuleEvent("RULE_ACTIVATED", rule, ecommerceId, new HashMap<>(), ruleType);
    }

    @Override
    public void publishRuleDeactivated(RuleEntity rule, UUID ecommerceId, String ruleType) {
        publishRuleEvent("RULE_DEACTIVATED", rule, ecommerceId, new HashMap<>(), ruleType);
    }

    @Override
    public void publishTiersAssignedToRule(UUID ruleId, UUID ecommerceId, Iterable<UUID> tierIds) {
        try {
            Map<String, Object> eventData = new HashMap<>();
            eventData.put("ruleId", ruleId);
            eventData.put("ecommerceId", ecommerceId);
            eventData.put("tierIds", tierIds);
            eventData.put("eventType", "TIERS_ASSIGNED_TO_RULE");
            eventData.put("timestamp", Instant.now());

            rabbitTemplate.convertAndSend("loyalty.events", "rule.updated", eventData);
            log.info("Published TIERS_ASSIGNED_TO_RULE event: ruleId={}, ecommerceId={}", ruleId, ecommerceId);
        } catch (Exception ex) {
            log.error("Failed to publish TIERS_ASSIGNED_TO_RULE event: ruleId={}, ecommerceId={}", ruleId, ecommerceId, ex);
        }
    }

    @Override
    public void publishTierRemovedFromRule(UUID ruleId, UUID ecommerceId, UUID tierId) {
        try {
            Map<String, Object> eventData = new HashMap<>();
            eventData.put("ruleId", ruleId);
            eventData.put("ecommerceId", ecommerceId);
            eventData.put("tierId", tierId);
            eventData.put("eventType", "TIER_REMOVED_FROM_RULE");
            eventData.put("timestamp", Instant.now());

            rabbitTemplate.convertAndSend("loyalty.events", "rule.updated", eventData);
            log.info("Published TIER_REMOVED_FROM_RULE event: ruleId={}, tierId={}", ruleId, tierId);
        } catch (Exception ex) {
            log.error("Failed to publish TIER_REMOVED_FROM_RULE event: ruleId={}, tierId={}", ruleId, tierId, ex);
        }
    }

    @Override
    public void publishClassificationRuleCreated(UUID tierId, UUID ecommerceId, Map<String, String> attributes) {
        try {
            Map<String, Object> eventData = new HashMap<>(attributes);
            eventData.put("tierId", tierId);
            eventData.put("ecommerceId", ecommerceId);
            eventData.put("eventType", "CLASSIFICATION_RULE_CREATED");
            eventData.put("timestamp", Instant.now());

            rabbitTemplate.convertAndSend("loyalty.events", "rule.updated", eventData);
            log.info("Published CLASSIFICATION_RULE_CREATED event: tierId={}, ecommerceId={}", tierId, ecommerceId);
        } catch (Exception ex) {
            log.error("Failed to publish CLASSIFICATION_RULE_CREATED event: tierId={}, ecommerceId={}", tierId, ecommerceId, ex);
        }
    }

    @Override
    public void publishClassificationRuleUpdated(UUID tierId, UUID ecommerceId, Map<String, String> attributes) {
        try {
            Map<String, Object> eventData = new HashMap<>(attributes);
            eventData.put("tierId", tierId);
            eventData.put("ecommerceId", ecommerceId);
            eventData.put("eventType", "CLASSIFICATION_RULE_UPDATED");
            eventData.put("timestamp", Instant.now());

            rabbitTemplate.convertAndSend("loyalty.events", "rule.updated", eventData);
            log.info("Published CLASSIFICATION_RULE_UPDATED event: tierId={}, ecommerceId={}", tierId, ecommerceId);
        } catch (Exception ex) {
            log.error("Failed to publish CLASSIFICATION_RULE_UPDATED event: tierId={}, ecommerceId={}", tierId, ecommerceId, ex);
        }
    }

    @Override
    public void publishClassificationRuleDeleted(UUID tierId, UUID ruleId, UUID ecommerceId) {
        try {
            Map<String, Object> eventData = new HashMap<>();
            eventData.put("tierId", tierId);
            eventData.put("ruleId", ruleId);
            eventData.put("ecommerceId", ecommerceId);
            eventData.put("eventType", "CLASSIFICATION_RULE_DELETED");
            eventData.put("timestamp", Instant.now());

            rabbitTemplate.convertAndSend("loyalty.events", "rule.updated", eventData);
            log.info("Published CLASSIFICATION_RULE_DELETED event: tierId={}, ruleId={}", tierId, ruleId);
        } catch (Exception ex) {
            log.error("Failed to publish CLASSIFICATION_RULE_DELETED event: tierId={}, ruleId={}", tierId, ruleId, ex);
        }
    }

    // ===== PRIVATE HELPERS =====

    private void publishRuleEvent(
            String eventType,
            RuleEntity rule,
            UUID ecommerceId,
            Map<String, String> attributes,
            String typeCode
    ) {
        try {
            Map<String, Object> logicConditions = new HashMap<>(attributes);

            RuleEvent event = new RuleEvent(
                eventType,
                rule.getId(),
                ecommerceId,
                rule.getName(),
                rule.getDescription(),
                typeCode,
                rule.getDiscountPercentage(),
                resolvePriorityLevel(rule.getDiscountPriorityId()),
                logicConditions,
                rule.getIsActive(),
                "INDIVIDUAL",
                Instant.now()
            );

            rabbitTemplate.convertAndSend("loyalty.events", "rule.updated", event);
            log.info("Published RuleEvent: eventType={}, ruleId={}, discountTypeCode={}, ecommerceId={}",
                eventType, rule.getId(), typeCode, ecommerceId);
        } catch (Exception ex) {
            log.error("Failed to publish RuleEvent: eventType={}, ruleId={}, typeCode={}",
                eventType, rule.getId(), typeCode, ex);
        }
    }

    private Integer resolvePriorityLevel(UUID discountPriorityId) {
        DiscountPriorityEntity priority = priorityRepository.findById(discountPriorityId).orElse(null);
        return priority != null ? priority.getPriorityLevel() : 1;
    }
}
