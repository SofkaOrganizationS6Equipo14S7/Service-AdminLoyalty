-- V4__Add_seasonal_rule_attributes.sql
-- Purpose: Add SEASONAL rule attributes metadata to enable date overlap validation
-- HU-06: Gestión de Reglas de Temporada - Validación de Superposición de Fechas
-- Created: 2026-04-07

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

-- Create index for faster attribute lookups
CREATE INDEX IF NOT EXISTS idx_rule_attributes_name_key 
ON rule_attributes(attribute_name, discount_type_id);
