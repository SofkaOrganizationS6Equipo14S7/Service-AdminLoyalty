-- V15__Add_fidelity_permissions.sql
-- Add permissions for fidelity range management

INSERT INTO permissions (code, description, module, action) VALUES
    ('fidelity:read', 'Visualizar rangos de fidelidad', 'fidelity', 'read'),
    ('fidelity:write', 'Crear y editar rangos de fidelidad', 'fidelity', 'write')
ON CONFLICT (code) DO NOTHING;
