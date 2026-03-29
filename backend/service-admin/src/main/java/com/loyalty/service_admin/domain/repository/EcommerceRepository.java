package com.loyalty.service_admin.domain.repository;

import com.loyalty.service_admin.domain.entity.EcommerceEntity;
import com.loyalty.service_admin.domain.model.ecommerce.EcommerceStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository para acceso a datos de Ecommerce.
 * 
 * SPEC-001: Registro y Gestión de Ecommerces
 * 
 * Responsabilidades:
 * - CRUD básico heredado de JpaRepository<EcommerceEntity, UUID>
 * - Queries especializadas por slug (unicidad)
 * - Filtrado por status (ACTIVE/INACTIVE)
 * - Soporte para Specifications (filtrado dinámico)
 * 
 * Las transacciones y lógica de negocio deben manejarse en la capa de Service,
 * no aquí. Este repositorio es solo un contrato de persistencia.
 */
@Repository
public interface EcommerceRepository extends JpaRepository<EcommerceEntity, UUID>, JpaSpecificationExecutor<EcommerceEntity> {
    
    /**
     * Busca un ecommerce por slug (identificador amigable).
     * 
     * @param slug identificador único (ej. "nike-store")
     * @return Optional con el ecommerce si existe
     */
    Optional<EcommerceEntity> findBySlug(String slug);
    
    /**
     * Lista todos los ecommerces con un estado específico.
     * 
     * @param status ACTIVE o INACTIVE
     * @return lista de ecommerces con ese estado
     */
    List<EcommerceEntity> findByStatus(EcommerceStatus status);
    
    /**
     * Verifica si existe un ecommerce con un slug específico.
     * 
     * @param slug identificador único
     * @return true si existe, false en contrario
     */
    boolean existsBySlug(String slug);
}
