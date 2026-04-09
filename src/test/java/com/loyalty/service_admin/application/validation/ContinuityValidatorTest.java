package com.loyalty.service_admin.application.validation;

import com.loyalty.service_admin.domain.entity.RuleAttributeEntity;
import com.loyalty.service_admin.domain.entity.RuleAttributeValueEntity;
import com.loyalty.service_admin.domain.entity.RuleEntity;
import com.loyalty.service_admin.domain.repository.RuleAttributeRepository;
import com.loyalty.service_admin.domain.repository.RuleAttributeValueRepository;
import com.loyalty.service_admin.infrastructure.exception.BadRequestException;
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
@DisplayName("ContinuityValidator Unit Tests")
class ContinuityValidatorTest {

    @Mock private RuleAttributeValueRepository ruleAttributeValueRepository;
    @Mock private RuleAttributeRepository ruleAttributeRepository;

    @InjectMocks
    private ContinuityValidator continuityValidator;

    private RuleEntity buildRule(UUID id) {
        return RuleEntity.builder()
                .id(id).ecommerceId(UUID.randomUUID()).discountPriorityId(UUID.randomUUID())
                .name("rule").discountPercentage(BigDecimal.TEN).isActive(true)
                .createdAt(Instant.now()).updatedAt(Instant.now())
                .build();
    }

    private void mockRangeAttributes(UUID ruleId, String min, String max) {
        UUID minAttrId = UUID.randomUUID();
        UUID maxAttrId = UUID.randomUUID();

        RuleAttributeValueEntity minVal = RuleAttributeValueEntity.builder()
                .ruleId(ruleId).attributeId(minAttrId).value(min).createdAt(Instant.now()).updatedAt(Instant.now()).build();
        RuleAttributeValueEntity maxVal = RuleAttributeValueEntity.builder()
                .ruleId(ruleId).attributeId(maxAttrId).value(max).createdAt(Instant.now()).updatedAt(Instant.now()).build();

        when(ruleAttributeValueRepository.findByRuleIdOrderByCreatedAtAsc(ruleId))
                .thenReturn(List.of(minVal, maxVal));

        RuleAttributeEntity minAttr = RuleAttributeEntity.builder()
                .id(minAttrId).attributeName("minValue").build();
        RuleAttributeEntity maxAttr = RuleAttributeEntity.builder()
                .id(maxAttrId).attributeName("maxValue").build();

        when(ruleAttributeRepository.findById(minAttrId)).thenReturn(Optional.of(minAttr));
        when(ruleAttributeRepository.findById(maxAttrId)).thenReturn(Optional.of(maxAttr));
    }

    @Test
    @DisplayName("validateContinuity - empty rules list passes")
    void validateContinuity_emptyList_passes() {
        assertDoesNotThrow(() ->
                continuityValidator.validateContinuity(List.of(), BigDecimal.ZERO, BigDecimal.TEN, null));
    }

    @Test
    @DisplayName("validateContinuity - excludeRuleId filters correctly")
    void validateContinuity_excludeRuleId_passes() {
        UUID ruleId = UUID.randomUUID();
        RuleEntity rule = buildRule(ruleId);

        assertDoesNotThrow(() ->
                continuityValidator.validateContinuity(List.of(rule), BigDecimal.ZERO, BigDecimal.TEN, ruleId));
    }

    @Test
    @DisplayName("validateContinuity - gap after existing range throws BadRequestException")
    void validateContinuity_gapAfter_throws() {
        UUID ruleId = UUID.randomUUID();
        RuleEntity existingRule = buildRule(ruleId);
        mockRangeAttributes(ruleId, "0", "50");

        assertThrows(BadRequestException.class, () ->
                continuityValidator.validateContinuity(
                        List.of(existingRule), new BigDecimal("100"), new BigDecimal("200"), null));
    }

    @Test
    @DisplayName("validateContinuity - contiguous ranges pass")
    void validateContinuity_contiguous_passes() {
        UUID ruleId = UUID.randomUUID();
        RuleEntity existingRule = buildRule(ruleId);
        mockRangeAttributes(ruleId, "0", "50");

        assertDoesNotThrow(() ->
                continuityValidator.validateContinuity(
                        List.of(existingRule), new BigDecimal("40"), new BigDecimal("80"), null));
    }
}
