-- V3__Add_ecommerce_id_to_users.sql
-- Agrega vinculación de usuarios a ecommerce (SPEC-002)
-- Cambios:
-- 1. Agregar columna ecommerce_id (UUID, NOT NULL)
-- 2. Crear índice para búsquedas rápidas
-- 3. Reemplazar constraint de unicidad: username global (no por ecommerce)

-- Agregar columna ecommerce_id (inicialmente nullable para migración)
ALTER TABLE users ADD COLUMN ecommerce_id UUID;

-- Crear índice para búsquedas por ecommerce
CREATE INDEX idx_ecommerce_id ON users(ecommerce_id);

-- Hacer la columna NOT NULL (después de asignar valores)
ALTER TABLE users ALTER COLUMN ecommerce_id SET NOT NULL;

-- Reemplazar constraint de unicidad de username
-- Remover índice anterior si existe
ALTER TABLE users DROP CONSTRAINT IF EXISTS users_username_key;

-- Agregar constraint de unicidad global en username
ALTER TABLE users ADD CONSTRAINT uk_username_global UNIQUE (username);
