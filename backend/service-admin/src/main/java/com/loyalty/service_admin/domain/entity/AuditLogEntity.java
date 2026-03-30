package com.loyalty.service_admin.domain.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

/**
 * Entidad de Auditoría para LOYALTY system.
 * SPEC-004 RN-08: Auditoría de cambios de perfil
 * 
 * Registra:
 * - user_uid: UID del usuario que fue modificado
 * - action: tipo de acción (PROFILE_UPDATE, PASSWORD_CHANGE, etc)
 * - description: descripción de qué cambió
 * - actor_uid: UID del usuario que realizó el cambio (el mismo usuario si auto-modificación)
 * - created_at: timestamp del cambio
 * 
 * Usado para:
 * - Trazabilidad de cambios
 * - Auditoría de seguridad
 * - Debugging y análisis
 */
@Entity
@Table(name = "audit_logs", indexes = {
    @Index(name = "idx_audit_user_uid", columnList = "user_uid"),
    @Index(name = "idx_audit_actor_uid", columnList = "actor_uid"),
    @Index(name = "idx_audit_action", columnList = "action"),
    @Index(name = "idx_audit_created_at", columnList = "created_at")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuditLogEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "user_uid", nullable = false)
    private UUID userUid;
    
    @Column(name = "action", nullable = false, length = 50)
    private String action;
    
    @Column(name = "description", length = 500)
    private String description;
    
    @Column(name = "actor_uid", nullable = false)
    private UUID actorUid;
    
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
    }
}
