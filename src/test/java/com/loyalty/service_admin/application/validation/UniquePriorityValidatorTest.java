package com.loyalty.service_admin.application.validation;

import com.loyalty.service_admin.domain.entity.RuleAttributeEntity;
import com.loyalty.service_admin.domain.entity.RuleAttributeValueEntity;
import com.loyalty.service_admin.domain.entity.RuleEntity;
import com.loyalty.service_admin.domain.repository.RuleAttributeRepository;
import com.loyalty.service_admin.domain.repository.RuleAttributeValueRepository;
import com.loyalty.service_admin.infrastructure.exception.ConflictException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("UniquePriorityValidator Unit Tests")
class UniquePriorityValidatorTest {

    @Mock private RuleAttributeValueRepository ruleAttributeValueRepository;
    @Mock private RuleAttributeRepository ruleAttributeRepository;

    @InjectMocks
    private UniquePriorityValidator uniquePriorityValidator;

    private RuleEntity buildRule(UUID id) {
        return RuleEntity.builder()
                .id(id).ecommerceId(UUID.randomUUID()).discountPriorityId(UUID.randomUUID())
                .name("rule").discountPercentage(BigDecimal.TEN).isActive(true)
                .createdAt(Instant.now()).updatedAt(Instant.now())
                .build();
    }

    private void mockPriorityAttribute(UUID ruleId, String priorityValue) {
        UUID attrId = UUID.randomUUID();
        RuleAttributeValueEntity attrVal = RuleAttributeValueEntity.builder()
                .ruleId(ruleId).attributeId(attrId).value(priorityValue)
                .createdAt(Instant.now()).updatedAt(Instant.now()).build();

        when(ruleAttributeValueRepository.findByRuleIdOrderByCreatedAtAsc(ruleId))
                .thenReturn(List.of(attrVal));

        RuleAttributeEntity attr = RuleAttributeEntity.builder()
                .id(attrId).attributeName("priority").build();

        when(ruleAttributeRepository.findById(attrId)).thenReturn(Optional.of(attr));
    }

    @Test
    @DisplayName("validateUniquePriority - empty rules list passes")
    void validateUniquePriority_emptyList_passes() {
        assertDoesNotThrow(() ->
                uniquePriorityValidator.validateUniquePriority(List.of(), 5, null));
    }

    @Test
    @DisplayName("validateUniquePriority - excludeRuleId filters correctly")
    void validateUniquePriority_excludeRuleId_passes() {
        UUID ruleId = UUID.randomUUID();
        RuleEntity rule = buildRule(ruleId);

        assertDoesNotThrow(() ->
                uniquePriorityValidator.validateUniquePriority(List.of(rule), 5, ruleId));
    }

    @Test
    @DisplayName("validateUniquePriority - unique priority passes")
    void validateUniquePriority_uniquePriority_passes() {
        UUID ruleId = UUID.randomUUID();
        RuleEntity existingRule = buildRule(ruleId);
        mockPriorityAttribute(ruleId, "3");

        assertDoesNotThrow(() ->
                uniquePriorityValidator.validateUniquePriority(List.of(existingRule), 5, null));
    }

    @Test
    @DisplayName("validateUniquePriority - duplicate priority throws ConflictException")
    void validateUniquePriority_duplicatePriority_throws() {
        UUID ruleId = UUID.randomUUID();
        RuleEntity existingRule = buildRule(ruleId);
        mockPriorityAttribute(ruleId, "5");

        assertThrows(ConflictException.class, () ->
                uniquePriorityValidator.validateUniquePriority(List.of(existingRule), 5, null));
    }

    @Test
    @DisplayName("validateUniquePriority - multiple rules, one duplicate throws")
    void validateUniquePriority_multipleRules_oneDuplicate_throws() {
        UUID ruleId1 = UUID.randomUUID();
        UUID ruleId2 = UUID.randomUUID();
        RuleEntity rule1 = buildRule(ruleId1);
        RuleEntity rule2 = buildRule(ruleId2);
        mockPriorityAttribute(ruleId1, "3");
        mockPriorityAttribute(ruleId2, "5");

        assertThrows(ConflictException.class, () ->
                uniquePriorityValidator.validateUniquePriority(List.of(rule1, rule2), 5, null));
    }

    @Test
    @DisplayName("validateUniquePriority - multiple rules, all unique passes")
    void validateUniquePriority_multipleRules_allUnique_passes() {
        UUID ruleId1 = UUID.randomUUID();
        UUID ruleId2 = UUID.randomUUID();
        RuleEntity rule1 = buildRule(ruleId1);
        RuleEntity rule2 = buildRule(ruleId2);
        mockPriorityAttribute(ruleId1, "3");
        mockPriorityAttribute(ruleId2, "7");

        assertDoesNotThrow(() ->
                uniquePriorityValidator.validateUniquePriority(List.of(rule1, rule2), 5, null));
    }
}
