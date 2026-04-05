-- V2__Seed_initial_data.sql
-- Seeder: Initial data for testing and development
-- Includes: Ecommerce, discount settings, priorities, customer tiers, and rule attributes

-- ==========================================
-- 1. ECOMMERCE SETUP
-- ==========================================

-- Insert test ecommerce
INSERT INTO ecommerce (id, name, slug, status) VALUES
    ('550e8400-e29b-41d4-a716-446655440000'::UUID, 'Test Ecommerce', 'test-ecommerce', 'ACTIVE'),
    ('550e8400-e29b-41d4-a716-446655440001'::UUID, 'Prod Ecommerce', 'prod-ecommerce', 'ACTIVE')
ON CONFLICT DO NOTHING;

-- ==========================================
-- 2. DISCOUNT SETTINGS & PRIORITIES
-- ==========================================

-- Create discount settings for test ecommerce
INSERT INTO discount_settings (id, ecommerce_id, max_discount_cap, currency_code, allow_stacking, is_active) VALUES
    ('550e8400-e29b-41d4-a716-446655440100'::UUID, '550e8400-e29b-41d4-a716-446655440000'::UUID, 50.00, 'USD', TRUE, TRUE)
ON CONFLICT DO NOTHING;

-- Map discount types to priorities (order of application)
INSERT INTO discount_priorities (id, discount_setting_id, discount_type_id, priority_level, is_active) 
SELECT 
    '550e8400-e29b-41d4-a716-446655440110'::UUID,
    '550e8400-e29b-41d4-a716-446655440100'::UUID,
    dt.id,
    1,
    TRUE
FROM discount_types dt WHERE dt.code = 'FIDELITY'
ON CONFLICT DO NOTHING;

INSERT INTO discount_priorities (id, discount_setting_id, discount_type_id, priority_level, is_active) 
SELECT 
    '550e8400-e29b-41d4-a716-446655440111'::UUID,
    '550e8400-e29b-41d4-a716-446655440100'::UUID,
    dt.id,
    2,
    TRUE
FROM discount_types dt WHERE dt.code = 'SEASONAL'
ON CONFLICT DO NOTHING;

INSERT INTO discount_priorities (id, discount_setting_id, discount_type_id, priority_level, is_active) 
SELECT 
    '550e8400-e29b-41d4-a716-446655440112'::UUID,
    '550e8400-e29b-41d4-a716-446655440100'::UUID,
    dt.id,
    3,
    TRUE
FROM discount_types dt WHERE dt.code = 'PRODUCT'
ON CONFLICT DO NOTHING;

INSERT INTO discount_priorities (id, discount_setting_id, discount_type_id, priority_level, is_active) 
SELECT 
    '550e8400-e29b-41d4-a716-446655440113'::UUID,
    '550e8400-e29b-41d4-a716-446655440100'::UUID,
    dt.id,
    4,
    TRUE
FROM discount_types dt WHERE dt.code = 'CLASSIFICATION'
ON CONFLICT DO NOTHING;

-- ==========================================
-- 3. CUSTOMER TIERS
-- ==========================================

INSERT INTO customer_tiers (id, ecommerce_id, name, discount_percentage, hierarchy_level, is_active) 
SELECT 
    '550e8400-e29b-41d4-a716-446655440200'::UUID,
    '550e8400-e29b-41d4-a716-446655440000'::UUID,
    'Gold',
    10.00,
    1,
    TRUE
FROM discount_types dt WHERE dt.code = 'FIDELITY'
ON CONFLICT DO NOTHING;

INSERT INTO customer_tiers (id, ecommerce_id, name, discount_percentage, hierarchy_level, is_active) 
SELECT 
    '550e8400-e29b-41d4-a716-446655440201'::UUID,
    '550e8400-e29b-41d4-a716-446655440000'::UUID,
    'Silver',
    5.00,
    2,
    TRUE
FROM discount_types dt WHERE dt.code = 'FIDELITY'
ON CONFLICT DO NOTHING;

INSERT INTO customer_tiers (id, ecommerce_id, name, discount_percentage, hierarchy_level, is_active) 
SELECT 
    '550e8400-e29b-41d4-a716-446655440202'::UUID,
    '550e8400-e29b-41d4-a716-446655440000'::UUID,
    'Bronze',
    2.00,
    3,
    TRUE
FROM discount_types dt WHERE dt.code = 'FIDELITY'
ON CONFLICT DO NOTHING;

-- ==========================================
-- 4. RULE ATTRIBUTES (METADATA CATALOG)
-- ==========================================

-- SEASONAL attributes
INSERT INTO rule_attributes (id, discount_type_id, attribute_name, attribute_type, is_required, description) 
SELECT 
    '550e8400-e29b-41d4-a716-446655440300'::UUID,
    dt.id,
    'start_date',
    'DATE',
    TRUE,
    'Fecha de inicio de la promoción'
FROM discount_types dt WHERE dt.code = 'SEASONAL'
ON CONFLICT DO NOTHING;

INSERT INTO rule_attributes (id, discount_type_id, attribute_name, attribute_type, is_required, description) 
SELECT 
    '550e8400-e29b-41d4-a716-446655440301'::UUID,
    dt.id,
    'end_date',
    'DATE',
    TRUE,
    'Fecha de fin de la promoción'
FROM discount_types dt WHERE dt.code = 'SEASONAL'
ON CONFLICT DO NOTHING;

INSERT INTO rule_attributes (id, discount_type_id, attribute_name, attribute_type, is_required, description) 
SELECT 
    '550e8400-e29b-41d4-a716-446655440302'::UUID,
    dt.id,
    'region',
    'VARCHAR',
    FALSE,
    'Región geográfica (EUR, US, LATAM, etc)'
FROM discount_types dt WHERE dt.code = 'SEASONAL'
ON CONFLICT DO NOTHING;

INSERT INTO rule_attributes (id, discount_type_id, attribute_name, attribute_type, is_required, description) 
SELECT 
    '550e8400-e29b-41d4-a716-446655440303'::UUID,
    dt.id,
    'season_name',
    'VARCHAR',
    FALSE,
    'Nombre de la temporada'
FROM discount_types dt WHERE dt.code = 'SEASONAL'
ON CONFLICT DO NOTHING;

INSERT INTO rule_attributes (id, discount_type_id, attribute_name, attribute_type, is_required, description) 
SELECT 
    '550e8400-e29b-41d4-a716-446655440304'::UUID,
    dt.id,
    'max_budget',
    'NUMERIC',
    FALSE,
    'Presupuesto máximo para esta temporada'
FROM discount_types dt WHERE dt.code = 'SEASONAL'
ON CONFLICT DO NOTHING;

INSERT INTO rule_attributes (id, discount_type_id, attribute_name, attribute_type, is_required, description) 
SELECT 
    '550e8400-e29b-41d4-a716-446655440305'::UUID,
    dt.id,
    'target_customer_tier',
    'VARCHAR',
    FALSE,
    'Tier de cliente objetivo'
FROM discount_types dt WHERE dt.code = 'SEASONAL'
ON CONFLICT DO NOTHING;

-- PRODUCT attributes
INSERT INTO rule_attributes (id, discount_type_id, attribute_name, attribute_type, is_required, description) 
SELECT 
    '550e8400-e29b-41d4-a716-446655440310'::UUID,
    dt.id,
    'product_type',
    'VARCHAR',
    TRUE,
    'Tipo o categoría del producto'
FROM discount_types dt WHERE dt.code = 'PRODUCT'
ON CONFLICT DO NOTHING;

INSERT INTO rule_attributes (id, discount_type_id, attribute_name, attribute_type, is_required, description) 
SELECT 
    '550e8400-e29b-41d4-a716-446655440311'::UUID,
    dt.id,
    'min_purchase',
    'NUMERIC',
    FALSE,
    'Compra mínima requerida'
FROM discount_types dt WHERE dt.code = 'PRODUCT'
ON CONFLICT DO NOTHING;

INSERT INTO rule_attributes (id, discount_type_id, attribute_name, attribute_type, is_required, description) 
SELECT 
    '550e8400-e29b-41d4-a716-446655440312'::UUID,
    dt.id,
    'bundle_code',
    'VARCHAR',
    FALSE,
    'Código de bundle'
FROM discount_types dt WHERE dt.code = 'PRODUCT'
ON CONFLICT DO NOTHING;

INSERT INTO rule_attributes (id, discount_type_id, attribute_name, attribute_type, is_required, description) 
SELECT 
    '550e8400-e29b-41d4-a716-446655440313'::UUID,
    dt.id,
    'category_level',
    'VARCHAR',
    FALSE,
    'Nivel de categoría (main, sub, detail)'
FROM discount_types dt WHERE dt.code = 'PRODUCT'
ON CONFLICT DO NOTHING;

INSERT INTO rule_attributes (id, discount_type_id, attribute_name, attribute_type, is_required, description) 
SELECT 
    '550e8400-e29b-41d4-a716-446655440314'::UUID,
    dt.id,
    'excluded_products',
    'VARCHAR',
    FALSE,
    'Productos excluidos (lista JSON como string)'
FROM discount_types dt WHERE dt.code = 'PRODUCT'
ON CONFLICT DO NOTHING;

-- CLASSIFICATION attributes
INSERT INTO rule_attributes (id, discount_type_id, attribute_name, attribute_type, is_required, description) 
SELECT 
    '550e8400-e29b-41d4-a716-446655440320'::UUID,
    dt.id,
    'metric_type',
    'VARCHAR',
    TRUE,
    'Tipo de métrica (total_spent, order_count, loyalty_points)'
FROM discount_types dt WHERE dt.code = 'CLASSIFICATION'
ON CONFLICT DO NOTHING;

INSERT INTO rule_attributes (id, discount_type_id, attribute_name, attribute_type, is_required, description) 
SELECT 
    '550e8400-e29b-41d4-a716-446655440321'::UUID,
    dt.id,
    'min_value',
    'NUMERIC',
    TRUE,
    'Valor mínimo del rango'
FROM discount_types dt WHERE dt.code = 'CLASSIFICATION'
ON CONFLICT DO NOTHING;

INSERT INTO rule_attributes (id, discount_type_id, attribute_name, attribute_type, is_required, description) 
SELECT 
    '550e8400-e29b-41d4-a716-446655440322'::UUID,
    dt.id,
    'max_value',
    'NUMERIC',
    FALSE,
    'Valor máximo del rango'
FROM discount_types dt WHERE dt.code = 'CLASSIFICATION'
ON CONFLICT DO NOTHING;

INSERT INTO rule_attributes (id, discount_type_id, attribute_name, attribute_type, is_required, description) 
SELECT 
    '550e8400-e29b-41d4-a716-446655440323'::UUID,
    dt.id,
    'customer_tier_id',
    'VARCHAR',
    TRUE,
    'UUID del tier de cliente'
FROM discount_types dt WHERE dt.code = 'CLASSIFICATION'
ON CONFLICT DO NOTHING;

INSERT INTO rule_attributes (id, discount_type_id, attribute_name, attribute_type, is_required, description) 
SELECT 
    '550e8400-e29b-41d4-a716-446655440324'::UUID,
    dt.id,
    'hierarchy_level',
    'NUMERIC',
    FALSE,
    'Nivel en la jerarquía'
FROM discount_types dt WHERE dt.code = 'CLASSIFICATION'
ON CONFLICT DO NOTHING;

INSERT INTO rule_attributes (id, discount_type_id, attribute_name, attribute_type, is_required, description) 
SELECT 
    '550e8400-e29b-41d4-a716-446655440325'::UUID,
    dt.id,
    'priority',
    'NUMERIC',
    FALSE,
    'Prioridad en caso de solapamiento'
FROM discount_types dt WHERE dt.code = 'CLASSIFICATION'
ON CONFLICT DO NOTHING;

-- FIDELITY attributes
INSERT INTO rule_attributes (id, discount_type_id, attribute_name, attribute_type, is_required, description) 
SELECT 
    '550e8400-e29b-41d4-a716-446655440330'::UUID,
    dt.id,
    'loyalty_points_required',
    'NUMERIC',
    TRUE,
    'Puntos de lealtad requeridos'
FROM discount_types dt WHERE dt.code = 'FIDELITY'
ON CONFLICT DO NOTHING;

INSERT INTO rule_attributes (id, discount_type_id, attribute_name, attribute_type, is_required, description) 
SELECT 
    '550e8400-e29b-41d4-a716-446655440331'::UUID,
    dt.id,
    'points_multiplier',
    'NUMERIC',
    FALSE,
    'Multiplicador de puntos ganados'
FROM discount_types dt WHERE dt.code = 'FIDELITY'
ON CONFLICT DO NOTHING;
