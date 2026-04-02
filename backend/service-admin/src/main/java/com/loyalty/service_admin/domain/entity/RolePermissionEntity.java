package com.loyalty.service_admin.domain.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "role_permissions", indexes = {
    @Index(name = "idx_role_permissions_role", columnList = "role_id"),
    @Index(name = "idx_role_permissions_permission", columnList = "permission_id")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RolePermissionEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "role_id", nullable = false)
    private UUID roleId;

    @Column(name = "permission_id", nullable = false)
    private UUID permissionId;
}