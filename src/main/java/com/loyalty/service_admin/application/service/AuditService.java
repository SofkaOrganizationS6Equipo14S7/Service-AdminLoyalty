package com.loyalty.service_admin.application.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.loyalty.service_admin.domain.entity.AuditLogEntity;
import com.loyalty.service_admin.domain.entity.UserEntity;
import com.loyalty.service_admin.domain.repository.AuditLogRepository;
import com.loyalty.service_admin.infrastructure.security.SecurityContextHelper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuditService {
    
    private final AuditLogRepository auditLogRepository;
    private final SecurityContextHelper securityContextHelper;
    private final ObjectMapper objectMapper;

    private String toJson(Map<String, Object> data) {
        try {
            return objectMapper.writeValueAsString(data);
        } catch (JsonProcessingException e) {
            log.warn("Error serializando audit value a JSON", e);
            return "{\"error\":\"serialization_failed\"}";
        }
    }
    
    @Transactional
    public void auditProfileUpdate(UUID userId, String email) {
        AuditLogEntity auditLog = AuditLogEntity.builder()
                .userId(userId)
                .action("PROFILE_UPDATE")
                .entityName("USER_PROFILE")
                .entityId(userId)
                .newValue(toJson(Map.of("email", email != null ? email : "")))
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
                .newValue(toJson(Map.of("description", description != null ? description : "Password changed")))
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
                .oldValue(toJson(Map.of("role", oldRole != null ? oldRole.toString() : "")))
                .newValue(toJson(Map.of("role", newRole != null ? newRole.toString() : "")))
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
                .oldValue(toJson(Map.of("ecommerceId", oldEcommerceId != null ? oldEcommerceId.toString() : "")))
                .newValue(toJson(Map.of("ecommerceId", newEcommerceId != null ? newEcommerceId.toString() : "")))
                .build();
        
        auditLogRepository.save(auditLog);
        log.debug("Auditoría registrada: ECOMMERCE_CHANGE para usuario {}", userId);
    }
    
    @Transactional
    public void auditUserDeletion(UserEntity user, UUID actorUid) {
        AuditLogEntity auditLog = AuditLogEntity.builder()
                .userId(user.getId())
                .ecommerceId(user.getEcommerceId())
                .action("USER_DELETE")
                .entityName("USER")
                .entityId(user.getId())
                .oldValue(toJson(Map.of(
                    "username", user.getUsername(),
                    "email", user.getEmail() != null ? user.getEmail() : "",
                    "role_name", user.getRole() != null ? user.getRole().getName() : "",
                    "ecommerce_id", user.getEcommerceId() != null ? user.getEcommerceId().toString() : ""
                )))
                .newValue(toJson(Map.of(
                    "actor_uid", actorUid.toString(),
                    "action", "USER_DELETE"
                )))
                .build();
        
        auditLogRepository.save(auditLog);
        log.info("Auditoría registrada: USER_DELETE para usuario {} por actor {}", user.getId(), actorUid);
    }
    
    @Transactional
    public void auditUserCreation(UserEntity user, UUID actorUid) {
        AuditLogEntity auditLog = AuditLogEntity.builder()
                .userId(user.getId())
                .ecommerceId(user.getEcommerceId())
                .action("USER_CREATE")
                .entityName("USER")
                .entityId(user.getId())
                .newValue(toJson(Map.of(
                    "username", user.getUsername(),
                    "email", user.getEmail() != null ? user.getEmail() : "",
                    "role_name", user.getRole() != null ? user.getRole().getName() : "",
                    "ecommerce_id", user.getEcommerceId() != null ? user.getEcommerceId().toString() : "",
                    "actor_uid", actorUid.toString()
                )))
                .build();
        
        auditLogRepository.save(auditLog);
        log.info("Auditoría registrada: USER_CREATE para usuario {} por actor {}", user.getId(), actorUid);
    }
    
    @Transactional
    public void auditUserUpdate(UserEntity user, UUID actorUid) {
        AuditLogEntity auditLog = AuditLogEntity.builder()
                .userId(user.getId())
                .ecommerceId(user.getEcommerceId())
                .action("USER_UPDATE")
                .entityName("USER")
                .entityId(user.getId())
                .newValue(toJson(Map.of(
                    "username", user.getUsername(),
                    "email", user.getEmail() != null ? user.getEmail() : "",
                    "role_name", user.getRole() != null ? user.getRole().getName() : "",
                    "ecommerce_id", user.getEcommerceId() != null ? user.getEcommerceId().toString() : "",
                    "actor_uid", actorUid.toString()
                )))
                .build();
        
        auditLogRepository.save(auditLog);
        log.info("Auditoría registrada: USER_UPDATE para usuario {} por actor {}", user.getId(), actorUid);
    }
}

