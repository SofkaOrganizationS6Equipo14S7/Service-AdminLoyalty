-- V1__Engine_Service_Database_Schema.sql
-- Engine Service: Database Schema (Normalized & Complete)
-- Purpose: Support HU-10 (Classification), HU-11 (Discount Calculation), HU-12 (Transaction History)
-- Architecture: Replica tables from Admin Service, synchronized via RabbitMQ
-- Eventual Consistency: Data synchronized from Admin, cached in Caffeine
-- ============================================================================

-- ==========================================
-- EXTENSIONS
-- ==========================================
CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- ==========================================
-- 1. API KEYS (Server-to-Server Authentication)
-- ==========================================

CREATE TABLE IF NOT EXISTS engine_api_keys (
    hashed_key VARCHAR(255) PRIMARY KEY,
    ecommerce_id UUID NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    synced_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT ck_expires_at CHECK (expires_at > CURRENT_TIMESTAMP)
);

CREATE INDEX IF NOT EXISTS idx_api_keys_ecommerce ON engine_api_keys(ecommerce_id);
CREATE INDEX IF NOT EXISTS idx_api_keys_active ON engine_api_keys(is_active);

COMMENT ON TABLE engine_api_keys IS 'Replica of admin.api_key for fast validation. Synchronized via RabbitMQ.';
COMMENT ON COLUMN engine_api_keys.hashed_key IS 'Primary key: hashed API key for validation';
COMMENT ON COLUMN engine_api_keys.ecommerce_id IS 'Tenant identifier';

-- ==========================================
-- 2. DISCOUNT SETTINGS (Configuration)
-- ==========================================

CREATE TABLE IF NOT EXISTS engine_discount_settings (
    ecommerce_id UUID PRIMARY KEY,
    max_discount_cap DECIMAL(12,4) NOT NULL,
    currency_code VARCHAR(3) NOT NULL DEFAULT 'USD',
    allow_stacking BOOLEAN NOT NULL DEFAULT TRUE,
    rounding_rule VARCHAR(20) NOT NULL DEFAULT 'ROUND_HALF_UP',
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    synced_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT ck_max_discount_cap CHECK (max_discount_cap > 0)
);

CREATE INDEX IF NOT EXISTS idx_discount_settings_active ON engine_discount_settings(is_active);

COMMENT ON TABLE engine_discount_settings IS 'Replica of admin.discount_settings. Global configuration per ecommerce.';
COMMENT ON COLUMN engine_discount_settings.max_discount_cap IS 'Maximum discount allowed per transaction';
COMMENT ON COLUMN engine_discount_settings.allow_stacking IS 'Whether multiple rules can stack their discounts';
COMMENT ON COLUMN engine_discount_settings.rounding_rule IS 'How to round final amounts (ROUND_HALF_UP, ROUND_DOWN, etc)';

-- ==========================================
-- 3. DISCOUNT PRIORITIES (Evaluation Order)
-- ==========================================

CREATE TABLE IF NOT EXISTS engine_discount_priorities (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    ecommerce_id UUID NOT NULL,
    discount_type_code VARCHAR(50) NOT NULL,
    priority_level INTEGER NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    synced_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT ck_priority_level CHECK (priority_level > 0),
    CONSTRAINT uk_ecommerce_discount_type UNIQUE(ecommerce_id, discount_type_code),
    CONSTRAINT uk_ecommerce_priority_level UNIQUE(ecommerce_id, priority_level)
);

CREATE INDEX IF NOT EXISTS idx_discount_priorities_ecommerce ON engine_discount_priorities(ecommerce_id);
CREATE INDEX IF NOT EXISTS idx_discount_priorities_type ON engine_discount_priorities(discount_type_code);
CREATE INDEX IF NOT EXISTS idx_discount_priorities_level ON engine_discount_priorities(ecommerce_id, priority_level);

COMMENT ON TABLE engine_discount_priorities IS 'Defines evaluation order for discount types (1=first, 2=second, etc).';
COMMENT ON COLUMN engine_discount_priorities.discount_type_code IS 'FIDELITY | SEASONAL | PRODUCT | CLASSIFICATION';
COMMENT ON COLUMN engine_discount_priorities.priority_level IS 'Lower number = evaluated first';

-- ==========================================
-- 4. CUSTOMER TIERS (Fidelity Levels)
-- ==========================================

CREATE TABLE IF NOT EXISTS engine_customer_tiers (
    id UUID PRIMARY KEY,
    ecommerce_id UUID NOT NULL,
    name VARCHAR(100) NOT NULL,
    discount_percentage DECIMAL(5,2) NOT NULL,
    hierarchy_level INTEGER NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    synced_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT ck_discount_percentage CHECK (discount_percentage >= 0 AND discount_percentage <= 100),
    CONSTRAINT ck_hierarchy_level CHECK (hierarchy_level >= 1),
    CONSTRAINT uk_ecommerce_tier_name UNIQUE(ecommerce_id, name)
);

CREATE INDEX IF NOT EXISTS idx_customer_tiers_ecommerce ON engine_customer_tiers(ecommerce_id);
CREATE INDEX IF NOT EXISTS idx_customer_tiers_active ON engine_customer_tiers(is_active);
CREATE INDEX IF NOT EXISTS idx_customer_tiers_hierarchy ON engine_customer_tiers(ecommerce_id, hierarchy_level);

COMMENT ON TABLE engine_customer_tiers IS 'Replica of admin.customer_tiers. Customer fidelity levels (Bronze, Silver, Gold, Platinum).';
COMMENT ON COLUMN engine_customer_tiers.hierarchy_level IS 'Ordering for tier classification (1=lowest, 4=highest)';

-- ==========================================
-- 5. ENGINE RULES (Consolidated Discount Rules)
-- ==========================================

CREATE TABLE IF NOT EXISTS engine_rules (
    id UUID PRIMARY KEY,
    ecommerce_id UUID NOT NULL,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    discount_type_code VARCHAR(50) NOT NULL,
    discount_type VARCHAR(50) NOT NULL,
    discount_value DECIMAL(12,4) NOT NULL,
    applied_with VARCHAR(50) NOT NULL DEFAULT 'INDIVIDUAL',
    logic_conditions JSONB NOT NULL,
    priority_level INTEGER NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    synced_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT ck_discount_value CHECK (discount_value > 0),
    CONSTRAINT ck_priority_level CHECK (priority_level > 0),
    CONSTRAINT uk_ecommerce_priority_type UNIQUE(ecommerce_id, priority_level, discount_type_code)
);

CREATE INDEX IF NOT EXISTS idx_engine_rules_ecommerce ON engine_rules(ecommerce_id);
CREATE INDEX IF NOT EXISTS idx_engine_rules_type ON engine_rules(discount_type_code);
CREATE INDEX IF NOT EXISTS idx_engine_rules_priority ON engine_rules(ecommerce_id, priority_level);
CREATE INDEX IF NOT EXISTS idx_engine_rules_active ON engine_rules(is_active);
CREATE INDEX IF NOT EXISTS idx_engine_rules_ecommerce_active ON engine_rules(ecommerce_id, is_active);

COMMENT ON TABLE engine_rules IS 'Consolidated rules for all discount types. Replicated from admin.rules. Criteria stored as JSONB for dynamic evaluation.';
COMMENT ON COLUMN engine_rules.discount_type_code IS 'FIDELITY | SEASONAL | PRODUCT | CLASSIFICATION';
COMMENT ON COLUMN engine_rules.discount_type IS 'PERCENTAGE | FIXED_AMOUNT (determines how discount_value is interpreted)';
COMMENT ON COLUMN engine_rules.applied_with IS 'INDIVIDUAL | CUMULATIVE | EXCLUSIVE (how to combine with other rules)';
COMMENT ON COLUMN engine_rules.logic_conditions IS 'JSON structure: {"field_name": {"type": "NUMERIC|STRING|ARRAY|DATE_RANGE", "value": ...}}';
COMMENT ON COLUMN engine_rules.priority_level IS 'Order of evaluation per ecommerce. Lower = evaluated first.';

-- ==========================================
-- 6. TRANSACTION LOGS (HU-12: Discount History)
-- ==========================================

CREATE TABLE IF NOT EXISTS transaction_logs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    ecommerce_id UUID NOT NULL,
    external_order_id VARCHAR(255) NOT NULL UNIQUE,
    subtotal_amount DECIMAL(12,4) NOT NULL,
    discount_calculated DECIMAL(12,2) NOT NULL,
    discount_applied DECIMAL(12,2) NOT NULL,
    final_amount DECIMAL(12,2) NOT NULL,
    was_capped BOOLEAN NOT NULL DEFAULT FALSE,
    status VARCHAR(20) NOT NULL DEFAULT 'SUCCESS',
    error_message TEXT,
    applied_rules_json JSONB NOT NULL,
    priority_evaluation_json JSONB,
    customer_tier VARCHAR(100),
    client_metrics_json JSONB,
    calculated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    expires_at TIMESTAMP WITH TIME ZONE GENERATED ALWAYS AS (created_at + INTERVAL '7 days') STORED,
    
    CONSTRAINT ck_amounts CHECK (subtotal_amount >= 0),
    CONSTRAINT ck_discount_calculated CHECK (discount_calculated >= 0),
    CONSTRAINT ck_discount_applied CHECK (discount_applied >= 0),
    CONSTRAINT ck_financial_consistency CHECK (final_amount = subtotal_amount - discount_applied)
);

CREATE INDEX IF NOT EXISTS idx_transaction_logs_ecommerce ON transaction_logs(ecommerce_id);
CREATE INDEX IF NOT EXISTS idx_transaction_logs_created ON transaction_logs(created_at DESC);
CREATE INDEX IF NOT EXISTS idx_transaction_logs_ecommerce_created ON transaction_logs(ecommerce_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_transaction_logs_status ON transaction_logs(status);
CREATE INDEX IF NOT EXISTS idx_transaction_logs_was_capped ON transaction_logs(was_capped);
CREATE INDEX IF NOT EXISTS idx_transaction_logs_expires ON transaction_logs(expires_at);

COMMENT ON TABLE transaction_logs IS 'Transaction history (HU-12). No PII stored. Auto-expires after 7 days.';
COMMENT ON COLUMN transaction_logs.external_order_id IS 'Reference to external order system (no customer PII)';
COMMENT ON COLUMN transaction_logs.was_capped IS 'TRUE if discount_applied < discount_calculated (hit max cap)';
COMMENT ON COLUMN transaction_logs.status IS 'SUCCESS | PARTIALLY_APPLIED | REJECTED';
COMMENT ON COLUMN transaction_logs.applied_rules_json IS 'Array of rules that were evaluated and their results';
COMMENT ON COLUMN transaction_logs.expires_at IS 'Auto-generated: created_at + 7 days (for retention compliance)';

-- ==========================================
-- DATA INTEGRITY VIEWS (Optional, for Admin)
-- ==========================================

-- View to check current active config per ecommerce
CREATE OR REPLACE VIEW v_active_discount_configs AS
SELECT 
    ecommerce_id,
    max_discount_cap,
    currency_code,
    allow_stacking,
    rounding_rule,
    synced_at
FROM engine_discount_settings
WHERE is_active = TRUE;

COMMENT ON VIEW v_active_discount_configs IS 'Shows active discount configuration per ecommerce for monitoring.';

-- ==========================================
-- SEED DATA (Optional for Testing)
-- ==========================================

-- INSERT INTO engine_api_keys (hashed_key, ecommerce_id, is_active, expires_at)
-- VALUES (
--     'sha256$example_hashed_key_123',
--     'uuid-of-ecommerce-1'::UUID,
--     TRUE,
--     CURRENT_TIMESTAMP + INTERVAL '1 year'
-- );

-- ==========================================
-- END OF SCHEMA
-- ==========================================
COMMENT ON SCHEMA public IS 'Engine Service Database: Replica+Cache+Logs for Discount Calculation (HU-10, HU-11, HU-12)';
