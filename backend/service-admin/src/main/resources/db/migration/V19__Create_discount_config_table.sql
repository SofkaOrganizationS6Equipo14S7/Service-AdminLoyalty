-- V16__Create_discount_config_table.sql
-- Tabla maestra de configuración de límite de descuentos para HU-09

CREATE TABLE IF NOT EXISTS discount_config (
    uid UUID PRIMARY KEY,
    ecommerce_id UUID NOT NULL REFERENCES ecommerces(uid) ON DELETE CASCADE,
    max_discount_limit DECIMAL(10, 2) NOT NULL CHECK (max_discount_limit > 0),
    currency_code VARCHAR(3) NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Índice para búsqueda rápida por ecommerce
CREATE INDEX IF NOT EXISTS idx_discount_config_ecommerce_id
    ON discount_config(ecommerce_id);

-- UK Parcial: solo una configuración activa por ecommerce
CREATE UNIQUE INDEX IF NOT EXISTS uq_discount_config_ecommerce_active
    ON discount_config(ecommerce_id, is_active) WHERE is_active = true;

-- Comentarios
COMMENT ON TABLE discount_config IS 'Configuración de límite máximo de descuentos por ecommerce. Source of truth para HU-09.';
COMMENT ON COLUMN discount_config.max_discount_limit IS 'Tope máximo de descuento acumulado. Debe ser > 0.';
COMMENT ON COLUMN discount_config.is_active IS 'Solo una configuración activa por ecommerce.';
