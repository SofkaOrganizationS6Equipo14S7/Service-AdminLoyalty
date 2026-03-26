package com.loyalty.service_engine.domain.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "api_keys", indexes = {
    @Index(name = "idx_hashed_key", columnList = "hashed_key", unique = true),
    @Index(name = "idx_ecommerce_id", columnList = "ecommerce_id")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ApiKeyEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @Column(name = "hashed_key", unique = true, nullable = false, length = 64)
    private String hashedKey;
    
    @Column(name = "ecommerce_id", nullable = false)
    private UUID ecommerceId;
    
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
    
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
        updatedAt = Instant.now();
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }
}
