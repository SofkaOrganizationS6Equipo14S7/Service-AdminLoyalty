Tengo mi siguiente base de datos, la cual es parte de un motor de descuentos para ecommerce. La base de datos está diseñada para soportar múltiples tenants (ecommerce) y tiene una estructura modular que permite gestionar usuarios, roles, permisos, configuraciones de descuento, reglas de clasificación, reglas estacionales y de producto, así como la observabilidad a través de logs de aplicación de descuentos y auditoría. A continuación se detallan las tablas principales:

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


Dicho esto, quiero que me ayudes a revisar los controllers que tengo para este proyecto, y me digas ayudes actualizar aquellos endpoints que sean necesarios para que estén alineados con la estructura de la base de datos y las funcionalidades que se esperan del motor de descuentos. Por favor, revisa los controllers actuales y sugiéreme las modificaciones o adiciones necesarias para asegurar que puedan manejar correctamente las operaciones CRUD, así como cualquier lógica de negocio relevante para la gestión de descuentos, usuarios, roles, permisos y logs.
Cabe señalar que la razon de este requerimiento surge debido a que anteriormente la base de datos estaba mal diseñada y algunos controller servicios, repositorios etc, estan alineados con esa estructura anterior. Ahora que la base de datos ha sido rediseñada para ser más robusta y modular, es crucial que los controllers también se actualicen para reflejar estos cambios y garantizar una integración fluida entre la capa de datos y la lógica de negocio.
Considera tambien que en caso de encontrar o ver una tabla que no este implementada omitela porque la prioridad es alinear los controllers con la nueva estructura de la base de datos, Los endpoints que estan son los que necesito revisar y actualizar, si encuentras o ves alguna oportunidad de mejora o inclusion de un controler sugieremelo antes de implementar.
