-- Flyway Migration: Add email field to users table
-- Version: 7.0
-- Description: Agregar columna email a la tabla users con validaciones de unicidad global
-- SPEC-003: Administración de Ecommerce por STORE_ADMIN

-- Agregar columna email a usuarios sin NOT NULL primero (para no fallar con datos existentes)
ALTER TABLE users ADD COLUMN email VARCHAR(255) UNIQUE;

-- Actualizar registros existentes con email derivado del username
UPDATE users SET email = CONCAT(username, '@placeholder.local') WHERE email IS NULL;

-- Ahora agregar NOT NULL
ALTER TABLE users ALTER COLUMN email SET NOT NULL;

-- Índice compuesto para filtrado eficiente por ecommerce (usado en listUsers)
CREATE INDEX IF NOT EXISTS idx_users_ecommerce_id_active ON users(ecommerce_id, active);

-- COMENTARIOS DE DOCUMENTACIÓN
COMMENT ON COLUMN users.email IS 'Email del usuario. Único globalmente para evitar colisiones entre tenants. Obligatorio para todos los roles excepto SUPER_ADMIN (TBD).';
