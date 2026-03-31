-- V17__Create_discount_limit_priority_table.sql
-- Tabla de prioridades de descuentos para HU-09

CREATE TABLE IF NOT EXISTS discount_limit_priority (
    uid UUID PRIMARY KEY,
    discount_config_id UUID NOT NULL REFERENCES discount_config(uid) ON DELETE CASCADE,
    discount_type VARCHAR(50) NOT NULL,
    priority_level INTEGER NOT NULL CHECK (priority_level > 0),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Índice para búsqueda rápida por configuración
CREATE INDEX IF NOT EXISTS idx_discount_limit_priority_config_id
    ON discount_limit_priority(discount_config_id);

-- UK: tipo de descuento único por configuración
CREATE UNIQUE INDEX IF NOT EXISTS idx_discount_limit_priority_config_type
    ON discount_limit_priority(discount_config_id, discount_type);

-- UK: nivel de prioridad único por configuración
CREATE UNIQUE INDEX IF NOT EXISTS idx_discount_limit_priority_config_level
    ON discount_limit_priority(discount_config_id, priority_level);

-- Comentarios
COMMENT ON TABLE discount_limit_priority IS 'Prioridades de tipos de descuento para una configuración. Source of truth para HU-09.';
COMMENT ON COLUMN discount_limit_priority.priority_level IS 'Niveles deben ser secuenciales: 1, 2, 3, ..., N sin duplicados ni huecos.';
