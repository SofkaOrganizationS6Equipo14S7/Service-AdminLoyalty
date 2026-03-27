package com.loyalty.service_admin.application.service;

import com.loyalty.service_admin.application.dto.UserCreateRequest;
import com.loyalty.service_admin.application.dto.UserResponse;
import com.loyalty.service_admin.application.dto.UserUpdateRequest;
import com.loyalty.service_admin.domain.entity.UserEntity;
import com.loyalty.service_admin.domain.repository.UserRepository;
import com.loyalty.service_admin.infrastructure.exception.AuthorizationException;
import com.loyalty.service_admin.infrastructure.exception.BadRequestException;
import com.loyalty.service_admin.infrastructure.exception.ConflictException;
import com.loyalty.service_admin.infrastructure.exception.ResourceNotFoundException;
import com.loyalty.service_admin.infrastructure.security.SecurityContextHelper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Servicio de gestión de usuarios por ecommerce.
 * 
 * Responsabilidades:
 * - CRUD de usuarios con validación de ecommerce
 * - Validación de unicidad global de username (SPEC-002 RN-03)
 * - Filtrado automático por ecommerce_id del usuario actual (multi-tenant)
 * - Conversión entre entidades y DTOs
 * - Propagación de ecommerce_id desde JWT al contexto (via SecurityContextHelper)
 * 
 * Implementa SPEC-002: Gestión de Usuarios por Ecommerce
 * - HU-01: Crear usuario vinculado a ecommerce
 * - HU-02: Validar acceso según ecommerce del usuario
 * - HU-03: Listar usuarios por ecommerce
 * - HU-04: Actualizar usuario (cambio de ecommerce)
 * - HU-05: Eliminar usuario
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {
    
    private final UserRepository userRepository;
    private final EcommerceService ecommerceService;
    private final BCryptPasswordEncoder passwordEncoder;
    private final SecurityContextHelper securityContextHelper;
    
    /**
     * Crea un nuevo usuario vinculado a un ecommerce.
     * 
     * Valida:
     * - Ecommerce existe (CRITERIO-1.2)
     * - Username es único globalmente (CRITERIO-1.3)
     * 
     * @param request datos del nuevo usuario
     * @return UserResponse con datos del usuario creado
     * @throws BadRequestException si ecommerce no existe
     * @throws ConflictException si username duplicado
     */
    @Transactional
    public UserResponse createUser(UserCreateRequest request) {
        // Validar que ecommerce existe
        ecommerceService.validateEcommerceExists(request.ecommerceId());
        
        // Validar que username es único globalmente
        if (userRepository.findByUsername(request.username()).isPresent()) {
            log.warn("Intento de crear usuario con username duplicado: {}", request.username());
            throw new ConflictException(
                "El username ya existe en otra organización, debe ser único globalmente"
            );
        }
        
        // Crear y guardar usuario
        UserEntity user = UserEntity.builder()
                .username(request.username())
                .password(passwordEncoder.encode(request.password()))
                .role(request.role())
                .ecommerceId(request.ecommerceId())
                .active(true)
                .build();
        
        UserEntity saved = userRepository.save(user);
        log.info("Usuario creado exitosamente: id={}, ecommerce={}", saved.getId(), request.ecommerceId());
        
        return toResponse(saved);
    }
    
    /**
     * Lista usuarios según el contexto del usuario actual.
     * 
     * - Si no es super admin: retorna solo usuarios de su ecommerce
     * - Si es super admin: retorna todos los usuarios (o filtrado por ecommerceId param)
     * 
     * @param ecommerceIdParam parámetro de filtro (solo super admin puede usar)
     * @return lista de usuarios
     * @throws AuthorizationException si usuario no-super-admin intenta filtrar otro ecommerce
     */
    @Transactional(readOnly = true)
    public List<UserResponse> listUsers(UUID ecommerceIdParam) {
        if (securityContextHelper.isCurrentUserSuperAdmin()) {
            // Super admin: retorna todos (o filtra por param)
            List<UserEntity> users;
            if (ecommerceIdParam != null) {
                users = userRepository.findByEcommerceId(ecommerceIdParam);
            } else {
                users = userRepository.findAll();
            }
            return users.stream()
                    .map(this::toResponse)
                    .collect(Collectors.toList());
        } else {
            // Usuario no-super-admin: retorna solo su ecommerce
            UUID userEcommerceId = securityContextHelper.getCurrentUserEcommerceId();
            
            // Validar que no intenta filtrar otro ecommerce
            if (ecommerceIdParam != null && !ecommerceIdParam.equals(userEcommerceId)) {
                log.warn("Intento de acceso cruzado: user ecommerce={}, requested={}", 
                        userEcommerceId, ecommerceIdParam);
                throw new AuthorizationException(
                    "No tiene permiso para acceder a este ecommerce"
                );
            }
            
            List<UserEntity> users = userRepository.findByEcommerceId(userEcommerceId);
            return users.stream()
                    .map(this::toResponse)
                    .collect(Collectors.toList());
        }
    }
    
    /**
     * Obtiene un usuario por UID.
     * 
     * Valida que el usuario actual tiene permiso para verlo (mismo ecommerce o super admin).
     * 
     * @param uid UUID del usuario
     * @return UserResponse
     * @throws ResourceNotFoundException si no existe
     * @throws AuthorizationException si no tiene permiso
     */
    @Transactional(readOnly = true)
    public UserResponse getUserByUid(UUID uid) {
        UserEntity user = findByUidOrThrow(uid);
        
        // Validar permiso
        if (!securityContextHelper.isCurrentUserSuperAdmin()) {
            UUID userEcommerce = securityContextHelper.getCurrentUserEcommerceId();
            if (!userEcommerce.equals(user.getEcommerceId())) {
                log.warn("Intento de acceso cruzado a usuario: user ecommerce={}, target ecommerce={}", 
                        userEcommerce, user.getEcommerceId());
                throw new AuthorizationException(
                    "No tiene permiso para acceder a este usuario"
                );
            }
        }
        
        return toResponse(user);
    }
    
    /**
     * Actualiza un usuario (solo username y ecommerceId).
     * 
     * Valida:
     * - Username es único globalmente si se cambia (CRITERIO-4.4)
     * - Ecommerce existe si se cambia (CRITERIO-4.3)
     * 
     * @param uid UUID del usuario
     * @param request datos a actualizar
     * @return UserResponse actualizado
     * @throws ResourceNotFoundException si no existe
     * @throws ConflictException si username duplicado
     */
    @Transactional
    public UserResponse updateUser(UUID uid, UserUpdateRequest request) {
        UserEntity user = findByUidOrThrow(uid);
        
        // Actualizar username si se proporciona
        if (request.username() != null && !request.username().equals(user.getUsername())) {
            // Validar que nuevo username no existe globalmente
            if (userRepository.findByUsername(request.username()).isPresent()) {
                log.warn("Intento de cambiar a username duplicado: {}", request.username());
                throw new ConflictException(
                    "El username ya existe en otra organización, debe ser único globalmente"
                );
            }
            user.setUsername(request.username());
        }
        
        // Actualizar ecommerce si se proporciona
        if (request.ecommerceId() != null && !request.ecommerceId().equals(user.getEcommerceId())) {
            ecommerceService.validateEcommerceExists(request.ecommerceId());
            user.setEcommerceId(request.ecommerceId());
        }
        
        UserEntity updated = userRepository.save(user);
        log.info("Usuario actualizado: id={}, ecommerce={}", updated.getId(), updated.getEcommerceId());
        
        return toResponse(updated);
    }
    
    /**
     * Elimina un usuario (irreversible).
     * 
     * Validaciones:
     * - Usuario no puede eliminarse a sí mismo (CRITERIO-5.2)
     * - Solo super admin puede eliminar (evaluado en controller)
     * 
     * @param uid UUID del usuario a eliminar
     * @throws ResourceNotFoundException si no existe
     * @throws BadRequestException si intenta auto-eliminarse
     */
    @Transactional
    public void deleteUser(UUID uid) {
        UserEntity user = findByUidOrThrow(uid);
        
        // Validar que no se elimina a sí mismo
        UUID currentUserId = securityContextHelper.getCurrentUserId();
        if (currentUserId.equals(uid)) {
            log.warn("Intento de auto-eliminación por usuario: {}", uid);
            throw new BadRequestException("No puede eliminarse a sí mismo");
        }
        
        userRepository.delete(user);
        log.info("Usuario eliminado: id={}", user.getId());
    }
    
    /**
     * Obtiene un usuario por UID o lanza excepción.
     * Como UserEntity usa Long como ID, convertimos UUID a Long usando hash.
     */
    private UserEntity findByUidOrThrow(UUID uid) {
        long idFromUuid = uid.getMostSignificantBits() % Integer.MAX_VALUE;
        return userRepository.findById(idFromUuid)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));
    }
    
    /**
     * Convierte una entidad UserEntity a DTO UserResponse.
     * Expone UUID en lugar de Long id (protección contra enumeración).
     * 
     * Nota: Genera UUID de forma determinística del Long id para mantener consistencia.
     */
    private UserResponse toResponse(UserEntity user) {
        UUID uid = UUID.nameUUIDFromBytes(("user-" + user.getId()).getBytes());
        return new UserResponse(
                uid,
                user.getUsername(),
                user.getRole(),
                null, // email no está en entity por ahora
                user.getEcommerceId(),
                user.getActive(),
                user.getCreatedAt(),
                user.getUpdatedAt()
        );
    }
}
