CREATE TABLE IF NOT EXISTS discount_configurations (
    id UUID PRIMARY KEY,
    ecommerce_id UUID NOT NULL UNIQUE,
    currency VARCHAR(3) NOT NULL,
    rounding_rule VARCHAR(20) NOT NULL,
    cap_type VARCHAR(20) NOT NULL,
    cap_value NUMERIC(12,4) NOT NULL CHECK (cap_value > 0),
    cap_applies_to VARCHAR(20) NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS discount_priorities (
    id UUID PRIMARY KEY,
    configuration_id UUID NOT NULL REFERENCES discount_configurations(id) ON DELETE CASCADE,
    discount_type VARCHAR(50) NOT NULL,
    priority_order INTEGER NOT NULL CHECK (priority_order > 0)
);

CREATE UNIQUE INDEX IF NOT EXISTS uq_discount_priorities_cfg_order
    ON discount_priorities(configuration_id, priority_order);

CREATE UNIQUE INDEX IF NOT EXISTS uq_discount_priorities_cfg_type
    ON discount_priorities(configuration_id, discount_type);
