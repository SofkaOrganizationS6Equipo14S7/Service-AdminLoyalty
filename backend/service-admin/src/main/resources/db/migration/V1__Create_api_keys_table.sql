-- Flyway Migration: Create api_keys table for Admin Service
-- Version: 1.0
-- Description: Tabla para almacenar API Keys hasheadas (SHA-256) de cada ecommerce

CREATE TABLE IF NOT EXISTS api_keys (
    id UUID PRIMARY KEY,
    hashed_key VARCHAR(64) NOT NULL UNIQUE,
    ecommerce_id UUID NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

-- Índices para búsquedas frecuentes
CREATE UNIQUE INDEX IF NOT EXISTS idx_hashed_key ON api_keys(hashed_key);
CREATE INDEX IF NOT EXISTS idx_ecommerce_id ON api_keys(ecommerce_id);

COMMENT ON TABLE api_keys IS 'API Keys (hasheadas con SHA-256) para autenticación de ecommerces';
COMMENT ON COLUMN api_keys.id IS 'Identificador único de la API Key';
COMMENT ON COLUMN api_keys.hashed_key IS 'Hash SHA-256 de la API Key (nunca almacenamos el valor plano)';
COMMENT ON COLUMN api_keys.ecommerce_id IS 'ID del ecommerce propietario';
COMMENT ON COLUMN api_keys.created_at IS 'Timestamp de creación (UTC)';
COMMENT ON COLUMN api_keys.updated_at IS 'Timestamp de última actualización (UTC)';
