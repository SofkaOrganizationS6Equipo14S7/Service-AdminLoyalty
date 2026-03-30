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
     * SPEC-003 RN-05: SUPER_ADMIN y STORE_ADMIN pueden crear usuarios
     * 
     * Validaciones:
     * - SUPER_ADMIN o STORE_ADMIN (CRITERIO-3.1.1)
     * - Role debe ser "STORE_USER" (no se pueden crear SUPER_ADMIN o STORE_ADMIN via API)
     * - Ecommerce existe (CRITERIO-3.1.5)
     * - Username es único globalmente (CRITERIO-3.1.2)
     * - Email es único globalmente (CRITERIO-3.1.4)
     * - Si STORE_ADMIN: solo puede crear en su propio ecommerce (CRITERIO-3.1.6)
     * 
     * @param request datos del nuevo usuario
     * @return UserResponse con datos del usuario creado
     * @throws AuthorizationException si no es SUPER_ADMIN/STORE_ADMIN o intenta crear en otro ecommerce
     * @throws BadRequestException si role no es "STORE_USER" o ecommerce no existe
     * @throws ConflictException si username/email duplicado globalmente
     */
    @Transactional
    public UserResponse createUser(UserCreateRequest request) {
        // AUTORIZACIÓN: Solo SUPER_ADMIN y STORE_ADMIN pueden crear usuarios
        String currentRole = securityContextHelper.getCurrentUserRole();
        if (!"SUPER_ADMIN".equals(currentRole) && !"STORE_ADMIN".equals(currentRole)) {
            log.warn("Intento de crear usuario sin permisos. Role actual: {}", currentRole);
            throw new AuthorizationException(
                "Solo administradores pueden crear usuarios"
            );
        }
        
        // VALIDACIÓN: Si es STORE_ADMIN, solo puede crear en su propio ecommerce
        if ("STORE_ADMIN".equals(currentRole)) {
            UUID storeAdminEcommerce = securityContextHelper.getCurrentUserEcommerceId();
            if (!storeAdminEcommerce.equals(request.ecommerceId())) {
                log.warn("STORE_ADMIN intenta crear usuario en otro ecommerce. Own: {}, Requested: {}", 
                        storeAdminEcommerce, request.ecommerceId());
                throw new AuthorizationException(
                    "No puede crear usuarios fuera de su ecommerce"
                );
            }
        }
        
        // VALIDACIÓN: Role debe ser "STORE_USER" (no permitimos crear SUPER_ADMIN o STORE_ADMIN)
        if (!"STORE_USER".equals(request.role())) {
            log.warn("Intento de crear usuario con rol no autorizado: {}", request.role());
            throw new BadRequestException(
                "Solo se pueden crear usuarios con rol STORE_USER"
            );
        }
        
        // Validar que ecommerce existe
        ecommerceService.validateEcommerceExists(request.ecommerceId());
        
        // Validar que username es único globalmente
        if (userRepository.findByUsername(request.username()).isPresent()) {
            log.warn("Intento de crear usuario con username duplicado: {}", request.username());
            throw new ConflictException(
                "Username ya existe en el sistema"
            );
        }
        
        // Validar que email es único globalmente
        if (userRepository.findByEmail(request.email()).isPresent()) {
            log.warn("Intento de crear usuario con email duplicado: {}", request.email());
            throw new ConflictException(
                "Email ya existe en el sistema"
            );
        }
        
        // Crear y guardar usuario
        UserEntity user = UserEntity.builder()
                .username(request.username())
                .email(request.email())
                .password(passwordEncoder.encode(request.password()))
                .role(request.role())
                .ecommerceId(request.ecommerceId())
                .active(true)
                .build();
        
        UserEntity saved = userRepository.save(user);
        log.info("Usuario creado exitosamente: uid={}, username={}, email={}, ecommerce={}", 
                saved.getUid(), saved.getUsername(), saved.getEmail(), saved.getEcommerceId());
        
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
     * @param uid UUID único del usuario
     * @return UserResponse
     * @throws ResourceNotFoundException si no existe
     * @throws AuthorizationException si no tiene permiso
     */
    @Transactional(readOnly = true)
    public UserResponse getUserByUid(UUID uid) {
        UserEntity user = userRepository.findByUid(uid)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));
        
        // Validar permiso (SUPER_ADMIN ve todos, USER solo su ecommerce)
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
     * Actualiza un usuario (username, email, password).
     * 
     * SPEC-003 HU-03.3: Actualizar datos de usuario estándar
     * SPEC-003 RN-05: SUPER_ADMIN y STORE_ADMIN pueden actualizar usuarios
     * 
     * Validaciones:
     * - SUPER_ADMIN o STORE_ADMIN (CRITERIO-3.3.2)
     * - Si STORE_ADMIN: usuario debe pertenecer a su propio ecommerce (TenantInterceptor)
     * - Username y email no se pueden cambiar para duplicar globalmente (CRITERIO-3.3.1)
     * - Role NO se puede cambiar (CRITERIO-3.3.3)
     * 
     * @param uid UUID único del usuario
     * @param request datos a actualizar (todos opcionales). NO soporta cambios de: role, ecommerceId, active
     * @return UserResponse actualizado
     * @throws AuthorizationException si no es SUPER_ADMIN/STORE_ADMIN o usuario no pertenece a su ecommerce
     * @throws ResourceNotFoundException si no existe
     * @throws ConflictException si username/email duplicado globalmente
     */
    @Transactional
    public UserResponse updateUser(UUID uid, UserUpdateRequest request) {
        // AUTORIZACIÓN: Solo SUPER_ADMIN y STORE_ADMIN pueden actualizar usuarios
        String currentRole = securityContextHelper.getCurrentUserRole();
        if (!"SUPER_ADMIN".equals(currentRole) && !"STORE_ADMIN".equals(currentRole)) {
            log.warn("Intento de actualizar usuario sin permisos. Role actual: {}", currentRole);
            throw new AuthorizationException(
                "Solo administradores pueden actualizar usuarios"
            );
        }
        
        UserEntity user = userRepository.findByUid(uid)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));
        
        // VALIDACIÓN: Si es STORE_ADMIN, el usuario debe pertenecer a su ecommerce
        // (suplementario a TenantInterceptor, pero buena práctica)
        if ("STORE_ADMIN".equals(currentRole)) {
            UUID storeAdminEcommerce = securityContextHelper.getCurrentUserEcommerceId();
            if (!storeAdminEcommerce.equals(user.getEcommerceId())) {
                log.warn("STORE_ADMIN intenta actualizar usuario de otro ecommerce. Own: {}, Target: {}", 
                        storeAdminEcommerce, user.getEcommerceId());
                throw new AuthorizationException(
                    "No puede editar usuarios de otro ecommerce"
                );
            }
        }
        
        // Actualizar username si se proporciona
        if (request.username() != null && !request.username().isEmpty() && 
                !request.username().equals(user.getUsername())) {
            // Validar que nuevo username no existe globalmente
            if (userRepository.findByUsername(request.username()).isPresent()) {
                log.warn("Intento de cambiar a username duplicado: {}", request.username());
                throw new ConflictException(
                    "Username ya existe en el sistema"
                );
            }
            user.setUsername(request.username());
        }
        
        // Actualizar email si se proporciona
        if (request.email() != null && !request.email().isEmpty() && 
                !request.email().equals(user.getEmail())) {
            // Validar que nuevo email no existe globalmente
            if (userRepository.findByEmail(request.email()).isPresent()) {
                log.warn("Intento de cambiar a email duplicado: {}", request.email());
                throw new ConflictException(
                    "Email ya existe en el sistema"
                );
            }
            user.setEmail(request.email());
        }
        
        // Actualizar password si se proporciona
        if (request.password() != null && !request.password().isEmpty()) {
            user.setPassword(passwordEncoder.encode(request.password()));
        }
        
        UserEntity updated = userRepository.save(user);
        log.info("Usuario actualizado: uid={}, username={}, email={}, ecommerce={}", 
                updated.getUid(), updated.getUsername(), updated.getEmail(), updated.getEcommerceId());
        
        return toResponse(updated);
    }
    
    /**
     * Elimina un usuario (irreversible).
     * 
     * SPEC-003 HU-03.4: Eliminar usuario estándar
     * SPEC-003 RN-05: SUPER_ADMIN y STORE_ADMIN pueden eliminar usuarios
     * 
     * Validaciones:
     * - SUPER_ADMIN o STORE_ADMIN (CRITERIO-3.4.2)
     * - Si STORE_ADMIN: usuario debe pertenecer a su propio ecommerce (TenantInterceptor)
     * - Usuario no puede eliminarse a sí mismo (CRITERIO-3.4.3)
     * 
     * @param uid UUID único del usuario a eliminar
     * @throws AuthorizationException si no es SUPER_ADMIN/STORE_ADMIN o usuario no pertenece a su ecommerce
     * @throws ResourceNotFoundException si no existe
     * @throws BadRequestException si intenta auto-eliminarse
     */
    @Transactional
    public void deleteUser(UUID uid) {
        // AUTORIZACIÓN: Solo SUPER_ADMIN y STORE_ADMIN pueden eliminar usuarios
        String currentRole = securityContextHelper.getCurrentUserRole();
        if (!"SUPER_ADMIN".equals(currentRole) && !"STORE_ADMIN".equals(currentRole)) {
            log.warn("Intento de eliminar usuario sin permisos. Role actual: {}", currentRole);
            throw new AuthorizationException(
                "Solo administradores pueden eliminar usuarios"
            );
        }
        
        UserEntity user = userRepository.findByUid(uid)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));
        
        // VALIDACIÓN: Si es STORE_ADMIN, el usuario debe pertenecer a su ecommerce
        // (suplementario a TenantInterceptor, pero buena práctica)
        if ("STORE_ADMIN".equals(currentRole)) {
            UUID storeAdminEcommerce = securityContextHelper.getCurrentUserEcommerceId();
            if (!storeAdminEcommerce.equals(user.getEcommerceId())) {
                log.warn("STORE_ADMIN intenta eliminar usuario de otro ecommerce. Own: {}, Target: {}", 
                        storeAdminEcommerce, user.getEcommerceId());
                throw new AuthorizationException(
                    "No puede eliminar usuarios de otro ecommerce"
                );
            }
        }
        
        // Validar que no se elimina a sí mismo
        UUID currentUserUid = securityContextHelper.getCurrentUserUid();
        if (currentUserUid.equals(uid)) {
            log.warn("Intento de auto-eliminación por usuario: {}", uid);
            throw new BadRequestException("No puede eliminarse a sí mismo");
        }
        
        userRepository.delete(user);
        log.info("Usuario eliminado: uid={}, username={}, ecommerce={}", 
                user.getUid(), user.getUsername(), user.getEcommerceId());
    }
    
    /**
     * Convierte una entidad UserEntity a DTO UserResponse.
     * Expone UUID en lugar de Long id (protección contra enumeración).
     * El uuid proviene del campo uid del entity (auto-generado en @PrePersist).
     */
    private UserResponse toResponse(UserEntity user) {
        return new UserResponse(
                user.getUid(),
                user.getUsername(),
                user.getRole(),
                user.getEmail(),
                user.getEcommerceId(),
                user.getActive(),
                user.getCreatedAt(),
                user.getUpdatedAt()
        );
    }
}
