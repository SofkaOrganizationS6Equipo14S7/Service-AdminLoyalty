package com.loyalty.service_admin.infrastructure.messaging;

import com.loyalty.service_admin.domain.entity.DiscountSettingsEntity;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.math.BigDecimal;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("DiscountConfigEventAdapter Unit Tests")
class DiscountConfigEventAdapterTest {

    @Mock
    private RabbitTemplate rabbitTemplate;

    @InjectMocks
    private DiscountConfigEventAdapter adapter;

    @Test
    void testPublishConfigUpdated_success() {
        // Arrange
        DiscountSettingsEntity config = new DiscountSettingsEntity();
        config.setId(UUID.randomUUID());
        config.setMaxDiscountCap(BigDecimal.valueOf(100));
        config.setCurrencyCode("USD");
        config.setAllowStacking(true);
        config.setRoundingRule("ROUND_HALF_UP");
        UUID ecommerceId = UUID.randomUUID();

        // Act
        adapter.publishConfigUpdated(config, ecommerceId);

        // Assert
        verify(rabbitTemplate).convertAndSend(eq("discount-exchange"), eq("discount.config.updated"), anyMap());
    }

    @Test
    void testPublishConfigDeleted_success() {
        // Arrange
        UUID configId = UUID.randomUUID();
        UUID ecommerceId = UUID.randomUUID();

        // Act
        adapter.publishConfigDeleted(configId, ecommerceId);

        // Assert
        verify(rabbitTemplate).convertAndSend(eq("discount-exchange"), eq("discount.config.updated"), anyMap());
    }

    @Test
    void testPublishConfigUpdated_exceptionHandled() {
        // Arrange
        DiscountSettingsEntity config = new DiscountSettingsEntity();
        config.setId(UUID.randomUUID());
        config.setMaxDiscountCap(BigDecimal.valueOf(100));
        config.setCurrencyCode("USD");
        config.setAllowStacking(true);
        config.setRoundingRule("ROUND_HALF_UP");
        doThrow(new RuntimeException("RabbitMQ down")).when(rabbitTemplate)
                .convertAndSend(anyString(), anyString(), anyMap());

        // Act - should not throw
        adapter.publishConfigUpdated(config, UUID.randomUUID());
    }

    @Test
    void testPublishConfigDeleted_exceptionHandled() {
        // Arrange
        doThrow(new RuntimeException("RabbitMQ down")).when(rabbitTemplate)
                .convertAndSend(anyString(), anyString(), anyMap());

        // Act - should not throw
        adapter.publishConfigDeleted(UUID.randomUUID(), UUID.randomUUID());
    }
}
