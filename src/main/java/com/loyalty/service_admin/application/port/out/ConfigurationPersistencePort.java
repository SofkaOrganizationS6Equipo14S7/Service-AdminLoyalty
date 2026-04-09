package com.loyalty.service_admin.application.port.out;

import com.loyalty.service_admin.domain.entity.DiscountSettingsEntity;

import java.util.Optional;
import java.util.UUID;

public interface ConfigurationPersistencePort {
    boolean existsByEcommerceId(UUID ecommerceId);

    Optional<DiscountSettingsEntity> findByEcommerceId(UUID ecommerceId);

    DiscountSettingsEntity save(DiscountSettingsEntity entity);
}
