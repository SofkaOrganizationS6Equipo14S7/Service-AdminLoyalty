package com.loyalty.service_admin.infrastructure.persistence.jpa;

import com.loyalty.service_admin.application.port.out.DiscountConfigPersistencePort;
import com.loyalty.service_admin.domain.entity.DiscountSettingsEntity;
import com.loyalty.service_admin.domain.repository.DiscountConfigRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

/**
 * JpaDiscountConfigAdapter - Adapter de persistencia para DiscountConfigService.
 *
 * Implementa DiscountConfigPersistencePort delegando a DiscountConfigRepository.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class JpaDiscountConfigAdapter implements DiscountConfigPersistencePort {

    private final DiscountConfigRepository repository;

    @Override
    public DiscountSettingsEntity saveConfig(DiscountSettingsEntity config) {
        log.debug("Saving discount config: {}", config.getId());
        return repository.save(config);
    }

    @Override
    public Optional<DiscountSettingsEntity> findActiveConfigByEcommerce(UUID ecommerceId) {
        log.debug("Finding active discount config for ecommerce: {}", ecommerceId);
        return repository.findActiveByEcommerceId(ecommerceId);
    }

    @Override
    public Optional<DiscountSettingsEntity> findConfigById(UUID configId) {
        log.debug("Finding discount config by id: {}", configId);
        return repository.findById(configId);
    }

    @Override
    public boolean existsActiveConfigForEcommerce(UUID ecommerceId) {
        log.debug("Checking if active discount config exists for ecommerce: {}", ecommerceId);
        return repository.existsByEcommerceIdAndIsActiveTrue(ecommerceId);
    }
}
