-- SPEC-004: Sistema de permisos granulares para STORE_USER
-- 
-- Tablas:
-- - permissions: catálogo de permisos disponibles (read, write, delete, etc. por módulo)
-- - role_permissions: relación muchos a muchos entre roles y permisos
-- - user_permissions: sobrescrituras de permisos específicos a nivel de usuario (futuro)
--
-- Permisos inicial para MVP:
-- - promotion:read    - visualizar promociones
-- - promotion:write   - crear/editar promociones
-- - promotion:delete  - eliminar promociones
-- - user:read         - visualizar usuarios
-- - user:write        - crear/editar usuarios
-- - user:delete       - eliminar usuarios
-- (Extensible para otros módulos)

CREATE TABLE permissions (
    id BIGSERIAL PRIMARY KEY,
    code VARCHAR(50) UNIQUE NOT NULL,
    description VARCHAR(255) NOT NULL,
    module VARCHAR(50) NOT NULL,
    action VARCHAR(50) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    CHECK (action IN ('read', 'write', 'delete'))
);

CREATE INDEX idx_permission_code ON permissions(code);
CREATE INDEX idx_permission_module ON permissions(module);

-- Tabla de relación: cada rol tiene un conjunto de permisos
CREATE TABLE role_permissions (
    id BIGSERIAL PRIMARY KEY,
    role VARCHAR(50) NOT NULL,
    permission_id BIGINT NOT NULL REFERENCES permissions(id) ON DELETE CASCADE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    UNIQUE(role, permission_id),
    CHECK (role IN ('SUPER_ADMIN', 'STORE_ADMIN', 'STORE_USER'))
);

CREATE INDEX idx_role_permissions_role ON role_permissions(role);
CREATE INDEX idx_role_permissions_permission ON role_permissions(permission_id);

-- Insertar permisos iniciales para MVP
INSERT INTO permissions (code, description, module, action) VALUES
    ('promotion:read', 'Visualizar promociones', 'promotion', 'read'),
    ('promotion:write', 'Crear y editar promociones', 'promotion', 'write'),
    ('promotion:delete', 'Eliminar promociones', 'promotion', 'delete'),
    ('user:read', 'Visualizar usuarios', 'user', 'read'),
    ('user:write', 'Crear y editar usuarios', 'user', 'write'),
    ('user:delete', 'Eliminar usuarios', 'user', 'delete'),
    ('ecommerce:read', 'Visualizar datos del ecommerce', 'ecommerce', 'read'),
    ('ecommerce:write', 'Editar datos del ecommerce', 'ecommerce', 'write')
ON CONFLICT (code) DO NOTHING;

-- Asignar permisos por rol
-- SUPER_ADMIN: todos los permisos
INSERT INTO role_permissions (role, permission_id)
SELECT 'SUPER_ADMIN', id FROM permissions
ON CONFLICT (role, permission_id) DO NOTHING;

-- STORE_ADMIN: gestión de usuarios y promociones de su ecommerce
INSERT INTO role_permissions (role, permission_id)
SELECT 'STORE_ADMIN', id FROM permissions WHERE code IN (
    'promotion:read', 'promotion:write', 'promotion:delete',
    'user:read', 'user:write', 'user:delete',
    'ecommerce:read', 'ecommerce:write'
)
ON CONFLICT (role, permission_id) DO NOTHING;

-- STORE_USER: lectura/escritura de promociones, lectura de datos del ecommerce
INSERT INTO role_permissions (role, permission_id)
SELECT 'STORE_USER', id FROM permissions WHERE code IN (
    'promotion:read', 'promotion:write',
    'ecommerce:read'
)
ON CONFLICT (role, permission_id) DO NOTHING;
