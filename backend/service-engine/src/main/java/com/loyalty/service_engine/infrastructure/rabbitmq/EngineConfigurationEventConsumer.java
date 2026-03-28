package com.loyalty.service_engine.infrastructure.rabbitmq;

import com.loyalty.service_engine.application.dto.configuration.ConfigurationUpdatedEvent;
import com.loyalty.service_engine.application.service.EngineConfigurationCacheService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class EngineConfigurationEventConsumer {

    private final EngineConfigurationCacheService configurationCacheService;

    public EngineConfigurationEventConsumer(EngineConfigurationCacheService configurationCacheService) {
        this.configurationCacheService = configurationCacheService;
    }

    @RabbitListener(queues = "${rabbitmq.queue.engine-config-updated:engine.config.updated.queue}", containerFactory = "configEventListenerContainerFactory")
    public void onConfigUpdated(ConfigurationUpdatedEvent event) {
        try {
            configurationCacheService.upsertFromEvent(event);
            log.info("CONFIG_UPDATED processed for ecommerce={} version={}", event.ecommerceId(), event.version());
        } catch (Exception ex) {
            log.error("Failed to process CONFIG_UPDATED event", ex);
            throw ex;
        }
    }
}
