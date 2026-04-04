-- Agrega constraints de validación a la tabla seasonal_rules
-- V5 creó la tabla base sin constraints; esta migración los añade

ALTER TABLE seasonal_rules
    ADD CONSTRAINT discount_percentage_range CHECK (discount_percentage >= 0 AND discount_percentage <= 100);

ALTER TABLE seasonal_rules
    ADD CONSTRAINT valid_date_range CHECK (start_date < end_date);
