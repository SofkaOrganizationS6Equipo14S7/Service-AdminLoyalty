package com.loyalty.service_admin.domain.repository;

import com.loyalty.service_admin.domain.entity.DiscountPriorityEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repositorio para acceder a configuraciones de prioridad de descuentos.
 */
@Repository
public interface DiscountPriorityRepository extends JpaRepository<DiscountPriorityEntity, UUID> {
    /**
     * Encuentra todas las prioridades de una configuración de descuentos.
     * @param discountConfigId ID de la configuración de descuentos
     * @return Lista de prioridades ordenadas por priority_level
     */
    List<DiscountPriorityEntity> findByDiscountConfigIdOrderByPriorityLevel(UUID discountConfigId);
    
    /**
     * Encuentra una prioridad de descuento específica.
     * @param discountConfigId ID de la configuración
     * @param discountType Tipo de descuento
     * @return Optional con la prioridad si existe
     */
    Optional<DiscountPriorityEntity> findByDiscountConfigIdAndDiscountType(UUID discountConfigId, String discountType);
    
    /**
     * Verifica si existe una prioridad específica en una configuración.
     * @param discountConfigId ID de la configuración
     * @param priorityLevel Nivel de prioridad
     * @return true si existe, false de lo contrario
     */
    boolean existsByDiscountConfigIdAndPriorityLevel(UUID discountConfigId, Integer priorityLevel);
    
    /**
     * Elimina todas las prioridades de una configuración.
     * @param discountConfigId ID de la configuración
     */
    void deleteByDiscountConfigId(UUID discountConfigId);
}
