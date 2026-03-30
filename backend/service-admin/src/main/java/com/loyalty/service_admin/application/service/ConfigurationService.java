package com.loyalty.service_admin.application.service;

import com.loyalty.service_admin.application.dto.configuration.ConfigurationCreateRequest;
import com.loyalty.service_admin.application.dto.configuration.ConfigurationPatchRequest;
import com.loyalty.service_admin.application.dto.configuration.ConfigurationUpdatedEvent;
import com.loyalty.service_admin.application.dto.configuration.ConfigurationWriteData;
import com.loyalty.service_admin.application.mapper.ConfigurationMapper;
import com.loyalty.service_admin.application.port.in.ConfigurationUseCase;
import com.loyalty.service_admin.application.port.out.ConfigurationEventPort;
import com.loyalty.service_admin.application.port.out.ConfigurationPersistencePort;
import com.loyalty.service_admin.application.port.out.CurrentUserPort;
import com.loyalty.service_admin.application.validation.ConfigurationBusinessValidator;
import com.loyalty.service_admin.domain.entity.DiscountConfigurationEntity;
import com.loyalty.service_admin.infrastructure.exception.AuthorizationException;
import com.loyalty.service_admin.infrastructure.exception.ConfigurationAlreadyExistsException;
import com.loyalty.service_admin.infrastructure.exception.ConfigurationNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ConfigurationService implements ConfigurationUseCase {

    private final ConfigurationPersistencePort configurationRepository;
    private final ConfigurationMapper mapper;
    private final ConfigurationEventPort eventPublisher;
    private final CurrentUserPort currentUserPort;
    private final ConfigurationBusinessValidator validator;

    @Override
    @Transactional
    public ConfigurationWriteData createConfiguration(ConfigurationCreateRequest request) {
        validator.validateCreate(request);
        assertUserCanAccessEcommerce(request.ecommerceId());

        if (configurationRepository.existsByEcommerceId(request.ecommerceId())) {
            throw new ConfigurationAlreadyExistsException("Configuration already exists for ecommerce");
        }

        DiscountConfigurationEntity entity = mapper.toEntity(request);
        DiscountConfigurationEntity saved = configurationRepository.save(entity);
        publishUpdatedEvent(saved);
        return mapper.toWriteData(saved);
    }

    @Override
    @Transactional
    public ConfigurationWriteData patchConfiguration(UUID ecommerceId, ConfigurationPatchRequest request) {
        validator.validatePatch(request);
        assertUserCanAccessEcommerce(ecommerceId);

        DiscountConfigurationEntity entity = configurationRepository.findByEcommerceId(ecommerceId)
                .orElseThrow(() -> new ConfigurationNotFoundException("Configuration not found for ecommerce"));

        mapper.applyPatch(entity, request);
        validator.validateEntityState(entity);

        DiscountConfigurationEntity saved = configurationRepository.save(entity);
        publishUpdatedEvent(saved);
        return mapper.toWriteData(saved);
    }

    private void publishUpdatedEvent(DiscountConfigurationEntity saved) {
        ConfigurationUpdatedEvent event = mapper.toUpdatedEvent(saved);
        eventPublisher.publishConfigUpdated(event);
        log.info("Configuration updated event published for ecommerce={} version={}", saved.getEcommerceId(), saved.getVersion());
    }

    private void assertUserCanAccessEcommerce(UUID ecommerceId) {
        if (currentUserPort.isSuperAdmin()) {
            return;
        }

        UUID currentEcommerceId = currentUserPort.getCurrentUserEcommerceId();
        if (!Objects.equals(currentEcommerceId, ecommerceId)) {
            throw new AuthorizationException("No puedes acceder a configuraciones de otro ecommerce");
        }
    }
}
