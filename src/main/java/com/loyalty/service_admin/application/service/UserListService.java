package com.loyalty.service_admin.application.service;

import com.loyalty.service_admin.application.dto.user.UserResponse;
import com.loyalty.service_admin.application.port.in.UserListUseCase;
import com.loyalty.service_admin.application.port.out.UserListPersistencePort;
import com.loyalty.service_admin.domain.entity.UserEntity;
import com.loyalty.service_admin.infrastructure.exception.AuthorizationException;
import com.loyalty.service_admin.infrastructure.exception.UnauthorizedException;
import com.loyalty.service_admin.infrastructure.security.SecurityContextHelper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Implementación del caso de uso: Listado de Usuarios.
 *
 * Responsabilidades:
 * - Validar autenticación
 * - Aplicar filtrado según rol
 * - Implementar paginación
 * - Retornar Page<UserResponse>
 *
 * Patrón: Inyecta puertos (NO implementaciones concretas).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class UserListService implements UserListUseCase {
    
    private final UserListPersistencePort userListPersistencePort;
    private final SecurityContextHelper securityContextHelper;
    
    /**
     * Lista usuarios del sistema con soporte para paginación y filtrado.
     *
     * Workflow:
     * 1. Validar usuario autenticado
     * 2. Obtener rol del usuario
     * 3. Aplicar filtrado según rol:
     *    - SUPER_ADMIN: listar todos (o filtrar por ecommerce si se proporciona)
     *    - STORE_ADMIN: listar solo usuarios de su ecommerce
     *    - STANDARD: lanzar AuthorizationException
     * 4. Retornar Page<UserResponse> paginada
     *
     * @param ecommerceId filtro opcional por ecommerce
     * @param pageable información de paginación
     * @return Page<UserResponse> con usuarios paginados
     * @throws UnauthorizedException si usuario no autenticado
     * @throws AuthorizationException si STANDARD intenta listar o acceso cruzado
     */
    @Transactional(readOnly = true)
    @Override
    public Page<UserResponse> listUsers(UUID ecommerceId, Pageable pageable) {
        // 1. Validar usuario autenticado
        String currentRole;
        UUID currentUserEcommerceId;
        
        try {
            currentRole = securityContextHelper.getCurrentUserRole();
            currentUserEcommerceId = securityContextHelper.getCurrentUserEcommerceId();
        } catch (Exception e) {
            log.warn("Error obteniendo usuario autenticado: {}", e.getMessage());
            throw new UnauthorizedException("Usuario no autenticado");
        }
        
        if (currentRole == null) {
            log.warn("Usuario autenticado sin rol válido");
            throw new UnauthorizedException("Usuario no autenticado");
        }
        
        // 2. Aplicar filtrado según rol
        Page<UserEntity> result;
        
        if ("SUPER_ADMIN".equals(currentRole)) {
            // SUPER_ADMIN: listar todos (o filtrar by param)
            if (ecommerceId != null) {
                result = userListPersistencePort.findByEcommerceId(ecommerceId, pageable);
            } else {
                result = userListPersistencePort.findAll(pageable);
            }
        } else if ("STORE_ADMIN".equals(currentRole)) {
            // STORE_ADMIN: listar solo de su ecommerce
            if (ecommerceId != null && !ecommerceId.equals(currentUserEcommerceId)) {
                log.warn("STORE_ADMIN intenta acceso cruzado: user ecommerce={}, requested={}", 
                        currentUserEcommerceId, ecommerceId);
                throw new AuthorizationException("No tiene permiso para acceder a este ecommerce");
            }
            result = userListPersistencePort.findByEcommerceId(currentUserEcommerceId, pageable);
        } else {
            // STANDARD: no puede listar usuarios
            log.warn("Usuario STANDARD intenta listar usuarios");
            throw new AuthorizationException("No tiene permiso para listar usuarios");
        }
        
        // 3. Convertir a UserResponse
        return result.map(this::toResponse);
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
