package com.loyalty.service_admin.domain.repository;

import com.loyalty.service_admin.domain.entity.EcommerceEntity;
import com.loyalty.service_admin.domain.model.ecommerce.EcommerceStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface EcommerceRepository extends JpaRepository<EcommerceEntity, UUID>, JpaSpecificationExecutor<EcommerceEntity> {
    
    /**
     * @param slug identificador único (ej. "nike-store")
     * @return Optional con el ecommerce si existe
     */
    Optional<EcommerceEntity> findBySlug(String slug);
    
    /**
     * @param status ACTIVE o INACTIVE
     * @return lista de ecommerces con ese estado
     */
    List<EcommerceEntity> findByStatus(EcommerceStatus status);
    
    /**
     * @param slug identificador único
     * @return true si existe, false en contrario
     */
    boolean existsBySlug(String slug);
}
