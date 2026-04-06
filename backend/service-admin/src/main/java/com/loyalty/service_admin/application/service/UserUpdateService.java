package com.loyalty.service_admin.application.service;

import com.loyalty.service_admin.application.dto.user.UserResponse;
import com.loyalty.service_admin.application.dto.user.UserUpdateRequest;
import com.loyalty.service_admin.application.port.in.UserUpdateUseCase;
import com.loyalty.service_admin.application.port.out.UserUpdatePersistencePort;
import com.loyalty.service_admin.domain.entity.RoleEntity;
import com.loyalty.service_admin.domain.entity.UserEntity;
import com.loyalty.service_admin.domain.repository.RoleRepository;
import com.loyalty.service_admin.infrastructure.exception.AuthorizationException;
import com.loyalty.service_admin.infrastructure.exception.BadRequestException;
import com.loyalty.service_admin.infrastructure.exception.ConflictException;
import com.loyalty.service_admin.infrastructure.exception.ResourceNotFoundException;
import com.loyalty.service_admin.infrastructure.exception.UnauthorizedException;
import com.loyalty.service_admin.infrastructure.security.SecurityContextHelper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Implementación del caso de uso: Actualización de Usuarios.
 *
 * Responsabilidades:
 * - Validar autenticación
 * - Validar existencia del usuario
 * - Validar autorización según rol
 * - Validar cambios (email, username, role)
 * - Registrar auditoría de cambios
 * - Retornar UserResponse actualizado
 *
 * Patrón: Inyecta puertos (NO implementaciones concretas).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class UserUpdateService implements UserUpdateUseCase {
    
    private final UserUpdatePersistencePort userUpdatePersistencePort;
    private final SecurityContextHelper securityContextHelper;
    private final RoleRepository roleRepository;
    private final EcommerceService ecommerceService;
    private final AuditService auditService;
    private final PasswordValidator passwordValidator;
    private final BCryptPasswordEncoder passwordEncoder;
    
    /**
     * Actualiza datos de un usuario existente.
     *
     * Workflow:
     * 1. Validar usuario autenticado
     * 2. Recuperar usuario target
     * 3. Validar autorización
     * 4. Validar cambios (email, username, role)
     * 5. Ejecutar update
     * 6. Registrar auditoría
     * 7. Retornar UserResponse actualizado
     *
     * @param uid UUID del usuario a actualizar
     * @param request datos a actualizar
     * @return UserResponse con datos actualizados
     * @throws UnauthorizedException si usuario no autenticado
     * @throws ResourceNotFoundException si usuario no existe
     * @throws AuthorizationException si sin permisos
     * @throws BadRequestException si validación falla
     * @throws ConflictException si email/username existen
     */
    @Transactional
    @Override
    public UserResponse updateUser(UUID uid, UserUpdateRequest request) {
        // 1. Validar usuario autenticado
        String currentRole;
        UUID currentUserUid;
        UUID currentUserEcommerceId;
        
        try {
            currentRole = securityContextHelper.getCurrentUserRole();
            currentUserUid = securityContextHelper.getCurrentUserUid();
            currentUserEcommerceId = securityContextHelper.getCurrentUserEcommerceId();
        } catch (Exception e) {
            log.warn("Error obteniendo usuario autenticado: {}", e.getMessage());
            throw new UnauthorizedException("Usuario no autenticado");
        }
        
        if (currentRole == null) {
            log.warn("Usuario autenticado sin rol válido");
            throw new UnauthorizedException("Usuario no autenticado");
        }
        
        // 2. Recuperar usuario target
        UserEntity targetUser = userUpdatePersistencePort.findById(uid)
                .orElseThrow(() -> {
                    log.warn("Intento de actualizar usuario inexistente: {}", uid);
                    return new ResourceNotFoundException("Usuario no encontrado");
                });
        
        // 3. Validar autorización
        boolean canAct = securityContextHelper.canActOnUser(targetUser.getEcommerceId(), uid);
        if (!canAct) {
            log.warn("Intento de acceso prohibido a usuario. Current: role={}, uid={}. Target: uid={}, ecommerce={}", 
                    currentRole, currentUserUid, uid, targetUser.getEcommerceId());
            throw new AuthorizationException("No tiene permiso para editar este usuario");
        }
        
        // 4. Validar cambios
        // 4.1 Validar que NO se cambie roleId
        if (request.roleId() != null) {
            log.warn("Intento de cambiar roleId del usuario uid={}. New roleId={}. Operación RECHAZADA.", 
                    uid, request.roleId());
            throw new BadRequestException("No se puede cambiar el roleId de un usuario. El rol es inmutable.");
        }
        
        // 4.2 Validar que STORE_ADMIN no cambie su propio role (implícitamente no puede cambiar roleId)
        // Ya validado arriba
        
        // 4.3 Validar cambios de ecommerceId (solo SUPER_ADMIN)
        if (request.ecommerceId() != null && !request.ecommerceId().equals(targetUser.getEcommerceId())) {
            if (!"SUPER_ADMIN".equals(currentRole)) {
                log.warn("Intento de cambiar ecommerce_id by user with role={}. UID={}", currentRole, currentUserUid);
                throw new AuthorizationException("No puede cambiar su ecommerce_id");
            }
            ecommerceService.validateEcommerceExists(request.ecommerceId());
            targetUser.setEcommerceId(request.ecommerceId());
            log.info("Ecommerce cambiado para usuario uid={}: {} -> {}", 
                    uid, targetUser.getEcommerceId(), request.ecommerceId());
        }
        
        // 4.4 Validar cambios de estado activo (solo SUPER_ADMIN)
        if (request.active() != null && !request.active().equals(targetUser.getIsActive())) {
            if (!"SUPER_ADMIN".equals(currentRole)) {
                log.warn("Intento de cambiar active by user with role={}. UID={}", currentRole, currentUserUid);
                throw new AuthorizationException("No puede cambiar su estado de activación");
            }
            targetUser.setIsActive(request.active());
        }
        
        // 4.5 Validar cambio de username (debe ser único)
        if (request.username() != null && !request.username().isEmpty() &&
                !request.username().equals(targetUser.getUsername())) {
            if (userUpdatePersistencePort.existsByUsernameExcludingUid(request.username(), uid)) {
                log.warn("Intento de cambiar a username duplicado: {}", request.username());
                throw new ConflictException("Username ya existe en el sistema");
            }
            targetUser.setUsername(request.username());
        }
        
        // 4.6 Validar cambio de email (debe ser único)
        if (request.email() != null && !request.email().isEmpty() &&
                !request.email().equals(targetUser.getEmail())) {
            if (userUpdatePersistencePort.existsByEmailExcludingUid(request.email(), uid)) {
                log.warn("Intento de cambiar a email duplicado: {}", request.email());
                throw new ConflictException("Email ya existe en el sistema");
            }
            targetUser.setEmail(request.email());
        }
        
        // 4.7 Validar cambio de password
        if (request.password() != null && !request.password().isEmpty()) {
            if (!passwordValidator.isValid(request.password())) {
                String errorMsg = passwordValidator.getErrorMessage(request.password());
                log.warn("Intento de cambiar a contraseña débil: {}. UID: {}", errorMsg, uid);
                throw new BadRequestException(errorMsg);
            }
            targetUser.setPasswordHash(passwordEncoder.encode(request.password()));
        }
        
        // 5. Ejecutar update
        UserEntity updated = userUpdatePersistencePort.save(targetUser);
        log.info("Usuario actualizado exitosamente: uid={}, username={}", updated.getId(), updated.getUsername());
        
        // 6. Registrar auditoría
        auditService.auditUserUpdate(targetUser, currentUserUid);
        
        // 7. Retornar UserResponse actualizado
        return toResponse(updated);
    }
    
    private UserResponse toResponse(UserEntity user) {
        return new UserResponse(
                user.getId(),
                user.getUsername(),
                user.getRoleId(),
                user.getRole() != null ? user.getRole().getName() : null,
                user.getEmail(),
                user.getEcommerceId(),
                user.getIsActive(),
                user.getCreatedAt(),
                user.getUpdatedAt()
        );
    }
}
