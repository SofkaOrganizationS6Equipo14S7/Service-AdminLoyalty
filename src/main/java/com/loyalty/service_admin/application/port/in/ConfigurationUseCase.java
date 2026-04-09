package com.loyalty.service_admin.application.port.in;

import com.loyalty.service_admin.application.dto.configuration.ConfigurationCreateRequest;
import com.loyalty.service_admin.application.dto.configuration.ConfigurationPatchRequest;
import com.loyalty.service_admin.application.dto.configuration.ConfigurationWriteData;

import java.util.UUID;

public interface ConfigurationUseCase {
    ConfigurationWriteData createConfiguration(ConfigurationCreateRequest request);

    ConfigurationWriteData patchConfiguration(UUID ecommerceId, ConfigurationPatchRequest request);
}
