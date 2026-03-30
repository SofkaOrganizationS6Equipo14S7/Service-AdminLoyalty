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
     * Actualiza un usuario con autorización multi-contexto.
     * 
     * SPEC-005 HU-02.3: Modificar usuario por SUPER_ADMIN, STORE_ADMIN o el dueño del perfil
     * SPEC-005 RN-05: Gestión de usuarios por contexto de autorización
     * 
     * Validaciones:
     * - SUPER_ADMIN (HU-02.3.1): puede actualizar cualquier usuario (global scope)
     * - STORE_ADMIN (HU-02.3.1B): puede actualizar usuarios de su ecommerce SOLO
     * - STORE_USER (HU-02.3.1C): puede actualizar su propio perfil (sin ecommerce_id ni active)
     * 
     * Restricciones de campo:
     * - role: NUNCA cambiar
     * - ecommerce_id: SOLO SUPER_ADMIN puede cambiar
     * - active: SOLO SUPER_ADMIN puede cambiar
     * - username/email/password: pueden cambiar SUPER_ADMIN, STORE_ADMIN (su ecommerce), STORE_USER (perfil propio)
     * 
     * @param uid UUID único del usuario a actualizar
     * @param request datos a actualizar (username, email, password, ecommerceId, active)
     * @return UserResponse actualizado
     * @throws AuthorizationException si no tiene permiso (CRITERIO-2.3.1D, CRITERIO-2.3.1E)
     * @throws ResourceNotFoundException si usuario no existe
     * @throws ConflictException si username/email duplicado
     * @throws BadRequestException si intenta cambiar campo prohibido
     */
    @Transactional
    public UserResponse updateUser(UUID uid, UserUpdateRequest request) {
        String currentRole = securityContextHelper.getCurrentUserRole();
        UUID currentUserUid = securityContextHelper.getCurrentUserUid();
        UUID currentUserEcommerceId = securityContextHelper.getCurrentUserEcommerceId();
        
        UserEntity user = userRepository.findByUid(uid)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));
        
        // ============ AUTORIZACIÓN MULTI-CONTEXTO ============
        // Validar que el usuario actual tiene permiso para actuar sobre este usuario
        boolean canAct = securityContextHelper.canActOnUser(user.getEcommerceId(), uid);
        if (!canAct) {
            log.warn("Intento de acceso prohibido a usuario. Current: role={}, uid={}. Target: uid={}, ecommerce={}", 
                    currentRole, currentUserUid, uid, user.getEcommerceId());
            throw new AuthorizationException(
                "No tiene permiso para editar este usuario"
            );
        }
        
        // ============ VALIDACIONES DE CAMPO ============
        // Verificar intentos de cambiar campos prohibidos según el rol
        if (!currentRole.equals("SUPER_ADMIN")) {
            // STORE_ADMIN y STORE_USER no pueden cambiar ecommerce_id
            if (request.ecommerceId() != null) {
                log.warn("Intento de cambiar ecommerce_id by user with role={}. UID={}", currentRole, currentUserUid);
                throw new AuthorizationException(
                    "No puede cambiar su ecommerce_id"
                );
            }
            
            // STORE_ADMIN y STORE_USER no pueden cambiar active
            if (request.active() != null) {
                log.warn("Intento de cambiar active by user with role={}. UID={}", currentRole, currentUserUid);
                throw new AuthorizationException(
                    "No puede cambiar su estado de activación"
                );
            }
        }
        
        // ============ ACTUALIZAR CAMPOS ============
        // Username (opcional)
        if (request.username() != null && !request.username().isEmpty() && 
                !request.username().equals(user.getUsername())) {
            if (userRepository.findByUsername(request.username()).isPresent()) {
                log.warn("Intento de cambiar a username duplicado: {}", request.username());
                throw new ConflictException("Username ya existe en el sistema");
            }
            user.setUsername(request.username());
        }
        
        // Email (opcional)
        if (request.email() != null && !request.email().isEmpty() && 
                !request.email().equals(user.getEmail())) {
            if (userRepository.findByEmail(request.email()).isPresent()) {
                log.warn("Intento de cambiar a email duplicado: {}", request.email());
                throw new ConflictException("Email ya existe en el sistema");
            }
            user.setEmail(request.email());
        }
        
        // Password (opcional)
        if (request.password() != null && !request.password().isEmpty()) {
            user.setPassword(passwordEncoder.encode(request.password()));
        }
        
        // Ecommerce ID (ONLY SUPER_ADMIN) (opcional)
        if (request.ecommerceId() != null && currentRole.equals("SUPER_ADMIN")) {
            if (!request.ecommerceId().equals(user.getEcommerceId())) {
                // Validar que el nuevo ecommerce existe
                ecommerceService.validateEcommerceExists(request.ecommerceId());
                user.setEcommerceId(request.ecommerceId());
                log.info("Ecommerce cambiado para usuario uid={}: {} -> {}", 
                        uid, user.getEcommerceId(), request.ecommerceId());
            }
        }
        
        // Active flag (ONLY SUPER_ADMIN) (opcional)
        if (request.active() != null && currentRole.equals("SUPER_ADMIN")) {
            user.setActive(request.active());
        }
        
        UserEntity updated = userRepository.save(user);
        log.info("Usuario actualizado: uid={}, username={}, ecommerce={}, active={}", 
                updated.getUid(), updated.getUsername(), updated.getEcommerceId(), updated.getActive());
        
        return toResponse(updated);
    }
    
    /**
     * Elimina un usuario permanentemente con autorización multi-contexto.
     * 
     * SPEC-005 HU-02.5: Eliminar STORE_ADMIN de un ecommerce
     * SPEC-005 RN-05: Gestión de usuarios por contexto de autorización
     * 
     * Validaciones:
     * - SUPER_ADMIN (HU-02.5.1): puede eliminar cualquier usuario (global scope)
     * - STORE_ADMIN (HU-02.5.1B): puede eliminar usuarios de su ecommerce SOLO
     * - STORE_USER: NO puede eliminar usuarios (403 Forbidden)
     * - Auto-eliminación: NEGADA para todos (400 Bad Request) (HU-02.5.2)
     * - STORE_ADMIN intenta eliminar de otro ecommerce: 403 Forbidden (HU-02.5.1C)
     * 
     * @param uid UUID único del usuario a eliminar
     * @throws AuthorizationException si no tiene permiso (CRITERIO-2.5.1C, CRITERIO-2.5.1D)
     * @throws ResourceNotFoundException si usuario no existe
     * @throws BadRequestException si intenta auto-eliminación (CRITERIO-2.5.2)
     */
    @Transactional
    public void deleteUser(UUID uid) {
        String currentRole = securityContextHelper.getCurrentUserRole();
        UUID currentUserUid = securityContextHelper.getCurrentUserUid();
        
        UserEntity user = userRepository.findByUid(uid)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));
        
        // ============ VALIDACIÓN DE AUTO-ELIMINACIÓN ============
        // Ningún usuario puede eliminar su propio perfil
        if (currentUserUid.equals(uid)) {
            log.warn("Intento de auto-eliminación por usuario: uid={}, role={}", uid, currentRole);
            throw new BadRequestException("No puede eliminarse a sí mismo");
        }
        
        // ============ AUTORIZACIÓN MULTI-CONTEXTO ============
        // Validar que el usuario actual tiene permiso para eliminar este usuario
        boolean canAct = securityContextHelper.canActOnUser(user.getEcommerceId(), uid);
        if (!canAct) {
            log.warn("Intento de eliminación prohibida. Current: role={}, uid={}. Target: uid={}, ecommerce={}", 
                    currentRole, currentUserUid, uid, user.getEcommerceId());
            throw new AuthorizationException(
                "No tiene permiso para eliminar este usuario"
            );
        }
        
        userRepository.delete(user);
        log.info("Usuario eliminado: uid={}, username={}, ecommerce={}, actor={}", 
                user.getUid(), user.getUsername(), user.getEcommerceId(), currentUserUid);
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
