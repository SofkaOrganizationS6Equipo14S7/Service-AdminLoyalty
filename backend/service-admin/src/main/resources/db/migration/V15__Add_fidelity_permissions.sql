-- V15__Add_fidelity_permissions.sql
-- Add permissions for fidelity range management

INSERT INTO permissions (code, module, action) VALUES
    ('fidelity:read', 'fidelity', 'read'),
    ('fidelity:write', 'fidelity', 'write')
ON CONFLICT (code) DO NOTHING;
