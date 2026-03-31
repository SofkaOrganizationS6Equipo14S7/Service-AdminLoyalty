
CREATE TABLE customer_tiers_replica (
    uid UUID PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    level INTEGER NOT NULL CHECK (level >= 1 AND level <= 4),
    is_active BOOLEAN NOT NULL DEFAULT true,
    last_synced TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_customer_tiers_replica_active_level ON customer_tiers_replica(is_active, level);
CREATE INDEX idx_customer_tiers_replica_name ON customer_tiers_replica(name);
