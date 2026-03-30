package com.loyalty.service_admin.application.service;

import com.loyalty.service_admin.domain.entity.AuditLogEntity;
import com.loyalty.service_admin.domain.repository.AuditLogRepository;
import com.loyalty.service_admin.infrastructure.security.SecurityContextHelper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Servicio de auditoría para LOYALTY system.
 * SPEC-004 RN-08: Registrar cambios de perfil con timestamp y user UID.
 * 
 * Responsabilidades:
 * - Registrar cambios de perfil (PROFILE_UPDATE)
 * - Registrar cambios de contraseña (PASSWORD_CHANGE)
 * - Registrar cambios de role (ROLE_CHANGE)
 * - Registrar cambios de ecommerce (ECOMMERCE_CHANGE)
 * - Mantener trazabilidad de quién realizó qué cambio y cuándo
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AuditService {
    
    private final AuditLogRepository auditLogRepository;
    private final SecurityContextHelper securityContextHelper;
    
    /**
     * Registra un cambio de perfil en la tabla de auditoría.
     * SPEC-004 RN-08: Registrar cambios de perfil con timestamp y user UID.
     * 
     * @param userUid UID del usuario cuyo perfil fue modificado
     * @param description descripción del cambio (ej "Email: old@example.com → new@example.com")
     */
    @Transactional
    public void auditProfileUpdate(UUID userUid, String description) {
        UUID actorUid = securityContextHelper.getCurrentUserUid();
        
        AuditLogEntity auditLog = AuditLogEntity.builder()
                .userUid(userUid)
                .action("PROFILE_UPDATE")
                .description(description)
                .actorUid(actorUid)
                .build();
        
        auditLogRepository.save(auditLog);
        log.debug("Auditoría registrada: PROFILE_UPDATE para usuario {} por actor {}", userUid, actorUid);
    }
    
    /**
     * Registra un cambio de contraseña en la tabla de auditoría.
     * 
     * @param userUid UID del usuario cuya contraseña fue modificada
     * @param description descripción opcional (típicamente null, solo registra que pasó)
     */
    @Transactional
    public void auditPasswordChange(UUID userUid, String description) {
        UUID actorUid = securityContextHelper.getCurrentUserUid();
        
        AuditLogEntity auditLog = AuditLogEntity.builder()
                .userUid(userUid)
                .action("PASSWORD_CHANGE")
                .description(description != null ? description : "Contraseña actualizada")
                .actorUid(actorUid)
                .build();
        
        auditLogRepository.save(auditLog);
        log.debug("Auditoría registrada: PASSWORD_CHANGE para usuario {} por actor {}", userUid, actorUid);
    }
    
    /**
     * Registra un cambio de role.
     * 
     * @param userUid UID del usuario cuyo role fue modificado
     * @param oldRole role anterior
     * @param newRole role nuevo
     */
    @Transactional
    public void auditRoleChange(UUID userUid, String oldRole, String newRole) {
        UUID actorUid = securityContextHelper.getCurrentUserUid();
        String description = String.format("Role: %s → %s", oldRole, newRole);
        
        AuditLogEntity auditLog = AuditLogEntity.builder()
                .userUid(userUid)
                .action("ROLE_CHANGE")
                .description(description)
                .actorUid(actorUid)
                .build();
        
        auditLogRepository.save(auditLog);
        log.debug("Auditoría registrada: ROLE_CHANGE para usuario {} por actor {}: {}", userUid, actorUid, description);
    }
    
    /**
     * Registra un cambio de ecommerce.
     * 
     * @param userUid UID del usuario cuyo ecommerce fue modificado
     * @param oldEcommerceId ecommerce anterior (puede ser null para SUPER_ADMIN)
     * @param newEcommerceId ecommerce nuevo (puede ser null para SUPER_ADMIN)
     */
    @Transactional
    public void auditEcommerceChange(UUID userUid, UUID oldEcommerceId, UUID newEcommerceId) {
        UUID actorUid = securityContextHelper.getCurrentUserUid();
        String description = String.format("Ecommerce: %s → %s", 
                oldEcommerceId != null ? oldEcommerceId.toString() : "null", 
                newEcommerceId != null ? newEcommerceId.toString() : "null");
        
        AuditLogEntity auditLog = AuditLogEntity.builder()
                .userUid(userUid)
                .action("ECOMMERCE_CHANGE")
                .description(description)
                .actorUid(actorUid)
                .build();
        
        auditLogRepository.save(auditLog);
        log.debug("Auditoría registrada: ECOMMERCE_CHANGE para usuario {} por actor {}: {}", userUid, actorUid, description);
    }
}
