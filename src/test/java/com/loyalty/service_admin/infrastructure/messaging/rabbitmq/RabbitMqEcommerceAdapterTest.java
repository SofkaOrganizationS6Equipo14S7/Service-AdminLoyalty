package com.loyalty.service_admin.infrastructure.messaging.rabbitmq;

import com.loyalty.service_admin.domain.model.ecommerce.EcommerceStatus;
import com.loyalty.service_admin.infrastructure.rabbitmq.EcommerceEventPublisher;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("RabbitMqEcommerceAdapter Unit Tests")
class RabbitMqEcommerceAdapterTest {

    @Mock
    private EcommerceEventPublisher eventPublisher;

    @InjectMocks
    private RabbitMqEcommerceAdapter adapter;

    @Test
    void testPublishEcommerceCreated_delegatesToPublisher() {
        UUID ecommerceId = UUID.randomUUID();
        String name = "Test Store";
        String slug = "test-store";

        adapter.publishEcommerceCreated(ecommerceId, name, slug);

        verify(eventPublisher).publishEcommerceCreated(ecommerceId, name, slug);
    }

    @Test
    void testPublishEcommerceStatusChanged_active() {
        UUID ecommerceId = UUID.randomUUID();

        adapter.publishEcommerceStatusChanged(ecommerceId, "ACTIVE");

        verify(eventPublisher).publishEcommerceStatusChanged(ecommerceId, EcommerceStatus.ACTIVE);
    }

    @Test
    void testPublishEcommerceStatusChanged_inactive() {
        UUID ecommerceId = UUID.randomUUID();

        adapter.publishEcommerceStatusChanged(ecommerceId, "INACTIVE");

        verify(eventPublisher).publishEcommerceStatusChanged(ecommerceId, EcommerceStatus.INACTIVE);
    }

    @Test
    void testPublishEcommerceStatusChanged_invalidStatus_throwsIllegalArgument() {
        UUID ecommerceId = UUID.randomUUID();

        assertThrows(IllegalArgumentException.class,
                () -> adapter.publishEcommerceStatusChanged(ecommerceId, "INVALID_STATUS"));
    }
}
