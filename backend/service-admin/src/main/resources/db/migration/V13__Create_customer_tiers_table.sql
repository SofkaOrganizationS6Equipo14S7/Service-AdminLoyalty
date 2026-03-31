
CREATE TABLE customer_tiers (
    uid UUID PRIMARY KEY,
    name VARCHAR(100) NOT NULL UNIQUE,
    level INTEGER NOT NULL CHECK (level >= 1 AND level <= 4),
    is_active BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_customer_tiers_active_level ON customer_tiers(is_active, level);
CREATE INDEX idx_customer_tiers_name_active ON customer_tiers(name, is_active);
