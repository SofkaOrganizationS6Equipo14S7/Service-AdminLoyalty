package com.loyalty.service_admin.infrastructure.persistence.jpa;

import com.loyalty.service_admin.domain.entity.DiscountConfigurationEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface JpaDiscountConfigurationRepository extends JpaRepository<DiscountConfigurationEntity, UUID> {
    Optional<DiscountConfigurationEntity> findByEcommerceId(UUID ecommerceId);

    boolean existsByEcommerceId(UUID ecommerceId);
}
