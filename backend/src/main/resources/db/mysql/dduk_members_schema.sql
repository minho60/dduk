-- DDUK ERP MySQL 8.0 members auth schema draft
-- Source of truth for this file:
-- 1. backend/src/main/java/com/dduk/entity/admin/Member.java
-- 2. backend/src/main/java/com/dduk/entity/admin/Role.java
-- 3. backend/src/main/java/com/dduk/controller/admin/AdminMemberController.java
--
-- Purpose:
-- - This file matches the current backend implementation for admin-managed accounts.
-- - It also keeps one small recommended extension column, `last_login_at`, which the backend may start using later.
-- - Use this file when you only need the auth/account schema aligned with the current `members` API.

CREATE DATABASE IF NOT EXISTS dduk_erp
  DEFAULT CHARACTER SET utf8mb4
  DEFAULT COLLATE utf8mb4_unicode_ci;

USE dduk_erp;

CREATE TABLE IF NOT EXISTS members (
    id BIGINT NOT NULL AUTO_INCREMENT,
    login_id VARCHAR(50) NOT NULL,
    password VARCHAR(255) NOT NULL,
    name VARCHAR(100) NOT NULL,
    role VARCHAR(30) NOT NULL,
    active TINYINT(1) NOT NULL DEFAULT 1,
    last_login_at DATETIME NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_members_login_id (login_id),
    KEY idx_members_role (role),
    KEY idx_members_active (active)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Allowed role values from the current Role enum:
-- - ADMIN
-- - HR
-- - INVENTORY
--
-- Example admin seed:
-- Replace the bcrypt hash before running if you want to insert a real account manually.
-- INSERT INTO members (login_id, password, name, role, active)
-- VALUES ('admin', '$2a$10$replace_with_real_bcrypt_hash', 'System Admin', 'ADMIN', 1);
