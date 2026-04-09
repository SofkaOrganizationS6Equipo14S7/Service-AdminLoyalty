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
@DisplayName("HierarchyValidator Unit Tests")
class HierarchyValidatorTest {

    @Mock private RuleAttributeValueRepository ruleAttributeValueRepository;
    @Mock private RuleAttributeRepository ruleAttributeRepository;

    @InjectMocks
    private HierarchyValidator hierarchyValidator;

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
    @DisplayName("validateHierarchy - empty rules list passes")
    void validateHierarchy_emptyList_passes() {
        assertDoesNotThrow(() ->
                hierarchyValidator.validateHierarchy(List.of(), 5, null));
    }

    @Test
    @DisplayName("validateHierarchy - excludeRuleId filters correctly")
    void validateHierarchy_excludeRuleId_passes() {
        UUID ruleId = UUID.randomUUID();
        RuleEntity rule = buildRule(ruleId);

        assertDoesNotThrow(() ->
                hierarchyValidator.validateHierarchy(List.of(rule), 1, ruleId));
    }

    @Test
    @DisplayName("validateHierarchy - priority greater than max passes")
    void validateHierarchy_priorityGreaterThanMax_passes() {
        UUID ruleId = UUID.randomUUID();
        RuleEntity existingRule = buildRule(ruleId);
        mockPriorityAttribute(ruleId, "3");

        assertDoesNotThrow(() ->
                hierarchyValidator.validateHierarchy(List.of(existingRule), 5, null));
    }

    @Test
    @DisplayName("validateHierarchy - priority less than or equal to max throws ConflictException")
    void validateHierarchy_priorityLessThanOrEqualMax_throws() {
        UUID ruleId = UUID.randomUUID();
        RuleEntity existingRule = buildRule(ruleId);
        mockPriorityAttribute(ruleId, "5");

        assertThrows(ConflictException.class, () ->
                hierarchyValidator.validateHierarchy(List.of(existingRule), 3, null));
    }

    @Test
    @DisplayName("validateHierarchy - priority equal to max throws ConflictException")
    void validateHierarchy_priorityEqualMax_throws() {
        UUID ruleId = UUID.randomUUID();
        RuleEntity existingRule = buildRule(ruleId);
        mockPriorityAttribute(ruleId, "5");

        assertThrows(ConflictException.class, () ->
                hierarchyValidator.validateHierarchy(List.of(existingRule), 5, null));
    }
}
