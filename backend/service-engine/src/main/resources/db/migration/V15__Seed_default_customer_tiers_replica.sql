-- V15__Seed_default_customer_tiers_replica.sql
-- Initialize Engine's read-only replica of customer tiers from Admin
-- Used for Cold Start autonomy when Admin service is unavailable

INSERT INTO customer_tiers_replica (uid, name, level, is_active, last_synced) VALUES
('550e8400-e29b-41d4-a716-446655440001', 'Bronze', 1, true, CURRENT_TIMESTAMP),
('550e8400-e29b-41d4-a716-446655440002', 'Silver', 2, true, CURRENT_TIMESTAMP),
('550e8400-e29b-41d4-a716-446655440003', 'Gold', 3, true, CURRENT_TIMESTAMP),
('550e8400-e29b-41d4-a716-446655440004', 'Platinum', 4, true, CURRENT_TIMESTAMP)
ON CONFLICT (uid) DO NOTHING;
