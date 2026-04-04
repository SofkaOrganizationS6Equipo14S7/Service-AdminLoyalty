package com.loyalty.service_admin.infrastructure.persistence.jpa;

import com.loyalty.service_admin.domain.entity.DiscountSettingsEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface JpaDiscountConfigurationRepository extends JpaRepository<DiscountSettingsEntity, UUID> {
    Optional<DiscountSettingsEntity> findByEcommerceId(UUID ecommerceId);

    boolean existsByEcommerceId(UUID ecommerceId);
}
