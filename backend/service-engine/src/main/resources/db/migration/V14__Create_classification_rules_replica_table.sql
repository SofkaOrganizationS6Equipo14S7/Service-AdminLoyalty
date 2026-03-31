
CREATE TABLE classification_rules_replica (
    uid UUID PRIMARY KEY,
    customer_tier_uid UUID NOT NULL,
    metric_type VARCHAR(50) NOT NULL,
    min_value NUMERIC(19, 2) NOT NULL DEFAULT 0,
    max_value NUMERIC(19, 2),
    priority INTEGER NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT true,
    last_synced TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT fk_classification_rules_replica_tier 
        FOREIGN KEY (customer_tier_uid) 
        REFERENCES customer_tiers_replica(uid) 
        ON DELETE RESTRICT
);

CREATE INDEX idx_classification_rules_replica_active ON classification_rules_replica(is_active);
CREATE INDEX idx_classification_rules_replica_tier_priority ON classification_rules_replica(customer_tier_uid, priority, is_active);
CREATE INDEX idx_classification_rules_replica_metric_type ON classification_rules_replica(metric_type, is_active);
