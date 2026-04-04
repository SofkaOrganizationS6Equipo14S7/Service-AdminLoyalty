CREATE TABLE product_rules (
    uid UUID PRIMARY KEY,
    ecommerce_id UUID NOT NULL,
    name VARCHAR(255) NOT NULL,
    product_type VARCHAR(100) NOT NULL,
    discount_percentage NUMERIC(5,2) NOT NULL 
        CHECK (discount_percentage >= 0 AND discount_percentage <= 100),
    benefit VARCHAR(255),
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(ecommerce_id, product_type, is_active)
);

CREATE INDEX idx_product_rules_ecommerce_id ON product_rules(ecommerce_id);
CREATE INDEX idx_product_rules_product_type ON product_rules(product_type);
CREATE INDEX idx_product_rules_active ON product_rules(is_active);
CREATE INDEX idx_product_rules_ecommerce_active 
    ON product_rules(ecommerce_id, is_active);
