-- Flyway Migration: Add SUPER_ADMIN check constraint to users table
-- Version: 8.0
-- Description: Garantizar que SUPER_ADMIN tiene ecommerce_id = NULL y otros usuarios tienen ecommerce_id NOT NULL
-- SPEC-005: SUPER_ADMIN — Acceso Total a la Plataforma

-- CHECK CONSTRAINT: Garantiza integridad referencial multi-tenant
-- Rule: (role='SUPER_ADMIN' AND ecommerce_id IS NULL) OR (role!='SUPER_ADMIN' AND ecommerce_id IS NOT NULL)
--
-- Justificación:
-- - SUPER_ADMIN no puede tener ecommerce_id (acceso global sin restricción)
-- - STORE_ADMIN y STORE_USER DEBEN tener ecommerce_id (vinculados a exactamente un tenant)
-- - Esta constraintase aplica a nivel BD para garantizar consistency sin lógica app-level
--
ALTER TABLE users
ADD CONSTRAINT check_superadmin_no_ecommerce_id
CHECK (
    (role = 'SUPER_ADMIN' AND ecommerce_id IS NULL) 
    OR 
    (role IN ('STORE_ADMIN', 'STORE_USER') AND ecommerce_id IS NOT NULL)
);

-- COMENTARIOS DE DOCUMENTACIÓN
COMMENT ON CONSTRAINT check_superadmin_no_ecommerce_id ON users IS 
'Regla RN-01 (SPEC-005): Garantiza que SUPER_ADMIN no tiene ecommerce_id y otros roles si tienen. Implementado a nivel BD para integridad automática.';
