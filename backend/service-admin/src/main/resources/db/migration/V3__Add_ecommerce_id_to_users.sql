-- V3__Add_ecommerce_id_to_users.sql
-- Agrega vinculación de usuarios a ecommerce (SPEC-002)
-- Cambios:
-- 1. Agregar columna ecommerce_id (UUID)
-- 2. Asignar un UUID default a usuarios existentes (para testing)
-- 3. Hacer la columna NOT NULL
-- 4. Crear índice para búsquedas rápidas
-- 5. Reemplazar constraint de unicidad: username global (no por ecommerce)

-- Agregar columna ecommerce_id (inicialmente nullable para migración)
ALTER TABLE users ADD COLUMN IF NOT EXISTS ecommerce_id UUID;

-- Asignar un UUID default a usuarios existentes (ej: todos a same test ecommerce)
-- En producción, esto debe ser manejado por un script de datos selectivo
UPDATE users SET ecommerce_id = '00000000-0000-0000-0000-000000000001' WHERE ecommerce_id IS NULL;

-- Hacer la columna NOT NULL
ALTER TABLE users ALTER COLUMN ecommerce_id SET NOT NULL;

-- Crear índice para búsquedas por ecommerce
CREATE INDEX IF NOT EXISTS idx_ecommerce_id ON users(ecommerce_id);

-- Reemplazar constraint de unicidad de username
-- Remover índice anterior si existe
ALTER TABLE users DROP CONSTRAINT IF EXISTS users_username_key;

-- Agregar constraint de unicidad global en username (si no existe)
DO $$ BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.table_constraints 
        WHERE table_name='users' AND constraint_name='uk_username_global'
    ) THEN
        ALTER TABLE users ADD CONSTRAINT uk_username_global UNIQUE (username);
    END IF;
END $$;
