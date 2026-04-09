package com.loyalty.service_admin.application.service;

import com.loyalty.service_admin.application.port.in.UserDeleteUseCase;
import com.loyalty.service_admin.application.port.out.UserDeletePersistencePort;
import com.loyalty.service_admin.domain.entity.RoleEntity;
import com.loyalty.service_admin.domain.entity.UserEntity;
import com.loyalty.service_admin.infrastructure.exception.AuthorizationException;
import com.loyalty.service_admin.infrastructure.exception.BadRequestException;
import com.loyalty.service_admin.infrastructure.exception.ResourceNotFoundException;
import com.loyalty.service_admin.infrastructure.exception.UnauthorizedException;
import com.loyalty.service_admin.infrastructure.security.SecurityContextHelper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Implementación del caso de uso: Hard Delete de Usuarios.
 *
 * Responsabilidades:
 * - Validar autorización (rol, ecommerce)
 * - Prevenir auto-eliminación
 * - Registrar auditoría previa
 * - Ejecutar hard delete físico
 *
 * Patrón: Inyecta puertos (NO implementaciones concretas).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class UserDeleteService implements UserDeleteUseCase {
    
    private final UserDeletePersistencePort userDeletePersistencePort;
    private final SecurityContextHelper securityContextHelper;
    private final AuditService auditService;
    
    /**
     * Elimina permanentemente un usuario (hard delete).
     *
     * Workflow:
     * 1. Validar usuario autenticado
     * 2. Recuperar usuario target
     * 3. Validar no auto-eliminación
     * 4. Validar autorización (rol y ecommerce)
     * 5. Registrar en audit_log previa
     * 6. Ejecutar hard delete
     * 7. Log de auditoría
     *
     * @param uid UUID del usuario a eliminar
     * @throws UnauthorizedException si usuario no autenticado
     * @throws BadRequestException si intenta auto-eliminarse
     * @throws AuthorizationException si rol insuficiente o ecommerce distinto
     * @throws ResourceNotFoundException si usuario no existe
     */
    @Transactional
    @Override
    public void hardDeleteUser(UUID uid) {
        
        // 1. Obtener usuario autenticado del SecurityContext
        UUID currentUserUid;
        String currentRole;
        UUID currentUserEcommerceId;
        
        try {
            currentUserUid = securityContextHelper.getCurrentUserUid();
            currentRole = securityContextHelper.getCurrentUserRole();
            currentUserEcommerceId = securityContextHelper.getCurrentUserEcommerceId();
        } catch (Exception e) {
            log.warn("Error obteniendo usuario autenticado: {}", e.getMessage());
            throw new UnauthorizedException("Usuario no autenticado");
        }
        
        if (currentUserUid == null || currentRole == null) {
            log.warn("Usuario autenticado sin uid o rol válido");
            throw new UnauthorizedException("Usuario no autenticado");
        }
        
        // 2. Recuperar usuario a eliminar
        UserEntity targetUser = userDeletePersistencePort.findById(uid)
                .orElseThrow(() -> {
                    log.warn("Intento de eliminar usuario inexistente: {}", uid);
                    return new ResourceNotFoundException("Usuario no encontrado");
                });
        
        // 3. Validar: Prohibición de auto-eliminación
        if (currentUserUid.equals(targetUser.getId())) {
            log.warn("Usuario intenta auto-eliminarse: {}", currentUserUid);
            throw new BadRequestException("No puede eliminarse a sí mismo");
        }
        
        // 4. Validar: Autorización multitenant (SUPER_ADMIN o STORE_ADMIN de su ecommerce)
        // canActOnUser encapsula la lógica: SUPER_ADMIN→global, STORE_ADMIN→su ecommerce, STORE_USER→negado
        boolean canAct = securityContextHelper.canActOnUser(targetUser.getEcommerceId(), uid);
        if (!canAct) {
            log.warn("Usuario con rol {} intenta eliminar usuario fuera de su scope: uid={}, ecommerce={}",
                    currentRole, uid, targetUser.getEcommerceId());
            throw new AuthorizationException(
                "SUPER_ADMIN".equals(currentRole)
                    ? "No tiene permiso para eliminar este usuario"
                    : "No puede eliminar usuarios de otro ecommerce"
            );
        }
        
        // 5. Registrar en audit_log ANTES de eliminar (crítico para GDPR)
        auditService.auditUserDeletion(targetUser, currentUserUid);
        log.debug("Auditoría registrada para USER_DELETE: uid={}", uid);
        
        // 6. Ejecutar hard delete (eliminación física)
        userDeletePersistencePort.deleteUser(targetUser);
        log.info("Hard delete ejecutado: uid={}, username={}, ecommerce={}, actor={}",
                targetUser.getId(), targetUser.getUsername(), targetUser.getEcommerceId(), currentUserUid);
        
        // 7. Si llegamos aquí sin excepciones, el controller retornará 204 No Content
    }
}
