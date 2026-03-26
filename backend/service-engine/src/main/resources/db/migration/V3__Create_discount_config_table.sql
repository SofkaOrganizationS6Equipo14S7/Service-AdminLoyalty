-- Create discount_config table for storing max discount limits per currency
CREATE TABLE IF NOT EXISTS discount_config (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    max_discount_limit NUMERIC(19,2) NOT NULL CHECK (max_discount_limit > 0),
    currency_code VARCHAR(3) NOT NULL DEFAULT 'USD',
    is_active BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by_user_id UUID NOT NULL
);

-- Unique constraint: only one active config at a time
CREATE UNIQUE INDEX idx_discount_config_active 
    ON discount_config(is_active) 
    WHERE is_active = true;

-- Index for faster lookups by active status
CREATE INDEX idx_discount_config_inactive 
    ON discount_config(is_active) 
    WHERE is_active = false;

-- Add comment
COMMENT ON TABLE discount_config IS 'Stores maximum discount limits per currency to prevent excessive discounts on transactions';
COMMENT ON COLUMN discount_config.is_active IS 'Only one config can be active; deactivating old config when new one created';
