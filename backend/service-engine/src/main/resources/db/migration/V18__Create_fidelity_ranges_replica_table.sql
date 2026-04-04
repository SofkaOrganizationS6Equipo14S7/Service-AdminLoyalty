-- Replica table for Cold Start autonomy
-- Synchronized from Admin Service via RabbitMQ events
-- Pre-loaded into Caffeine cache at service startup

CREATE TABLE IF NOT EXISTS fidelity_ranges (
    uid UUID PRIMARY KEY,
    ecommerce_id UUID NOT NULL,
    name VARCHAR(255) NOT NULL,
    min_points INTEGER NOT NULL CHECK (min_points >= 0),
    max_points INTEGER NOT NULL CHECK (max_points > min_points),
    discount_percentage NUMERIC(5,2) NOT NULL CHECK (discount_percentage BETWEEN 0 AND 100),
    is_active BOOLEAN DEFAULT true,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT ck_fidelity_discount_range CHECK (discount_percentage >= 0 AND discount_percentage <= 100)
);

-- Indices for query optimization
CREATE INDEX idx_fidelity_ranges_ecommerce_id ON fidelity_ranges(ecommerce_id);
CREATE INDEX idx_fidelity_ranges_active ON fidelity_ranges(is_active);
CREATE INDEX idx_fidelity_ranges_ecommerce_active ON fidelity_ranges(ecommerce_id, is_active);
CREATE INDEX idx_fidelity_ranges_min_points ON fidelity_ranges(min_points);

-- Table documentation
COMMENT ON TABLE fidelity_ranges IS 'Replica: Synchronized from Admin Service via RabbitMQ. Pre-loaded in Caffeine at startup.';
COMMENT ON COLUMN fidelity_ranges.min_points IS 'Minimum points (inclusive) to access this level';
COMMENT ON COLUMN fidelity_ranges.max_points IS 'Maximum points (inclusive) for this level. Gaps allowed.';
