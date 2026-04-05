-- Migration V16__Create_transaction_logs_table.sql
-- Tabla para auditoría de cálculos de descuentos (HU-11)
-- Almacena cada transacción incluyendo descuentos aplicados, caps y métricas del cliente

CREATE TABLE IF NOT EXISTS transaction_logs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    ecommerce_id UUID NOT NULL REFERENCES ecommerce(id),
    external_order_id VARCHAR(255) NOT NULL,
    subtotal_amount NUMERIC(12, 4) NOT NULL,
    discount_calculated NUMERIC(12, 2) NOT NULL,
    discount_applied NUMERIC(12, 2) NOT NULL,
    final_amount NUMERIC(12, 2) NOT NULL,
    was_capped BOOLEAN NOT NULL DEFAULT FALSE,
    cap_reason VARCHAR(100),
    applied_rules_json JSONB,
    customer_tier VARCHAR(50),
    client_metrics_json JSONB,
    status VARCHAR(50) NOT NULL DEFAULT 'SUCCESS',
    calculated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    expires_at TIMESTAMP WITH TIME ZONE,
    
    CONSTRAINT uk_transaction_external_order UNIQUE(ecommerce_id, external_order_id)
);

-- Índices para búsqueda eficiente
CREATE INDEX IF NOT EXISTS idx_transaction_logs_ecommerce_created 
    ON transaction_logs(ecommerce_id, created_at DESC);

CREATE INDEX IF NOT EXISTS idx_transaction_logs_expires_at 
    ON transaction_logs(created_at, expires_at);

-- Comentarios
COMMENT ON TABLE transaction_logs IS 'Auditoría de cálculos de descuentos sin datos personales (PII)';
COMMENT ON COLUMN transaction_logs.applied_rules_json IS 'Array de reglas aplicadas: [{rule_id, rule_name, discount_type, discount_amount, ...}]';
COMMENT ON COLUMN transaction_logs.client_metrics_json IS 'Métricas sin PII: {total_spent, order_count, membership_days}';
COMMENT ON COLUMN transaction_logs.expires_at IS 'Timestamp para limpieza automática (7 días)';
