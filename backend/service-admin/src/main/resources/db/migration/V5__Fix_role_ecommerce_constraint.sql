-- V4__Fix_role_ecommerce_constraint.sql
-- Corrige la constraint chk_role_ecommerce_id según SPEC-002 v2.0
-- ADMIN → ecommerce_id NULL (acceso global)
-- USER → ecommerce_id NOT NULL (acceso por ecommerce)
-- Cambios:
-- 1. Hacer columna ecommerce_id NULLABLE para ADMIN
-- 2. Crear constraint correcta role/ecommerce

-- Hacer columna nullable (permite ADMIN sin ecommerce)
ALTER TABLE users ALTER COLUMN ecommerce_id DROP NOT NULL;

-- Eliminar constraint anterior si existe
ALTER TABLE users DROP CONSTRAINT IF EXISTS chk_role_ecommerce_id;

-- Crear nueva constraint según spec
-- ADMIN: ecommerce_id NULL | USER: ecommerce_id NOT NULL
ALTER TABLE users ADD CONSTRAINT chk_role_ecommerce_id CHECK (
    (role = 'ADMIN' AND ecommerce_id IS NULL) OR
    (role = 'USER' AND ecommerce_id IS NOT NULL)
);
