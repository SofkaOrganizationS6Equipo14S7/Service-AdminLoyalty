-- V1__Create_database_schema.sql
-- Complete LOYALTY system database schema v2.0
-- Metadata-driven rules architecture with dynamic attributes
-- ============================================================================

-- ==========================================
-- EXTENSIONS
-- ==========================================
CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- ==========================================
-- 1. IDENTITY & TENANT
-- ==========================================

CREATE TABLE IF NOT EXISTS ecommerce (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(255) NOT NULL,
    slug VARCHAR(255) UNIQUE NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE' CHECK (status IN ('ACTIVE', 'INACTIVE')),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_slug_format CHECK (slug ~ '^[a-z0-9]([a-z0-9-]{0,252}[a-z0-9])?$')
);

CREATE TABLE IF NOT EXISTS roles (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(50) UNIQUE NOT NULL CHECK (name IN ('SUPER_ADMIN', 'STORE_ADMIN', 'STORE_USER')),
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS permissions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    description VARCHAR(255),
    code VARCHAR(50) UNIQUE NOT NULL,
    module VARCHAR(50) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS role_permissions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    role_id UUID NOT NULL REFERENCES roles(id) ON DELETE CASCADE,
    permission_id UUID NOT NULL REFERENCES permissions(id) ON DELETE CASCADE
);

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
-- 2. SERVER-TO-SERVER (S2S)
-- ==========================================

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
-- 3. DISCOUNT STRATEGY (GLOBAL)
-- ==========================================

CREATE TABLE IF NOT EXISTS discount_types (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    code VARCHAR(50) UNIQUE NOT NULL,
    display_name VARCHAR(150),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS discount_settings (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    ecommerce_id UUID NOT NULL REFERENCES ecommerce(id) ON DELETE CASCADE,
    max_discount_cap DECIMAL(12,4) NOT NULL CHECK (max_discount_cap > 0),
    currency_code VARCHAR(3) NOT NULL DEFAULT 'USD',
    allow_stacking BOOLEAN DEFAULT TRUE,
    rounding_rule VARCHAR(20) NOT NULL DEFAULT 'ROUND_HALF_UP',
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    version BIGINT NOT NULL DEFAULT 1,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS discount_priorities (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    discount_setting_id UUID NOT NULL REFERENCES discount_settings(id) ON DELETE CASCADE,
    discount_type_id UUID NOT NULL REFERENCES discount_types(id),
    priority_level INTEGER NOT NULL CHECK (priority_level > 0),
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(discount_setting_id, discount_type_id),
    UNIQUE(discount_setting_id, priority_level)
);

-- ==========================================
-- 4. RULES (Generic & Dynamic) - NEW ARCHITECTURE
-- ==========================================

CREATE TABLE IF NOT EXISTS rules (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    ecommerce_id UUID NOT NULL REFERENCES ecommerce(id) ON DELETE CASCADE,
    discount_priority_id UUID NOT NULL REFERENCES discount_priorities(id) ON DELETE CASCADE,
    name VARCHAR(255) NOT NULL,
    description VARCHAR(1000),
    discount_percentage NUMERIC(5,2) NOT NULL CHECK (discount_percentage >= 0 AND discount_percentage <= 100),
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS rule_attributes (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    discount_type_id UUID NOT NULL REFERENCES discount_types(id),
    attribute_name VARCHAR(100) NOT NULL,
    attribute_type VARCHAR(20) NOT NULL CHECK (attribute_type IN ('VARCHAR', 'NUMERIC', 'DATE', 'BOOLEAN')),
    is_required BOOLEAN NOT NULL DEFAULT FALSE,
    default_value VARCHAR(500),
    description VARCHAR(500),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(discount_type_id, attribute_name)
);

CREATE TABLE IF NOT EXISTS rule_attribute_values (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    rule_id UUID NOT NULL REFERENCES rules(id) ON DELETE CASCADE,
    attribute_id UUID NOT NULL REFERENCES rule_attributes(id) ON DELETE CASCADE,
    value VARCHAR(1000) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(rule_id, attribute_id)
);

-- ==========================================
-- 5. CLASSIFICATION (Customer Tiers)
-- ==========================================

CREATE TABLE IF NOT EXISTS customer_tiers (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    ecommerce_id UUID NOT NULL REFERENCES ecommerce(id) ON DELETE CASCADE,
    name VARCHAR(100) NOT NULL,
    hierarchy_level INTEGER NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(ecommerce_id, name)
);

-- ==========================================
-- 6. OBSERVABILITY (Audit & Logs)
-- ==========================================

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
-- INDEXES (Performance Optimization)
-- ==========================================

CREATE INDEX IF NOT EXISTS idx_ecommerce_slug ON ecommerce(slug);
CREATE INDEX IF NOT EXISTS idx_ecommerce_status ON ecommerce(status);

CREATE INDEX IF NOT EXISTS idx_user_ecommerce ON app_user(ecommerce_id);
CREATE INDEX IF NOT EXISTS idx_user_role ON app_user(role_id);
CREATE INDEX IF NOT EXISTS idx_user_active ON app_user(is_active);

CREATE INDEX IF NOT EXISTS idx_api_key_ecommerce ON api_key(ecommerce_id);

CREATE UNIQUE INDEX IF NOT EXISTS idx_discount_settings_ecommerce_active ON discount_settings(ecommerce_id) WHERE is_active = TRUE;

CREATE INDEX IF NOT EXISTS idx_discount_priority_setting ON discount_priorities(discount_setting_id);
CREATE INDEX IF NOT EXISTS idx_discount_priority_type ON discount_priorities(discount_type_id);
CREATE INDEX IF NOT EXISTS idx_discount_priority_level ON discount_priorities(discount_setting_id, priority_level);

CREATE INDEX IF NOT EXISTS idx_rules_ecommerce ON rules(ecommerce_id);
CREATE INDEX IF NOT EXISTS idx_rules_discount_priority ON rules(discount_priority_id);
CREATE INDEX IF NOT EXISTS idx_rules_active ON rules(is_active);

CREATE INDEX IF NOT EXISTS idx_rule_attributes_discount_type_id ON rule_attributes(discount_type_id);

CREATE INDEX IF NOT EXISTS idx_rule_attribute_values_rule ON rule_attribute_values(rule_id);
CREATE INDEX IF NOT EXISTS idx_rule_attribute_values_attribute ON rule_attribute_values(attribute_id);

CREATE INDEX IF NOT EXISTS idx_customer_tiers_ecommerce ON customer_tiers(ecommerce_id);
CREATE INDEX IF NOT EXISTS idx_customer_tiers_active ON customer_tiers(is_active);

CREATE INDEX IF NOT EXISTS idx_discount_log_ecommerce ON discount_application_log(ecommerce_id);
CREATE INDEX IF NOT EXISTS idx_discount_log_created ON discount_application_log(created_at DESC);

CREATE INDEX IF NOT EXISTS idx_audit_log_ecommerce ON audit_log(ecommerce_id);
CREATE INDEX IF NOT EXISTS idx_audit_log_user ON audit_log(user_id);
CREATE INDEX IF NOT EXISTS idx_audit_log_action ON audit_log(action);
CREATE INDEX IF NOT EXISTS idx_audit_log_entity ON audit_log(entity_name);
CREATE INDEX IF NOT EXISTS idx_audit_log_created ON audit_log(created_at DESC);

-- ==========================================
-- SEED DATA (Initial Data)
-- ==========================================

INSERT INTO roles (id, name, is_active) VALUES
    ('97f4b983-6454-4086-83ba-be641c6c9f0b'::UUID, 'SUPER_ADMIN', TRUE),
    ('fa94e179-670c-484a-a8ae-5e55a400b724'::UUID, 'STORE_ADMIN', TRUE),
    ('8c23eacb-dbbd-4f31-9613-af9cda1d4cce'::UUID, 'STORE_USER', TRUE)
ON CONFLICT DO NOTHING;

INSERT INTO discount_types (code, display_name) VALUES
    ('FIDELITY', 'Fidelidad'),
    ('SEASONAL', 'Estacional'),
    ('PRODUCT', 'Producto'),
    ('CLASSIFICATION', 'Clasificación')
ON CONFLICT DO NOTHING;
