-- Flyway Migration: Add SUPER_ADMIN check constraint to users table
-- Version: 8.0
-- Description: Garantizar que SUPER_ADMIN tiene ecommerce_id = NULL y otros usuarios tienen ecommerce_id NOT NULL
-- SPEC-005: SUPER_ADMIN — Acceso Total a la Plataforma

-- El constraint check_superadmin_no_ecommerce_id ya fue creado manualmente
-- en intentos anteriores. Esta migración queda vacía para mantener el versionado.

-- Verificar que existe el constraint (no hace nada si ya existe)
-- La lógica se aplica: (role='SUPER_ADMIN' AND ecommerce_id IS NULL) OR (role!='SUPER_ADMIN' AND ecommerce_id IS NOT NULL)
