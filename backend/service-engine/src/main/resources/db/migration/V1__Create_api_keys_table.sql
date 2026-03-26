-- Flyway Migration: Create api_keys table for Engine Service (synchronization table)
-- Version: 1.0
-- Description: Tabla de sincronización para API Keys desde Admin Service

CREATE TABLE IF NOT EXISTS api_keys (
    id UUID PRIMARY KEY,
    key_string VARCHAR(36) NOT NULL UNIQUE,
    ecommerce_id UUID NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

-- Índices para búsquedas frecuentes
CREATE UNIQUE INDEX IF NOT EXISTS idx_key_string ON api_keys(key_string);
CREATE INDEX IF NOT EXISTS idx_ecommerce_id ON api_keys(ecommerce_id);

COMMENT ON TABLE api_keys IS 'Tabla de sincronización de API Keys desde Admin Service';
COMMENT ON COLUMN api_keys.id IS 'Identificador único de la API Key';
COMMENT ON COLUMN api_keys.key_string IS 'La clave completa en formato UUID v4 (solo lectura, poblada vía eventos)';
COMMENT ON COLUMN api_keys.ecommerce_id IS 'ID del ecommerce propietario';
COMMENT ON COLUMN api_keys.created_at IS 'Timestamp de creación (UTC)';
COMMENT ON COLUMN api_keys.updated_at IS 'Timestamp de última actualización (UTC)';
