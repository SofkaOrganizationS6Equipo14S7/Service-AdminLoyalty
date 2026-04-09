package com.loyalty.service_admin.infrastructure.messaging;

import com.loyalty.service_admin.domain.entity.CustomerTierEntity;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("CustomerTierEventAdapter Unit Tests")
class CustomerTierEventAdapterTest {

    @Mock
    private RabbitTemplate rabbitTemplate;

    @InjectMocks
    private CustomerTierEventAdapter adapter;

    private CustomerTierEntity createTier() {
        CustomerTierEntity tier = new CustomerTierEntity();
        tier.setId(UUID.randomUUID());
        tier.setName("Gold");
        tier.setHierarchyLevel(1);
        tier.setIsActive(true);
        return tier;
    }

    @Test
    void testPublishTierCreated_success() {
        // Arrange
        CustomerTierEntity tier = createTier();
        UUID ecommerceId = UUID.randomUUID();

        // Act
        adapter.publishTierCreated(tier, ecommerceId);

        // Assert
        verify(rabbitTemplate).convertAndSend(eq("loyalty.events"), eq("tier.updated"), anyMap());
    }

    @Test
    void testPublishTierUpdated_success() {
        // Arrange
        CustomerTierEntity tier = createTier();
        UUID ecommerceId = UUID.randomUUID();

        // Act
        adapter.publishTierUpdated(tier, ecommerceId);

        // Assert
        verify(rabbitTemplate).convertAndSend(eq("loyalty.events"), eq("tier.updated"), anyMap());
    }

    @Test
    void testPublishTierDeleted_success() {
        // Arrange
        UUID tierId = UUID.randomUUID();
        UUID ecommerceId = UUID.randomUUID();

        // Act
        adapter.publishTierDeleted(tierId, ecommerceId);

        // Assert
        verify(rabbitTemplate).convertAndSend(eq("loyalty.events"), eq("tier.updated"), anyMap());
    }

    @Test
    void testPublishTierActivated_success() {
        // Arrange
        CustomerTierEntity tier = createTier();
        UUID ecommerceId = UUID.randomUUID();

        // Act
        adapter.publishTierActivated(tier, ecommerceId);

        // Assert
        verify(rabbitTemplate).convertAndSend(eq("loyalty.events"), eq("tier.updated"), anyMap());
    }

    @Test
    void testPublishTierDeactivated_success() {
        // Arrange
        CustomerTierEntity tier = createTier();
        UUID ecommerceId = UUID.randomUUID();

        // Act
        adapter.publishTierDeactivated(tier, ecommerceId);

        // Assert
        verify(rabbitTemplate).convertAndSend(eq("loyalty.events"), eq("tier.updated"), anyMap());
    }

    @Test
    void testPublishTierCreated_exceptionHandled() {
        // Arrange
        CustomerTierEntity tier = createTier();
        UUID ecommerceId = UUID.randomUUID();
        doThrow(new RuntimeException("RabbitMQ down")).when(rabbitTemplate)
                .convertAndSend(anyString(), anyString(), anyMap());

        // Act - should not throw
        adapter.publishTierCreated(tier, ecommerceId);

        // Assert - exception is caught internally
        verify(rabbitTemplate).convertAndSend(anyString(), anyString(), anyMap());
    }

    @Test
    void testPublishTierDeleted_exceptionHandled() {
        // Arrange
        doThrow(new RuntimeException("RabbitMQ down")).when(rabbitTemplate)
                .convertAndSend(anyString(), anyString(), anyMap());

        // Act - should not throw
        adapter.publishTierDeleted(UUID.randomUUID(), UUID.randomUUID());

        // Assert
        verify(rabbitTemplate).convertAndSend(anyString(), anyString(), anyMap());
    }
}
