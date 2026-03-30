-- SPEC-004 RN-08: Auditoría de cambios de perfil
-- 
-- Tabla para registrar cambios en perfiles de usuarios:
-- - Cambios de nombre, email, contraseña
-- - Quién hizo el cambio (user_uid)
-- - Cuándo se hizo (timestamp)
-- - Qué acción fue (action: PROFILE_UPDATE, PASSWORD_CHANGE)

CREATE TABLE audit_logs (
    id BIGSERIAL PRIMARY KEY,
    user_uid UUID NOT NULL,
    action VARCHAR(50) NOT NULL,
    description VARCHAR(500),
    actor_uid UUID NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    FOREIGN KEY (user_uid) REFERENCES users(uid) ON DELETE CASCADE,
    FOREIGN KEY (actor_uid) REFERENCES users(uid) ON DELETE SET NULL,
    CHECK (action IN ('PROFILE_UPDATE', 'PASSWORD_CHANGE', 'ROLE_CHANGE', 'ECOMMERCE_CHANGE'))
);

CREATE INDEX idx_audit_user_uid ON audit_logs(user_uid);
CREATE INDEX idx_audit_actor_uid ON audit_logs(actor_uid);
CREATE INDEX idx_audit_action ON audit_logs(action);
CREATE INDEX idx_audit_created_at ON audit_logs(created_at);
