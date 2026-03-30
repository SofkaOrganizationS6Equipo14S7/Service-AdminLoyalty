package com.loyalty.service_admin.application.port.out;

import com.loyalty.service_admin.domain.entity.DiscountConfigurationEntity;

import java.util.Optional;
import java.util.UUID;

public interface ConfigurationPersistencePort {
    boolean existsByEcommerceId(UUID ecommerceId);

    Optional<DiscountConfigurationEntity> findByEcommerceId(UUID ecommerceId);

    DiscountConfigurationEntity save(DiscountConfigurationEntity entity);
}
