package com.loyalty.service_admin.infrastructure.adapter;

import com.loyalty.service_admin.application.port.out.ConfigurationPersistencePort;
import com.loyalty.service_admin.domain.entity.DiscountSettingsEntity;
import com.loyalty.service_admin.infrastructure.persistence.jpa.JpaDiscountConfigurationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class ConfigurationPersistenceAdapter implements ConfigurationPersistencePort {

    private final JpaDiscountConfigurationRepository repository;

    @Override
    public boolean existsByEcommerceId(UUID ecommerceId) {
        return repository.existsByEcommerceId(ecommerceId);
    }

    @Override
    public Optional<DiscountSettingsEntity> findByEcommerceId(UUID ecommerceId) {
        return repository.findByEcommerceId(ecommerceId);
    }

    @Override
    public DiscountSettingsEntity save(DiscountSettingsEntity entity) {
        return repository.save(entity);
    }
}
