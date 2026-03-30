-- Tabla de Reglas de Temporada (Seasonal Rules)
-- Sistema de Record: Service-Admin
-- Tabla replicada en Service-Engine via RabbitMQ

CREATE TABLE seasonal_rules (
    uid UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    ecommerce_id UUID NOT NULL,
    name VARCHAR(255) NOT NULL,
    description VARCHAR(1000),
    discount_percentage NUMERIC(5,2) NOT NULL,
    discount_type VARCHAR(50) NOT NULL,
    start_date TIMESTAMP WITH TIME ZONE NOT NULL,
    end_date TIMESTAMP WITH TIME ZONE NOT NULL,
    is_active BOOLEAN DEFAULT true,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT fk_seasonal_rules_ecommerce FOREIGN KEY (ecommerce_id) REFERENCES ecommerces(uid) ON DELETE CASCADE,
    CONSTRAINT discount_percentage_range CHECK (discount_percentage >= 0 AND discount_percentage <= 100),
    CONSTRAINT valid_date_range CHECK (start_date < end_date),
    CONSTRAINT unique_seasonal_rule_per_ecommerce UNIQUE (ecommerce_id, start_date, end_date) WHERE is_active = true
);

CREATE INDEX idx_seasonal_rules_ecommerce_id ON seasonal_rules(ecommerce_id);
CREATE INDEX idx_seasonal_rules_date_range ON seasonal_rules(start_date, end_date);
CREATE INDEX idx_seasonal_rules_active ON seasonal_rules(is_active);
