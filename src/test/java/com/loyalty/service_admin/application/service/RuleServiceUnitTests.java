package com.loyalty.service_admin.application.service;

import com.loyalty.service_admin.application.dto.rules.RuleCreateRequest;
import com.loyalty.service_admin.application.dto.rules.RuleResponse;
import com.loyalty.service_admin.application.port.out.RulePersistencePort;
import com.loyalty.service_admin.application.port.out.RuleEventPort;
import com.loyalty.service_admin.application.validation.ContinuityValidator;
import com.loyalty.service_admin.application.validation.HierarchyValidator;
import com.loyalty.service_admin.application.validation.UniquePriorityValidator;
import com.loyalty.service_admin.domain.entity.*;
import com.loyalty.service_admin.domain.repository.*;
import com.loyalty.service_admin.infrastructure.exception.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("RuleService Unit Tests")
class RuleServiceUnitTests {

    @Mock private RulePersistencePort rulePersistencePort;
    @Mock private RuleEventPort ruleEventPort;
    @Mock private RuleRepository ruleRepository;
    @Mock private RuleAttributeRepository ruleAttributeRepository;
    @Mock private RuleAttributeValueRepository ruleAttributeValueRepository;
    @Mock private DiscountLimitPriorityRepository discountLimitPriorityRepository;
    @Mock private RuleCustomerTierRepository ruleCustomerTierRepository;
    @Mock private CustomerTierRepository customerTierRepository;
    @Mock private DiscountTypeRepository discountTypeRepository;
    @Mock private DiscountConfigRepository discountConfigRepository;
    @Mock private ContinuityValidator continuityValidator;
    @Mock private HierarchyValidator hierarchyValidator;
    @Mock private UniquePriorityValidator uniquePriorityValidator;

    @InjectMocks
    private RuleService ruleService;

    private UUID ecommerceId;
    private UUID ruleId;
    private UUID priorityId;
    private RuleEntity ruleEntity;

    @BeforeEach
    void setUp() {
        ecommerceId = UUID.randomUUID();
        ruleId = UUID.randomUUID();
        priorityId = UUID.randomUUID();

        ruleEntity = RuleEntity.builder()
                .id(ruleId)
                .ecommerceId(ecommerceId)
                .discountPriorityId(priorityId)
                .name("Test Rule")
                .description("A test rule")
                .discountPercentage(new BigDecimal("10.00"))
                .isActive(true)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
    }

    @Test
    @DisplayName("testServiceUsesPortInjection")
    void testServiceUsesPortInjection() {
        assertNotNull(ruleService, "Service should be injected with ports");
    }

    @Test
    @DisplayName("testGetRuleById_Success")
    void testGetRuleById_Success() {
        // Arrange
        when(ruleRepository.findByIdAndEcommerceId(ruleId, ecommerceId)).thenReturn(Optional.of(ruleEntity));
        when(ruleAttributeValueRepository.findByRuleIdOrderByCreatedAtAsc(ruleId)).thenReturn(Collections.emptyList());

        // Act
        RuleResponse response = ruleService.getRuleById(ecommerceId, ruleId);

        // Assert
        assertNotNull(response);
        assertEquals(ruleId, response.id());
        assertEquals("Test Rule", response.name());
    }

    @Test
    @DisplayName("testGetRuleById_NotFound_ThrowsResourceNotFoundException")
    void testGetRuleById_NotFound_ThrowsResourceNotFoundException() {
        // Arrange
        when(ruleRepository.findByIdAndEcommerceId(ruleId, ecommerceId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(ResourceNotFoundException.class,
                () -> ruleService.getRuleById(ecommerceId, ruleId));
    }

    @Test
    @DisplayName("testListRules_AllRules")
    void testListRules_AllRules() {
        // Arrange
        Pageable pageable = PageRequest.of(0, 10);
        Page<RuleEntity> page = new PageImpl<>(List.of(ruleEntity));
        when(ruleRepository.findByEcommerceIdOrderByCreatedAtDesc(ecommerceId, pageable)).thenReturn(page);
        when(ruleAttributeValueRepository.findByRuleIdOrderByCreatedAtAsc(ruleId)).thenReturn(Collections.emptyList());

        // Act
        Page<RuleResponse> result = ruleService.listRules(ecommerceId, null, pageable);

        // Assert
        assertEquals(1, result.getContent().size());
    }

    @Test
    @DisplayName("testListRules_ActiveOnly")
    void testListRules_ActiveOnly() {
        // Arrange
        Pageable pageable = PageRequest.of(0, 10);
        Page<RuleEntity> page = new PageImpl<>(List.of(ruleEntity));
        when(ruleRepository.findByEcommerceIdAndIsActiveTrueOrderByCreatedAtDesc(ecommerceId, pageable))
                .thenReturn(page);
        when(ruleAttributeValueRepository.findByRuleIdOrderByCreatedAtAsc(ruleId)).thenReturn(Collections.emptyList());

        // Act
        Page<RuleResponse> result = ruleService.listRules(ecommerceId, true, pageable);

        // Assert
        assertEquals(1, result.getContent().size());
        verify(ruleRepository).findByEcommerceIdAndIsActiveTrueOrderByCreatedAtDesc(ecommerceId, pageable);
    }

    @Test
    @DisplayName("testDeleteRule_Success")
    void testDeleteRule_Success() {
        // Arrange
        when(ruleRepository.findByIdAndEcommerceId(ruleId, ecommerceId)).thenReturn(Optional.of(ruleEntity));
        when(ruleRepository.save(ruleEntity)).thenReturn(ruleEntity);
        when(discountLimitPriorityRepository.findById(priorityId)).thenReturn(Optional.empty());
        when(ruleAttributeValueRepository.findByRuleIdOrderByCreatedAtAsc(ruleId)).thenReturn(Collections.emptyList());

        // Act
        ruleService.deleteRule(ecommerceId, ruleId);

        // Assert
        assertFalse(ruleEntity.getIsActive());
        verify(ruleRepository).save(ruleEntity);
    }

    @Test
    @DisplayName("testDeleteRule_NotFound_ThrowsResourceNotFoundException")
    void testDeleteRule_NotFound_ThrowsResourceNotFoundException() {
        // Arrange
        when(ruleRepository.findByIdAndEcommerceId(ruleId, ecommerceId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(ResourceNotFoundException.class,
                () -> ruleService.deleteRule(ecommerceId, ruleId));
    }

    @Test
    @DisplayName("testUpdateRuleStatus_Activate")
    void testUpdateRuleStatus_Activate() {
        // Arrange
        ruleEntity.setIsActive(false);
        when(ruleRepository.findByIdAndEcommerceId(ruleId, ecommerceId)).thenReturn(Optional.of(ruleEntity));
        when(ruleRepository.save(ruleEntity)).thenReturn(ruleEntity);
        when(discountLimitPriorityRepository.findById(priorityId)).thenReturn(Optional.empty());
        when(ruleAttributeValueRepository.findByRuleIdOrderByCreatedAtAsc(ruleId)).thenReturn(Collections.emptyList());

        // Act
        RuleResponse response = ruleService.updateRuleStatus(ecommerceId, ruleId, true);

        // Assert
        assertNotNull(response);
        assertTrue(ruleEntity.getIsActive());
    }

    @Test
    @DisplayName("testUpdateRuleStatus_Deactivate")
    void testUpdateRuleStatus_Deactivate() {
        // Arrange
        ruleEntity.setIsActive(true);
        when(ruleRepository.findByIdAndEcommerceId(ruleId, ecommerceId)).thenReturn(Optional.of(ruleEntity));
        when(ruleRepository.save(ruleEntity)).thenReturn(ruleEntity);
        when(discountLimitPriorityRepository.findById(priorityId)).thenReturn(Optional.empty());
        when(ruleAttributeValueRepository.findByRuleIdOrderByCreatedAtAsc(ruleId)).thenReturn(Collections.emptyList());

        // Act
        RuleResponse response = ruleService.updateRuleStatus(ecommerceId, ruleId, false);

        // Assert
        assertNotNull(response);
        assertFalse(ruleEntity.getIsActive());
    }

    @Test
    @DisplayName("testUpdateRuleStatus_NotFound_ThrowsResourceNotFoundException")
    void testUpdateRuleStatus_NotFound_ThrowsResourceNotFoundException() {
        // Arrange
        when(ruleRepository.findByIdAndEcommerceId(ruleId, ecommerceId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(ResourceNotFoundException.class,
                () -> ruleService.updateRuleStatus(ecommerceId, ruleId, true));
    }

    @Test
    @DisplayName("testUpdateRuleStatus_EventPublishingFailure_DoesNotRollback")
    void testUpdateRuleStatus_EventPublishingFailure_DoesNotRollback() {
        // Arrange
        when(ruleRepository.findByIdAndEcommerceId(ruleId, ecommerceId)).thenReturn(Optional.of(ruleEntity));
        when(ruleRepository.save(ruleEntity)).thenReturn(ruleEntity);
        when(discountLimitPriorityRepository.findById(priorityId)).thenReturn(Optional.empty());
        when(ruleAttributeValueRepository.findByRuleIdOrderByCreatedAtAsc(ruleId)).thenReturn(Collections.emptyList());
        doThrow(new RuntimeException("RabbitMQ unavailable"))
                .when(ruleEventPort).publishRuleDeactivated(any(), any(), any());

        // Act & Assert - should not throw
        assertDoesNotThrow(() -> ruleService.updateRuleStatus(ecommerceId, ruleId, false));
        verify(ruleRepository).save(ruleEntity);
    }
}
