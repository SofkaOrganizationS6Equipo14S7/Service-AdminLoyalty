-- V16__Seed_default_classification_rules_replica.sql
-- Initialize Engine's read-only replica of classification rules from Admin
-- Used for Cold Start autonomy and cache population

INSERT INTO classification_rules_replica 
(uid, customer_tier_uid, metric_type, min_value, max_value, priority, is_active, last_synced) 
VALUES 
-- Bronze tier rules
('550e8400-e29b-41d4-a716-446655450001', '550e8400-e29b-41d4-a716-446655440001', 'total_spent', 0, 100, 1, true, CURRENT_TIMESTAMP),

-- Silver tier rules
('550e8400-e29b-41d4-a716-446655450002', '550e8400-e29b-41d4-a716-446655440002', 'total_spent', 100, 500, 1, true, CURRENT_TIMESTAMP),
('550e8400-e29b-41d4-a716-446655450003', '550e8400-e29b-41d4-a716-446655440002', 'order_count', 5, 15, 2, true, CURRENT_TIMESTAMP),

-- Gold tier rules
('550e8400-e29b-41d4-a716-446655450004', '550e8400-e29b-41d4-a716-446655440003', 'total_spent', 500, 2000, 1, true, CURRENT_TIMESTAMP),
('550e8400-e29b-41d4-a716-446655450005', '550e8400-e29b-41d4-a716-446655440003', 'order_count', 15, 50, 2, true, CURRENT_TIMESTAMP),

-- Platinum tier rules
('550e8400-e29b-41d4-a716-446655450006', '550e8400-e29b-41d4-a716-446655440004', 'total_spent', 2000, NULL, 1, true, CURRENT_TIMESTAMP),
('550e8400-e29b-41d4-a716-446655450007', '550e8400-e29b-41d4-a716-446655440004', 'order_count', 50, NULL, 2, true, CURRENT_TIMESTAMP)

ON CONFLICT (uid) DO NOTHING;
