package com.loyalty.service_admin.infrastructure.messaging;

import com.loyalty.service_admin.application.dto.events.RuleEvent;
import com.loyalty.service_admin.domain.entity.DiscountPriorityEntity;
import com.loyalty.service_admin.domain.entity.RuleEntity;
import com.loyalty.service_admin.domain.repository.DiscountLimitPriorityRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.math.BigDecimal;
import java.util.*;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("RuleEventAdapter Unit Tests")
class RuleEventAdapterTest {

    @Mock
    private RabbitTemplate rabbitTemplate;

    @Mock
    private DiscountLimitPriorityRepository priorityRepository;

    @InjectMocks
    private RuleEventAdapter adapter;

    private RuleEntity createRule() {
        RuleEntity rule = new RuleEntity();
        rule.setId(UUID.randomUUID());
        rule.setName("Test Rule");
        rule.setDescription("Description");
        rule.setDiscountPercentage(BigDecimal.TEN);
        rule.setDiscountPriorityId(UUID.randomUUID());
        rule.setIsActive(true);
        return rule;
    }

    @Test
    void testPublishRuleCreated_success() {
        // Arrange
        RuleEntity rule = createRule();
        UUID ecommerceId = UUID.randomUUID();
        Map<String, String> attrs = Map.of("key", "value");
        DiscountPriorityEntity priority = new DiscountPriorityEntity();
        priority.setPriorityLevel(1);
        when(priorityRepository.findById(rule.getDiscountPriorityId())).thenReturn(Optional.of(priority));

        // Act
        adapter.publishRuleCreated(rule, ecommerceId, attrs, "FIDELITY");

        // Assert
        verify(rabbitTemplate).convertAndSend(eq("loyalty.events"), eq("rule.updated"), any(RuleEvent.class));
    }

    @Test
    void testPublishRuleUpdated_success() {
        // Arrange
        RuleEntity rule = createRule();
        UUID ecommerceId = UUID.randomUUID();
        DiscountPriorityEntity priority = new DiscountPriorityEntity();
        priority.setPriorityLevel(2);
        when(priorityRepository.findById(rule.getDiscountPriorityId())).thenReturn(Optional.of(priority));

        // Act
        adapter.publishRuleUpdated(rule, ecommerceId, new HashMap<>(), "SEASONAL");

        // Assert
        verify(rabbitTemplate).convertAndSend(eq("loyalty.events"), eq("rule.updated"), any(RuleEvent.class));
    }

    @Test
    void testPublishRuleCreated_noPriorityFound_defaultsToOne() {
        // Arrange
        RuleEntity rule = createRule();
        UUID ecommerceId = UUID.randomUUID();
        when(priorityRepository.findById(rule.getDiscountPriorityId())).thenReturn(Optional.empty());

        // Act
        adapter.publishRuleCreated(rule, ecommerceId, new HashMap<>(), "FIDELITY");

        // Assert
        verify(rabbitTemplate).convertAndSend(eq("loyalty.events"), eq("rule.updated"), any(RuleEvent.class));
    }

    @Test
    void testPublishRuleDeleted_success() {
        // Arrange
        UUID ruleId = UUID.randomUUID();
        UUID ecommerceId = UUID.randomUUID();

        // Act
        adapter.publishRuleDeleted(ruleId, ecommerceId, "FIDELITY");

        // Assert
        verify(rabbitTemplate).convertAndSend(eq("loyalty.events"), eq("rule.updated"), anyMap());
    }

    @Test
    void testPublishRuleActivated_success() {
        // Arrange
        RuleEntity rule = createRule();
        UUID ecommerceId = UUID.randomUUID();
        when(priorityRepository.findById(rule.getDiscountPriorityId())).thenReturn(Optional.empty());

        // Act
        adapter.publishRuleActivated(rule, ecommerceId, "FIDELITY");

        // Assert
        verify(rabbitTemplate).convertAndSend(eq("loyalty.events"), eq("rule.updated"), any(RuleEvent.class));
    }

    @Test
    void testPublishRuleDeactivated_success() {
        // Arrange
        RuleEntity rule = createRule();
        UUID ecommerceId = UUID.randomUUID();
        when(priorityRepository.findById(rule.getDiscountPriorityId())).thenReturn(Optional.empty());

        // Act
        adapter.publishRuleDeactivated(rule, ecommerceId, "FIDELITY");

        // Assert
        verify(rabbitTemplate).convertAndSend(eq("loyalty.events"), eq("rule.updated"), any(RuleEvent.class));
    }

    @Test
    void testPublishTiersAssignedToRule_success() {
        // Arrange
        UUID ruleId = UUID.randomUUID();
        UUID ecommerceId = UUID.randomUUID();
        List<UUID> tierIds = List.of(UUID.randomUUID(), UUID.randomUUID());

        // Act
        adapter.publishTiersAssignedToRule(ruleId, ecommerceId, tierIds);

        // Assert
        verify(rabbitTemplate).convertAndSend(eq("loyalty.events"), eq("rule.updated"), anyMap());
    }

    @Test
    void testPublishTierRemovedFromRule_success() {
        // Arrange
        UUID ruleId = UUID.randomUUID();
        UUID ecommerceId = UUID.randomUUID();
        UUID tierId = UUID.randomUUID();

        // Act
        adapter.publishTierRemovedFromRule(ruleId, ecommerceId, tierId);

        // Assert
        verify(rabbitTemplate).convertAndSend(eq("loyalty.events"), eq("rule.updated"), anyMap());
    }

    @Test
    void testPublishClassificationRuleCreated_success() {
        // Arrange
        UUID tierId = UUID.randomUUID();
        UUID ecommerceId = UUID.randomUUID();
        Map<String, String> attrs = Map.of("min_spend", "100");

        // Act
        adapter.publishClassificationRuleCreated(tierId, ecommerceId, new HashMap<>(attrs));

        // Assert
        verify(rabbitTemplate).convertAndSend(eq("loyalty.events"), eq("rule.updated"), anyMap());
    }

    @Test
    void testPublishClassificationRuleUpdated_success() {
        // Arrange
        UUID tierId = UUID.randomUUID();
        UUID ecommerceId = UUID.randomUUID();

        // Act
        adapter.publishClassificationRuleUpdated(tierId, ecommerceId, new HashMap<>());

        // Assert
        verify(rabbitTemplate).convertAndSend(eq("loyalty.events"), eq("rule.updated"), anyMap());
    }

    @Test
    void testPublishClassificationRuleDeleted_success() {
        // Arrange
        UUID tierId = UUID.randomUUID();
        UUID ruleId = UUID.randomUUID();
        UUID ecommerceId = UUID.randomUUID();

        // Act
        adapter.publishClassificationRuleDeleted(tierId, ruleId, ecommerceId);

        // Assert
        verify(rabbitTemplate).convertAndSend(eq("loyalty.events"), eq("rule.updated"), anyMap());
    }

    @Test
    void testPublishRuleDeleted_exceptionHandled() {
        // Arrange
        doThrow(new RuntimeException("error")).when(rabbitTemplate)
                .convertAndSend(anyString(), anyString(), anyMap());

        // Act - should not throw
        adapter.publishRuleDeleted(UUID.randomUUID(), UUID.randomUUID(), "FIDELITY");
    }

    @Test
    void testPublishRuleCreated_exceptionHandled() {
        // Arrange
        RuleEntity rule = createRule();
        when(priorityRepository.findById(any())).thenReturn(Optional.empty());
        doThrow(new RuntimeException("error")).when(rabbitTemplate)
                .convertAndSend(anyString(), anyString(), any(Object.class));

        // Act - should not throw
        adapter.publishRuleCreated(rule, UUID.randomUUID(), new HashMap<>(), "FIDELITY");
    }

    @Test
    void testPublishTiersAssigned_exceptionHandled() {
        // Arrange
        doThrow(new RuntimeException("error")).when(rabbitTemplate)
                .convertAndSend(anyString(), anyString(), anyMap());

        // Act - should not throw
        adapter.publishTiersAssignedToRule(UUID.randomUUID(), UUID.randomUUID(), List.of());
    }

    @Test
    void testPublishTierRemoved_exceptionHandled() {
        // Arrange
        doThrow(new RuntimeException("error")).when(rabbitTemplate)
                .convertAndSend(anyString(), anyString(), anyMap());

        // Act - should not throw
        adapter.publishTierRemovedFromRule(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID());
    }

    @Test
    void testPublishClassificationCreated_exceptionHandled() {
        // Arrange
        doThrow(new RuntimeException("error")).when(rabbitTemplate)
                .convertAndSend(anyString(), anyString(), anyMap());

        // Act - should not throw
        adapter.publishClassificationRuleCreated(UUID.randomUUID(), UUID.randomUUID(), new HashMap<>());
    }

    @Test
    void testPublishClassificationUpdated_exceptionHandled() {
        // Arrange
        doThrow(new RuntimeException("error")).when(rabbitTemplate)
                .convertAndSend(anyString(), anyString(), anyMap());

        // Act - should not throw
        adapter.publishClassificationRuleUpdated(UUID.randomUUID(), UUID.randomUUID(), new HashMap<>());
    }

    @Test
    void testPublishClassificationDeleted_exceptionHandled() {
        // Arrange
        doThrow(new RuntimeException("error")).when(rabbitTemplate)
                .convertAndSend(anyString(), anyString(), anyMap());

        // Act - should not throw
        adapter.publishClassificationRuleDeleted(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID());
    }
}
