-- Flyway Migration: Create users table for authentication
-- Version: 2.0
-- Description: Tabla para almacenar usuarios y sus credenciales

CREATE TABLE IF NOT EXISTS users (
    id BIGSERIAL PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    role VARCHAR(50) NOT NULL DEFAULT 'USER',
    active BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

-- Índices para búsquedas frecuentes
CREATE UNIQUE INDEX IF NOT EXISTS idx_username ON users(username);
CREATE INDEX IF NOT EXISTS idx_active ON users(active);

COMMENT ON TABLE users IS 'Usuarios del sistema con credenciales de acceso';
COMMENT ON COLUMN users.id IS 'Identificador único del usuario';
COMMENT ON COLUMN users.username IS 'Nombre de usuario único';
COMMENT ON COLUMN users.password IS 'Contraseña del usuario (plaintext en v1, será hashing en v2)';
COMMENT ON COLUMN users.role IS 'Rol del usuario (ADMIN, USER)';
COMMENT ON COLUMN users.active IS 'Estado del usuario (activo=true, inactivo=false)';
COMMENT ON COLUMN users.created_at IS 'Timestamp de creación (UTC)';
COMMENT ON COLUMN users.updated_at IS 'Timestamp de última actualización (UTC)';
