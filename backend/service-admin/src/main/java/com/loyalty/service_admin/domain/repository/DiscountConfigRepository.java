package com.loyalty.service_admin.domain.repository;

import com.loyalty.service_admin.domain.entity.DiscountConfigEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * Repositorio para acceder a configuraciones de tope máximo de descuentos.
 */
@Repository
public interface DiscountConfigRepository extends JpaRepository<DiscountConfigEntity, UUID> {
    /**
     * Encuentra la configuración activa (is_active = true).
     * @return Optional con la configuración activa
     */
    Optional<DiscountConfigEntity> findByIsActiveTrue();
}
