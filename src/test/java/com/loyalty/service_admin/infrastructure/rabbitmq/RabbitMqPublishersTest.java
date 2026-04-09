package com.loyalty.service_admin.infrastructure.rabbitmq;

import com.loyalty.service_admin.application.dto.apikey.ApiKeyEventPayload;
import com.loyalty.service_admin.application.dto.configuration.ConfigurationUpdatedEvent;
import com.loyalty.service_admin.application.dto.ecommerce.EcommerceCreatedEvent;
import com.loyalty.service_admin.application.dto.ecommerce.EcommerceStatusChangedEvent;
import com.loyalty.service_admin.domain.entity.DiscountPriorityEntity;
import com.loyalty.service_admin.domain.entity.DiscountSettingsEntity;
import com.loyalty.service_admin.domain.model.CapAppliesTo;
import com.loyalty.service_admin.domain.model.CapType;
import com.loyalty.service_admin.domain.model.RoundingRule;
import com.loyalty.service_admin.domain.model.ecommerce.EcommerceStatus;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.core.MessagePostProcessor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("RabbitMQ Publisher Unit Tests")
class RabbitMqPublishersTest {

    // ==================== ApiKeyEventPublisher ====================
    @Nested
    @DisplayName("ApiKeyEventPublisher")
    class ApiKeyPublisherTests {

        @Mock
        private RabbitTemplate rabbitTemplate;

        @Mock
        private ObjectMapper objectMapper;

        @InjectMocks
        private ApiKeyEventPublisher publisher;

        private void setFields() {
            ReflectionTestUtils.setField(publisher, "exchangeName", "loyalty.config.exchange");
            ReflectionTestUtils.setField(publisher, "routingKey", "api.keys");
            ReflectionTestUtils.setField(publisher, "deadLetterExchange", "loyalty.config.dlx");
            ReflectionTestUtils.setField(publisher, "deadLetterRoutingKey", "api.keys.dlq");
        }

        private ApiKeyEventPayload createPayload(String type) {
            return new ApiKeyEventPayload(type, "key-1", "hashed", "ecom-1", Instant.now());
        }

        @Test
        void testPublishApiKeyCreated_success() throws Exception {
            setFields();
            ApiKeyEventPayload payload = createPayload("API_KEY_CREATED");
            when(objectMapper.writeValueAsString(payload)).thenReturn("{}");

            publisher.publishApiKeyCreated(payload);

            verify(rabbitTemplate).convertAndSend(eq("loyalty.config.exchange"), eq("api.keys"), eq("{}"), any(MessagePostProcessor.class));
        }

        @Test
        void testPublishApiKeyDeleted_success() throws Exception {
            setFields();
            ApiKeyEventPayload payload = createPayload("API_KEY_DELETED");
            when(objectMapper.writeValueAsString(payload)).thenReturn("{}");

            publisher.publishApiKeyDeleted(payload);

            verify(rabbitTemplate).convertAndSend(eq("loyalty.config.exchange"), eq("api.keys"), eq("{}"), any(MessagePostProcessor.class));
        }

        @Test
        void testPublishApiKey_failure_sendsToDLQ() throws Exception {
            setFields();
            ApiKeyEventPayload payload = createPayload("API_KEY_CREATED");
            when(objectMapper.writeValueAsString(payload)).thenThrow(new RuntimeException("serialize error"));

            assertDoesNotThrow(() -> publisher.publishApiKeyCreated(payload));

            verify(rabbitTemplate).convertAndSend(eq("loyalty.config.dlx"), eq("api.keys.dlq"), eq(payload), any(MessagePostProcessor.class));
        }
    }

    // ==================== ConfigurationEventPublisher ====================
    @Nested
    @DisplayName("ConfigurationEventPublisher")
    class ConfigPublisherTests {

        @Mock
        private RabbitTemplate rabbitTemplate;

        @InjectMocks
        private ConfigurationEventPublisher publisher;

        private void setFields() {
            ReflectionTestUtils.setField(publisher, "exchange", "loyalty.config.exchange");
            ReflectionTestUtils.setField(publisher, "routingKey", "config.updated");
            ReflectionTestUtils.setField(publisher, "deadLetterExchange", "loyalty.config.dlx");
            ReflectionTestUtils.setField(publisher, "deadLetterRoutingKey", "config.updated.dlq");
        }

        @Test
        void testPublishConfigUpdated_success() {
            setFields();
            ConfigurationUpdatedEvent event = new ConfigurationUpdatedEvent(
                    "CONFIG_UPDATED", UUID.randomUUID(), UUID.randomUUID(), 1L,
                    "USD", RoundingRule.HALF_UP, CapType.PERCENTAGE,
                    BigDecimal.TEN, CapAppliesTo.TOTAL, List.of(), Instant.now()
            );

            publisher.publishConfigUpdated(event);

            verify(rabbitTemplate).convertAndSend(eq("loyalty.config.exchange"), eq("config.updated"), eq(event), any(MessagePostProcessor.class));
        }

        @Test
        void testPublishConfigUpdated_failure_sendsToDLQ() {
            setFields();
            ConfigurationUpdatedEvent event = new ConfigurationUpdatedEvent(
                    "CONFIG_UPDATED", UUID.randomUUID(), UUID.randomUUID(), 1L,
                    "USD", RoundingRule.HALF_UP, CapType.PERCENTAGE,
                    BigDecimal.TEN, CapAppliesTo.TOTAL, List.of(), Instant.now()
            );
            doThrow(new RuntimeException("connection error"))
                    .doNothing()
                    .when(rabbitTemplate).convertAndSend(eq("loyalty.config.exchange"), eq("config.updated"), eq(event), any(MessagePostProcessor.class));

            assertDoesNotThrow(() -> publisher.publishConfigUpdated(event));

            verify(rabbitTemplate).convertAndSend(eq("loyalty.config.dlx"), eq("config.updated.dlq"), eq(event), any(MessagePostProcessor.class));
        }
    }

    // ==================== DiscountConfigEventPublisher ====================
    @Nested
    @DisplayName("DiscountConfigEventPublisher")
    class DiscountConfigPublisherTests {

        @Mock
        private RabbitTemplate rabbitTemplate;

        @InjectMocks
        private DiscountConfigEventPublisher publisher;

        @Test
        void testPublishDiscountConfigUpdated_success() {
            DiscountSettingsEntity config = new DiscountSettingsEntity();
            config.setId(UUID.randomUUID());
            config.setEcommerceId(UUID.randomUUID());
            config.setMaxDiscountCap(BigDecimal.TEN);
            config.setCurrencyCode("USD");
            config.setIsActive(true);
            config.setUpdatedAt(Instant.now());

            publisher.publishDiscountConfigUpdated(config);

            verify(rabbitTemplate).convertAndSend(eq("discount-exchange"), eq("discount.config.updated"), any(DiscountConfigEventPublisher.DiscountConfigUpdatedEvent.class));
        }

        @Test
        void testPublishDiscountConfigUpdated_failure_noException() {
            DiscountSettingsEntity config = new DiscountSettingsEntity();
            config.setId(UUID.randomUUID());
            config.setEcommerceId(UUID.randomUUID());
            config.setMaxDiscountCap(BigDecimal.TEN);
            config.setCurrencyCode("USD");
            config.setIsActive(true);
            config.setUpdatedAt(Instant.now());

            doThrow(new RuntimeException("RabbitMQ down"))
                    .when(rabbitTemplate).convertAndSend(anyString(), anyString(), any(Object.class));

            assertDoesNotThrow(() -> publisher.publishDiscountConfigUpdated(config));
        }

        @Test
        void testPublishDiscountPriorityUpdated_success() {
            UUID configId = UUID.randomUUID();
            List<DiscountPriorityEntity> priorities = List.of(new DiscountPriorityEntity());

            publisher.publishDiscountPriorityUpdated(configId, priorities);

            verify(rabbitTemplate).convertAndSend(eq("discount-exchange"), eq("discount.priority.updated"), any(DiscountConfigEventPublisher.DiscountPriorityUpdatedEvent.class));
        }

        @Test
        void testPublishDiscountPriorityUpdated_failure_noException() {
            UUID configId = UUID.randomUUID();
            doThrow(new RuntimeException("RabbitMQ down"))
                    .when(rabbitTemplate).convertAndSend(anyString(), anyString(), any(Object.class));

            assertDoesNotThrow(() -> publisher.publishDiscountPriorityUpdated(configId, List.of()));
        }

        @Test
        void testDiscountConfigUpdatedEvent_record() {
            var event = new DiscountConfigEventPublisher.DiscountConfigUpdatedEvent(
                    "config-1", "ecom-1", "100.00", "USD", true, "2024-01-01T00:00:00Z"
            );
            assertEquals("config-1", event.configUid());
            assertEquals("USD", event.currencyCode());
        }

        @Test
        void testDiscountPriorityUpdatedEvent_record() {
            var event = new DiscountConfigEventPublisher.DiscountPriorityUpdatedEvent(
                    "config-1", 3, 1234567890L
            );
            assertEquals("config-1", event.configId());
            assertEquals(3, event.priorityCount());
        }
    }

    // ==================== EcommerceEventPublisher ====================
    @Nested
    @DisplayName("EcommerceEventPublisher")
    class EcommercePublisherTests {

        @Mock
        private RabbitTemplate rabbitTemplate;

        @Mock
        private ObjectMapper objectMapper;

        @InjectMocks
        private EcommerceEventPublisher publisher;

        private void setFields() {
            ReflectionTestUtils.setField(publisher, "exchangeName", "loyalty.events");
        }

        @Test
        void testPublishEcommerceCreated_success() throws Exception {
            setFields();
            UUID ecommerceId = UUID.randomUUID();
            when(objectMapper.writeValueAsString(any())).thenReturn("{}");

            publisher.publishEcommerceCreated(ecommerceId, "Test Store", "test-store");

            verify(rabbitTemplate).convertAndSend(eq("loyalty.events"), eq(""), eq("{}"));
        }

        @Test
        void testPublishEcommerceStatusChanged_success() throws Exception {
            setFields();
            UUID ecommerceId = UUID.randomUUID();
            when(objectMapper.writeValueAsString(any())).thenReturn("{}");

            publisher.publishEcommerceStatusChanged(ecommerceId, EcommerceStatus.INACTIVE);

            verify(rabbitTemplate).convertAndSend(eq("loyalty.events"), eq(""), eq("{}"));
        }

        @Test
        void testPublishEvent_failure_throwsRuntimeException() throws Exception {
            setFields();
            when(objectMapper.writeValueAsString(any())).thenThrow(new RuntimeException("serialize error"));

            assertThrows(RuntimeException.class,
                    () -> publisher.publishEcommerceCreated(UUID.randomUUID(), "name", "slug"));
        }
    }
}
