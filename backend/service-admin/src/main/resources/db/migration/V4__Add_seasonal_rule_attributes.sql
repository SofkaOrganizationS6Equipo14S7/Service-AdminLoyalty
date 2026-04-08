-- V4__Add_seasonal_rule_attributes.sql
-- Purpose: Add SEASONAL and CLASSIFICATION rule attributes metadata
-- HU-06: Gestión de Reglas de Temporada - Validación de Superposición de Fechas
-- HU-07: Gestión de Reglas de Clasificación - Atributos dinámicos
-- Created: 2026-04-07
-- 
-- NOTE: All attributes use camelCase for API consistency
-- This aligns with DTOs (ClassificationRuleCreateRequest, etc.)
-- V2 seed also updated to use camelCase exclusively
-- Example: metricType, minValue, maxValue (not metric_type, min_value, max_value)

-- ============================================================
-- SEASONAL Rule Attributes
-- ============================================================

-- Ensure rule_attributes for SEASONAL discount type exists
INSERT INTO rule_attributes (discount_type_id, attribute_name, attribute_type, is_required, description)
SELECT 
    dt.id,
    'start_date',
    'DATE',
    TRUE,
    'Season rule start date (YYYY-MM-DD format)'
FROM discount_types dt 
WHERE dt.code = 'SEASONAL'
    AND NOT EXISTS (
        SELECT 1 FROM rule_attributes ra 
        WHERE ra.discount_type_id = dt.id 
        AND ra.attribute_name = 'start_date'
    )
ON CONFLICT DO NOTHING;

INSERT INTO rule_attributes (discount_type_id, attribute_name, attribute_type, is_required, description)
SELECT 
    dt.id,
    'end_date',
    'DATE',
    TRUE,
    'Season rule end date (YYYY-MM-DD format)'
FROM discount_types dt 
WHERE dt.code = 'SEASONAL'
    AND NOT EXISTS (
        SELECT 1 FROM rule_attributes ra 
        WHERE ra.discount_type_id = dt.id 
        AND ra.attribute_name = 'end_date'
    )
ON CONFLICT DO NOTHING;

-- ============================================================
-- CLASSIFICATION Rule Attributes
-- ============================================================

-- metricType: Classification metric (total_spent, order_count, loyalty_points, custom)
INSERT INTO rule_attributes (discount_type_id, attribute_name, attribute_type, is_required, description)
SELECT 
    dt.id,
    'metricType',
    'VARCHAR',
    TRUE,
    'Classification metric type: total_spent, order_count, loyalty_points, or custom'
FROM discount_types dt 
WHERE dt.code = 'CLASSIFICATION'
    AND NOT EXISTS (
        SELECT 1 FROM rule_attributes ra 
        WHERE ra.discount_type_id = dt.id 
        AND ra.attribute_name = 'metricType'
    )
ON CONFLICT DO NOTHING;

-- minValue: Minimum value for classification range
INSERT INTO rule_attributes (discount_type_id, attribute_name, attribute_type, is_required, description)
SELECT 
    dt.id,
    'minValue',
    'NUMERIC',
    TRUE,
    'Minimum value for classification range'
FROM discount_types dt 
WHERE dt.code = 'CLASSIFICATION'
    AND NOT EXISTS (
        SELECT 1 FROM rule_attributes ra 
        WHERE ra.discount_type_id = dt.id 
        AND ra.attribute_name = 'minValue'
    )
ON CONFLICT DO NOTHING;

-- maxValue: Maximum value for classification range
INSERT INTO rule_attributes (discount_type_id, attribute_name, attribute_type, is_required, description)
SELECT 
    dt.id,
    'maxValue',
    'NUMERIC',
    TRUE,
    'Maximum value for classification range'
FROM discount_types dt 
WHERE dt.code = 'CLASSIFICATION'
    AND NOT EXISTS (
        SELECT 1 FROM rule_attributes ra 
        WHERE ra.discount_type_id = dt.id 
        AND ra.attribute_name = 'maxValue'
    )
ON CONFLICT DO NOTHING;

-- priority: Evaluation priority within tier
INSERT INTO rule_attributes (discount_type_id, attribute_name, attribute_type, is_required, description)
SELECT 
    dt.id,
    'priority',
    'NUMERIC',
    TRUE,
    'Evaluation priority (lower = higher priority)'
FROM discount_types dt 
WHERE dt.code = 'CLASSIFICATION'
    AND NOT EXISTS (
        SELECT 1 FROM rule_attributes ra 
        WHERE ra.discount_type_id = dt.id 
        AND ra.attribute_name = 'priority'
    )
ON CONFLICT DO NOTHING;

-- Create index for faster attribute lookups
CREATE INDEX IF NOT EXISTS idx_rule_attributes_name_key 
ON rule_attributes(attribute_name, discount_type_id);
