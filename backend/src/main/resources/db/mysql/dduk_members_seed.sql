-- DDUK ERP MySQL 8.0 members seed data
-- Run this after dduk_members_schema.sql.
-- Purpose:
-- - Bootstrap one admin account
-- - Bootstrap one inventory/purchase account
-- - Bootstrap one hr/accounting account
--
-- Passwords are stored as BCrypt hashes to match the current Spring Security setup.
-- If the same login_id already exists, the account will be updated in place.

USE dduk_erp;

INSERT INTO members (
    login_id,
    password,
    name,
    role,
    active,
    created_at,
    updated_at
)
VALUES
    (
        'admin',
        '$2a$10$yOCLiBL.jnjWNnTz8H3MBugkf7Nk73W1smvvQSclH3Gj8dgyd1ERG',
        'System Admin',
        'ADMIN',
        1,
        NOW(),
        NOW()
    ),
    (
        'inventory',
        '$2a$10$jYIYRocKBwTDRrjeOc.I9uhI8Bqzn9gHzrVT0rj/FNk9sea7Qwv.W',
        'Inventory Manager',
        'INVENTORY',
        1,
        NOW(),
        NOW()
    ),
    (
        'hr',
        '$2a$10$2zR.ygAdbZzH2FDe5dBL0.uXx91hUNqbFyt8xn54Gf.ISAC/rWI9i',
        'HR Manager',
        'HR',
        1,
        NOW(),
        NOW()
    )
ON DUPLICATE KEY UPDATE
    password = VALUES(password),
    name = VALUES(name),
    role = VALUES(role),
    active = VALUES(active),
    updated_at = NOW();
