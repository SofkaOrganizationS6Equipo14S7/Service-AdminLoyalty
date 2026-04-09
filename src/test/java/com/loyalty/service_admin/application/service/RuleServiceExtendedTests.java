package com.loyalty.service_admin.application.service;

import com.loyalty.service_admin.application.dto.classificationrule.ClassificationRuleCreateRequest;
import com.loyalty.service_admin.application.dto.classificationrule.ClassificationRuleResponse;
import com.loyalty.service_admin.application.dto.classificationrule.ClassificationRuleUpdateRequest;
import com.loyalty.service_admin.application.dto.discount.DiscountPriorityDTO;
import com.loyalty.service_admin.application.dto.discount.DiscountTypeDTO;
import com.loyalty.service_admin.application.dto.rules.*;
import com.loyalty.service_admin.application.port.out.RuleEventPort;
import com.loyalty.service_admin.application.port.out.RulePersistencePort;
import com.loyalty.service_admin.application.validation.ContinuityValidator;
import com.loyalty.service_admin.application.validation.HierarchyValidator;
import com.loyalty.service_admin.application.validation.UniquePriorityValidator;
import com.loyalty.service_admin.domain.entity.*;
import com.loyalty.service_admin.domain.repository.*;
import com.loyalty.service_admin.infrastructure.exception.BadRequestException;
import com.loyalty.service_admin.infrastructure.exception.ConflictException;
import com.loyalty.service_admin.infrastructure.exception.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
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
@DisplayName("RuleService Extended Tests")
class RuleServiceExtendedTests {

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
    private UUID discountTypeId;
    private RuleEntity ruleEntity;
    private DiscountPriorityEntity priorityEntity;
    private DiscountTypeEntity productType;
    private DiscountSettingsEntity discountSettings;

    @BeforeEach
    void setUp() {
        ecommerceId = UUID.randomUUID();
        ruleId = UUID.randomUUID();
        priorityId = UUID.randomUUID();
        discountTypeId = UUID.randomUUID();

        ruleEntity = RuleEntity.builder()
                .id(ruleId).ecommerceId(ecommerceId).discountPriorityId(priorityId)
                .name("Test Rule").description("desc").discountPercentage(new BigDecimal("10.00"))
                .isActive(true).createdAt(Instant.now()).updatedAt(Instant.now())
                .build();

        priorityEntity = DiscountPriorityEntity.builder()
                .id(priorityId).discountTypeId(discountTypeId).priorityLevel(1)
                .isActive(true).createdAt(Instant.now()).updatedAt(Instant.now())
                .build();

        productType = new DiscountTypeEntity();
        productType.setId(discountTypeId);
        productType.setCode("PRODUCT");
        productType.setDisplayName("Product");
        productType.setCreatedAt(Instant.now());

        discountSettings = DiscountSettingsEntity.builder()
                .id(UUID.randomUUID()).ecommerceId(ecommerceId)
                .maxDiscountCap(new BigDecimal("50.00")).isActive(true)
                .createdAt(Instant.now()).updatedAt(Instant.now())
                .build();
    }

    // ======================== createRule ========================

    @Nested @DisplayName("createRule")
    class CreateRuleTests {

        @Test @DisplayName("success - PRODUCT rule")
        void createRule_product_success() {
            Map<String, String> attrs = Map.of("product_type", "ELECTRONICS");
            RuleCreateRequest req = new RuleCreateRequest("Rule1", "desc", BigDecimal.TEN, priorityId.toString(), attrs);

            when(discountLimitPriorityRepository.findById(priorityId)).thenReturn(Optional.of(priorityEntity));
            when(discountTypeRepository.findById(discountTypeId)).thenReturn(Optional.of(productType));
            when(ruleRepository.findByEcommerceIdAndIsActiveTrueOrderByCreatedAtDesc(eq(ecommerceId), any()))
                    .thenReturn(new PageImpl<>(List.of()));
            when(discountConfigRepository.findActiveByEcommerceId(ecommerceId)).thenReturn(Optional.of(discountSettings));
            when(ruleRepository.save(any(RuleEntity.class))).thenAnswer(inv -> {
                RuleEntity e = inv.getArgument(0);
                e.setId(ruleId);
                return e;
            });
            when(ruleAttributeRepository.findByDiscountTypeIdOrderByAttributeNameAsc(discountTypeId))
                    .thenReturn(List.of(buildAttrEntity("product_type")));
            when(ruleAttributeRepository.findByDiscountTypeIdAndAttributeName(discountTypeId, "product_type"))
                    .thenReturn(Optional.of(buildAttrEntity("product_type")));
            when(ruleAttributeValueRepository.findByRuleIdAndAttributeId(any(), any())).thenReturn(Optional.empty());
            when(ruleAttributeValueRepository.findByRuleIdOrderByCreatedAtAsc(any())).thenReturn(List.of());

            RuleResponse response = ruleService.createRule(ecommerceId, req);

            assertNotNull(response);
            verify(ruleRepository).save(any(RuleEntity.class));
        }

        @Test @DisplayName("priorityNotFound throws")
        void createRule_priorityNotFound() {
            RuleCreateRequest req = new RuleCreateRequest("Rule1", "desc", BigDecimal.TEN, priorityId.toString(), Map.of());
            when(discountLimitPriorityRepository.findById(priorityId)).thenReturn(Optional.empty());

            assertThrows(ResourceNotFoundException.class, () -> ruleService.createRule(ecommerceId, req));
        }

        @Test @DisplayName("discountTypeNotFound throws")
        void createRule_discountTypeNotFound() {
            RuleCreateRequest req = new RuleCreateRequest("Rule1", "desc", BigDecimal.TEN, priorityId.toString(), Map.of());
            when(discountLimitPriorityRepository.findById(priorityId)).thenReturn(Optional.of(priorityEntity));
            when(discountTypeRepository.findById(discountTypeId)).thenReturn(Optional.empty());

            assertThrows(ResourceNotFoundException.class, () -> ruleService.createRule(ecommerceId, req));
        }

        @Test @DisplayName("PRODUCT - missing product_type throws BadRequest")
        void createRule_product_missingProductType() {
            RuleCreateRequest req = new RuleCreateRequest("Rule1", "desc", BigDecimal.TEN, priorityId.toString(), Map.of());
            when(discountLimitPriorityRepository.findById(priorityId)).thenReturn(Optional.of(priorityEntity));
            when(discountTypeRepository.findById(discountTypeId)).thenReturn(Optional.of(productType));

            assertThrows(BadRequestException.class, () -> ruleService.createRule(ecommerceId, req));
        }

        @Test @DisplayName("PRODUCT - duplicate product_type throws Conflict")
        void createRule_product_duplicateProductType() {
            Map<String, String> attrs = Map.of("product_type", "ELECTRONICS");
            RuleCreateRequest req = new RuleCreateRequest("Rule1", "desc", BigDecimal.TEN, priorityId.toString(), attrs);

            UUID existingRuleId = UUID.randomUUID();
            RuleEntity existingRule = RuleEntity.builder().id(existingRuleId).ecommerceId(ecommerceId)
                    .discountPriorityId(priorityId).name("Existing").discountPercentage(BigDecimal.ONE)
                    .isActive(true).createdAt(Instant.now()).updatedAt(Instant.now()).build();

            UUID attrId = UUID.randomUUID();
            RuleAttributeValueEntity attrVal = RuleAttributeValueEntity.builder()
                    .ruleId(existingRuleId).attributeId(attrId).value("ELECTRONICS")
                    .createdAt(Instant.now()).updatedAt(Instant.now()).build();
            RuleAttributeEntity attrDef = RuleAttributeEntity.builder()
                    .id(attrId).attributeName("product_type").build();

            when(discountLimitPriorityRepository.findById(priorityId)).thenReturn(Optional.of(priorityEntity));
            when(discountTypeRepository.findById(discountTypeId)).thenReturn(Optional.of(productType));
            when(ruleRepository.findByEcommerceIdAndIsActiveTrueOrderByCreatedAtDesc(eq(ecommerceId), any()))
                    .thenReturn(new PageImpl<>(List.of(existingRule)));
            when(ruleAttributeValueRepository.findByRuleIdOrderByCreatedAtAsc(existingRuleId))
                    .thenReturn(List.of(attrVal));
            when(ruleAttributeRepository.findById(attrId)).thenReturn(Optional.of(attrDef));

            assertThrows(ConflictException.class, () -> ruleService.createRule(ecommerceId, req));
        }

        @Test @DisplayName("discount exceeds limit throws BadRequest")
        void createRule_discountExceedsLimit() {
            DiscountTypeEntity seasonalType = new DiscountTypeEntity();
            seasonalType.setId(discountTypeId);
            seasonalType.setCode("SEASONAL");
            seasonalType.setCreatedAt(Instant.now());

            Map<String, String> attrs = Map.of("start_date", "2025-01-01", "end_date", "2025-01-31");
            RuleCreateRequest req = new RuleCreateRequest("Rule1", "desc", new BigDecimal("99.00"), priorityId.toString(), attrs);

            when(discountLimitPriorityRepository.findById(priorityId)).thenReturn(Optional.of(priorityEntity));
            when(discountTypeRepository.findById(discountTypeId)).thenReturn(Optional.of(seasonalType));
            when(ruleRepository.findByEcommerceIdAndIsActiveTrueOrderByCreatedAtDesc(eq(ecommerceId), any()))
                    .thenReturn(new PageImpl<>(List.of()));
            when(discountConfigRepository.findActiveByEcommerceId(ecommerceId)).thenReturn(Optional.of(discountSettings));

            assertThrows(BadRequestException.class, () -> ruleService.createRule(ecommerceId, req));
        }

        @Test @DisplayName("SEASONAL - date overlap throws Conflict")
        void createRule_seasonal_dateOverlap() {
            DiscountTypeEntity seasonalType = new DiscountTypeEntity();
            seasonalType.setId(discountTypeId);
            seasonalType.setCode("SEASONAL");
            seasonalType.setCreatedAt(Instant.now());

            Map<String, String> attrs = Map.of("start_date", "2025-01-10", "end_date", "2025-01-20");
            RuleCreateRequest req = new RuleCreateRequest("Rule1", "desc", BigDecimal.TEN, priorityId.toString(), attrs);

            UUID existingRuleId = UUID.randomUUID();
            RuleEntity existingRule = RuleEntity.builder().id(existingRuleId).ecommerceId(ecommerceId)
                    .discountPriorityId(priorityId).name("Ex").discountPercentage(BigDecimal.ONE)
                    .isActive(true).createdAt(Instant.now()).updatedAt(Instant.now()).build();

            UUID startAttrId = UUID.randomUUID();
            UUID endAttrId = UUID.randomUUID();
            RuleAttributeValueEntity startVal = RuleAttributeValueEntity.builder()
                    .ruleId(existingRuleId).attributeId(startAttrId).value("2025-01-05")
                    .createdAt(Instant.now()).updatedAt(Instant.now()).build();
            RuleAttributeValueEntity endVal = RuleAttributeValueEntity.builder()
                    .ruleId(existingRuleId).attributeId(endAttrId).value("2025-01-15")
                    .createdAt(Instant.now()).updatedAt(Instant.now()).build();
            RuleAttributeEntity startAttr = RuleAttributeEntity.builder().id(startAttrId).attributeName("start_date").build();
            RuleAttributeEntity endAttr = RuleAttributeEntity.builder().id(endAttrId).attributeName("end_date").build();

            when(discountLimitPriorityRepository.findById(priorityId)).thenReturn(Optional.of(priorityEntity));
            when(discountTypeRepository.findById(discountTypeId)).thenReturn(Optional.of(seasonalType));
            // For validateSeasonalDateOverlap → findActiveSeasonalRulesByEcommerce
            when(ruleRepository.findByEcommerceIdAndIsActiveTrueOrderByCreatedAtDesc(eq(ecommerceId), any()))
                    .thenReturn(new PageImpl<>(List.of(existingRule)));
            when(discountLimitPriorityRepository.findById(existingRule.getDiscountPriorityId()))
                    .thenReturn(Optional.of(priorityEntity));
            when(discountTypeRepository.findById(discountTypeId)).thenReturn(Optional.of(seasonalType));
            // For getAttributeDate
            when(ruleAttributeValueRepository.findByRuleIdOrderByCreatedAtAsc(existingRuleId))
                    .thenReturn(List.of(startVal, endVal));
            when(ruleAttributeRepository.findById(startAttrId)).thenReturn(Optional.of(startAttr));
            when(ruleAttributeRepository.findById(endAttrId)).thenReturn(Optional.of(endAttr));

            assertThrows(ConflictException.class, () -> ruleService.createRule(ecommerceId, req));
        }

        @Test @DisplayName("SEASONAL - start after end throws BadRequest")
        void createRule_seasonal_startAfterEnd() {
            DiscountTypeEntity seasonalType = new DiscountTypeEntity();
            seasonalType.setId(discountTypeId);
            seasonalType.setCode("SEASONAL");
            seasonalType.setCreatedAt(Instant.now());

            Map<String, String> attrs = Map.of("start_date", "2025-02-01", "end_date", "2025-01-01");
            RuleCreateRequest req = new RuleCreateRequest("Rule1", "desc", BigDecimal.TEN, priorityId.toString(), attrs);

            when(discountLimitPriorityRepository.findById(priorityId)).thenReturn(Optional.of(priorityEntity));
            when(discountTypeRepository.findById(discountTypeId)).thenReturn(Optional.of(seasonalType));

            assertThrows(BadRequestException.class, () -> ruleService.createRule(ecommerceId, req));
        }
    }

    // ======================== updateRule ========================

    @Nested @DisplayName("updateRule")
    class UpdateRuleTests {

        @Test @DisplayName("success - updates rule fields")
        void updateRule_success() {
            DiscountTypeEntity classType = new DiscountTypeEntity();
            classType.setId(discountTypeId);
            classType.setCode("CLASSIFICATION");
            classType.setCreatedAt(Instant.now());

            Map<String, String> attrs = Map.of("metricType", "TOTAL_SPENT", "minValue", "0", "maxValue", "100", "priority", "1");
            RuleCreateRequest req = new RuleCreateRequest("Updated", "updated desc", new BigDecimal("15"), priorityId.toString(), attrs);

            when(ruleRepository.findByIdAndEcommerceId(ruleId, ecommerceId)).thenReturn(Optional.of(ruleEntity));
            when(discountLimitPriorityRepository.findById(priorityId)).thenReturn(Optional.of(priorityEntity));
            when(discountTypeRepository.findById(discountTypeId)).thenReturn(Optional.of(classType));
            when(discountConfigRepository.findActiveByEcommerceId(ecommerceId)).thenReturn(Optional.of(discountSettings));
            when(ruleRepository.save(any(RuleEntity.class))).thenReturn(ruleEntity);
            when(ruleAttributeRepository.findByDiscountTypeIdOrderByAttributeNameAsc(discountTypeId))
                    .thenReturn(List.of(
                            buildAttrEntity("metricType"), buildAttrEntity("minValue"),
                            buildAttrEntity("maxValue"), buildAttrEntity("priority")));
            when(ruleAttributeRepository.findByDiscountTypeIdAndAttributeName(eq(discountTypeId), anyString()))
                    .thenAnswer(inv -> Optional.of(buildAttrEntity(inv.getArgument(1))));
            when(ruleAttributeValueRepository.findByRuleIdAndAttributeId(any(), any())).thenReturn(Optional.empty());
            when(ruleAttributeValueRepository.findByRuleIdOrderByCreatedAtAsc(any())).thenReturn(List.of());

            RuleResponse result = ruleService.updateRule(ecommerceId, ruleId, req);

            assertNotNull(result);
            assertEquals("Updated", ruleEntity.getName());
        }

        @Test @DisplayName("ruleNotFound throws")
        void updateRule_notFound() {
            RuleCreateRequest req = new RuleCreateRequest("R", "d", BigDecimal.ONE, priorityId.toString(), Map.of());
            when(ruleRepository.findByIdAndEcommerceId(ruleId, ecommerceId)).thenReturn(Optional.empty());

            assertThrows(ResourceNotFoundException.class, () -> ruleService.updateRule(ecommerceId, ruleId, req));
        }

        @Test @DisplayName("PRODUCT - changed product_type duplicate throws Conflict")
        void updateRule_product_duplicateProductType() {
            Map<String, String> attrs = Map.of("product_type", "SHOES");
            RuleCreateRequest req = new RuleCreateRequest("Updated", "desc", BigDecimal.TEN, priorityId.toString(), attrs);

            UUID currentAttrId = UUID.randomUUID();
            RuleAttributeValueEntity currentVal = RuleAttributeValueEntity.builder()
                    .ruleId(ruleId).attributeId(currentAttrId).value("ORIGINAL")
                    .createdAt(Instant.now()).updatedAt(Instant.now()).build();
            RuleAttributeEntity currentAttrDef = RuleAttributeEntity.builder()
                    .id(currentAttrId).attributeName("product_type").build();

            UUID otherRuleId = UUID.randomUUID();
            RuleEntity otherRule = RuleEntity.builder().id(otherRuleId).ecommerceId(ecommerceId)
                    .discountPriorityId(priorityId).name("Other").discountPercentage(BigDecimal.ONE)
                    .isActive(true).createdAt(Instant.now()).updatedAt(Instant.now()).build();

            UUID otherAttrId = UUID.randomUUID();
            RuleAttributeValueEntity otherVal = RuleAttributeValueEntity.builder()
                    .ruleId(otherRuleId).attributeId(otherAttrId).value("SHOES")
                    .createdAt(Instant.now()).updatedAt(Instant.now()).build();
            RuleAttributeEntity otherAttrDef = RuleAttributeEntity.builder()
                    .id(otherAttrId).attributeName("product_type").build();

            when(ruleRepository.findByIdAndEcommerceId(ruleId, ecommerceId)).thenReturn(Optional.of(ruleEntity));
            when(discountLimitPriorityRepository.findById(priorityId)).thenReturn(Optional.of(priorityEntity));
            when(discountTypeRepository.findById(discountTypeId)).thenReturn(Optional.of(productType));
            when(ruleAttributeValueRepository.findByRuleIdOrderByCreatedAtAsc(ruleId)).thenReturn(List.of(currentVal));
            when(ruleAttributeRepository.findById(currentAttrId)).thenReturn(Optional.of(currentAttrDef));
            when(ruleRepository.findByEcommerceIdAndIsActiveTrueOrderByCreatedAtDesc(eq(ecommerceId), any()))
                    .thenReturn(new PageImpl<>(List.of(otherRule)));
            when(ruleAttributeValueRepository.findByRuleIdOrderByCreatedAtAsc(otherRuleId)).thenReturn(List.of(otherVal));
            when(ruleAttributeRepository.findById(otherAttrId)).thenReturn(Optional.of(otherAttrDef));

            assertThrows(ConflictException.class, () -> ruleService.updateRule(ecommerceId, ruleId, req));
        }
    }

    // ======================== assignCustomerTiersToRule ========================

    @Nested @DisplayName("assignCustomerTiersToRule")
    class AssignTiersTests {

        @Test @DisplayName("success")
        void assignTiers_success() {
            UUID tierId = UUID.randomUUID();
            CustomerTierEntity tier = CustomerTierEntity.builder()
                    .id(tierId).ecommerceId(ecommerceId).name("Gold").hierarchyLevel(1)
                    .isActive(true).createdAt(Instant.now()).updatedAt(Instant.now()).build();

            when(ruleRepository.findByIdAndEcommerceId(ruleId, ecommerceId)).thenReturn(Optional.of(ruleEntity));
            when(customerTierRepository.findById(tierId)).thenReturn(Optional.of(tier));
            when(ruleCustomerTierRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(ruleAttributeValueRepository.findByRuleIdOrderByCreatedAtAsc(ruleId)).thenReturn(List.of());
            when(ruleCustomerTierRepository.findByRuleId(ruleId)).thenReturn(List.of(
                    RuleCustomerTierEntity.builder().rule(ruleEntity).customerTier(tier).build()));

            RuleResponseWithTiers result = ruleService.assignCustomerTiersToRule(ecommerceId, ruleId, List.of(tierId));

            assertNotNull(result);
            verify(ruleCustomerTierRepository).deleteByRuleId(ruleId);
        }

        @Test @DisplayName("ruleNotFound throws")
        void assignTiers_ruleNotFound() {
            when(ruleRepository.findByIdAndEcommerceId(ruleId, ecommerceId)).thenReturn(Optional.empty());
            assertThrows(ResourceNotFoundException.class, () ->
                    ruleService.assignCustomerTiersToRule(ecommerceId, ruleId, List.of(UUID.randomUUID())));
        }

        @Test @DisplayName("tierNotFound throws")
        void assignTiers_tierNotFound() {
            UUID tierId = UUID.randomUUID();
            when(ruleRepository.findByIdAndEcommerceId(ruleId, ecommerceId)).thenReturn(Optional.of(ruleEntity));
            when(customerTierRepository.findById(tierId)).thenReturn(Optional.empty());

            assertThrows(ResourceNotFoundException.class, () ->
                    ruleService.assignCustomerTiersToRule(ecommerceId, ruleId, List.of(tierId)));
        }
    }

    // ======================== getRuleAssignedTiers ========================

    @Test @DisplayName("getRuleAssignedTiers - success")
    void getRuleAssignedTiers_success() {
        UUID tierId = UUID.randomUUID();
        CustomerTierEntity tier = CustomerTierEntity.builder()
                .id(tierId).name("Gold").build();
        RuleCustomerTierEntity mapping = RuleCustomerTierEntity.builder()
                .rule(ruleEntity).customerTier(tier).build();

        when(ruleRepository.findByIdAndEcommerceId(ruleId, ecommerceId)).thenReturn(Optional.of(ruleEntity));
        when(ruleCustomerTierRepository.findByRuleId(ruleId)).thenReturn(List.of(mapping));

        List<RuleCustomerTierDTO> result = ruleService.getRuleAssignedTiers(ecommerceId, ruleId);

        assertEquals(1, result.size());
        assertEquals("Gold", result.get(0).customerTierName());
    }

    @Test @DisplayName("getRuleAssignedTiers - ruleNotFound throws")
    void getRuleAssignedTiers_notFound() {
        when(ruleRepository.findByIdAndEcommerceId(ruleId, ecommerceId)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () ->
                ruleService.getRuleAssignedTiers(ecommerceId, ruleId));
    }

    // ======================== deleteCustomerTierFromRule ========================

    @Test @DisplayName("deleteCustomerTierFromRule - success")
    void deleteTierFromRule_success() {
        UUID tierId = UUID.randomUUID();
        when(ruleRepository.findByIdAndEcommerceId(ruleId, ecommerceId)).thenReturn(Optional.of(ruleEntity));
        when(ruleCustomerTierRepository.existsByRuleIdAndCustomerTierId(ruleId, tierId)).thenReturn(true);

        ruleService.deleteCustomerTierFromRule(ecommerceId, ruleId, tierId);

        verify(ruleCustomerTierRepository).deleteByRuleIdAndCustomerTierId(ruleId, tierId);
    }

    @Test @DisplayName("deleteCustomerTierFromRule - not assigned throws")
    void deleteTierFromRule_notAssigned() {
        UUID tierId = UUID.randomUUID();
        when(ruleRepository.findByIdAndEcommerceId(ruleId, ecommerceId)).thenReturn(Optional.of(ruleEntity));
        when(ruleCustomerTierRepository.existsByRuleIdAndCustomerTierId(ruleId, tierId)).thenReturn(false);

        assertThrows(ResourceNotFoundException.class, () ->
                ruleService.deleteCustomerTierFromRule(ecommerceId, ruleId, tierId));
    }

    // ======================== getAllDiscountTypes ========================

    @Test @DisplayName("getAllDiscountTypes - returns all")
    void getAllDiscountTypes_success() {
        DiscountTypeEntity dt1 = new DiscountTypeEntity();
        dt1.setId(UUID.randomUUID()); dt1.setCode("PRODUCT"); dt1.setDisplayName("Product"); dt1.setCreatedAt(Instant.now());
        DiscountTypeEntity dt2 = new DiscountTypeEntity();
        dt2.setId(UUID.randomUUID()); dt2.setCode("SEASONAL"); dt2.setDisplayName("Seasonal"); dt2.setCreatedAt(Instant.now());

        when(discountTypeRepository.findAll()).thenReturn(List.of(dt1, dt2));

        List<DiscountTypeDTO> result = ruleService.getAllDiscountTypes();

        assertEquals(2, result.size());
    }

    // ======================== getDiscountPrioritiesByType ========================

    @Test @DisplayName("getDiscountPrioritiesByType - success")
    void getDiscountPrioritiesByType_success() {
        when(discountTypeRepository.findById(discountTypeId)).thenReturn(Optional.of(productType));
        when(discountLimitPriorityRepository.findAll()).thenReturn(List.of(priorityEntity));

        List<DiscountPriorityDTO> result = ruleService.getDiscountPrioritiesByType(discountTypeId);

        assertEquals(1, result.size());
    }

    @Test @DisplayName("getDiscountPrioritiesByType - typeNotFound throws")
    void getDiscountPrioritiesByType_notFound() {
        when(discountTypeRepository.findById(discountTypeId)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () ->
                ruleService.getDiscountPrioritiesByType(discountTypeId));
    }

    // ======================== getAvailableAttributesForDiscountType ========================

    @Test @DisplayName("getAvailableAttributesForDiscountType returns list")
    void getAvailableAttributes_success() {
        RuleAttributeEntity attr = RuleAttributeEntity.builder()
                .id(UUID.randomUUID()).attributeName("product_type").attributeType("STRING")
                .isRequired(true).description("Type of product").build();

        when(ruleAttributeRepository.findByDiscountTypeIdOrderByAttributeNameAsc(discountTypeId))
                .thenReturn(List.of(attr));

        List<RuleAttributeMetadataDTO> result = ruleService.getAvailableAttributesForDiscountType(discountTypeId);

        assertEquals(1, result.size());
        assertEquals("product_type", result.get(0).attributeName());
    }

    // ======================== Classification CRUD ========================

    @Nested @DisplayName("Classification Rules")
    class ClassificationRuleTests {

        private UUID tierId;
        private CustomerTierEntity tier;
        private DiscountTypeEntity classificationType;

        @BeforeEach
        void classSetUp() {
            tierId = UUID.randomUUID();
            tier = CustomerTierEntity.builder()
                    .id(tierId).ecommerceId(ecommerceId).name("Gold").hierarchyLevel(1)
                    .isActive(true).createdAt(Instant.now()).updatedAt(Instant.now()).build();

            classificationType = new DiscountTypeEntity();
            classificationType.setId(discountTypeId);
            classificationType.setCode("CLASSIFICATION");
            classificationType.setDisplayName("Classification");
            classificationType.setCreatedAt(Instant.now());
        }

        @Test @DisplayName("createClassificationRuleForTier - success")
        void createClassification_success() {
            ClassificationRuleCreateRequest req = new ClassificationRuleCreateRequest(
                    priorityId.toString(), "CRule", "desc", BigDecimal.TEN,
                    "total_spent", BigDecimal.ZERO, new BigDecimal("100"), 1);

            when(customerTierRepository.findById(tierId)).thenReturn(Optional.of(tier));
            when(discountLimitPriorityRepository.findById(priorityId)).thenReturn(Optional.of(priorityEntity));
            when(discountTypeRepository.findById(discountTypeId)).thenReturn(Optional.of(classificationType));
            when(ruleRepository.findActiveClassificationRulesByEcommerce(ecommerceId)).thenReturn(List.of());
            when(ruleRepository.save(any(RuleEntity.class))).thenAnswer(inv -> {
                RuleEntity e = inv.getArgument(0);
                e.setId(ruleId);
                return e;
            });
            when(ruleAttributeRepository.findByDiscountTypeIdOrderByAttributeNameAsc(discountTypeId))
                    .thenReturn(List.of(buildAttrEntity("metricType"), buildAttrEntity("minValue"),
                            buildAttrEntity("maxValue"), buildAttrEntity("priority")));
            when(ruleAttributeRepository.findByDiscountTypeIdAndAttributeName(eq(discountTypeId), anyString()))
                    .thenAnswer(inv -> Optional.of(buildAttrEntity(inv.getArgument(1))));
            when(ruleAttributeValueRepository.findByRuleIdAndAttributeId(any(), any())).thenReturn(Optional.empty());
            when(ruleCustomerTierRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(ruleAttributeValueRepository.findByRuleIdOrderByCreatedAtAsc(any())).thenReturn(List.of());

            ClassificationRuleResponse result = ruleService.createClassificationRuleForTier(ecommerceId, tierId, req);

            assertNotNull(result);
            verify(continuityValidator).validateContinuity(anyList(), any(), any(), isNull());
            verify(hierarchyValidator).validateHierarchy(anyList(), eq(1), isNull());
            verify(uniquePriorityValidator).validateUniquePriority(anyList(), eq(1), isNull());
        }

        @Test @DisplayName("createClassificationRuleForTier - non-CLASSIFICATION type throws BadRequest")
        void createClassification_wrongType() {
            ClassificationRuleCreateRequest req = new ClassificationRuleCreateRequest(
                    priorityId.toString(), "CRule", "desc", BigDecimal.TEN,
                    "total_spent", BigDecimal.ZERO, new BigDecimal("100"), 1);

            when(customerTierRepository.findById(tierId)).thenReturn(Optional.of(tier));
            when(discountLimitPriorityRepository.findById(priorityId)).thenReturn(Optional.of(priorityEntity));
            when(discountTypeRepository.findById(discountTypeId)).thenReturn(Optional.of(productType)); // PRODUCT instead of CLASSIFICATION

            assertThrows(BadRequestException.class, () ->
                    ruleService.createClassificationRuleForTier(ecommerceId, tierId, req));
        }

        @Test @DisplayName("createClassificationRuleForTier - minValue >= maxValue throws in constructor")
        void createClassification_minGteMax() {
            // ClassificationRuleCreateRequest compact constructor validates min < max
            assertThrows(IllegalArgumentException.class, () ->
                    new ClassificationRuleCreateRequest(
                            priorityId.toString(), "CRule", "desc", BigDecimal.TEN,
                            "total_spent", new BigDecimal("100"), new BigDecimal("50"), 1));
        }

        @Test @DisplayName("listClassificationRulesForTier - success")
        void listClassification_success() {
            RuleCustomerTierEntity link = RuleCustomerTierEntity.builder()
                    .rule(ruleEntity).customerTier(tier).build();

            when(customerTierRepository.findById(tierId)).thenReturn(Optional.of(tier));
            when(ruleCustomerTierRepository.findByCustomerTierId(tierId)).thenReturn(List.of(link));
            when(ruleAttributeValueRepository.findByRuleIdOrderByCreatedAtAsc(ruleId)).thenReturn(List.of());

            List<ClassificationRuleResponse> result = ruleService.listClassificationRulesForTier(ecommerceId, tierId);

            assertEquals(1, result.size());
        }

        @Test @DisplayName("listClassificationRulesForTier - tierNotFound throws")
        void listClassification_tierNotFound() {
            when(customerTierRepository.findById(tierId)).thenReturn(Optional.empty());
            assertThrows(ResourceNotFoundException.class, () ->
                    ruleService.listClassificationRulesForTier(ecommerceId, tierId));
        }

        @Test @DisplayName("updateClassificationRuleForTier - success")
        void updateClassification_success() {
            ClassificationRuleUpdateRequest req = new ClassificationRuleUpdateRequest(
                    "Updated", null, null, null, null, null, null);

            RuleCustomerTierEntity link = RuleCustomerTierEntity.builder()
                    .rule(ruleEntity).customerTier(tier).build();

            when(customerTierRepository.findById(tierId)).thenReturn(Optional.of(tier));
            when(ruleRepository.findByIdAndEcommerceId(ruleId, ecommerceId)).thenReturn(Optional.of(ruleEntity));
            when(ruleCustomerTierRepository.findByRuleIdAndCustomerTierId(ruleId, tierId)).thenReturn(Optional.of(link));
            when(discountLimitPriorityRepository.findById(priorityId)).thenReturn(Optional.of(priorityEntity));
            when(ruleRepository.save(any(RuleEntity.class))).thenReturn(ruleEntity);
            when(ruleAttributeValueRepository.findByRuleIdOrderByCreatedAtAsc(ruleId)).thenReturn(List.of());

            ClassificationRuleResponse result = ruleService.updateClassificationRuleForTier(ecommerceId, tierId, ruleId, req);

            assertNotNull(result);
            assertEquals("Updated", ruleEntity.getName());
        }

        @Test @DisplayName("updateClassificationRuleForTier - with attributes triggers validators")
        void updateClassification_withAttributes() {
            ClassificationRuleUpdateRequest req = new ClassificationRuleUpdateRequest(
                    null, null, null, null, new BigDecimal("10"), new BigDecimal("200"), 2);

            RuleCustomerTierEntity link = RuleCustomerTierEntity.builder()
                    .rule(ruleEntity).customerTier(tier).build();

            when(customerTierRepository.findById(tierId)).thenReturn(Optional.of(tier));
            when(ruleRepository.findByIdAndEcommerceId(ruleId, ecommerceId)).thenReturn(Optional.of(ruleEntity));
            when(ruleCustomerTierRepository.findByRuleIdAndCustomerTierId(ruleId, tierId)).thenReturn(Optional.of(link));
            when(discountLimitPriorityRepository.findById(priorityId)).thenReturn(Optional.of(priorityEntity));
            when(ruleRepository.save(any(RuleEntity.class))).thenReturn(ruleEntity);
            when(ruleAttributeValueRepository.findByRuleIdOrderByCreatedAtAsc(ruleId)).thenReturn(List.of());
            when(ruleRepository.findActiveClassificationRulesByEcommerce(ecommerceId)).thenReturn(List.of());
            when(ruleAttributeRepository.findByDiscountTypeIdOrderByAttributeNameAsc(discountTypeId))
                    .thenReturn(List.of(buildAttrEntity("minValue"), buildAttrEntity("maxValue"), buildAttrEntity("priority")));
            when(ruleAttributeRepository.findByDiscountTypeIdAndAttributeName(eq(discountTypeId), anyString()))
                    .thenAnswer(inv -> Optional.of(buildAttrEntity(inv.getArgument(1))));
            when(ruleAttributeValueRepository.findByRuleIdAndAttributeId(any(), any())).thenReturn(Optional.empty());

            ClassificationRuleResponse result = ruleService.updateClassificationRuleForTier(ecommerceId, tierId, ruleId, req);

            assertNotNull(result);
            verify(continuityValidator).validateContinuity(anyList(), any(), any(), eq(ruleId));
            verify(hierarchyValidator).validateHierarchy(anyList(), eq(2), eq(ruleId));
        }

        @Test @DisplayName("deleteClassificationRuleForTier - success")
        void deleteClassification_success() {
            RuleCustomerTierEntity link = RuleCustomerTierEntity.builder()
                    .rule(ruleEntity).customerTier(tier).build();

            when(customerTierRepository.findById(tierId)).thenReturn(Optional.of(tier));
            when(ruleRepository.findByIdAndEcommerceId(ruleId, ecommerceId)).thenReturn(Optional.of(ruleEntity));
            when(ruleCustomerTierRepository.findByRuleIdAndCustomerTierId(ruleId, tierId)).thenReturn(Optional.of(link));
            when(ruleRepository.save(any(RuleEntity.class))).thenReturn(ruleEntity);

            ruleService.deleteClassificationRuleForTier(ecommerceId, tierId, ruleId);

            assertFalse(ruleEntity.getIsActive());
        }

        @Test @DisplayName("deleteClassificationRuleForTier - not linked throws BadRequest")
        void deleteClassification_notLinked() {
            when(customerTierRepository.findById(tierId)).thenReturn(Optional.of(tier));
            when(ruleRepository.findByIdAndEcommerceId(ruleId, ecommerceId)).thenReturn(Optional.of(ruleEntity));
            when(ruleCustomerTierRepository.findByRuleIdAndCustomerTierId(ruleId, tierId)).thenReturn(Optional.empty());

            assertThrows(BadRequestException.class, () ->
                    ruleService.deleteClassificationRuleForTier(ecommerceId, tierId, ruleId));
        }
    }

    // ======================== listRules with isActive=false ========================

    @Test @DisplayName("listRules - isActive null returns all")
    void listRules_allRules() {
        Pageable pageable = PageRequest.of(0, 10);
        when(ruleRepository.findByEcommerceIdOrderByCreatedAtDesc(ecommerceId, pageable))
                .thenReturn(new PageImpl<>(List.of(ruleEntity)));
        when(ruleAttributeValueRepository.findByRuleIdOrderByCreatedAtAsc(ruleId)).thenReturn(List.of());

        Page<RuleResponse> result = ruleService.listRules(ecommerceId, null, pageable);

        assertEquals(1, result.getContent().size());
    }

    @Test @DisplayName("listRules - isActive=false returns all")
    void listRules_inactiveFilter() {
        Pageable pageable = PageRequest.of(0, 10);
        when(ruleRepository.findByEcommerceIdOrderByCreatedAtDesc(ecommerceId, pageable))
                .thenReturn(new PageImpl<>(List.of(ruleEntity)));
        when(ruleAttributeValueRepository.findByRuleIdOrderByCreatedAtAsc(ruleId)).thenReturn(List.of());

        Page<RuleResponse> result = ruleService.listRules(ecommerceId, false, pageable);

        assertEquals(1, result.getContent().size());
        verify(ruleRepository).findByEcommerceIdOrderByCreatedAtDesc(ecommerceId, pageable);
    }

    // ======================== deleteRule with priority resolution ========================

    @Test @DisplayName("deleteRule - resolves discount type for event")
    void deleteRule_withPriorityResolution() {
        when(ruleRepository.findByIdAndEcommerceId(ruleId, ecommerceId)).thenReturn(Optional.of(ruleEntity));
        when(ruleRepository.save(any())).thenReturn(ruleEntity);
        when(discountLimitPriorityRepository.findById(priorityId)).thenReturn(Optional.of(priorityEntity));
        when(discountTypeRepository.findById(discountTypeId)).thenReturn(Optional.of(productType));
        when(ruleAttributeValueRepository.findByRuleIdOrderByCreatedAtAsc(ruleId)).thenReturn(List.of());

        ruleService.deleteRule(ecommerceId, ruleId);

        assertFalse(ruleEntity.getIsActive());
        verify(ruleEventPort).publishRuleUpdated(any(), eq(ecommerceId), anyMap(), anyString());
    }

    // ======================== Helper ========================

    private RuleAttributeEntity buildAttrEntity(String name) {
        return RuleAttributeEntity.builder()
                .id(UUID.randomUUID()).attributeName(name).attributeType("STRING")
                .isRequired(true).description(name + " desc")
                .discountTypeId(discountTypeId)
                .build();
    }
}
