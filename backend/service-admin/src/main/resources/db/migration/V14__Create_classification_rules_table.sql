
CREATE TABLE classification_rules (
    uid UUID PRIMARY KEY,
    customer_tier_uid UUID NOT NULL,
    metric_type VARCHAR(50) NOT NULL, -- 'total_spent', 'order_count', 'loyalty_points', 'custom', etc.
    min_value NUMERIC(19, 2) NOT NULL DEFAULT 0,
    max_value NUMERIC(19, 2), -- NULL means unbounded (no upper limit)
    priority INTEGER NOT NULL, -- Lower number = higher priority; must be unique per tier
    is_active BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT fk_classification_rules_tier 
        FOREIGN KEY (customer_tier_uid) 
        REFERENCES customer_tiers(uid) 
        ON DELETE RESTRICT
);

CREATE INDEX idx_classification_rules_active ON classification_rules(is_active);
CREATE INDEX idx_classification_rules_tier_priority ON classification_rules(customer_tier_uid, priority, is_active);
CREATE INDEX idx_classification_rules_metric_type ON classification_rules(metric_type, is_active);

CREATE UNIQUE INDEX idx_classification_rules_tier_priority_unique 
    ON classification_rules(customer_tier_uid, priority) 
    WHERE is_active = true;
