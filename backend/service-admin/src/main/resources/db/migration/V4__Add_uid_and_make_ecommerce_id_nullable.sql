-- Migration V4: Add uid field and make ecommerce_id nullable
-- SPEC-002: Gestión de Usuarios por Ecommerce (v2.0)
--
-- Changes:
-- 1. Add uid UUID column (unique, auto-generated)
-- 2. Make ecommerce_id nullable (supports SUPER_ADMIN with no ecommerce binding)
-- 3. Add CHECK constraint to enforce role + ecommerce_id consistency
-- 4. Add idx_role and idx_uid indexes

-- Step 1: Add uid column as nullable first (to allow backfill)
ALTER TABLE users 
ADD COLUMN uid UUID,
ADD CONSTRAINT idx_uid UNIQUE (uid);

-- Step 2: Generate UUIDs for existing rows
UPDATE users 
SET uid = gen_random_uuid() 
WHERE uid IS NULL;

-- Step 3: Make uid NOT NULL after backfill
ALTER TABLE users 
ALTER COLUMN uid SET NOT NULL;

-- Step 4: Make ecommerce_id nullable
ALTER TABLE users 
ALTER COLUMN ecommerce_id DROP NOT NULL;

-- Step 5: Add CHECK constraint for role + ecommerce_id consistency
-- SUPER_ADMIN must have ecommerce_id = NULL (power centralized)
-- USER must have ecommerce_id NOT NULL (scoped to ecommerce)
ALTER TABLE users 
ADD CONSTRAINT chk_role_ecommerce_id CHECK (
    (role = 'SUPER_ADMIN' AND ecommerce_id IS NULL) OR 
    (role = 'USER' AND ecommerce_id IS NOT NULL)
);

-- Step 6: Add idx_role index (for findByRoleAndEcommerceId queries)
CREATE INDEX idx_role ON users(role);

-- RESULT:
-- - uid: UUID, unique, NOT NULL (auto-generated via @PrePersist in UserEntity)
-- - ecommerce_id: UUID, nullable, UNIQUE per role constraint
-- - Constraint ensures data consistency: SUPER_ADMIN=null, USER=notnull
