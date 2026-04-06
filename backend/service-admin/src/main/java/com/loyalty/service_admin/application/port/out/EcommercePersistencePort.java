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

/**
 * Puerto de Salida: Persistencia
 * 
 * Define operaciones de acceso a datos para Ecommerce.
 * Esta interfaz abstrae la implementación de persistencia (JPA, MongoDB, etc.),
 * permitiendo cambiar la BD sin modificar la lógica de negocio.
 * 
 * SPEC-015: Ecommerce Onboarding con Arquitectura Hexagonal
 * Implementado por: JpaEcommerceAdapter en infrastructure/persistence/jpa
 */
public interface EcommercePersistencePort {
    
    // ==================== Operaciones CRUD en Ecommerce ====================
    
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
    
    // ==================== Cascada: Usuarios ====================
    
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
     * Marca todos como isActive=false y persiste en BD.
     * Se ejecuta dentro de la transacción del updateEcommerceStatus,
     * por lo que si falla, toda la tx rollback.
     * 
     * @param users lista de usuarios a inactivar
     */
    void inactivateUsers(List<UserEntity> users);
    
    // ==================== Cascada: API Keys ====================
    
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
     * Marca todas como isActive=false y persiste en BD.
     * Se ejecuta dentro de la transacción del updateEcommerceStatus,
     * por lo que si falla, toda la tx rollback.
     * 
     * @param keys lista de API Keys a desactivar
     */
    void deactivateApiKeys(List<ApiKeyEntity> keys);
}
