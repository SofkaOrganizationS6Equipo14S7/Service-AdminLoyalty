package com.loyalty.service_admin.application.port.out;

import com.loyalty.service_admin.domain.entity.EcommerceEntity;
import com.loyalty.service_admin.domain.entity.UserEntity;
import com.loyalty.service_admin.domain.entity.ApiKeyEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface EcommercePersistencePort {
    
    /**
     * Persiste un ecommerce en la BD.
     * 
     * @param entity ecommerce a persistir
     * @return entidad persistida con ID generado o actualizado
     */
    EcommerceEntity save(EcommerceEntity entity);
    
    /**
     * Obtiene un ecommerce por uid.
     * 
     * @param id uuid del ecommerce
     * @return Optional con la entidad si existe
     */
    Optional<EcommerceEntity> findById(UUID id);
    
    /**
     * Obtiene página de ecommerces con especificación JPA (filtros complejos).
     * 
     * @param spec especificación de criterios (ej. status = ACTIVE)
     * @param pageable información de paginación
     * @return página con ecommerces
     */
    Page<EcommerceEntity> findAll(Specification<EcommerceEntity> spec, Pageable pageable);
    
    /**
     * Verifica si existe un ecommerce con ese slug.
     * 
     * @param slug slug a verificar
     * @return true si existe, false en caso contrario
     */
    boolean existsBySlug(String slug);
    
    /**
     * Verifica si existe un ecommerce con ese uuid.
     * 
     * @param id uuid a verificar
     * @return true si existe, false en caso contrario
     */
    boolean existsById(UUID id);
    
    /**
     * Obtiene todos los usuarios vinculados a un ecommerce.
     * 
     * @param ecommerceId uuid del ecommerce
     * @return lista de usuarios (puede estar vacía)
     */
    List<UserEntity> findUsersByEcommerceId(UUID ecommerceId);
    
    /**
     * Inactiva múltiples usuarios de forma atómica.
     * 
     * @param users lista de usuarios a inactivar
     */
    void inactivateUsers(List<UserEntity> users);
    
    /**
     * Obtiene todas las API Keys vinculadas a un ecommerce.
     * 
     * @param ecommerceId uuid del ecommerce
     * @return lista de API Keys (puede estar vacía)
     */
    List<ApiKeyEntity> findApiKeysByEcommerceId(UUID ecommerceId);
    
    /**
     * Desactiva múltiples API Keys de forma atómica.
     * 
     * @param keys lista de API Keys a desactivar
     */
    void deactivateApiKeys(List<ApiKeyEntity> keys);
}
