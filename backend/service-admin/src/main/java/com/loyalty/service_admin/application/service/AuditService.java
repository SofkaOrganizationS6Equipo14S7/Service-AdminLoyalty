package com.loyalty.service_admin.application.service;

import com.loyalty.service_admin.domain.entity.AuditLogEntity;
import com.loyalty.service_admin.domain.repository.AuditLogRepository;
import com.loyalty.service_admin.infrastructure.security.SecurityContextHelper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuditService {
    
    private final AuditLogRepository auditLogRepository;
    private final SecurityContextHelper securityContextHelper;
    
    @Transactional
    public void auditProfileUpdate(UUID userId, String description) {
        AuditLogEntity auditLog = AuditLogEntity.builder()
                .userId(userId)
                .action("PROFILE_UPDATE")
                .entityName("USER_PROFILE")
                .entityId(userId)
                .newValue(description != null ? description : "Profile updated")
                .build();
        
        auditLogRepository.save(auditLog);
        log.debug("Auditoría registrada: PROFILE_UPDATE para usuario {}", userId);
    }
    
    @Transactional
    public void auditPasswordChange(UUID userId, String description) {
        AuditLogEntity auditLog = AuditLogEntity.builder()
                .userId(userId)
                .action("PASSWORD_CHANGE")
                .entityName("USER_PASSWORD")
                .entityId(userId)
                .newValue("Password changed")
                .build();
        
        auditLogRepository.save(auditLog);
        log.debug("Auditoría registrada: PASSWORD_CHANGE para usuario {}", userId);
    }
    
    @Transactional
    public void auditRoleChange(UUID userId, Object oldRole, Object newRole) {
        AuditLogEntity auditLog = AuditLogEntity.builder()
                .userId(userId)
                .action("ROLE_CHANGE")
                .entityName("USER_ROLE")
                .entityId(userId)
                .oldValue(oldRole != null ? oldRole.toString() : "null")
                .newValue(newRole != null ? newRole.toString() : "null")
                .build();
        
        auditLogRepository.save(auditLog);
        log.debug("Auditoría registrada: ROLE_CHANGE para usuario {}", userId);
    }
    
    @Transactional
    public void auditEcommerceChange(UUID userId, UUID oldEcommerceId, UUID newEcommerceId) {
        AuditLogEntity auditLog = AuditLogEntity.builder()
                .userId(userId)
                .action("ECOMMERCE_CHANGE")
                .entityName("USER_ECOMMERCE")
                .entityId(userId)
                .oldValue(oldEcommerceId != null ? oldEcommerceId.toString() : "null")
                .newValue(newEcommerceId != null ? newEcommerceId.toString() : "null")
                .build();
        
        auditLogRepository.save(auditLog);
        log.debug("Auditoría registrada: ECOMMERCE_CHANGE para usuario {}", userId);
    }
}

