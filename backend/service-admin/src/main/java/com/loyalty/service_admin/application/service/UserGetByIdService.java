package com.loyalty.service_admin.application.service;

import com.loyalty.service_admin.application.dto.user.UserResponse;
import com.loyalty.service_admin.application.port.in.UserGetByIdUseCase;
import com.loyalty.service_admin.application.port.out.UserGetPersistencePort;
import com.loyalty.service_admin.domain.entity.UserEntity;
import com.loyalty.service_admin.infrastructure.exception.AuthorizationException;
import com.loyalty.service_admin.infrastructure.exception.ResourceNotFoundException;
import com.loyalty.service_admin.infrastructure.exception.UnauthorizedException;
import com.loyalty.service_admin.infrastructure.security.SecurityContextHelper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Implementación del caso de uso: Obtener Usuario por ID.
 *
 * Responsabilidades:
 * - Validar autenticación
 * - Validar existencia del usuario
 * - Validar autorización según rol
 * - Retornar UserResponse
 *
 * Patrón: Inyecta puertos (NO implementaciones concretas).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class UserGetByIdService implements UserGetByIdUseCase {
    
    private final UserGetPersistencePort userGetPersistencePort;
    private final SecurityContextHelper securityContextHelper;
    
    /**
     * Obtiene un usuario específico por su UID.
     *
     * Workflow:
     * 1. Validar usuario autenticado
     * 2. Recuperar usuario target
     * 3. Validar autorización según rol:
     *    - SUPER_ADMIN: acceso a cualquier usuario
     *    - STORE_ADMIN: solo de su ecommerce
     *    - STANDARD: solo acceso a sí mismo
     * 4. Retornar UserResponse
     *
     * @param uid UUID del usuario a obtener
     * @return UserResponse con datos del usuario
     * @throws UnauthorizedException si usuario no autenticado
     * @throws ResourceNotFoundException si usuario no existe
     * @throws AuthorizationException si sin permisos
     */
    @Transactional(readOnly = true)
    @Override
    public UserResponse getUserById(UUID uid) {
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
        UserEntity targetUser = userGetPersistencePort.findById(uid)
                .orElseThrow(() -> {
                    log.warn("Intento de acceder a usuario inexistente: {}", uid);
                    return new ResourceNotFoundException("Usuario no encontrado");
                });
        
        // 3. Validar autorización según rol
        if ("SUPER_ADMIN".equals(currentRole)) {
            // SUPER_ADMIN: acceso a cualquier usuario
            // No hay restricción
        } else if ("STORE_ADMIN".equals(currentRole)) {
            // STORE_ADMIN: solo usuarios de su ecommerce
            if (!currentUserEcommerceId.equals(targetUser.getEcommerceId())) {
                log.warn("STORE_ADMIN intenta acceso a usuario de otro ecommerce: user ecommerce={}, target ecommerce={}", 
                        currentUserEcommerceId, targetUser.getEcommerceId());
                throw new AuthorizationException("No tiene permiso para acceder a este usuario");
            }
        } else {
            // STANDARD: solo acceso a sí mismo
            if (!currentUserUid.equals(targetUser.getId())) {
                log.warn("Usuario STANDARD intenta acceso a otro usuario: current uid={}, target uid={}", 
                        currentUserUid, uid);
                throw new AuthorizationException("No tiene permiso para acceder a este usuario");
            }
        }
        
        // 4. Retornar UserResponse
        return toResponse(targetUser);
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
