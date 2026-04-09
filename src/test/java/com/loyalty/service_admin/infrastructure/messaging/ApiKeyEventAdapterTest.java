package com.loyalty.service_admin.infrastructure.messaging;

import com.loyalty.service_admin.application.dto.apikey.ApiKeyEventPayload;
import com.loyalty.service_admin.infrastructure.rabbitmq.ApiKeyEventPublisher;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ApiKeyEventAdapter Unit Tests")
class ApiKeyEventAdapterTest {

    @Mock
    private ApiKeyEventPublisher publisher;

    @InjectMocks
    private ApiKeyEventAdapter adapter;

    @Test
    void testPublishApiKeyCreated_delegatesToPublisher() {
        // Arrange
        ApiKeyEventPayload event = mock(ApiKeyEventPayload.class);

        // Act
        adapter.publishApiKeyCreated(event);

        // Assert
        verify(publisher).publishApiKeyCreated(event);
    }

    @Test
    void testPublishApiKeyDeleted_delegatesToPublisher() {
        // Arrange
        ApiKeyEventPayload event = mock(ApiKeyEventPayload.class);

        // Act
        adapter.publishApiKeyDeleted(event);

        // Assert
        verify(publisher).publishApiKeyDeleted(event);
    }
}
