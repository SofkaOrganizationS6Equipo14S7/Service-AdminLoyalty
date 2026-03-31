package com.loyalty.service_engine.domain.repository;

import com.loyalty.service_engine.domain.entity.DiscountConfigEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * Repositorio para acceder a configuraciones de tope máximo de descuentos (réplica).
 */
@Repository
public interface DiscountConfigRepository extends JpaRepository<DiscountConfigEntity, UUID> {
    /**
     * Encuentra la configuración activa (is_active = true) más reciente.
     * @return Optional con la configuración activa
     */
    Optional<DiscountConfigEntity> findByIsActiveTrue();
    
    /**
     * Encuentra la configuración activa para un ecommerce específico.
     * @param ecommerceId UUID del ecommerce
     * @return Optional con la configuración activa
     */
    Optional<DiscountConfigEntity> findByEcommerceIdAndIsActiveTrue(UUID ecommerceId);
}
