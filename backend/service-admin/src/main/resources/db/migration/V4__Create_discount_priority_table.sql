-- Flyway Migration: Create discount_priority table
-- Version: 4.0
-- Description: Tabla para almacenar la prioridad de aplicación de descuentos

CREATE TABLE IF NOT EXISTS discount_priority (
    id UUID PRIMARY KEY,
    discount_config_id UUID NOT NULL REFERENCES discount_config(id) ON DELETE CASCADE,
    discount_type VARCHAR(50) NOT NULL,
    priority_level INTEGER NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT uk_config_discount_type UNIQUE (discount_config_id, discount_type),
    CONSTRAINT uk_config_priority_level UNIQUE (discount_config_id, priority_level)
);

-- Índices para búsquedas frecuentes
CREATE INDEX IF NOT EXISTS idx_config_id ON discount_priority(discount_config_id);
CREATE INDEX IF NOT EXISTS idx_discount_type ON discount_priority(discount_type);

-- Comentarios de tabla y columnas
COMMENT ON TABLE discount_priority IS 'Configuración de prioridad de aplicación de descuentos';
COMMENT ON COLUMN discount_priority.id IS 'Identificador único (UUID)';
COMMENT ON COLUMN discount_priority.discount_config_id IS 'FK a discount_config.id';
COMMENT ON COLUMN discount_priority.discount_type IS 'Tipo de descuento (ej. LOYALTY_POINTS, COUPON, BIRTHDAY, SEASONAL)';
COMMENT ON COLUMN discount_priority.priority_level IS 'Nivel de prioridad (1 = máxima prioridad, secuencial per config)';
COMMENT ON COLUMN discount_priority.created_at IS 'Timestamp de creación (UTC)';
COMMENT ON CONSTRAINT uk_config_discount_type ON discount_priority IS 'Cada tipo de descuento es único por configuración';
COMMENT ON CONSTRAINT uk_config_priority_level ON discount_priority IS 'Cada nivel de prioridad es único por configuración';
