-- Flyway Migration: Create ecommerces table for multi-tenant support
-- Version: 6.0
-- Description: Tabla para almacenar ecommerces (tenants) de la plataforma LOYALTY
-- SPEC-001: Registro y Gestión de Ecommerces (Onboarding)

CREATE TABLE IF NOT EXISTS ecommerces (
    uid UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(255) NOT NULL,
    slug VARCHAR(255) UNIQUE NOT NULL,
    status VARCHAR(10) NOT NULL CHECK (status IN ('ACTIVE', 'INACTIVE')) DEFAULT 'ACTIVE',
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    -- Constraint para validar formato slug: solo letras minúsculas, números y guiones
    -- Formato: ^[a-z0-9]([a-z0-9-]{0,252}[a-z0-9])?$
    CONSTRAINT slug_format CHECK (slug ~ '^[a-z0-9]([a-z0-9-]{0,252}[a-z0-9])?$'),
    
    -- Constraint para unicidad de name (comentado porque queremos permitir names duplicados pero slug único)
    -- CONSTRAINT unique_name UNIQUE (name)
    
    CONSTRAINT ecommerce_name_not_empty CHECK (name != ''),
    CONSTRAINT ecommerce_slug_not_empty CHECK (slug != '')
);

-- ÍNDICES CRÍTICOS PARA PERFORMANCE Y USABILIDAD
-- idx_suffix convención: idx_<table>_<columns>_<type>
CREATE UNIQUE INDEX IF NOT EXISTS idx_ecommerces_slug_unique ON ecommerces(slug);
CREATE INDEX IF NOT EXISTS idx_ecommerces_status ON ecommerces(status);
CREATE INDEX IF NOT EXISTS idx_ecommerces_created_at ON ecommerces(created_at);
CREATE INDEX IF NOT EXISTS idx_ecommerces_name ON ecommerces(name);

-- FOREIGN KEY CONSTRAINTS: asegurar integridad referencial
-- En tablas users y api_keys que ya existen
ALTER TABLE users 
    ADD CONSTRAINT fk_users_ecommerce_uid 
    FOREIGN KEY (ecommerce_id) REFERENCES ecommerces(uid) ON DELETE RESTRICT;

ALTER TABLE api_keys
    ADD CONSTRAINT fk_api_keys_ecommerce_uid
    FOREIGN KEY (ecommerce_id) REFERENCES ecommerces(uid) ON DELETE RESTRICT;

-- COMENTARIOS DE DOCUMENTACIÓN
COMMENT ON TABLE ecommerces IS 'Tabla de ecommerces (tenants). Cada fila representa un cliente/tienda que opera en la plataforma LOYALTY.';
COMMENT ON COLUMN ecommerces.uid IS 'Identificador único UUID generado por el sistema. Referenciado por users.ecommerce_id y api_keys.ecommerce_id';
COMMENT ON COLUMN ecommerces.name IS 'Nombre del ecommerce (ej. "Tienda Nike"). Máximo 255 caracteres.';
COMMENT ON COLUMN ecommerces.slug IS 'Identificador amigable único (ej. "nike-store"). Usado en URLs y como human-readable id. Sólo letras minúsculas, números y guiones.';
COMMENT ON COLUMN ecommerces.status IS 'Estado del ecommerce: ACTIVE (operando) o INACTIVE (pausado/desactivado). Soft delete vía INACTIVE, no DELETE físico.';
COMMENT ON COLUMN ecommerces.created_at IS 'Timestamp de creación en UTC. Auto-poblado, inmutable.';
COMMENT ON COLUMN ecommerces.updated_at IS 'Timestamp de última actualización en UTC. Auto-poblado, actualizado en cada cambio.';
