-- Migration: Add rule_customer_tiers junction table
-- Purpose: Link rules to specific customer tiers for flexible rule application

CREATE TABLE rule_customer_tiers (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    rule_id UUID NOT NULL REFERENCES rules(id) ON DELETE CASCADE,
    customer_tier_id UUID NOT NULL REFERENCES customer_tiers(id) ON DELETE CASCADE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(rule_id, customer_tier_id)
);

CREATE INDEX idx_rule_customer_tiers_rule ON rule_customer_tiers(rule_id);
CREATE INDEX idx_rule_customer_tiers_tier ON rule_customer_tiers(customer_tier_id);
