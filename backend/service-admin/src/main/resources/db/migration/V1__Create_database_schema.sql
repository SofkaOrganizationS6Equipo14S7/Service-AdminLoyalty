-- V2__Create_database_schema.sql
-- Migration completa del modelo LOYALTY según spec database-model.spec.md
-- Incluye todas las 17 tablas, constraints, índices e integridad referencial

-- ==========================================
-- 0. EXTENSIONES REQUERIDAS
-- ==========================================
CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- ==========================================
-- 1. IDENTIDAD Y TENANT
-- ==========================================

-- Tabla: ecommerce (tenant raíz)
CREATE TABLE IF NOT EXISTS ecommerce (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(255) NOT NULL,
    slug VARCHAR(255) UNIQUE NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE' CHECK (status IN ('ACTIVE', 'INACTIVE')),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_slug_format CHECK (slug ~ '^[a-z0-9]([a-z0-9-]{0,252}[a-z0-9])?$')
);

-- Tabla: roles
CREATE TABLE IF NOT EXISTS roles (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(50) UNIQUE NOT NULL CHECK (name IN ('SUPER_ADMIN', 'STORE_ADMIN', 'STORE_USER')),
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Tabla: permissions
CREATE TABLE IF NOT EXISTS permissions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    description VARCHAR(255),
    code VARCHAR(50) UNIQUE NOT NULL,
    module VARCHAR(50) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Tabla: role_permissions (many-to-many)
CREATE TABLE IF NOT EXISTS role_permissions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    role_id UUID NOT NULL REFERENCES roles(id) ON DELETE CASCADE,
    permission_id UUID NOT NULL REFERENCES permissions(id) ON DELETE CASCADE
);

-- Tabla: app_user
CREATE TABLE IF NOT EXISTS app_user (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    ecommerce_id UUID REFERENCES ecommerce(id) ON DELETE RESTRICT,
    role_id UUID NOT NULL REFERENCES roles(id) ON DELETE RESTRICT,
    username VARCHAR(100) UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    email VARCHAR(255),
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    last_login TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- ==========================================
-- 2. CONECTIVIDAD S2S
-- ==========================================

-- Tabla: api_key
CREATE TABLE IF NOT EXISTS api_key (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    ecommerce_id UUID NOT NULL REFERENCES ecommerce(id) ON DELETE CASCADE,
    hashed_key VARCHAR(255) UNIQUE NOT NULL,
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL CHECK (expires_at > CURRENT_TIMESTAMP),
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- ==========================================
-- 3. ESTRATEGIA GLOBAL
-- ==========================================

-- Tabla: discount_types (catálogo predefinido)
CREATE TABLE IF NOT EXISTS discount_types (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    code VARCHAR(50) UNIQUE NOT NULL,
    display_name VARCHAR(150)
);

-- Tabla: discount_settings
CREATE TABLE IF NOT EXISTS discount_settings (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    ecommerce_id UUID NOT NULL REFERENCES ecommerce(id) ON DELETE CASCADE,
    max_discount_cap DECIMAL(12,4) NOT NULL CHECK (max_discount_cap > 0),
    currency_code VARCHAR(3) NOT NULL DEFAULT 'USD',
    allow_stacking BOOLEAN DEFAULT TRUE,
    rounding_rule VARCHAR(20) NOT NULL DEFAULT 'ROUND_HALF_UP',
    -- todo revisar si elimina los siguientes tres campos
    cap_type VARCHAR(20),
    cap_value DECIMAL(12,4),
    cap_applies_to VARCHAR(20),
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    version BIGINT NOT NULL DEFAULT 1,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Tabla: discount_priorities
CREATE TABLE IF NOT EXISTS discount_priorities (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    discount_setting_id UUID NOT NULL REFERENCES discount_settings(id) ON DELETE CASCADE,
    discount_type_id UUID NOT NULL REFERENCES discount_types(id),
    priority_level INTEGER NOT NULL CHECK (priority_level > 0),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(discount_setting_id, discount_type_id),
    UNIQUE(discount_setting_id, priority_level)
);

-- ==========================================
-- 4. REGLAS (ENGINE)
-- ==========================================

-- Tabla: customer_tiers
CREATE TABLE IF NOT EXISTS customer_tiers (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    ecommerce_id UUID NOT NULL REFERENCES ecommerce(id) ON DELETE CASCADE,
    discount_type_id UUID NOT NULL REFERENCES discount_types(id),
    name VARCHAR(100) NOT NULL,
    discount_percentage DECIMAL(5,2) NOT NULL,
    hierarchy_level INTEGER NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(ecommerce_id, name)
);

-- Tabla: classification_rule
CREATE TABLE IF NOT EXISTS classification_rule (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    customer_tier_id UUID NOT NULL REFERENCES customer_tiers(id) ON DELETE CASCADE,
    metric_type VARCHAR(50) NOT NULL CHECK (metric_type IN ('total_spent', 'order_count', 'loyalty_points', 'custom')),
    min_value NUMERIC(19,2) NOT NULL DEFAULT 0,
    max_value NUMERIC(19,2),
    priority INTEGER NOT NULL DEFAULT 1,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Tabla: seasonal_rules
CREATE TABLE IF NOT EXISTS seasonal_rules (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    ecommerce_id UUID NOT NULL REFERENCES ecommerce(id) ON DELETE CASCADE,
    discount_type_id UUID NOT NULL REFERENCES discount_types(id),
    name VARCHAR(255) NOT NULL,
    description VARCHAR(1000),
    discount_percentage NUMERIC(5,2) NOT NULL CHECK (discount_percentage >= 0 AND discount_percentage <= 100),
    start_date TIMESTAMP WITH TIME ZONE NOT NULL,
    end_date TIMESTAMP WITH TIME ZONE NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT valid_date_range CHECK (start_date < end_date)
);

-- Tabla: product_rules
CREATE TABLE IF NOT EXISTS product_rules (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    ecommerce_id UUID NOT NULL REFERENCES ecommerce(id) ON DELETE CASCADE,
    discount_type_id UUID NOT NULL REFERENCES discount_types(id),
    name VARCHAR(255) NOT NULL,
    product_type VARCHAR(100) NOT NULL,
    discount_percentage NUMERIC(5,2) NOT NULL CHECK (discount_percentage >= 0 AND discount_percentage <= 100),
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(ecommerce_id, product_type, is_active)
);

-- ==========================================
-- 5. OBSERVABILIDAD
-- ==========================================

-- Tabla: discount_application_log
CREATE TABLE IF NOT EXISTS discount_application_log (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    ecommerce_id UUID NOT NULL REFERENCES ecommerce(id) ON DELETE CASCADE,
    external_order_id VARCHAR(255),
    original_amount DECIMAL(12,4) NOT NULL,
    discount_applied DECIMAL(12,2) NOT NULL,
    final_amount DECIMAL(12,2) NOT NULL,
    applied_rules_details JSONB,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Tabla: audit_log
CREATE TABLE IF NOT EXISTS audit_log (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID REFERENCES app_user(id) ON DELETE SET NULL,
    ecommerce_id UUID REFERENCES ecommerce(id),
    action VARCHAR(50) NOT NULL,
    entity_name VARCHAR(100) NOT NULL,
    entity_id UUID,
    old_value JSONB,
    new_value JSONB,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- ==========================================
-- ÍNDICES OPTIMIZADOS
-- ==========================================

-- ecommerce
CREATE INDEX IF NOT EXISTS idx_ecommerce_slug ON ecommerce(slug);
CREATE INDEX IF NOT EXISTS idx_ecommerce_status ON ecommerce(status);

-- app_user
CREATE INDEX IF NOT EXISTS idx_user_ecommerce ON app_user(ecommerce_id);
CREATE INDEX IF NOT EXISTS idx_user_role ON app_user(role_id);
CREATE INDEX IF NOT EXISTS idx_user_active ON app_user(is_active);

-- api_key
CREATE INDEX IF NOT EXISTS idx_api_key_ecommerce ON api_key(ecommerce_id);

-- seasonal_rules
CREATE INDEX IF NOT EXISTS idx_seasonal_rules_date ON seasonal_rules(start_date, end_date);
CREATE INDEX IF NOT EXISTS idx_seasonal_rules_ecommerce ON seasonal_rules(ecommerce_id);
CREATE INDEX IF NOT EXISTS idx_seasonal_rules_active ON seasonal_rules(is_active);

-- product_rules
CREATE INDEX IF NOT EXISTS idx_product_rules_ecommerce ON product_rules(ecommerce_id);
CREATE INDEX IF NOT EXISTS idx_product_rules_type ON product_rules(product_type);
CREATE INDEX IF NOT EXISTS idx_product_rules_active ON product_rules(is_active);

-- roles
CREATE INDEX IF NOT EXISTS idx_role_name ON roles(name);

-- permissions
CREATE INDEX IF NOT EXISTS idx_permission_code ON permissions(code);
CREATE INDEX IF NOT EXISTS idx_permission_module ON permissions(module);

-- role_permissions
CREATE INDEX IF NOT EXISTS idx_role_permissions_role ON role_permissions(role_id);
CREATE INDEX IF NOT EXISTS idx_role_permissions_permission ON role_permissions(permission_id);

-- classification_rule
CREATE INDEX IF NOT EXISTS idx_classification_rule_tier ON classification_rule(customer_tier_id);
CREATE UNIQUE INDEX IF NOT EXISTS idx_classification_rule_active ON classification_rule(customer_tier_id, priority) WHERE is_active = TRUE;

-- discount_types
CREATE INDEX IF NOT EXISTS idx_discount_type_code ON discount_types(code);

-- discount_log
CREATE INDEX IF NOT EXISTS idx_discount_log_ecommerce ON discount_application_log(ecommerce_id);

-- audit_log
CREATE INDEX IF NOT EXISTS idx_audit_log_ecommerce ON audit_log(ecommerce_id);
CREATE INDEX IF NOT EXISTS idx_audit_log_user ON audit_log(user_id);
CREATE INDEX IF NOT EXISTS idx_audit_log_action ON audit_log(action);
CREATE INDEX IF NOT EXISTS idx_audit_log_entity_name ON audit_log(entity_name);
CREATE INDEX IF NOT EXISTS idx_audit_log_created_at ON audit_log(created_at DESC);

-- ==========================================
-- SEEDERS (DATOS BASE)
-- ==========================================

-- Insertar roles predefinidos
INSERT INTO roles (name, is_active) VALUES
    ('SUPER_ADMIN', TRUE),
    ('STORE_ADMIN', TRUE),
    ('STORE_USER', TRUE)
ON CONFLICT DO NOTHING;

-- Insertar tipos de descuento predefinidos
INSERT INTO discount_types (code, display_name) VALUES
    ('FIDELITY', 'Fidelidad'),
    ('SEASONAL', 'Estacional'),
    ('PRODUCT', 'Producto')
ON CONFLICT DO NOTHING;
