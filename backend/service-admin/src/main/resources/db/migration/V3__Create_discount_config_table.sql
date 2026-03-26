-- Flyway Migration: Create discount_config table
-- Version: 3.0
-- Description: Tabla para almacenar la configuración del tope máximo de descuentos

CREATE TABLE IF NOT EXISTS discount_config (
    id UUID PRIMARY KEY,
    max_discount_limit NUMERIC(10, 2) NOT NULL,
    currency_code VARCHAR(3) NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    created_by_user_id UUID NOT NULL
);

-- Índices para búsquedas frecuentes
CREATE INDEX IF NOT EXISTS idx_is_active ON discount_config(is_active);

-- Comentarios de tabla y columnas
COMMENT ON TABLE discount_config IS 'Configuración del tope máximo de descuentos';
COMMENT ON COLUMN discount_config.id IS 'Identificador único (UUID)';
COMMENT ON COLUMN discount_config.max_discount_limit IS 'Límite máximo de descuentos en moneda base (NUMERIC 10,2)';
COMMENT ON COLUMN discount_config.currency_code IS 'Código de moneda ISO 4217 (ej. USD)';
COMMENT ON COLUMN discount_config.is_active IS 'Flag indicando si esta configuración está vigente';
COMMENT ON COLUMN discount_config.created_at IS 'Timestamp de creación (UTC)';
COMMENT ON COLUMN discount_config.updated_at IS 'Timestamp de última actualización (UTC)';
COMMENT ON COLUMN discount_config.created_by_user_id IS 'ID del usuario que creó esta configuración';
