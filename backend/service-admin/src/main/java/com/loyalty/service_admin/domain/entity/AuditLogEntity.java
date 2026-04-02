package com.loyalty.service_admin.domain.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "audit_log", indexes = {
    @Index(name = "idx_audit_log_ecommerce", columnList = "ecommerce_id"),
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AuditLogEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @Column(name = "user_id")
    private UUID userId;
    
    @Column(name = "ecommerce_id")
    private UUID ecommerceId;
    
    @Column(name = "action", nullable = false, length = 50)
    private String action;
    
    @Column(name = "entity_name", nullable = false, length = 100)
    private String entityName;
    
    @Column(name = "entity_id")
    private UUID entityId;
    
    @Column(name = "old_value", columnDefinition = "jsonb")
    private String oldValue;
    
    @Column(name = "new_value", columnDefinition = "jsonb")
    private String newValue;
    
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
    }
}