package com.loyalty.service_admin.domain.repository;

import com.loyalty.service_admin.domain.entity.DiscountPriorityEntity;
import com.loyalty.service_admin.domain.model.DiscountType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface DiscountLimitPriorityRepository extends JpaRepository<DiscountPriorityEntity, UUID> {

    /**
     * Obtiene todas las prioridades para una configuración, ordenadas por nivel.
     */
    @Query("SELECT dp FROM DiscountPriorityEntity dp WHERE dp.discountSettingId = :settingId ORDER BY dp.priorityLevel ASC")
    List<DiscountPriorityEntity> findByDiscountSettingsIdOrderByPriorityLevel(@Param("settingId") UUID settingId);

    /**
     * Obtiene la prioridad para un tipo de descuento específico en una configuración.
     */
    Optional<DiscountPriorityEntity> findByDiscountSettingIdAndDiscountTypeId(UUID settingId, UUID discountTypeId);

    /**
     * Elimina todas las prioridades para una configuración.
     */
    @Modifying
    @Query("DELETE FROM DiscountPriorityEntity dp WHERE dp.discountSettingId = :settingId")
    void deleteByDiscountSettingsId(@Param("settingId") UUID settingId);

    /**
     * Verifica si existe una prioridad con un nivel específico en una configuración.
     */
    @Query("SELECT CASE WHEN COUNT(dp) > 0 THEN true ELSE false END FROM DiscountPriorityEntity dp WHERE dp.discountSettingId = :settingId AND dp.priorityLevel = :priorityLevel")
    boolean existsByDiscountSettingsIdAndPriorityLevel(@Param("settingId") UUID settingId, @Param("priorityLevel") Integer priorityLevel);

    /**
     * Obtiene el máximo nivel de prioridad para una configuración.
     */
    @Query("SELECT COALESCE(MAX(dp.priorityLevel), 0) FROM DiscountPriorityEntity dp WHERE dp.discountSettingId = :settingId")
    Integer findMaxPriorityLevel(@Param("settingId") UUID settingId);
}
