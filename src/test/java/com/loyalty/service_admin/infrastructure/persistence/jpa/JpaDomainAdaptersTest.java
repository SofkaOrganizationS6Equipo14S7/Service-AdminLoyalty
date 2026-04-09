package com.loyalty.service_admin.infrastructure.persistence.jpa;

import com.loyalty.service_admin.domain.entity.DiscountPriorityEntity;
import com.loyalty.service_admin.domain.entity.DiscountSettingsEntity;
import com.loyalty.service_admin.domain.entity.RuleAttributeEntity;
import com.loyalty.service_admin.domain.entity.RuleAttributeValueEntity;
import com.loyalty.service_admin.domain.entity.RuleEntity;
import com.loyalty.service_admin.domain.repository.DiscountConfigRepository;
import com.loyalty.service_admin.domain.repository.DiscountLimitPriorityRepository;
import com.loyalty.service_admin.domain.repository.RuleAttributeRepository;
import com.loyalty.service_admin.domain.repository.RuleAttributeValueRepository;
import com.loyalty.service_admin.domain.repository.RuleRepository;
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

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("JPA Domain Adapters Unit Tests")
class JpaDomainAdaptersTest {

    // ==================== Discount Config Adapter ====================
    @Nested
    @DisplayName("JpaDiscountConfigAdapter")
    class DiscountConfigAdapterTests {

        @Mock
        private DiscountConfigRepository repository;

        @InjectMocks
        private JpaDiscountConfigAdapter adapter;

        @Test
        void testSaveConfig() {
            DiscountSettingsEntity config = new DiscountSettingsEntity();
            when(repository.save(config)).thenReturn(config);
            assertEquals(config, adapter.saveConfig(config));
        }

        @Test
        void testFindActiveConfigByEcommerce() {
            UUID ecommerceId = UUID.randomUUID();
            when(repository.findActiveByEcommerceId(ecommerceId)).thenReturn(Optional.of(new DiscountSettingsEntity()));
            assertTrue(adapter.findActiveConfigByEcommerce(ecommerceId).isPresent());
        }

        @Test
        void testFindConfigById() {
            UUID id = UUID.randomUUID();
            when(repository.findById(id)).thenReturn(Optional.of(new DiscountSettingsEntity()));
            assertTrue(adapter.findConfigById(id).isPresent());
        }

        @Test
        void testExistsActiveConfigForEcommerce() {
            UUID ecommerceId = UUID.randomUUID();
            when(repository.existsByEcommerceIdAndIsActiveTrue(ecommerceId)).thenReturn(true);
            assertTrue(adapter.existsActiveConfigForEcommerce(ecommerceId));
        }
    }

    // ==================== Discount Limit Priority Adapter ====================
    @Nested
    @DisplayName("JpaDiscountLimitPriorityAdapter")
    class DiscountLimitPriorityAdapterTests {

        @Mock
        private DiscountLimitPriorityRepository repository;

        @InjectMocks
        private JpaDiscountLimitPriorityAdapter adapter;

        @Test
        void testSavePriority() {
            DiscountPriorityEntity priority = new DiscountPriorityEntity();
            when(repository.save(priority)).thenReturn(priority);
            assertEquals(priority, adapter.savePriority(priority));
        }

        @Test
        void testFindPriorityById() {
            UUID id = UUID.randomUUID();
            when(repository.findById(id)).thenReturn(Optional.of(new DiscountPriorityEntity()));
            assertTrue(adapter.findPriorityById(id).isPresent());
        }

        @Test
        void testFindPrioritiesByConfig() {
            UUID configId = UUID.randomUUID();
            when(repository.findByDiscountSettingsIdOrderByPriorityLevel(configId))
                    .thenReturn(List.of(new DiscountPriorityEntity()));
            assertEquals(1, adapter.findPrioritiesByConfig(configId).size());
        }

        @Test
        void testDeletePriority() {
            UUID id = UUID.randomUUID();
            adapter.deletePriority(id);
            verify(repository).deleteById(id);
        }

        @Test
        void testExistsPriorityWithLevel() {
            UUID configId = UUID.randomUUID();
            when(repository.existsByDiscountSettingsIdAndPriorityLevel(configId, 1)).thenReturn(true);
            assertTrue(adapter.existsPriorityWithLevel(configId, 1));
        }
    }

    // ==================== Rule Adapter ====================
    @Nested
    @DisplayName("JpaRuleAdapter")
    class RuleAdapterTests {

        @Mock
        private RuleRepository ruleRepository;

        @Mock
        private RuleAttributeRepository ruleAttributeRepository;

        @Mock
        private RuleAttributeValueRepository ruleAttributeValueRepository;

        @InjectMocks
        private JpaRuleAdapter adapter;

        @Test
        void testSaveRule() {
            RuleEntity rule = new RuleEntity();
            when(ruleRepository.save(rule)).thenReturn(rule);
            assertEquals(rule, adapter.saveRule(rule));
        }

        @Test
        void testFindRuleById() {
            UUID id = UUID.randomUUID();
            when(ruleRepository.findById(id)).thenReturn(Optional.of(new RuleEntity()));
            assertTrue(adapter.findRuleById(id).isPresent());
        }

        @Test
        void testFindRuleByIdAndEcommerce() {
            UUID ruleId = UUID.randomUUID();
            UUID ecommerceId = UUID.randomUUID();
            when(ruleRepository.findByIdAndEcommerceId(ruleId, ecommerceId)).thenReturn(Optional.of(new RuleEntity()));
            assertTrue(adapter.findRuleByIdAndEcommerce(ruleId, ecommerceId).isPresent());
        }

        @Test
        void testFindRulesByEcommerce() {
            UUID ecommerceId = UUID.randomUUID();
            Pageable pageable = PageRequest.of(0, 10);
            Page<RuleEntity> page = new PageImpl<>(List.of(new RuleEntity()));
            when(ruleRepository.findByEcommerceIdOrderByCreatedAtDesc(ecommerceId, pageable)).thenReturn(page);
            assertEquals(1, adapter.findRulesByEcommerce(ecommerceId, pageable).getTotalElements());
        }

        @Test
        void testFindActiveRulesByEcommerce() {
            UUID ecommerceId = UUID.randomUUID();
            Pageable pageable = PageRequest.of(0, 10);
            Page<RuleEntity> page = new PageImpl<>(List.of(new RuleEntity()));
            when(ruleRepository.findByEcommerceIdAndIsActiveTrueOrderByCreatedAtDesc(ecommerceId, pageable)).thenReturn(page);
            assertEquals(1, adapter.findActiveRulesByEcommerce(ecommerceId, pageable).getTotalElements());
        }

        @Test
        void testFindRulesByStatus_active() {
            UUID ecommerceId = UUID.randomUUID();
            Page<RuleEntity> page = new PageImpl<>(List.of(new RuleEntity()));
            when(ruleRepository.findByEcommerceIdAndIsActiveTrueOrderByCreatedAtDesc(eq(ecommerceId), any()))
                    .thenReturn(page);
            assertEquals(1, adapter.findRulesByStatus(ecommerceId, true).size());
        }

        @Test
        void testFindRulesByStatus_all() {
            UUID ecommerceId = UUID.randomUUID();
            Page<RuleEntity> page = new PageImpl<>(List.of(new RuleEntity()));
            when(ruleRepository.findByEcommerceIdOrderByCreatedAtDesc(eq(ecommerceId), any()))
                    .thenReturn(page);
            assertEquals(1, adapter.findRulesByStatus(ecommerceId, false).size());
        }

        @Test
        void testDeleteRule_softDelete() {
            RuleEntity rule = new RuleEntity();
            rule.setIsActive(true);
            when(ruleRepository.save(rule)).thenReturn(rule);
            adapter.deleteRule(rule);
            assertFalse(rule.getIsActive());
            verify(ruleRepository).save(rule);
        }

        @Test
        void testExistsRule() {
            UUID id = UUID.randomUUID();
            when(ruleRepository.existsById(id)).thenReturn(true);
            assertTrue(adapter.existsRule(id));
        }

        @Test
        void testExistsActiveRuleWithAttribute_found() {
            UUID ecommerceId = UUID.randomUUID();
            UUID ruleId = UUID.randomUUID();
            UUID attrId = UUID.randomUUID();

            RuleEntity rule = new RuleEntity();
            rule.setId(ruleId);
            Page<RuleEntity> page = new PageImpl<>(List.of(rule));
            when(ruleRepository.findByEcommerceIdAndIsActiveTrueOrderByCreatedAtDesc(eq(ecommerceId), any()))
                    .thenReturn(page);

            RuleAttributeValueEntity attrValue = new RuleAttributeValueEntity();
            attrValue.setAttributeId(attrId);
            attrValue.setValue("test-value");
            when(ruleAttributeValueRepository.findByRuleIdOrderByCreatedAtAsc(ruleId))
                    .thenReturn(List.of(attrValue));

            RuleAttributeEntity attrDef = new RuleAttributeEntity();
            attrDef.setAttributeName("test-attr");
            when(ruleAttributeRepository.findById(attrId)).thenReturn(Optional.of(attrDef));

            assertTrue(adapter.existsActiveRuleWithAttribute(ecommerceId, "test-attr", "test-value"));
        }

        @Test
        void testExistsActiveRuleWithAttribute_notFound() {
            UUID ecommerceId = UUID.randomUUID();
            Page<RuleEntity> page = new PageImpl<>(List.of());
            when(ruleRepository.findByEcommerceIdAndIsActiveTrueOrderByCreatedAtDesc(eq(ecommerceId), any()))
                    .thenReturn(page);

            assertFalse(adapter.existsActiveRuleWithAttribute(ecommerceId, "attr", "value"));
        }

        @Test
        void testExistsActiveRuleWithAttribute_noMatchingAttribute() {
            UUID ecommerceId = UUID.randomUUID();
            UUID ruleId = UUID.randomUUID();
            UUID attrId = UUID.randomUUID();

            RuleEntity rule = new RuleEntity();
            rule.setId(ruleId);
            Page<RuleEntity> page = new PageImpl<>(List.of(rule));
            when(ruleRepository.findByEcommerceIdAndIsActiveTrueOrderByCreatedAtDesc(eq(ecommerceId), any()))
                    .thenReturn(page);

            RuleAttributeValueEntity attrValue = new RuleAttributeValueEntity();
            attrValue.setAttributeId(attrId);
            attrValue.setValue("other-value");
            when(ruleAttributeValueRepository.findByRuleIdOrderByCreatedAtAsc(ruleId))
                    .thenReturn(List.of(attrValue));

            RuleAttributeEntity attrDef = new RuleAttributeEntity();
            attrDef.setAttributeName("different-attr");
            when(ruleAttributeRepository.findById(attrId)).thenReturn(Optional.of(attrDef));

            assertFalse(adapter.existsActiveRuleWithAttribute(ecommerceId, "test-attr", "test-value"));
        }
    }
}
