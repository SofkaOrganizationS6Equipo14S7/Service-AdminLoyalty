-- V16__Seed_default_classification_rules.sql
-- Insert default classification rules for each tier
-- Rules define metric thresholds that trigger tier assignment

-- Bronze tier (level 1): Entry-level
-- Rule 1: total_spent 0-100 (priority 1)
INSERT INTO classification_rules 
(uid, customer_tier_uid, metric_type, min_value, max_value, priority, is_active, created_at, updated_at) 
VALUES 
('550e8400-e29b-41d4-a716-446655450001', 
 '550e8400-e29b-41d4-a716-446655440001', 
 'total_spent', 0, 100, 1, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
ON CONFLICT DO NOTHING;

-- Silver tier (level 2): Growing customer
-- Rule 1: total_spent 100-500 (priority 1)
INSERT INTO classification_rules 
(uid, customer_tier_uid, metric_type, min_value, max_value, priority, is_active, created_at, updated_at) 
VALUES 
('550e8400-e29b-41d4-a716-446655450002', 
 '550e8400-e29b-41d4-a716-446655440002', 
 'total_spent', 100, 500, 1, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
ON CONFLICT DO NOTHING;

-- Rule 2: order_count 5-15 (priority 2)
INSERT INTO classification_rules 
(uid, customer_tier_uid, metric_type, min_value, max_value, priority, is_active, created_at, updated_at) 
VALUES 
('550e8400-e29b-41d4-a716-446655450003', 
 '550e8400-e29b-41d4-a716-446655440002', 
 'order_count', 5, 15, 2, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
ON CONFLICT DO NOTHING;

-- Gold tier (level 3): Loyal customer
-- Rule 1: total_spent 500-2000 (priority 1)
INSERT INTO classification_rules 
(uid, customer_tier_uid, metric_type, min_value, max_value, priority, is_active, created_at, updated_at) 
VALUES 
('550e8400-e29b-41d4-a716-446655450004', 
 '550e8400-e29b-41d4-a716-446655440003', 
 'total_spent', 500, 2000, 1, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
ON CONFLICT DO NOTHING;

-- Rule 2: order_count 15-50 (priority 2)
INSERT INTO classification_rules 
(uid, customer_tier_uid, metric_type, min_value, max_value, priority, is_active, created_at, updated_at) 
VALUES 
('550e8400-e29b-41d4-a716-446655450005', 
 '550e8400-e29b-41d4-a716-446655440003', 
 'order_count', 15, 50, 2, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
ON CONFLICT DO NOTHING;

-- Platinum tier (level 4): VIP customer
-- Rule 1: total_spent >= 2000 (priority 1, no max limit)
INSERT INTO classification_rules 
(uid, customer_tier_uid, metric_type, min_value, max_value, priority, is_active, created_at, updated_at) 
VALUES 
('550e8400-e29b-41d4-a716-446655450006', 
 '550e8400-e29b-41d4-a716-446655440004', 
 'total_spent', 2000, NULL, 1, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
ON CONFLICT DO NOTHING;

-- Rule 2: order_count >= 50 (priority 2, no max limit)
INSERT INTO classification_rules 
(uid, customer_tier_uid, metric_type, min_value, max_value, priority, is_active, created_at, updated_at) 
VALUES 
('550e8400-e29b-41d4-a716-446655450007', 
 '550e8400-e29b-41d4-a716-446655440004', 
 'order_count', 50, NULL, 2, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
ON CONFLICT DO NOTHING;
