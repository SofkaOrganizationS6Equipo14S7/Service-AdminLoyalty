package com.loyalty.service_admin.infrastructure.adapter;

import com.loyalty.service_admin.application.dto.configuration.ConfigurationUpdatedEvent;
import com.loyalty.service_admin.application.port.out.ConfigurationEventPort;
import com.loyalty.service_admin.infrastructure.rabbitmq.ConfigurationEventPublisher;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ConfigurationEventAdapter implements ConfigurationEventPort {

    private final ConfigurationEventPublisher publisher;

    @Override
    public void publishConfigUpdated(ConfigurationUpdatedEvent event) {
        publisher.publishConfigUpdated(event);
    }
}
