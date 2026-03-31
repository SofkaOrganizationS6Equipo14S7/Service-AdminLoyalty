-- Tabla de Reglas de Temporada (Seasonal Rules)
-- Sistema de Fidelización - Réplica en Service-Engine
-- Sincronizada desde Service-Admin via RabbitMQ

CREATE TABLE seasonal_rules (
    uid UUID PRIMARY KEY,
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
    
    CONSTRAINT discount_percentage_range CHECK (discount_percentage >= 0 AND discount_percentage <= 100),
    CONSTRAINT valid_date_range CHECK (start_date < end_date)
);

CREATE INDEX idx_seasonal_rules_ecommerce_id ON seasonal_rules(ecommerce_id);
CREATE INDEX idx_seasonal_rules_date_range ON seasonal_rules(start_date, end_date);
CREATE INDEX idx_seasonal_rules_active ON seasonal_rules(is_active);
