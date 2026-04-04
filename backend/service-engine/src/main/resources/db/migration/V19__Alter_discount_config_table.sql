-- Align discount_config table with DiscountConfigEntity (rename id->uid, add ecommerce_id, drop created_by_user_id)

-- 1. Rename primary key column
ALTER TABLE discount_config RENAME COLUMN id TO uid;

-- 2. Add ecommerce_id column
ALTER TABLE discount_config ADD COLUMN ecommerce_id UUID;

-- 3. Backfill with a generated UUID so NOT NULL constraint can be applied
UPDATE discount_config SET ecommerce_id = gen_random_uuid() WHERE ecommerce_id IS NULL;

-- 4. Apply NOT NULL constraint
ALTER TABLE discount_config ALTER COLUMN ecommerce_id SET NOT NULL;

-- 5. Remove column no longer in the entity
ALTER TABLE discount_config DROP COLUMN IF EXISTS created_by_user_id;

-- 6. Create indexes expected by the entity
CREATE INDEX IF NOT EXISTS idx_discount_config_ecommerce_id ON discount_config(ecommerce_id);
CREATE INDEX IF NOT EXISTS idx_discount_config_ecommerce_active ON discount_config(ecommerce_id, is_active);
