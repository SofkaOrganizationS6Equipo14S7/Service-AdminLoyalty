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
            boolean applied = configurationCacheService.upsertFromEvent(event);
            log.info("event=config_updated_consumed ecommerceId={} version={} configId={} applied={}",
                    event.ecommerceId(), event.version(), event.configId(), applied);
        } catch (Exception ex) {
            log.error("event=config_updated_consume_failed ecommerceId={} version={} configId={}",
                    event != null ? event.ecommerceId() : null,
                    event != null ? event.version() : null,
                    event != null ? event.configId() : null,
                    ex);
            throw ex;
        }
    }
}
