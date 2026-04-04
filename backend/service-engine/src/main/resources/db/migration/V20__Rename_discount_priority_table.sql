-- Align discount_priority table with DiscountPriorityEntity
-- Entity expects table name 'discount_limit_priority' with PK column 'uid'
-- V4 created it as 'discount_priority' with PK column 'id'

-- 1. Drop old indexes referencing the old table (they will be recreated below)
DROP INDEX IF EXISTS idx_discount_priority_type_config;
DROP INDEX IF EXISTS idx_discount_priority_level_config;
DROP INDEX IF EXISTS idx_discount_priority_config;
DROP INDEX IF EXISTS idx_discount_priority_level_sorted;

-- 2. Drop FK constraint referencing discount_config(id), now renamed to uid
ALTER TABLE discount_priority DROP CONSTRAINT IF EXISTS discount_priority_discount_config_id_fkey;

-- 3. Rename PK column id -> uid
ALTER TABLE discount_priority RENAME COLUMN id TO uid;

-- 4. Rename the table
ALTER TABLE discount_priority RENAME TO discount_limit_priority;

-- 5. Re-create expected indexes
CREATE INDEX IF NOT EXISTS idx_discount_limit_priority_config_id
    ON discount_limit_priority(discount_config_id);

CREATE UNIQUE INDEX IF NOT EXISTS idx_discount_limit_priority_config_type
    ON discount_limit_priority(discount_config_id, discount_type);

CREATE UNIQUE INDEX IF NOT EXISTS idx_discount_limit_priority_config_level
    ON discount_limit_priority(discount_config_id, priority_level);
