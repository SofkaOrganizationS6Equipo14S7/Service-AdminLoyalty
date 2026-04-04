package com.loyalty.service_admin.application.service;

import com.loyalty.service_admin.application.dto.user.UserCreateRequest;
import com.loyalty.service_admin.application.dto.user.UserResponse;
import com.loyalty.service_admin.application.dto.user.UserUpdateRequest;
import com.loyalty.service_admin.application.dto.user.UpdateProfileRequest;
import com.loyalty.service_admin.application.dto.auth.ChangePasswordRequest;
import com.loyalty.service_admin.application.dto.auth.LoginResponse;
import com.loyalty.service_admin.domain.entity.UserEntity;
import com.loyalty.service_admin.domain.entity.RoleEntity;
import com.loyalty.service_admin.domain.repository.UserRepository;
import com.loyalty.service_admin.domain.repository.RoleRepository;
import com.loyalty.service_admin.infrastructure.exception.AuthorizationException;
import com.loyalty.service_admin.infrastructure.exception.BadRequestException;
import com.loyalty.service_admin.infrastructure.exception.ConflictException;
import com.loyalty.service_admin.infrastructure.exception.ResourceNotFoundException;
import com.loyalty.service_admin.infrastructure.exception.UnauthorizedException;
import com.loyalty.service_admin.infrastructure.security.SecurityContextHelper;
import com.loyalty.service_admin.infrastructure.security.JwtProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {
    
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final EcommerceService ecommerceService;
    private final BCryptPasswordEncoder passwordEncoder;
    private final SecurityContextHelper securityContextHelper;
    private final JwtProvider jwtProvider;
    private final AuditService auditService;
    private final PasswordValidator passwordValidator;

    @Transactional
    public UserResponse createUser(UserCreateRequest request) {
        String currentRole = securityContextHelper.getCurrentUserRole();
        UUID currentUserEcommerceId = securityContextHelper.getCurrentUserEcommerceId();
        
        // ============ AUTORIZACIÓN ============
        // Solo SUPER_ADMIN y STORE_ADMIN pueden crear usuarios
        if (!"SUPER_ADMIN".equals(currentRole) && !"STORE_ADMIN".equals(currentRole)) {
            log.warn("Intento de crear usuario sin permisos. Role actual: {}", currentRole);
            throw new AuthorizationException(
                "Solo SUPER_ADMIN o STORE_ADMIN pueden crear usuarios"
            );
        }
        
        // ============ VALIDACIÓN DE ROL ============
        // Roles permitidos: SUPER_ADMIN, STORE_ADMIN, STORE_USER
        // Validar que el roleId existe
        RoleEntity roleEntity = roleRepository.findById(request.roleId())
                .orElseThrow(() -> {
                    log.warn("Intento de crear usuario con roleId inválido: {}", request.roleId());
                    return new BadRequestException("El roleId proporcionado no existe");
                });
        
        String roleName = roleEntity.getName();
        
        // ============ VALIDACIÓN DE ECOMMERCE_ID SEGÚN ROLE ============
        if ("SUPER_ADMIN".equals(roleName)) {
            if (request.ecommerceId() != null) {
                log.warn("Intento de crear SUPER_ADMIN con ecommerce_id: {}", request.ecommerceId());
                throw new BadRequestException(
                    "SUPER_ADMIN no puede tener ecommerce_id asignado"
                );
            }
        } else {
            if (request.ecommerceId() == null) {
                log.warn("Intento de crear usuario {} sin ecommerce_id", roleName);
                throw new BadRequestException(
                    String.format("%s requiere ecommerce_id obligatorio", roleName)
                );
            }
            
            ecommerceService.validateEcommerceExists(request.ecommerceId());
        }
        
        // ============ VALIDACIÓN DE CONTEXTO (STORE_ADMIN) ============
        // STORE_ADMIN solo puede crear en su propio ecommerce
        if ("STORE_ADMIN".equals(currentRole)) {
            if (!currentUserEcommerceId.equals(request.ecommerceId())) {
                log.warn("STORE_ADMIN intenta crear usuario en otro ecommerce. Own: {}, Requested: {}", 
                        currentUserEcommerceId, request.ecommerceId());
                throw new AuthorizationException(
                    "Solo puede crear usuarios dentro de su ecommerce"
                );
            }
        }
        
        // ============ VALIDACIÓN DE UNICIDAD ============
        // Username único globalmente
        if (userRepository.findByUsername(request.username()).isPresent()) {
            log.warn("Intento de crear usuario con username duplicado: {}", request.username());
            throw new ConflictException("El username ya existe. Use otro.");
        }
        
        // Email único globalmente
        if (userRepository.findByEmail(request.email()).isPresent()) {
            log.warn("Intento de crear usuario con email duplicado: {}", request.email());
            throw new ConflictException("El email ya existe. Use otro.");
        }
        
        // ============ VALIDAR COMPLEJIDAD DE CONTRASEÑA (SPEC-004 RN-06) ============
        if (!passwordValidator.isValid(request.password())) {
            String errorMsg = passwordValidator.getErrorMessage(request.password());
            log.warn("Intento de crear usuario con contraseña débil: {}. Username: {}", errorMsg, request.username());
            throw new BadRequestException(errorMsg);
        }
        
        UserEntity user = UserEntity.builder()
                .username(request.username())
                .email(request.email())
                .passwordHash(passwordEncoder.encode(request.password()))
                .roleId(roleEntity.getId())
                .role(roleEntity)
                .ecommerceId(request.ecommerceId())
                .isActive(true)
                .build();
        
        UserEntity saved = userRepository.save(user);
        log.info("Usuario creado exitosamente: uid={}, username={}, role={}, ecommerce={}", 
                saved.getId(), saved.getUsername(), saved.getRoleId(), saved.getEcommerceId());
        
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
        UserEntity user = userRepository.findById(uid)
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
        
        UserEntity user = userRepository.findById(uid)
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
            user.setPasswordHash(passwordEncoder.encode(request.password()));
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
            user.setIsActive(request.active());
        }
        
        UserEntity updated = userRepository.save(user);
        log.info("Usuario actualizado: uid={}, username={}, ecommerce={}, active={}", 
                updated.getId(), updated.getUsername(), updated.getEcommerceId(), updated.getIsActive());
        
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
        
        UserEntity user = userRepository.findById(uid)
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
                user.getId(), user.getUsername(), user.getEcommerceId(), currentUserUid);
    }
    
    /**
     * Actualiza el perfil del usuario autenticado (nombre y email).
     * 
     * SPEC-004 HU-03: Actualizar mi información de perfil
     * CRITERIO-3.1: Actualización de nombre y email
     * 
     * El usuario STORE_USER puede cambiar:
     * - name: nombre completo (1-100 caracteres)  
     * - email: dirección de email (debe ser único globalmente sin importar ecommerce)
     * 
     * Restricciones:
     * - No puede cambiar username (identificador de login)
     * - No puede cambiar role
     * - No puede cambiar ecommerce_id
     * - Email debe ser único globalmente (CRITERIO-3.3: validación global, no limitada al ecommerce)
     * - El cambio se registra en tabla de auditoría con timestamp y user UID
     * 
     * @param request datos a actualizar (name, email)
     * @return UserResponse actualizado {uid, username, email, role, ecommerceId, ...}
     * @throws BadRequestException si email inválido o nombre vacío
     * @throws ConflictException si email ya existe en otro usuario (CRITERIO-3.3)
     * @throws ResourceNotFoundException si usuario no existe
     */
    @Transactional
    public UserResponse updateProfile(UpdateProfileRequest request) {
        UUID currentUserUid = securityContextHelper.getCurrentUserUid();
        
        // Obtener usuario actual desde contexto
        UserEntity user = userRepository.findById(currentUserUid)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));
        
        // ============ ACTUALIZAR EMAIL (Validación Global) ============
        if (request.email() != null && !request.email().isBlank() && 
                !request.email().equals(user.getEmail())) {
            // Validar unicidad global (no limitada al ecommerce) - CRITERIO-3.3
            if (userRepository.findByEmail(request.email()).isPresent()) {
                log.warn("Intento de cambiar a email duplicado globalmente: {}. Usuario actual: {}", 
                        request.email(), currentUserUid);
                throw new ConflictException("El email ya está en uso"); // CRITERIO-3.3 message
            }
            user.setEmail(request.email());
        }
        
        UserEntity updated = userRepository.save(user);
        log.info("Perfil actualizado para usuario: uid={}, email={}, changed_by={}", 
                updated.getId(), updated.getEmail(), currentUserUid);
        
        // Registrar en tabla de auditoría (SPEC-004 RN-08)
        auditService.auditProfileUpdate(updated.getId(), 
                String.format("Perfil actualizado: email=%s", request.email()));
        
        return toResponse(updated);
    }
    
    /**
     * Cambia la contraseña del usuario autenticado.
     * 
     * SPEC-004 HU-03: Actualizar mi información de perfil
     * CRITERIO-3.2: Cambio seguro de contraseña
     * 
     * El usuario proporciona:
     * - currentPassword: contraseña actual (para validación) (CRITERIO-3.4)
     * - newPassword: nueva contraseña (mínimo 12 caracteres, mayúscula, minúscula, número)
     * - confirmPassword: confirmación (debe ser igual a newPassword)
     * 
     * Validaciones:
     * - currentPassword debe ser correcto (401 Unauthorized si falla) (CRITERIO-3.4)
     * - newPassword y confirmPassword deben coincidir (400) (CRITERIO-3.2)
     * - newPassword debe cumplir policy de complejidad
     * - No puede reutilizar la misma contraseña
     * - El cambio se registra en tabla de auditoría
     * - Se genera un nuevo JWT automáticamente
     * 
     * @param request datos (currentPassword, newPassword, confirmPassword)
     * @return LoginResponse con nuevo JWT token generado automáticamente
     * @throws BadRequestException si nuevas contraseñas no coinciden (400) (CRITERIO-3.2)
     * @throws UnauthorizedException si contraseña actual es incorrecta (401) (CRITERIO-3.4)
     * @throws ConflictException si nueva contraseña es igual a la anterior
     * @throws ResourceNotFoundException si usuario no existe
     */
    @Transactional
    public LoginResponse changePassword(ChangePasswordRequest request) {
        UUID currentUserUid = securityContextHelper.getCurrentUserUid();
        
        // Obtener usuario actual desde contexto
        UserEntity user = userRepository.findById(currentUserUid)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));
        
        // ============ VALIDAR CONTRASEÑA ACTUAL (CRITERIO-3.4) ============
        if (!passwordEncoder.matches(request.currentPassword(), user.getPasswordHash())) {
            log.warn("Intento de cambio de contraseña fallido: contraseña actual incorrecta. Usuario: {}", currentUserUid);
            throw new UnauthorizedException("Contraseña actual incorrecta"); // CRITERIO-3.4
        }
        
        // ============ VALIDAR COINCIDENCIA DE NUEVAS CONTRASEÑAS (CRITERIO-3.2) ============
        if (!request.newPassword().equals(request.confirmPassword())) {
            log.warn("Intento de cambio de contraseña: nuevas contraseñas no coinciden. Usuario: {}", currentUserUid);
            throw new BadRequestException("Las nuevas contraseñas no coinciden"); // CRITERIO-3.2
        }
        
        // ============ VALIDAR COMPLEJIDAD DE CONTRASEÑA (SPEC-004 RN-06) ============
        if (!passwordValidator.isValid(request.newPassword())) {
            String errorMsg = passwordValidator.getErrorMessage(request.newPassword());
            log.warn("Intento de cambio con contraseña débil: {}. Usuario: {}", errorMsg, currentUserUid);
            throw new BadRequestException(errorMsg);
        }
        
        // ============ VALIDAR QUE NO SEA LA MISMA CONTRASEÑA ============
        if (passwordEncoder.matches(request.newPassword(), user.getPasswordHash())) {
            log.warn("Intento de reutilizar misma contraseña. Usuario: {}", currentUserUid);
            throw new ConflictException("La nueva contraseña no puede ser igual a la actual");
        }
        
        // ============ ACTUALIZAR CONTRASEÑA ============
        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        UserEntity updated = userRepository.save(user);
        
        log.info("Contraseña cambiada exitosamente para usuario: uid={}, changed_by={}", 
                updated.getId(), currentUserUid);
        
        // Registrar en tabla de auditoría (SPEC-004 RN-08)
        auditService.auditPasswordChange(updated.getId(), null);
        
        // ============ GENERAR NUEVO JWT ============
        // Generar un nuevo token JWT automáticamente después del cambio (CRITERIO-3.2)
        String newToken = jwtProvider.generateTokenFull(
                updated.getId(),
                updated.getUsername(),
                updated.getId(),
                updated.getRole().getName(),
                updated.getEcommerceId()
        );
        
        LoginResponse response = new LoginResponse(
                newToken,
                "Bearer",
                updated.getUsername(),
                updated.getRole().getName()
        );
        
        return response;
    }
    
    /**
     * Convierte una entidad UserEntity a DTO UserResponse.
     * Expone UUID en lugar de Long id (protección contra enumeración).
     * El uuid proviene del campo uid del entity (auto-generado en @PrePersist).
     */
    private UserResponse toResponse(UserEntity user) {
        return new UserResponse(
                user.getId(),
                user.getUsername(),
                user.getRoleId(),
                user.getRole().getName(),
                user.getEmail(),
                user.getEcommerceId(),
                user.getIsActive(),
                user.getCreatedAt(),
                user.getUpdatedAt()
        );
    }
}
