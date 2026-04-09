package com.loyalty.service_admin.application.port.out;

import com.loyalty.service_admin.application.dto.configuration.ConfigurationUpdatedEvent;

public interface ConfigurationEventPort {
    void publishConfigUpdated(ConfigurationUpdatedEvent event);
}
