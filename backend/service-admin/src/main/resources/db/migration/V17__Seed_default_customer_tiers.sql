-- V15__Seed_default_customer_tiers.sql
-- Insert default loyalty tiers for classification system

-- Generate UUIDs using PostgreSQL uuid-ossp (should be enabled in extensions)
-- If not available locally, use predefined UUIDs for consistency across environments

INSERT INTO customer_tiers (uid, name, level, is_active, created_at, updated_at) VALUES
-- Bronze: Entry-level tier for new/low-value customers
('550e8400-e29b-41d4-a716-446655440001', 'Bronze', 1, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),

-- Silver: Mid-level tier for growing customer relationships
('550e8400-e29b-41d4-a716-446655440002', 'Silver', 2, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),

-- Gold: High-value tier for loyal, frequent customers
('550e8400-e29b-41d4-a716-446655440003', 'Gold', 3, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),

-- Platinum: Premium tier for VIP customers with highest loyalty
('550e8400-e29b-41d4-a716-446655440004', 'Platinum', 4, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)

ON CONFLICT (uid) DO NOTHING;
