-- Create discount_priority table for ordering how different discount types are applied
CREATE TABLE IF NOT EXISTS discount_priority (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    discount_config_id UUID NOT NULL REFERENCES discount_config(id) ON DELETE CASCADE,
    discount_type VARCHAR(50) NOT NULL,
    priority_level INTEGER NOT NULL CHECK (priority_level > 0),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Unique constraint: one discount type per config
CREATE UNIQUE INDEX idx_discount_priority_type_config 
    ON discount_priority(discount_config_id, discount_type);

-- Unique constraint: priority levels are unique per config
CREATE UNIQUE INDEX idx_discount_priority_level_config 
    ON discount_priority(discount_config_id, priority_level);

-- Index for faster queries by config
CREATE INDEX idx_discount_priority_config 
    ON discount_priority(discount_config_id);

-- Index for sorted retrieval by priority
CREATE INDEX idx_discount_priority_level_sorted 
    ON discount_priority(discount_config_id, priority_level);

-- Add comment
COMMENT ON TABLE discount_priority IS 'Maps discount types to priority levels; lower priority level = higher priority (applied first)';
COMMENT ON COLUMN discount_priority.priority_level IS 'Sequential integers 1..N; 1 has highest priority (applied first)';
