package com.loyalty.service_admin.domain.repository;

import com.loyalty.service_admin.domain.entity.DiscountLimitPriorityEntity;
import com.loyalty.service_admin.domain.model.DiscountType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface DiscountLimitPriorityRepository extends JpaRepository<DiscountLimitPriorityEntity, UUID> {

    /**
     * Obtiene todas las prioridades para una configuración, ordenadas por nivel.
     */
    @Query("SELECT dlp FROM DiscountLimitPriorityEntity dlp WHERE dlp.discountConfigId = :configId ORDER BY dlp.priorityLevel ASC")
    List<DiscountLimitPriorityEntity> findByDiscountConfigIdOrderByPriorityLevel(@Param("configId") UUID configId);

    /**
     * Obtiene la prioridad para un tipo de descuento específico en una configuración.
     */
    Optional<DiscountLimitPriorityEntity> findByDiscountConfigIdAndDiscountType(UUID configId, DiscountType discountType);

    /**
     * Elimina todas las prioridades para una configuración.
     */
    void deleteByDiscountConfigId(UUID configId);

    /**
     * Verifica si existe una prioridad con un nivel específico en una configuración.
     */
    boolean existsByDiscountConfigIdAndPriorityLevel(UUID configId, Integer priorityLevel);

    /**
     * Obtiene el máximo nivel de prioridad para una configuración.
     */
    @Query("SELECT COALESCE(MAX(dlp.priorityLevel), 0) FROM DiscountLimitPriorityEntity dlp WHERE dlp.discountConfigId = :configId")
    Integer findMaxPriorityLevel(@Param("configId") UUID configId);
}
