-- Extensión para UUIDs
CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- 1. IDENTIDAD Y TENANT
CREATE TABLE ecommerce (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(255) NOT NULL,
    slug VARCHAR(255) UNIQUE NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE' CHECK (status IN ('ACTIVE', 'INACTIVE')),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT slug_format CHECK (slug ~ '^[a-z0-9]([a-z0-9-]{0,252}[a-z0-9])?$')
);

CREATE TABLE roles (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(50) UNIQUE NOT NULL CHECK (name IN ('SUPER_ADMIN', 'STORE_ADMIN', 'STORE_USER')),
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE permissions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    description VARCHAR(255),
    code VARCHAR(50) UNIQUE NOT NULL,
    module VARCHAR(50) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE role_permissions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    role_id UUID NOT NULL REFERENCES roles(id) ON DELETE CASCADE,
    permission_id UUID NOT NULL REFERENCES permissions(id) ON DELETE CASCADE
);

CREATE TABLE app_user (
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

-- 2. CONECTIVIDAD S2S
CREATE TABLE api_key (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    ecommerce_id UUID NOT NULL REFERENCES ecommerce(id) ON DELETE CASCADE,
    key_prefix VARCHAR(10) NOT NULL,
    hashed_key VARCHAR(255) UNIQUE NOT NULL,
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL CHECK (expires_at > CURRENT_TIMESTAMP),
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 3. ESTRATEGIA GLOBAL
CREATE TABLE discount_types (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    code VARCHAR(50) UNIQUE NOT NULL, -- FIDELITY, SEASONAL, PRODUCT
    display_name VARCHAR(150)
);

CREATE TABLE discount_settings (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    ecommerce_id UUID NOT NULL REFERENCES ecommerce(id) ON DELETE CASCADE,
    max_discount_cap DECIMAL(12,4) NOT NULL CHECK (max_discount_cap > 0),
    currency_code VARCHAR(3) NOT NULL DEFAULT 'USD',
    allow_stacking BOOLEAN DEFAULT TRUE,
    rounding_rule VARCHAR(20) NOT NULL DEFAULT 'ROUND_HALF_UP',
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE discount_priorities (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    discount_setting_id UUID NOT NULL REFERENCES discount_settings(id) ON DELETE CASCADE,
    discount_type_id UUID NOT NULL REFERENCES discount_types(id),
    priority_level INTEGER NOT NULL CHECK (priority_level > 0),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(discount_setting_id, discount_type_id),
    UNIQUE(discount_setting_id, priority_level)
);

-- 4. REGLAS (ENGINE)
CREATE TABLE customer_tiers (
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

CREATE TABLE classification_rule (
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

CREATE TABLE seasonal_rules (
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

CREATE TABLE product_rules (
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

-- 5. OBSERVABILIDAD
CREATE TABLE discount_application_log (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    ecommerce_id UUID NOT NULL REFERENCES ecommerce(id) ON DELETE CASCADE,
    external_order_id VARCHAR(255),
    original_amount DECIMAL(12,4) NOT NULL,
    discount_applied DECIMAL(12,2) NOT NULL,
    final_amount DECIMAL(12,2) NOT NULL,
    applied_rules_details JSONB,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE audit_log (
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
CREATE INDEX idx_user_ecommerce ON app_user(ecommerce_id);
CREATE INDEX idx_user_role ON app_user(role_id);
CREATE INDEX idx_user_active ON app_user(is_active);
CREATE INDEX idx_api_key_ecommerce ON api_key(ecommerce_id);
CREATE INDEX idx_seasonal_rules_date ON seasonal_rules(start_date, end_date);
CREATE INDEX idx_seasonal_rules_ecommerce ON seasonal_rules(ecommerce_id);
CREATE INDEX idx_seasonal_rules_active ON seasonal_rules(is_active);

CREATE INDEX idx_product_rules_ecommerce ON product_rules(ecommerce_id);
CREATE INDEX idx_product_rules_type ON product_rules(product_type);
CREATE INDEX idx_product_rules_active ON product_rules(is_active);
CREATE INDEX idx_role_name ON roles(name);

CREATE INDEX idx_classification_rule_tier ON classification_rule(customer_tier_id);
CREATE INDEX idx_discount_type_code ON discount_types(code);
CREATE INDEX idx_ecommerce_slug ON ecommerce(slug);
CREATE INDEX idx_ecommerce_status ON ecommerce(status);
CREATE INDEX idx_permission_code ON ecommerce(code);
CREATE INDEX idx_permission_module ON ecommerce(module);

CREATE INDEX idx_role_permissions_role ON role_permissions(role_id);
CREATE INDEX idx_role_permissions_permission ON role_permissions(permission_id);

CREATE UNIQUE INDEX idx_classification_rule_active ON classification_rule(customer_tier_id, priority) WHERE is_active = TRUE;
CREATE INDEX idx_discount_log_ecommerce ON discount_application_log(ecommerce_id);
CREATE INDEX idx_audit_log_ecommerce ON audit_log(ecommerce_id);
CREATE UNIQUE INDEX idx_discount_settings_active ON discount_settings(ecommerce_id) WHERE is_active = TRUE;