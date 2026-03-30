package com.loyalty.service_admin.domain.repository;

import com.loyalty.service_admin.domain.entity.AuditLogEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Repositorio para la entidad AuditLog.
 * Query methods:
 * - findByUserUid(): obtener auditoría de cambios de un usuario
 * - findByActorUid(): obtener cambios realizados por un usuario
 * - findByAction(): obtener cambios de un tipo específico
 */
@Repository
public interface AuditLogRepository extends JpaRepository<AuditLogEntity, Long> {
    List<AuditLogEntity> findByUserUid(UUID userUid);
    List<AuditLogEntity> findByActorUid(UUID actorUid);
    List<AuditLogEntity> findByAction(String action);
}
