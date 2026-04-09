package com.loyalty.service_admin.presentation.controller;

import com.loyalty.service_admin.application.dto.classificationrule.*;
import com.loyalty.service_admin.application.dto.discount.DiscountPriorityDTO;
import com.loyalty.service_admin.application.dto.discount.DiscountTypeDTO;
import com.loyalty.service_admin.application.dto.rules.*;
import com.loyalty.service_admin.application.port.in.RuleUseCase;
import com.loyalty.service_admin.infrastructure.exception.AuthorizationException;
import com.loyalty.service_admin.infrastructure.security.SecurityContextHelper;
import com.loyalty.service_admin.presentation.dto.rules.RuleStatusUpdateRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("RuleController Unit Tests")
class RuleControllerTest {

    @Mock
    private RuleUseCase ruleUseCase;
    @Mock
    private SecurityContextHelper securityContextHelper;

    @InjectMocks
    private RuleController ruleController;

    private final UUID ecommerceId = UUID.randomUUID();

    private void mockEcommerceId() {
        when(securityContextHelper.getCurrentUserEcommerceId()).thenReturn(ecommerceId);
    }

    // ===== METADATA =====

    @Test
    @DisplayName("listDiscountTypes returns 200")
    void listDiscountTypes_returns200() {
        DiscountTypeDTO dto = mock(DiscountTypeDTO.class);
        when(ruleUseCase.getAllDiscountTypes()).thenReturn(List.of(dto));

        ResponseEntity<List<DiscountTypeDTO>> result = ruleController.listDiscountTypes();

        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertEquals(1, result.getBody().size());
    }

    @Test
    @DisplayName("getAvailableAttributes returns 200")
    void getAvailableAttributes_returns200() {
        UUID discountTypeId = UUID.randomUUID();
        RuleAttributeMetadataDTO dto = mock(RuleAttributeMetadataDTO.class);
        when(ruleUseCase.getAvailableAttributesForDiscountType(discountTypeId)).thenReturn(List.of(dto));

        ResponseEntity<List<RuleAttributeMetadataDTO>> result = ruleController.getAvailableAttributes(discountTypeId);

        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertEquals(1, result.getBody().size());
    }

    @Test
    @DisplayName("getDiscountPrioritiesByType returns 200")
    void getDiscountPrioritiesByType_returns200() {
        UUID discountTypeId = UUID.randomUUID();
        DiscountPriorityDTO dto = mock(DiscountPriorityDTO.class);
        when(ruleUseCase.getDiscountPrioritiesByType(discountTypeId)).thenReturn(List.of(dto));

        ResponseEntity<List<DiscountPriorityDTO>> result = ruleController.getDiscountPrioritiesByType(discountTypeId);

        assertEquals(HttpStatus.OK, result.getStatusCode());
    }

    // ===== CRUD RULES =====

    @Test
    @DisplayName("createRule returns 201 Created")
    void createRule_returns201() {
        mockEcommerceId();
        RuleCreateRequest request = mock(RuleCreateRequest.class);
        RuleResponse response = mock(RuleResponse.class);
        when(ruleUseCase.createRule(eq(ecommerceId), any())).thenReturn(response);

        ResponseEntity<RuleResponse> result = ruleController.createRule(request);

        assertEquals(HttpStatus.CREATED, result.getStatusCode());
        assertEquals(response, result.getBody());
    }

    @Test
    @DisplayName("createRule with null ecommerceId throws AuthorizationException")
    void createRule_nullEcommerce_throws() {
        when(securityContextHelper.getCurrentUserEcommerceId()).thenReturn(null);
        RuleCreateRequest request = mock(RuleCreateRequest.class);

        assertThrows(AuthorizationException.class, () -> ruleController.createRule(request));
    }

    @Test
    @DisplayName("listRules returns 200 with paginated results")
    void listRules_returns200() {
        mockEcommerceId();
        Page<RuleResponse> page = new PageImpl<>(List.of(mock(RuleResponse.class)));
        when(ruleUseCase.listRules(eq(ecommerceId), eq(true), any())).thenReturn(page);

        ResponseEntity<Page<RuleResponse>> result = ruleController.listRules(0, 20, true);

        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertEquals(1, result.getBody().getTotalElements());
    }

    @Test
    @DisplayName("getRuleById returns 200 with tiers")
    void getRuleById_returns200() {
        mockEcommerceId();
        UUID ruleId = UUID.randomUUID();
        RuleResponse baseResponse = mock(RuleResponse.class);
        when(baseResponse.id()).thenReturn(ruleId);
        when(baseResponse.ecommerceId()).thenReturn(ecommerceId);
        when(baseResponse.discountPriorityId()).thenReturn(UUID.randomUUID());
        when(baseResponse.name()).thenReturn("Test Rule");
        when(baseResponse.description()).thenReturn("desc");
        when(baseResponse.discountPercentage()).thenReturn(java.math.BigDecimal.TEN);
        when(baseResponse.isActive()).thenReturn(true);
        when(baseResponse.attributes()).thenReturn(List.of());
        when(baseResponse.createdAt()).thenReturn(java.time.Instant.now());
        when(baseResponse.updatedAt()).thenReturn(java.time.Instant.now());

        when(ruleUseCase.getRuleById(ecommerceId, ruleId)).thenReturn(baseResponse);
        when(ruleUseCase.getRuleAssignedTiers(ecommerceId, ruleId)).thenReturn(List.of());

        ResponseEntity<RuleResponseWithTiers> result = ruleController.getRuleById(ruleId);

        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertEquals("Test Rule", result.getBody().name());
    }

    @Test
    @DisplayName("updateRule returns 200")
    void updateRule_returns200() {
        mockEcommerceId();
        UUID ruleId = UUID.randomUUID();
        RuleCreateRequest request = mock(RuleCreateRequest.class);
        RuleResponse response = mock(RuleResponse.class);
        when(ruleUseCase.updateRule(eq(ecommerceId), eq(ruleId), any())).thenReturn(response);

        ResponseEntity<RuleResponse> result = ruleController.updateRule(ruleId, request);

        assertEquals(HttpStatus.OK, result.getStatusCode());
    }

    @Test
    @DisplayName("updateRule with null ecommerceId throws AuthorizationException")
    void updateRule_nullEcommerce_throws() {
        when(securityContextHelper.getCurrentUserEcommerceId()).thenReturn(null);
        assertThrows(AuthorizationException.class,
                () -> ruleController.updateRule(UUID.randomUUID(), mock(RuleCreateRequest.class)));
    }

    @Test
    @DisplayName("deleteRule returns 204")
    void deleteRule_returns204() {
        mockEcommerceId();
        UUID ruleId = UUID.randomUUID();
        doNothing().when(ruleUseCase).deleteRule(ecommerceId, ruleId);

        ResponseEntity<Void> result = ruleController.deleteRule(ruleId);

        assertEquals(HttpStatus.NO_CONTENT, result.getStatusCode());
    }

    @Test
    @DisplayName("deleteRule with null ecommerceId throws AuthorizationException")
    void deleteRule_nullEcommerce_throws() {
        when(securityContextHelper.getCurrentUserEcommerceId()).thenReturn(null);
        assertThrows(AuthorizationException.class,
                () -> ruleController.deleteRule(UUID.randomUUID()));
    }

    @Test
    @DisplayName("updateRuleStatus returns 200")
    void updateRuleStatus_returns200() {
        mockEcommerceId();
        UUID ruleId = UUID.randomUUID();
        RuleStatusUpdateRequest request = new RuleStatusUpdateRequest(true);
        RuleResponse response = mock(RuleResponse.class);
        when(ruleUseCase.updateRuleStatus(ecommerceId, ruleId, true)).thenReturn(response);

        ResponseEntity<RuleResponse> result = ruleController.updateRuleStatus(ruleId, request);

        assertEquals(HttpStatus.OK, result.getStatusCode());
    }

    @Test
    @DisplayName("updateRuleStatus with null ecommerceId throws AuthorizationException")
    void updateRuleStatus_nullEcommerce_throws() {
        when(securityContextHelper.getCurrentUserEcommerceId()).thenReturn(null);
        assertThrows(AuthorizationException.class,
                () -> ruleController.updateRuleStatus(UUID.randomUUID(), new RuleStatusUpdateRequest(true)));
    }

    // ===== TIERS =====

    @Test
    @DisplayName("assignCustomerTiersToRule returns 201")
    void assignCustomerTiersToRule_returns201() {
        mockEcommerceId();
        UUID ruleId = UUID.randomUUID();
        List<UUID> tierIds = List.of(UUID.randomUUID());
        AssignCustomerTiersRequest request = new AssignCustomerTiersRequest(tierIds);
        RuleResponseWithTiers response = mock(RuleResponseWithTiers.class);
        when(ruleUseCase.assignCustomerTiersToRule(eq(ecommerceId), eq(ruleId), eq(tierIds))).thenReturn(response);

        ResponseEntity<RuleResponseWithTiers> result = ruleController.assignCustomerTiersToRule(ruleId, request);

        assertEquals(HttpStatus.CREATED, result.getStatusCode());
    }

    @Test
    @DisplayName("assignCustomerTiersToRule with null ecommerceId throws")
    void assignTiers_nullEcommerce_throws() {
        when(securityContextHelper.getCurrentUserEcommerceId()).thenReturn(null);
        assertThrows(AuthorizationException.class,
                () -> ruleController.assignCustomerTiersToRule(UUID.randomUUID(),
                        new AssignCustomerTiersRequest(List.of())));
    }

    @Test
    @DisplayName("getRuleAssignedTiers returns 200")
    void getRuleAssignedTiers_returns200() {
        mockEcommerceId();
        UUID ruleId = UUID.randomUUID();
        RuleCustomerTierDTO dto = mock(RuleCustomerTierDTO.class);
        when(ruleUseCase.getRuleAssignedTiers(ecommerceId, ruleId)).thenReturn(List.of(dto));

        ResponseEntity<List<RuleCustomerTierDTO>> result = ruleController.getRuleAssignedTiers(ruleId);

        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertEquals(1, result.getBody().size());
    }

    @Test
    @DisplayName("deleteCustomerTierFromRule returns 204")
    void deleteCustomerTierFromRule_returns204() {
        mockEcommerceId();
        UUID ruleId = UUID.randomUUID();
        UUID tierId = UUID.randomUUID();
        doNothing().when(ruleUseCase).deleteCustomerTierFromRule(ecommerceId, ruleId, tierId);

        ResponseEntity<Void> result = ruleController.deleteCustomerTierFromRule(ruleId, tierId);

        assertEquals(HttpStatus.NO_CONTENT, result.getStatusCode());
    }

    @Test
    @DisplayName("deleteCustomerTierFromRule with null ecommerceId throws")
    void deleteTier_nullEcommerce_throws() {
        when(securityContextHelper.getCurrentUserEcommerceId()).thenReturn(null);
        assertThrows(AuthorizationException.class,
                () -> ruleController.deleteCustomerTierFromRule(UUID.randomUUID(), UUID.randomUUID()));
    }

    // ===== CLASSIFICATION RULES =====

    @Test
    @DisplayName("createClassificationRuleForTier returns 201")
    void createClassificationRuleForTier_returns201() {
        mockEcommerceId();
        UUID tierId = UUID.randomUUID();
        ClassificationRuleCreateRequest request = mock(ClassificationRuleCreateRequest.class);
        ClassificationRuleResponse response = mock(ClassificationRuleResponse.class);
        when(ruleUseCase.createClassificationRuleForTier(eq(ecommerceId), eq(tierId), any())).thenReturn(response);

        ResponseEntity<ClassificationRuleResponse> result =
                ruleController.createClassificationRuleForTier(tierId, request);

        assertEquals(HttpStatus.CREATED, result.getStatusCode());
    }

    @Test
    @DisplayName("createClassificationRuleForTier with null ecommerceId throws")
    void createClassificationRule_nullEcommerce_throws() {
        when(securityContextHelper.getCurrentUserEcommerceId()).thenReturn(null);
        assertThrows(AuthorizationException.class,
                () -> ruleController.createClassificationRuleForTier(UUID.randomUUID(),
                        mock(ClassificationRuleCreateRequest.class)));
    }

    @Test
    @DisplayName("listClassificationRulesForTier returns 200")
    void listClassificationRulesForTier_returns200() {
        mockEcommerceId();
        UUID tierId = UUID.randomUUID();
        ClassificationRuleResponse response = mock(ClassificationRuleResponse.class);
        when(ruleUseCase.listClassificationRulesForTier(ecommerceId, tierId)).thenReturn(List.of(response));

        ResponseEntity<List<ClassificationRuleResponse>> result =
                ruleController.listClassificationRulesForTier(tierId);

        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertEquals(1, result.getBody().size());
    }

    @Test
    @DisplayName("listClassificationRulesForTier with null ecommerceId throws")
    void listClassificationRules_nullEcommerce_throws() {
        when(securityContextHelper.getCurrentUserEcommerceId()).thenReturn(null);
        assertThrows(AuthorizationException.class,
                () -> ruleController.listClassificationRulesForTier(UUID.randomUUID()));
    }

    @Test
    @DisplayName("updateClassificationRuleForTier returns 200")
    void updateClassificationRuleForTier_returns200() {
        mockEcommerceId();
        UUID tierId = UUID.randomUUID();
        UUID ruleId = UUID.randomUUID();
        ClassificationRuleUpdateRequest request = mock(ClassificationRuleUpdateRequest.class);
        ClassificationRuleResponse response = mock(ClassificationRuleResponse.class);
        when(ruleUseCase.updateClassificationRuleForTier(eq(ecommerceId), eq(tierId), eq(ruleId), any()))
                .thenReturn(response);

        ResponseEntity<ClassificationRuleResponse> result =
                ruleController.updateClassificationRuleForTier(tierId, ruleId, request);

        assertEquals(HttpStatus.OK, result.getStatusCode());
    }

    @Test
    @DisplayName("updateClassificationRuleForTier with null ecommerceId throws")
    void updateClassificationRule_nullEcommerce_throws() {
        when(securityContextHelper.getCurrentUserEcommerceId()).thenReturn(null);
        assertThrows(AuthorizationException.class,
                () -> ruleController.updateClassificationRuleForTier(UUID.randomUUID(), UUID.randomUUID(),
                        mock(ClassificationRuleUpdateRequest.class)));
    }

    @Test
    @DisplayName("deleteClassificationRuleForTier returns 204")
    void deleteClassificationRuleForTier_returns204() {
        mockEcommerceId();
        UUID tierId = UUID.randomUUID();
        UUID ruleId = UUID.randomUUID();
        doNothing().when(ruleUseCase).deleteClassificationRuleForTier(ecommerceId, tierId, ruleId);

        ResponseEntity<Void> result = ruleController.deleteClassificationRuleForTier(tierId, ruleId);

        assertEquals(HttpStatus.NO_CONTENT, result.getStatusCode());
    }

    @Test
    @DisplayName("deleteClassificationRuleForTier with null ecommerceId throws")
    void deleteClassificationRule_nullEcommerce_throws() {
        when(securityContextHelper.getCurrentUserEcommerceId()).thenReturn(null);
        assertThrows(AuthorizationException.class,
                () -> ruleController.deleteClassificationRuleForTier(UUID.randomUUID(), UUID.randomUUID()));
    }
}
