package com.loyalty.service_admin.infrastructure.adapter;

import com.loyalty.service_admin.application.dto.configuration.ConfigurationUpdatedEvent;
import com.loyalty.service_admin.application.port.out.ConfigurationEventPort;
import com.loyalty.service_admin.domain.entity.DiscountSettingsEntity;
import com.loyalty.service_admin.infrastructure.persistence.jpa.JpaDiscountConfigurationRepository;
import com.loyalty.service_admin.infrastructure.rabbitmq.ConfigurationEventPublisher;
import com.loyalty.service_admin.infrastructure.security.SecurityContextHelper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Infrastructure Adapter Unit Tests")
class InfrastructureAdaptersTest {

    // ==================== ConfigurationEventAdapter ====================
    @Nested
    @DisplayName("ConfigurationEventAdapter")
    class ConfigEventAdapterTests {

        @Mock
        private ConfigurationEventPublisher publisher;

        @InjectMocks
        private ConfigurationEventAdapter adapter;

        @Test
        void testPublishConfigUpdated_delegatesToPublisher() {
            ConfigurationUpdatedEvent event = mock(ConfigurationUpdatedEvent.class);

            adapter.publishConfigUpdated(event);

            verify(publisher).publishConfigUpdated(event);
        }
    }

    // ==================== ConfigurationPersistenceAdapter ====================
    @Nested
    @DisplayName("ConfigurationPersistenceAdapter")
    class ConfigPersistenceAdapterTests {

        @Mock
        private JpaDiscountConfigurationRepository repository;

        @InjectMocks
        private ConfigurationPersistenceAdapter adapter;

        @Test
        void testExistsByEcommerceId() {
            UUID ecommerceId = UUID.randomUUID();
            when(repository.existsByEcommerceId(ecommerceId)).thenReturn(true);
            assertTrue(adapter.existsByEcommerceId(ecommerceId));
        }

        @Test
        void testFindByEcommerceId_found() {
            UUID ecommerceId = UUID.randomUUID();
            DiscountSettingsEntity entity = new DiscountSettingsEntity();
            when(repository.findByEcommerceId(ecommerceId)).thenReturn(Optional.of(entity));
            assertTrue(adapter.findByEcommerceId(ecommerceId).isPresent());
        }

        @Test
        void testFindByEcommerceId_notFound() {
            UUID ecommerceId = UUID.randomUUID();
            when(repository.findByEcommerceId(ecommerceId)).thenReturn(Optional.empty());
            assertFalse(adapter.findByEcommerceId(ecommerceId).isPresent());
        }

        @Test
        void testSave() {
            DiscountSettingsEntity entity = new DiscountSettingsEntity();
            when(repository.save(entity)).thenReturn(entity);
            assertEquals(entity, adapter.save(entity));
        }
    }

    // ==================== CurrentUserSecurityAdapter ====================
    @Nested
    @DisplayName("CurrentUserSecurityAdapter")
    class CurrentUserSecurityAdapterTests {

        @Mock
        private SecurityContextHelper securityContextHelper;

        @InjectMocks
        private CurrentUserSecurityAdapter adapter;

        @Test
        void testIsSuperAdmin_true() {
            when(securityContextHelper.isCurrentUserSuperAdmin()).thenReturn(true);
            assertTrue(adapter.isSuperAdmin());
        }

        @Test
        void testIsSuperAdmin_false() {
            when(securityContextHelper.isCurrentUserSuperAdmin()).thenReturn(false);
            assertFalse(adapter.isSuperAdmin());
        }

        @Test
        void testGetCurrentUserEcommerceId() {
            UUID ecommerceId = UUID.randomUUID();
            when(securityContextHelper.getCurrentUserEcommerceId()).thenReturn(ecommerceId);
            assertEquals(ecommerceId, adapter.getCurrentUserEcommerceId());
        }
    }
}
