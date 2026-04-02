-- ============================================================
-- Database Model - Service Admin (Normalized)
-- Generated for PowerDesigner Import
-- PostgreSQL
-- ============================================================

-- ecommerce (tenants)
CREATE TABLE ecommerce (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(255) NOT NULL,
    slug VARCHAR(255) UNIQUE NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE' CHECK (status IN ('ACTIVE', 'INACTIVE')),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT slug_format CHECK (slug ~ '^[a-z0-9]([a-z0-9-]{0,252}[a-z0-9])?$')
);

-- role
CREATE TABLE role (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(50) UNIQUE NOT NULL CHECK (name IN ('SUPER_ADMIN', 'STORE_ADMIN', 'STORE_USER')),
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- permission
CREATE TABLE permission (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    code VARCHAR(50) UNIQUE NOT NULL,
    description VARCHAR(255),
    module VARCHAR(50) NOT NULL,
    action VARCHAR(20) NOT NULL CHECK (action IN ('read', 'write', 'delete')),
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- user
CREATE TABLE user (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    ecommerce_id UUID REFERENCES ecommerce(id) ON DELETE RESTRICT,
    role_id UUID REFERENCES role(id) ON DELETE RESTRICT,
    username VARCHAR(100) UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    email VARCHAR(255),
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    last_login TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- role_permission (many-to-many)
CREATE TABLE role_permission (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    role_id UUID NOT NULL REFERENCES role(id) ON DELETE CASCADE,
    permission_id UUID NOT NULL REFERENCES permission(id) ON DELETE CASCADE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(role_id, permission_id)
);

-- api_key
CREATE TABLE api_key (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    ecommerce_id UUID NOT NULL REFERENCES ecommerce(id) ON DELETE CASCADE,
    key_prefix VARCHAR(10) NOT NULL,
    hashed_key VARCHAR(255) UNIQUE NOT NULL,
    expires_at TIMESTAMP WITH TIME ZONE,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- discount_setting (configuración de descuentos)
CREATE TABLE discount_setting (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    ecommerce_id UUID NOT NULL REFERENCES ecommerce(id) ON DELETE CASCADE,
    max_discount_cap DECIMAL(12,4) NOT NULL CHECK (max_discount_cap > 0),
    currency_code VARCHAR(3) NOT NULL DEFAULT 'USD',
    rounding_rule VARCHAR(20) NOT NULL DEFAULT 'ROUND_HALF_UP',
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(ecommerce_id, is_active) WHERE is_active = TRUE
);

-- discount_priority
CREATE TABLE discount_priority (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    discount_setting_id UUID NOT NULL REFERENCES discount_setting(id) ON DELETE CASCADE,
    discount_type VARCHAR(50) NOT NULL CHECK (discount_type IN ('SEASONAL', 'PRODUCT', 'FIDELITY', 'CUSTOMER_TIER')),
    priority_level INTEGER NOT NULL CHECK (priority_level > 0),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(discount_setting_id, discount_type),
    UNIQUE(discount_setting_id, priority_level)
);

-- seasonal_rule
CREATE TABLE seasonal_rule (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    ecommerce_id UUID NOT NULL REFERENCES ecommerce(id) ON DELETE CASCADE,
    name VARCHAR(255) NOT NULL,
    description VARCHAR(1000),
    discount_percentage NUMERIC(5,2) NOT NULL CHECK (discount_percentage >= 0 AND discount_percentage <= 100),
    discount_type VARCHAR(50) NOT NULL DEFAULT 'PERCENTAGE',
    start_date TIMESTAMP WITH TIME ZONE NOT NULL,
    end_date TIMESTAMP WITH TIME ZONE NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT valid_date_range CHECK (start_date < end_date)
);

-- product_rule
CREATE TABLE product_rule (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    ecommerce_id UUID NOT NULL REFERENCES ecommerce(id) ON DELETE CASCADE,
    name VARCHAR(255) NOT NULL,
    product_type VARCHAR(100) NOT NULL,
    discount_percentage NUMERIC(5,2) NOT NULL CHECK (discount_percentage >= 0 AND discount_percentage <= 100),
    benefit VARCHAR(500),
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(ecommerce_id, product_type, is_active)
);

-- fidelity_range
CREATE TABLE fidelity_range (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    ecommerce_id UUID NOT NULL REFERENCES ecommerce(id) ON DELETE CASCADE,
    name VARCHAR(255) NOT NULL,
    min_points INTEGER NOT NULL CHECK (min_points >= 0),
    max_points INTEGER NOT NULL CHECK (max_points > min_points),
    discount_percentage NUMERIC(5,2) NOT NULL CHECK (discount_percentage >= 0 AND discount_percentage <= 100),
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(ecommerce_id, name) WHERE is_active = TRUE
);

-- customer_tier
CREATE TABLE customer_tier (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(100) UNIQUE NOT NULL,
    level INTEGER NOT NULL CHECK (level >= 1 AND level <= 10),
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- classification_rule
CREATE TABLE classification_rule (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    customer_tier_id UUID NOT NULL REFERENCES customer_tier(id) ON DELETE RESTRICT,
    metric_type VARCHAR(50) NOT NULL CHECK (metric_type IN ('total_spent', 'order_count', 'loyalty_points', 'custom')),
    min_value NUMERIC(19,2) NOT NULL DEFAULT 0,
    max_value NUMERIC(19,2),
    priority INTEGER NOT NULL CHECK (priority > 0),
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(customer_tier_id, priority) WHERE is_active = TRUE
);

-- discount_application_log
CREATE TABLE discount_application_log (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    ecommerce_id UUID NOT NULL REFERENCES ecommerce(id) ON DELETE CASCADE,
    external_order_id VARCHAR(255),
    original_amount DECIMAL(12,4) NOT NULL,
    final_amount DECIMAL(12,4) NOT NULL,
    total_discount_applied DECIMAL(12,4) NOT NULL,
    applied_rules_details JSONB,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- discount_usage_history
CREATE TABLE discount_usage_history (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    ecommerce_id UUID NOT NULL REFERENCES ecommerce(id) ON DELETE CASCADE,
    external_order_id VARCHAR(255) NOT NULL,
    subtotal_before DECIMAL(12,4) NOT NULL,
    total_discount DECIMAL(12,4) NOT NULL,
    final_amount DECIMAL(12,4) NOT NULL,
    applied_rules_snapshot JSONB,
    processed_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- audit_log
CREATE TABLE audit_log (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID REFERENCES user(id) ON DELETE SET NULL,
    action VARCHAR(20) NOT NULL CHECK (action IN ('CREATE', 'UPDATE', 'DELETE')),
    target_entity VARCHAR(50) NOT NULL,
    change_data JSONB,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- ============================================================
-- INDEXES
-- ============================================================

-- ecommerce
CREATE INDEX idx_ecommerce_status ON ecommerce(status);
CREATE INDEX idx_ecommerce_slug ON ecommerce(slug);

-- user
CREATE INDEX idx_user_ecommerce ON user(ecommerce_id);
CREATE INDEX idx_user_role ON user(role_id);
CREATE INDEX idx_user_active ON user(is_active);

-- role_permission
CREATE INDEX idx_role_permission_role ON role_permission(role_id);
CREATE INDEX idx_role_permission_permission ON role_permission(permission_id);

-- api_key
CREATE INDEX idx_api_key_ecommerce ON api_key(ecommerce_id);
CREATE INDEX idx_api_key_active ON api_key(is_active);

-- discount_setting
CREATE INDEX idx_discount_setting_ecommerce ON discount_setting(ecommerce_id);

-- seasonal_rule
CREATE INDEX idx_seasonal_rule_ecommerce ON seasonal_rule(ecommerce_id);
CREATE INDEX idx_seasonal_rule_date ON seasonal_rule(start_date, end_date);
CREATE INDEX idx_seasonal_rule_active ON seasonal_rule(is_active);

-- product_rule
CREATE INDEX idx_product_rule_ecommerce ON product_rule(ecommerce_id);
CREATE INDEX idx_product_rule_type ON product_rule(product_type);
CREATE INDEX idx_product_rule_active ON product_rule(is_active);

-- fidelity_range
CREATE INDEX idx_fidelity_range_ecommerce ON fidelity_range(ecommerce_id);
CREATE INDEX idx_fidelity_range_points ON fidelity_range(min_points);
CREATE INDEX idx_fidelity_range_active ON fidelity_range(is_active);

-- classification_rule
CREATE INDEX idx_classification_rule_tier ON classification_rule(customer_tier_id);
CREATE INDEX idx_classification_rule_active ON classification_rule(is_active);

-- discount_application_log
CREATE INDEX idx_discount_log_ecommerce ON discount_application_log(ecommerce_id);
CREATE INDEX idx_discount_log_order ON discount_application_log(external_order_id);
CREATE INDEX idx_discount_log_created ON discount_application_log(created_at);

-- discount_usage_history
CREATE INDEX idx_discount_history_ecommerce ON discount_usage_history(ecommerce_id);
CREATE INDEX idx_discount_history_order ON discount_usage_history(external_order_id);
CREATE INDEX idx_discount_history_processed ON discount_usage_history(processed_at);

-- audit_log
CREATE INDEX idx_audit_log_user ON audit_log(user_id);
CREATE INDEX idx_audit_log_action ON audit_log(action);
CREATE INDEX idx_audit_log_target ON audit_log(target_entity);
CREATE INDEX idx_audit_log_created ON audit_log(created_at);

-- ============================================================
-- SEED DATA (Roles y Permisos)
-- ============================================================

-- Insert roles
INSERT INTO role (name) VALUES 
    ('SUPER_ADMIN'),
    ('STORE_ADMIN'),
    ('STORE_USER')
ON CONFLICT (name) DO NOTHING;

-- Insert permissions
INSERT INTO permission (code, description, module, action) VALUES
    ('promotion:read', 'Visualizar promociones', 'promotion', 'read'),
    ('promotion:write', 'Crear y editar promociones', 'promotion', 'write'),
    ('promotion:delete', 'Eliminar promociones', 'promotion', 'delete'),
    ('user:read', 'Visualizar usuarios', 'user', 'read'),
    ('user:write', 'Crear y editar usuarios', 'user', 'write'),
    ('user:delete', 'Eliminar usuarios', 'user', 'delete'),
    ('ecommerce:read', 'Visualizar datos del ecommerce', 'ecommerce', 'read'),
    ('ecommerce:write', 'Editar datos del ecommerce', 'ecommerce', 'write')
ON CONFLICT (code) DO NOTHING;

-- Insert customer tiers
INSERT INTO customer_tier (name, level) VALUES
    ('BRONZE', 1),
    ('SILVER', 2),
    ('GOLD', 3),
    ('PLATINUM', 4)
ON CONFLICT (name) DO NOTHING;
