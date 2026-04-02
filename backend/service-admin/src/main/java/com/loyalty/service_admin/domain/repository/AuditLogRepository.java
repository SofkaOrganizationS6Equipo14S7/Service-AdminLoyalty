package com.loyalty.service_admin.domain.repository;

import com.loyalty.service_admin.domain.entity.AuditLogEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLogEntity, UUID> {
    List<AuditLogEntity> findByUserId(UUID userId);
    List<AuditLogEntity> findByEcommerceId(UUID ecommerceId);
    List<AuditLogEntity> findByAction(String action);
    List<AuditLogEntity> findByEntityName(String entityName);
    List<AuditLogEntity> findByEntityId(UUID entityId);
}