package com.loyalty.service_admin.domain.entity;

import com.loyalty.service_admin.domain.model.ecommerce.EcommerceStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Domain Entity Lifecycle Tests")
class EntityLifecycleTest {

    // ==================== UserEntity ====================
    @Nested
    @DisplayName("UserEntity")
    class UserEntityTests {

        @Test
        void testOnCreate_setsTimestamps() {
            UserEntity entity = new UserEntity();
            entity.onCreate();

            assertNotNull(entity.getCreatedAt());
            assertNotNull(entity.getUpdatedAt());
        }

        @Test
        void testOnUpdate_setsUpdatedAt() {
            UserEntity entity = new UserEntity();
            entity.onCreate();
            Instant originalUpdatedAt = entity.getUpdatedAt();

            entity.onUpdate();

            assertNotNull(entity.getUpdatedAt());
            assertTrue(entity.getUpdatedAt().compareTo(originalUpdatedAt) >= 0);
        }
    }

    // ==================== EcommerceEntity ====================
    @Nested
    @DisplayName("EcommerceEntity")
    class EcommerceEntityTests {

        @Test
        void testOnCreate_setsTimestampsAndDefaultStatus() {
            EcommerceEntity entity = new EcommerceEntity();
            entity.onCreate();

            assertNotNull(entity.getCreatedAt());
            assertNotNull(entity.getUpdatedAt());
            assertEquals(EcommerceStatus.ACTIVE, entity.getStatus());
        }

        @Test
        void testOnCreate_preservesExistingStatus() {
            EcommerceEntity entity = new EcommerceEntity();
            entity.setStatus(EcommerceStatus.INACTIVE);
            entity.onCreate();

            assertEquals(EcommerceStatus.INACTIVE, entity.getStatus());
        }

        @Test
        void testOnUpdate_setsUpdatedAt() {
            EcommerceEntity entity = new EcommerceEntity();
            entity.onCreate();
            entity.onUpdate();

            assertNotNull(entity.getUpdatedAt());
        }
    }

    // ==================== RuleEntity ====================
    @Nested
    @DisplayName("RuleEntity")
    class RuleEntityTests {

        @Test
        void testOnCreate_setsTimestamps() {
            RuleEntity entity = new RuleEntity();
            entity.onCreate();

            assertNotNull(entity.getCreatedAt());
            assertNotNull(entity.getUpdatedAt());
        }

        @Test
        void testOnUpdate_setsUpdatedAt() {
            RuleEntity entity = new RuleEntity();
            entity.onCreate();
            entity.onUpdate();

            assertNotNull(entity.getUpdatedAt());
        }

        @Test
        void testBuilder() {
            UUID id = UUID.randomUUID();
            RuleEntity entity = RuleEntity.builder()
                    .id(id)
                    .name("Test Rule")
                    .ecommerceId(UUID.randomUUID())
                    .discountPercentage(BigDecimal.TEN)
                    .isActive(true)
                    .build();

            assertEquals(id, entity.getId());
            assertEquals("Test Rule", entity.getName());
            assertTrue(entity.getIsActive());
        }
    }

    // ==================== ApiKeyEntity ====================
    @Nested
    @DisplayName("ApiKeyEntity")
    class ApiKeyEntityTests {

        @Test
        void testOnCreate_setsTimestamps() {
            ApiKeyEntity entity = new ApiKeyEntity();
            entity.onCreate();

            assertNotNull(entity.getCreatedAt());
            assertNotNull(entity.getUpdatedAt());
        }

        @Test
        void testOnUpdate_setsUpdatedAt() {
            ApiKeyEntity entity = new ApiKeyEntity();
            entity.onCreate();
            entity.onUpdate();

            assertNotNull(entity.getUpdatedAt());
        }
    }

    // ==================== AuditLogEntity ====================
    @Nested
    @DisplayName("AuditLogEntity")
    class AuditLogEntityTests {

        @Test
        void testOnCreate_setsCreatedAt() {
            AuditLogEntity entity = new AuditLogEntity();
            entity.onCreate();

            assertNotNull(entity.getCreatedAt());
        }

        @Test
        void testBuilder() {
            AuditLogEntity entity = AuditLogEntity.builder()
                    .action("CREATE")
                    .entityName("user")
                    .build();

            assertEquals("CREATE", entity.getAction());
            assertEquals("user", entity.getEntityName());
        }
    }

    // ==================== CustomerTierEntity ====================
    @Nested
    @DisplayName("CustomerTierEntity")
    class CustomerTierEntityTests {

        @Test
        void testOnCreate_setsTimestamps() {
            CustomerTierEntity entity = new CustomerTierEntity();
            entity.onCreate();

            assertNotNull(entity.getCreatedAt());
            assertNotNull(entity.getUpdatedAt());
        }

        @Test
        void testOnUpdate_setsUpdatedAt() {
            CustomerTierEntity entity = new CustomerTierEntity();
            entity.onCreate();
            entity.onUpdate();

            assertNotNull(entity.getUpdatedAt());
        }
    }

    // ==================== DiscountSettingsEntity ====================
    @Nested
    @DisplayName("DiscountSettingsEntity")
    class DiscountSettingsEntityTests {

        @Test
        void testOnCreate_setsTimestamps() {
            DiscountSettingsEntity entity = new DiscountSettingsEntity();
            entity.onCreate();

            assertNotNull(entity.getCreatedAt());
            assertNotNull(entity.getUpdatedAt());
        }

        @Test
        void testOnUpdate_setsUpdatedAtAndIncrementsVersion() {
            DiscountSettingsEntity entity = new DiscountSettingsEntity();
            entity.setVersion(1L);
            entity.onCreate();
            entity.onUpdate();

            assertNotNull(entity.getUpdatedAt());
            assertEquals(2L, entity.getVersion());
        }

        @Test
        void testOnUpdate_nullVersion_setsToTwo() {
            DiscountSettingsEntity entity = new DiscountSettingsEntity();
            entity.setVersion(null);
            entity.onUpdate();

            assertEquals(2L, entity.getVersion());
        }

        @Test
        void testReplacePriorities() {
            DiscountSettingsEntity entity = new DiscountSettingsEntity();
            entity.setPriorities(new ArrayList<>());

            DiscountPriorityEntity p1 = new DiscountPriorityEntity();
            DiscountPriorityEntity p2 = new DiscountPriorityEntity();
            entity.replacePriorities(List.of(p1, p2));

            assertEquals(2, entity.getPriorities().size());
        }

        @Test
        void testReplacePriorities_withNull() {
            DiscountSettingsEntity entity = new DiscountSettingsEntity();
            entity.setPriorities(new ArrayList<>(List.of(new DiscountPriorityEntity())));

            entity.replacePriorities(null);

            assertTrue(entity.getPriorities().isEmpty());
        }

        @Test
        void testReplacePriorities_clearsPrevious() {
            DiscountSettingsEntity entity = new DiscountSettingsEntity();
            entity.setPriorities(new ArrayList<>(List.of(new DiscountPriorityEntity())));

            entity.replacePriorities(List.of(new DiscountPriorityEntity()));

            assertEquals(1, entity.getPriorities().size());
        }
    }

    // ==================== DiscountPriorityEntity ====================
    @Nested
    @DisplayName("DiscountPriorityEntity")
    class DiscountPriorityEntityTests {

        @Test
        void testOnCreate_setsTimestamps() {
            DiscountPriorityEntity entity = new DiscountPriorityEntity();
            entity.onCreate();

            assertNotNull(entity.getCreatedAt());
            assertNotNull(entity.getUpdatedAt());
        }

        @Test
        void testOnUpdate_setsUpdatedAt() {
            DiscountPriorityEntity entity = new DiscountPriorityEntity();
            entity.onCreate();
            entity.onUpdate();

            assertNotNull(entity.getUpdatedAt());
        }
    }

    // ==================== RoleEntity ====================
    @Nested
    @DisplayName("RoleEntity")
    class RoleEntityTests {

        @Test
        void testOnCreate_setsTimestamps() {
            RoleEntity entity = new RoleEntity();
            entity.onCreate();

            assertNotNull(entity.getCreatedAt());
            assertNotNull(entity.getUpdatedAt());
        }

        @Test
        void testOnUpdate_setsUpdatedAt() {
            RoleEntity entity = new RoleEntity();
            entity.onCreate();
            entity.onUpdate();

            assertNotNull(entity.getUpdatedAt());
        }
    }

    // ==================== PermissionEntity ====================
    @Nested
    @DisplayName("PermissionEntity")
    class PermissionEntityTests {

        @Test
        void testOnCreate_setsTimestamps() {
            PermissionEntity entity = new PermissionEntity();
            entity.onCreate();

            assertNotNull(entity.getCreatedAt());
            assertNotNull(entity.getUpdatedAt());
        }

        @Test
        void testOnUpdate_setsUpdatedAt() {
            PermissionEntity entity = new PermissionEntity();
            entity.onCreate();
            entity.onUpdate();

            assertNotNull(entity.getUpdatedAt());
        }
    }

    // ==================== RuleCustomerTierEntity ====================
    @Nested
    @DisplayName("RuleCustomerTierEntity")
    class RuleCustomerTierEntityTests {

        @Test
        void testOnCreate_setsTimestamps() {
            RuleCustomerTierEntity entity = new RuleCustomerTierEntity();
            entity.onCreate();

            assertNotNull(entity.getCreatedAt());
            assertNotNull(entity.getUpdatedAt());
        }

        @Test
        void testOnUpdate_setsUpdatedAt() {
            RuleCustomerTierEntity entity = new RuleCustomerTierEntity();
            entity.onCreate();
            entity.onUpdate();

            assertNotNull(entity.getUpdatedAt());
        }
    }

    // ==================== RuleAttributeValueEntity ====================
    @Nested
    @DisplayName("RuleAttributeValueEntity")
    class RuleAttributeValueEntityTests {

        @Test
        void testOnCreate_setsTimestamps() {
            RuleAttributeValueEntity entity = new RuleAttributeValueEntity();
            entity.onCreate();

            assertNotNull(entity.getCreatedAt());
            assertNotNull(entity.getUpdatedAt());
        }

        @Test
        void testOnUpdate_setsUpdatedAt() {
            RuleAttributeValueEntity entity = new RuleAttributeValueEntity();
            entity.onCreate();
            entity.onUpdate();

            assertNotNull(entity.getUpdatedAt());
        }
    }

    // ==================== RuleAttributeEntity ====================
    @Nested
    @DisplayName("RuleAttributeEntity")
    class RuleAttributeEntityTests {

        @Test
        void testOnCreate_setsCreatedAt() {
            RuleAttributeEntity entity = new RuleAttributeEntity();
            entity.onCreate();

            assertNotNull(entity.getCreatedAt());
        }
    }

    // ==================== DiscountApplicationLogEntity ====================
    @Nested
    @DisplayName("DiscountApplicationLogEntity")
    class DiscountApplicationLogEntityTests {

        @Test
        void testOnCreate_setsCreatedAt() {
            DiscountApplicationLogEntity entity = new DiscountApplicationLogEntity();
            entity.onCreate();

            assertNotNull(entity.getCreatedAt());
        }
    }

    // ==================== DiscountTypeEntity ====================
    @Nested
    @DisplayName("DiscountTypeEntity")
    class DiscountTypeEntityTests {

        @Test
        void testOnCreate_setsCreatedAt() {
            DiscountTypeEntity entity = new DiscountTypeEntity();
            entity.onCreate();

            assertNotNull(entity.getCreatedAt());
        }
    }
}
