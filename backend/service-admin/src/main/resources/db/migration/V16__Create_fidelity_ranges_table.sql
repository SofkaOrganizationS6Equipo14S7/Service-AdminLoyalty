-- V14__Create_fidelity_ranges_table.sql
-- Create the fidelity_ranges table for managing fidelity level classifications
-- Source of Truth: Admin Service manages this table
-- Replica: Engine Service synchronizes via RabbitMQ

CREATE TABLE IF NOT EXISTS fidelity_ranges (
    uid UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    ecommerce_id UUID NOT NULL REFERENCES ecommerces(uid) ON DELETE CASCADE,
    name VARCHAR(255) NOT NULL,
    min_points INTEGER NOT NULL CHECK (min_points >= 0),
    max_points INTEGER NOT NULL CHECK (max_points > min_points),
    discount_percentage NUMERIC(5,2) NOT NULL CHECK (discount_percentage BETWEEN 0 AND 100),
    is_active BOOLEAN DEFAULT true NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL
);

-- Indices for frequent queries
CREATE INDEX idx_fidelity_ranges_ecommerce_id ON fidelity_ranges(ecommerce_id);
CREATE INDEX idx_fidelity_ranges_active ON fidelity_ranges(is_active);
CREATE INDEX idx_fidelity_ranges_ecommerce_active ON fidelity_ranges(ecommerce_id, is_active);
CREATE INDEX idx_fidelity_ranges_min_points ON fidelity_ranges(min_points);

-- Unique partial index: Only one ACTIVE range with the same name per ecommerce
-- Allows historical soft-deleted ranges with the same name
CREATE UNIQUE INDEX uq_active_fidelity_name 
  ON fidelity_ranges (ecommerce_id, name) 
  WHERE (is_active IS TRUE);

-- Comments for documentation
COMMENT ON TABLE fidelity_ranges IS 'Source of Truth: Rangos de clasificación de fidelidad (Admin Service). Réplica en Engine vía RabbitMQ.';
COMMENT ON COLUMN fidelity_ranges.uid IS 'Identificador único generado por el sistema';
COMMENT ON COLUMN fidelity_ranges.ecommerce_id IS 'Tenant identifier - isolates data per ecommerce';
COMMENT ON COLUMN fidelity_ranges.name IS 'Nombre del nivel de fidelidad (Bronce, Plata, Oro, Platino)';
COMMENT ON COLUMN fidelity_ranges.min_points IS 'Mínimo de puntos (incluido) para acceder a este nivel';
COMMENT ON COLUMN fidelity_ranges.max_points IS 'Máximo de puntos (incluido) para este nivel. Se permiten huecos entre rangos.';
COMMENT ON COLUMN fidelity_ranges.discount_percentage IS 'Descuento aplicable a clientes en este nivel [0-100]';
COMMENT ON COLUMN fidelity_ranges.is_active IS 'Soft delete flag - false indica eliminado lógico';
COMMENT ON COLUMN fidelity_ranges.created_at IS 'Timestamp de creación en UTC';
COMMENT ON COLUMN fidelity_ranges.updated_at IS 'Timestamp de última actualización en UTC';
