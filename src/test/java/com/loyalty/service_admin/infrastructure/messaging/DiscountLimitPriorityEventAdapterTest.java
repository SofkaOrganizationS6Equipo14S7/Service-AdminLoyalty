package com.loyalty.service_admin.infrastructure.messaging;

import com.loyalty.service_admin.domain.entity.DiscountPriorityEntity;
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
@DisplayName("DiscountLimitPriorityEventAdapter Unit Tests")
class DiscountLimitPriorityEventAdapterTest {

    @Mock
    private RabbitTemplate rabbitTemplate;

    @InjectMocks
    private DiscountLimitPriorityEventAdapter adapter;

    @Test
    void testPublishPriorityUpdated_success() {
        // Arrange
        DiscountPriorityEntity priority = new DiscountPriorityEntity();
        priority.setId(UUID.randomUUID());
        priority.setDiscountTypeId(UUID.randomUUID());
        priority.setPriorityLevel(1);
        priority.setDiscountSettingId(UUID.randomUUID());
        UUID ecommerceId = UUID.randomUUID();

        // Act
        adapter.publishPriorityUpdated(priority, ecommerceId);

        // Assert
        verify(rabbitTemplate).convertAndSend(eq("discount-exchange"), eq("discount.config.updated"), anyMap());
    }

    @Test
    void testPublishPriorityDeleted_success() {
        // Arrange
        UUID priorityId = UUID.randomUUID();
        UUID ecommerceId = UUID.randomUUID();

        // Act
        adapter.publishPriorityDeleted(priorityId, ecommerceId);

        // Assert
        verify(rabbitTemplate).convertAndSend(eq("discount-exchange"), eq("discount.config.updated"), anyMap());
    }

    @Test
    void testPublishPriorityUpdated_exceptionHandled() {
        // Arrange
        DiscountPriorityEntity priority = new DiscountPriorityEntity();
        priority.setId(UUID.randomUUID());
        priority.setDiscountTypeId(UUID.randomUUID());
        priority.setPriorityLevel(1);
        priority.setDiscountSettingId(UUID.randomUUID());
        doThrow(new RuntimeException("error")).when(rabbitTemplate)
                .convertAndSend(anyString(), anyString(), anyMap());

        // Act - should not throw
        adapter.publishPriorityUpdated(priority, UUID.randomUUID());
    }

    @Test
    void testPublishPriorityDeleted_exceptionHandled() {
        // Arrange
        doThrow(new RuntimeException("error")).when(rabbitTemplate)
                .convertAndSend(anyString(), anyString(), anyMap());

        // Act - should not throw
        adapter.publishPriorityDeleted(UUID.randomUUID(), UUID.randomUUID());
    }
}
