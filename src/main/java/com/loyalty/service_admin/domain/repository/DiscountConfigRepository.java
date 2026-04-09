package com.loyalty.service_admin.domain.repository;

import com.loyalty.service_admin.domain.entity.DiscountSettingsEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface DiscountConfigRepository extends JpaRepository<DiscountSettingsEntity, UUID> {

    /**
     * Obtiene la configuración activa para un ecommerce.
     * Solo debe haber una configuración activa por ecommerce.
     */
    @Query("SELECT dc FROM DiscountSettingsEntity dc WHERE dc.ecommerceId = :ecommerceId AND dc.isActive = true")
    Optional<DiscountSettingsEntity> findActiveByEcommerceId(@Param("ecommerceId") UUID ecommerceId);

    /**
     * Verifica si existe una configuración activa para un ecommerce.
     */
    boolean existsByEcommerceIdAndIsActiveTrue(UUID ecommerceId);
}
