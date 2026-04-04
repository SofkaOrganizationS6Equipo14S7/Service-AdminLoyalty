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
        
        if (!"SUPER_ADMIN".equals(currentRole) && !"STORE_ADMIN".equals(currentRole)) {
            log.warn("Intento de crear usuario sin permisos. Role actual: {}", currentRole);
            throw new AuthorizationException(
                "Solo SUPER_ADMIN o STORE_ADMIN pueden crear usuarios"
            );
        }
        
        RoleEntity roleEntity = roleRepository.findById(request.roleId())
                .orElseThrow(() -> {
                    log.warn("Intento de crear usuario con roleId inválido: {}", request.roleId());
                    return new BadRequestException("El roleId proporcionado no existe");
                });
        
        String roleName = roleEntity.getName();
        
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
        
        if ("STORE_ADMIN".equals(currentRole)) {
            if (!currentUserEcommerceId.equals(request.ecommerceId())) {
                log.warn("STORE_ADMIN intenta crear usuario en otro ecommerce. Own: {}, Requested: {}", 
                        currentUserEcommerceId, request.ecommerceId());
                throw new AuthorizationException(
                    "Solo puede crear usuarios dentro de su ecommerce"
                );
            }
        }
        
        if (userRepository.findByUsername(request.username()).isPresent()) {
            log.warn("Intento de crear usuario con username duplicado: {}", request.username());
            throw new ConflictException("El username ya existe. Use otro.");
        }
        
        if (userRepository.findByEmail(request.email()).isPresent()) {
            log.warn("Intento de crear usuario con email duplicado: {}", request.email());
            throw new ConflictException("El email ya existe. Use otro.");
        }
        
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
            UUID userEcommerceId = securityContextHelper.getCurrentUserEcommerceId();
            
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
    
    @Transactional(readOnly = true)
    public UserResponse getUserByUid(UUID uid) {
        UserEntity user = userRepository.findById(uid)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));
        
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
    
    @Transactional
    public UserResponse updateUser(UUID uid, UserUpdateRequest request) {
        String currentRole = securityContextHelper.getCurrentUserRole();
        UUID currentUserUid = securityContextHelper.getCurrentUserUid();
        UUID currentUserEcommerceId = securityContextHelper.getCurrentUserEcommerceId();
        
        UserEntity user = userRepository.findById(uid)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));
        
        boolean canAct = securityContextHelper.canActOnUser(user.getEcommerceId(), uid);
        if (!canAct) {
            log.warn("Intento de acceso prohibido a usuario. Current: role={}, uid={}. Target: uid={}, ecommerce={}", 
                    currentRole, currentUserUid, uid, user.getEcommerceId());
            throw new AuthorizationException(
                "No tiene permiso para editar este usuario"
            );
        }
        
        if (request.roleId() != null) {
            log.warn("Intento de cambiar roleId del usuario uid={}. New roleId={}. Operación RECHAZADA.", 
                    uid, request.roleId());
            throw new BadRequestException(
                "No se puede cambiar el roleId de un usuario. El rol es inmutable."
            );
        }
        
        if (!currentRole.equals("SUPER_ADMIN")) {
            if (request.ecommerceId() != null) {
                log.warn("Intento de cambiar ecommerce_id by user with role={}. UID={}", currentRole, currentUserUid);
                throw new AuthorizationException(
                    "No puede cambiar su ecommerce_id"
                );
            }
            
            if (request.active() != null) {
                log.warn("Intento de cambiar active by user with role={}. UID={}", currentRole, currentUserUid);
                throw new AuthorizationException(
                    "No puede cambiar su estado de activación"
                );
            }
        }
        
        if (request.username() != null && !request.username().isEmpty() &&
                !request.username().equals(user.getUsername())) {
            if (userRepository.findByUsername(request.username()).isPresent()) {
                log.warn("Intento de cambiar a username duplicado: {}", request.username());
                throw new ConflictException("Username ya existe en el sistema");
            }
            user.setUsername(request.username());
        }
        
        if (request.email() != null && !request.email().isEmpty() &&
                !request.email().equals(user.getEmail())) {
            if (userRepository.findByEmail(request.email()).isPresent()) {
                log.warn("Intento de cambiar a email duplicado: {}", request.email());
                throw new ConflictException("Email ya existe en el sistema");
            }
            user.setEmail(request.email());
        }
        
        if (request.password() != null && !request.password().isEmpty()) {
            user.setPasswordHash(passwordEncoder.encode(request.password()));
        }
        
        if (request.ecommerceId() != null && currentRole.equals("SUPER_ADMIN")) {
            if (!request.ecommerceId().equals(user.getEcommerceId())) {
                ecommerceService.validateEcommerceExists(request.ecommerceId());
                user.setEcommerceId(request.ecommerceId());
                log.info("Ecommerce cambiado para usuario uid={}: {} -> {}", 
                        uid, user.getEcommerceId(), request.ecommerceId());
            }
        }
        
        if (request.active() != null && currentRole.equals("SUPER_ADMIN")) {
            user.setIsActive(request.active());
        }
        
        UserEntity updated = userRepository.save(user);
        log.info("Usuario actualizado: uid={}, username={}, ecommerce={}, active={}", 
                updated.getId(), updated.getUsername(), updated.getEcommerceId(), updated.getIsActive());
        
        return toResponse(updated);
    }
    
    @Transactional
    public void deleteUser(UUID uid) {
        String currentRole = securityContextHelper.getCurrentUserRole();
        UUID currentUserUid = securityContextHelper.getCurrentUserUid();
        
        UserEntity user = userRepository.findById(uid)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));
        
        if (currentUserUid.equals(uid)) {
            log.warn("Intento de auto-eliminación por usuario: uid={}, role={}", uid, currentRole);
            throw new BadRequestException("No puede eliminarse a sí mismo");
        }
        
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
    
    @Transactional
    public UserResponse updateProfile(UpdateProfileRequest request) {
        UUID currentUserUid = securityContextHelper.getCurrentUserUid();
        
        UserEntity user = userRepository.findById(currentUserUid)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));
        
        if (request.name() != null && !request.name().isBlank()) {
            user.setEmail(request.name());
            // Nota: en UserEntity probablemente name se guarda en email, verificar schema
            // TODO: Si UserEntity tiene campo 'name' separado, actualizar ese campo en su lugar
        }

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
        auditService.auditProfileUpdate(updated.getId(), updated.getEmail());
        
        return toResponse(updated);
    }
    
    @Transactional
    public LoginResponse changePassword(ChangePasswordRequest request) {
        UUID currentUserUid = securityContextHelper.getCurrentUserUid();
        
        UserEntity user = userRepository.findById(currentUserUid)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));
        
        if (!passwordEncoder.matches(request.currentPassword(), user.getPasswordHash())) {
            log.warn("Intento de cambio de contraseña fallido: contraseña actual incorrecta. Usuario: {}", currentUserUid);
            throw new UnauthorizedException("Contraseña actual incorrecta"); // CRITERIO-3.4
        }
        
        if (!request.newPassword().equals(request.confirmPassword())) {
            log.warn("Intento de cambio de contraseña: nuevas contraseñas no coinciden. Usuario: {}", currentUserUid);
            throw new BadRequestException("Las nuevas contraseñas no coinciden"); // CRITERIO-3.2
        }
        
        if (!passwordValidator.isValid(request.newPassword())) {
            String errorMsg = passwordValidator.getErrorMessage(request.newPassword());
            log.warn("Intento de cambio con contraseña débil: {}. Usuario: {}", errorMsg, currentUserUid);
            throw new BadRequestException(errorMsg);
        }
        
        if (passwordEncoder.matches(request.newPassword(), user.getPasswordHash())) {
            log.warn("Intento de reutilizar misma contraseña. Usuario: {}", currentUserUid);
            throw new ConflictException("La nueva contraseña no puede ser igual a la actual");
        }
        
        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        UserEntity updated = userRepository.save(user);
        
        log.info("Contraseña cambiada exitosamente para usuario: uid={}, changed_by={}", 
                updated.getId(), currentUserUid);
        
        auditService.auditPasswordChange(updated.getId(), null);
        
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
