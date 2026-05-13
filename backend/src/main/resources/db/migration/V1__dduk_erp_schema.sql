-- DDUK ERP MySQL 8.0 schema draft
-- Source: docs/DB_DRAFT.md
-- Note:
-- 1. This schema follows the current DB draft document, not the existing backend auth entity names.
-- 2. Current backend auth code still references `members`, so this file is for manual review/copy-paste first.
-- 3. Run on MySQL 8.0+.

CREATE DATABASE IF NOT EXISTS dduk_erp
  DEFAULT CHARACTER SET utf8mb4
  DEFAULT COLLATE utf8mb4_unicode_ci;

USE dduk_erp;

CREATE TABLE IF NOT EXISTS users (
    id BIGINT NOT NULL AUTO_INCREMENT,
    username VARCHAR(50) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    email VARCHAR(100) NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE',
    last_login_at DATETIME NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_users_username (username),
    UNIQUE KEY uk_users_email (email)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS roles (
    id BIGINT NOT NULL AUTO_INCREMENT,
    code VARCHAR(50) NOT NULL,
    name VARCHAR(100) NOT NULL,
    description VARCHAR(255) NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_roles_code (code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS menus (
    id BIGINT NOT NULL AUTO_INCREMENT,
    menu_key VARCHAR(50) NOT NULL,
    name VARCHAR(100) NOT NULL,
    path VARCHAR(255) NOT NULL,
    parent_id BIGINT NULL,
    sort_order INT NOT NULL DEFAULT 0,
    is_active TINYINT(1) NOT NULL DEFAULT 1,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_menus_menu_key (menu_key),
    KEY idx_menus_parent_id (parent_id),
    CONSTRAINT fk_menus_parent
        FOREIGN KEY (parent_id) REFERENCES menus (id)
        ON DELETE SET NULL
        ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS employees (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NULL,
    employee_no VARCHAR(50) NOT NULL,
    name VARCHAR(100) NOT NULL,
    department VARCHAR(100) NOT NULL,
    position VARCHAR(100) NOT NULL,
    employment_status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE',
    hire_date DATE NOT NULL,
    email VARCHAR(100) NOT NULL,
    phone VARCHAR(30) NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_employees_employee_no (employee_no),
    UNIQUE KEY uk_employees_user_id (user_id),
    UNIQUE KEY uk_employees_email (email),
    CONSTRAINT fk_employees_user
        FOREIGN KEY (user_id) REFERENCES users (id)
        ON DELETE SET NULL
        ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;


-- 공급업체 정보
CREATE TABLE IF NOT EXISTS vendors (
    id BIGINT NOT NULL AUTO_INCREMENT,
    vendor_code VARCHAR(50) NOT NULL,
    business_registration_no VARCHAR(20) NOT NULL,
    name VARCHAR(100) NOT NULL,
    representative_name VARCHAR(100) NOT NULL,
    business_type VARCHAR(100) NULL,
    business_item VARCHAR(100) NULL,
    contact_name VARCHAR(100) NULL,
    contact_phone VARCHAR(30) NULL,
    email VARCHAR(100) NULL,
    address VARCHAR(255) NULL,
    bank_name VARCHAR(50) NULL,
    bank_account_no VARCHAR(50) NULL,
    bank_account_holder VARCHAR(100) NULL,
    bankbook_copy_file_path VARCHAR(255) NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_vendors_vendor_code (vendor_code),
    UNIQUE KEY uk_vendors_business_registration_no (business_registration_no),
    CONSTRAINT chk_vendors_status CHECK (status IN ('ACTIVE', 'INACTIVE', 'BLOCKED', 'PENDING'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS user_roles (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    role_id BIGINT NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_user_roles_user_role (user_id, role_id),
    KEY idx_user_roles_role_id (role_id),
    CONSTRAINT fk_user_roles_user
        FOREIGN KEY (user_id) REFERENCES users (id)
        ON DELETE CASCADE
        ON UPDATE CASCADE,
    CONSTRAINT fk_user_roles_role
        FOREIGN KEY (role_id) REFERENCES roles (id)
        ON DELETE CASCADE
        ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS menu_permissions (
    id BIGINT NOT NULL AUTO_INCREMENT,
    role_id BIGINT NOT NULL,
    menu_id BIGINT NOT NULL,
    can_view TINYINT(1) NOT NULL DEFAULT 0,
    can_create TINYINT(1) NOT NULL DEFAULT 0,
    can_update TINYINT(1) NOT NULL DEFAULT 0,
    can_delete TINYINT(1) NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_menu_permissions_role_menu (role_id, menu_id),
    KEY idx_menu_permissions_menu_id (menu_id),
    CONSTRAINT fk_menu_permissions_role
        FOREIGN KEY (role_id) REFERENCES roles (id)
        ON DELETE CASCADE
        ON UPDATE CASCADE,
    CONSTRAINT fk_menu_permissions_menu
        FOREIGN KEY (menu_id) REFERENCES menus (id)
        ON DELETE CASCADE
        ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS audit_logs (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NULL,
    action VARCHAR(100) NOT NULL,
    target_type VARCHAR(50) NOT NULL,
    target_id BIGINT NULL,
    ip_address VARCHAR(64) NULL,
    user_agent VARCHAR(255) NULL,
    details TEXT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_audit_logs_user_id (user_id),
    KEY idx_audit_logs_target (target_type, target_id),
    CONSTRAINT fk_audit_logs_user
        FOREIGN KEY (user_id) REFERENCES users (id)
        ON DELETE SET NULL
        ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 자재/제품 정보
CREATE TABLE IF NOT EXISTS items (
    id BIGINT NOT NULL AUTO_INCREMENT,
    item_code VARCHAR(50) NOT NULL,
    item_type VARCHAR(30) NOT NULL,
    name VARCHAR(100) NOT NULL,
    category VARCHAR(100) NULL,
    spec VARCHAR(100) NULL,
    unit VARCHAR(30) NOT NULL,
    barcode VARCHAR(100) NULL,
    default_vendor_id BIGINT NULL,
    purchase_unit_price DECIMAL(15,2) NOT NULL DEFAULT 0.00,
    sales_unit_price DECIMAL(15,2) NOT NULL DEFAULT 0.00,
    safety_stock INT NOT NULL DEFAULT 0,
    storage_type VARCHAR(30) NULL,
    shelf_life_days INT NULL,
    tax_type VARCHAR(30) NOT NULL DEFAULT 'TAXABLE',
    status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE',
    description VARCHAR(255) NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_items_item_code (item_code),
    UNIQUE KEY uk_items_barcode (barcode),
    KEY idx_items_default_vendor_id (default_vendor_id),
    KEY idx_items_item_type (item_type),
    KEY idx_items_category (category),
    KEY idx_items_status (status),
    CONSTRAINT chk_items_safety_stock_non_negative CHECK (safety_stock >= 0),
    CONSTRAINT chk_items_shelf_life_days_positive CHECK (shelf_life_days IS NULL OR shelf_life_days > 0),
    CONSTRAINT chk_items_item_type CHECK (item_type IN ('RAW_MATERIAL', 'PRODUCT', 'SUB_MATERIAL', 'PACKAGING')),
    CONSTRAINT chk_items_storage_type CHECK (storage_type IS NULL OR storage_type IN ('ROOM_TEMP', 'REFRIGERATED', 'FROZEN')),
    CONSTRAINT chk_items_tax_type CHECK (tax_type IN ('TAXABLE', 'TAX_FREE', 'ZERO_RATED')),
    CONSTRAINT chk_items_status CHECK (status IN ('ACTIVE', 'INACTIVE', 'DISCONTINUED', 'PENDING')),
    CONSTRAINT fk_items_default_vendor
        FOREIGN KEY (default_vendor_id) REFERENCES vendors (id)
        ON DELETE SET NULL
        ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 재고 현황
CREATE TABLE IF NOT EXISTS inventories (
    id BIGINT NOT NULL AUTO_INCREMENT,
    item_id BIGINT NOT NULL,
    quantity INT NOT NULL DEFAULT 0,
    allocated_quantity INT NOT NULL DEFAULT 0,
    location VARCHAR(100) NOT NULL,
    lot_no VARCHAR(100) NOT NULL DEFAULT '',
    expiration_date DATE NOT NULL DEFAULT '9999-12-31',
    status VARCHAR(30) NOT NULL DEFAULT 'AVAILABLE',
    last_movement_at DATETIME NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_inventories_item_location_lot_expiration (item_id, location, lot_no, expiration_date),
    KEY idx_inventories_item_id (item_id),
    KEY idx_inventories_location (location),
    KEY idx_inventories_expiration_date (expiration_date),
    KEY idx_inventories_status (status),
    KEY idx_inventories_item_expiration (item_id, expiration_date),
    CONSTRAINT chk_inventories_quantity_non_negative CHECK (quantity >= 0),
    CONSTRAINT chk_inventories_allocated_quantity_non_negative CHECK (allocated_quantity >= 0),
    CONSTRAINT chk_inventories_allocated_quantity_available CHECK (allocated_quantity <= quantity),
    CONSTRAINT chk_inventories_status CHECK (status IN ('AVAILABLE', 'HOLD', 'DAMAGED', 'EXPIRED', 'DISPOSED')),
    CONSTRAINT fk_inventories_item
        FOREIGN KEY (item_id) REFERENCES items (id)
        ON DELETE RESTRICT
        ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS attendances ( 
    id BIGINT NOT NULL AUTO_INCREMENT,
    employee_id BIGINT NOT NULL,
    work_date DATE NOT NULL,
    check_in_at DATETIME NULL,
    check_out_at DATETIME NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'PRESENT',
    note VARCHAR(255) NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_attendances_employee_work_date (employee_id, work_date),
    KEY idx_attendances_work_date (work_date),
    CONSTRAINT fk_attendances_employee
        FOREIGN KEY (employee_id) REFERENCES employees (id)
        ON DELETE CASCADE
        ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS payrolls (
    id BIGINT NOT NULL AUTO_INCREMENT,
    employee_id BIGINT NOT NULL,
    pay_month CHAR(7) NOT NULL,
    base_salary DECIMAL(15,2) NOT NULL DEFAULT 0.00,
    allowance_amount DECIMAL(15,2) NOT NULL DEFAULT 0.00,
    deduction_amount DECIMAL(15,2) NOT NULL DEFAULT 0.00,
    net_salary DECIMAL(15,2) NOT NULL DEFAULT 0.00,
    status VARCHAR(30) NOT NULL DEFAULT 'DRAFT',
    paid_at DATETIME NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_payrolls_employee_pay_month (employee_id, pay_month),
    CONSTRAINT fk_payrolls_employee
        FOREIGN KEY (employee_id) REFERENCES employees (id)
        ON DELETE CASCADE
        ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 비용/경비 등록 정보
CREATE TABLE IF NOT EXISTS expenses (
    id BIGINT NOT NULL AUTO_INCREMENT,
    expense_no VARCHAR(50) NOT NULL,
    employee_id BIGINT NULL,
    vendor_id BIGINT NULL,
    expense_date DATE NOT NULL,
    category VARCHAR(50) NOT NULL,
    description VARCHAR(255) NOT NULL,
    supply_amount DECIMAL(15,2) NOT NULL DEFAULT 0.00,
    tax_amount DECIMAL(15,2) NOT NULL DEFAULT 0.00,
    amount DECIMAL(15,2) NOT NULL DEFAULT 0.00,
    tax_type VARCHAR(30) NOT NULL DEFAULT 'TAXABLE',
    payment_method VARCHAR(30) NULL,
    payment_status VARCHAR(30) NOT NULL DEFAULT 'UNPAID',
    receipt_no VARCHAR(100) NULL,
    receipt_file_path VARCHAR(255) NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'SUBMITTED',
    approved_by BIGINT NULL,
    approved_at DATETIME NULL,
    paid_at DATETIME NULL,
    note VARCHAR(255) NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_expenses_expense_no (expense_no),
    KEY idx_expenses_employee_id (employee_id),
    KEY idx_expenses_vendor_id (vendor_id),
    KEY idx_expenses_expense_date (expense_date),
    KEY idx_expenses_status (status),
    KEY idx_expenses_payment_status (payment_status),
    KEY idx_expenses_expense_date_status (expense_date, status),
    CONSTRAINT chk_expenses_supply_amount_non_negative CHECK (supply_amount >= 0),
    CONSTRAINT chk_expenses_tax_amount_non_negative CHECK (tax_amount >= 0),
    CONSTRAINT chk_expenses_amount_non_negative CHECK (amount >= 0),
    CONSTRAINT chk_expenses_amount_matches CHECK (amount = supply_amount + tax_amount),
    CONSTRAINT chk_expenses_tax_type CHECK (tax_type IN ('TAXABLE', 'TAX_FREE', 'ZERO_RATED')),
    CONSTRAINT chk_expenses_payment_status CHECK (payment_status IN ('UNPAID', 'PAID', 'CANCELLED')),
    CONSTRAINT chk_expenses_status CHECK (status IN ('DRAFT', 'SUBMITTED', 'APPROVED', 'REJECTED', 'CANCELLED')),
    CONSTRAINT fk_expenses_employee
        FOREIGN KEY (employee_id) REFERENCES employees (id)
        ON DELETE SET NULL
        ON UPDATE CASCADE,
    CONSTRAINT fk_expenses_vendor
        FOREIGN KEY (vendor_id) REFERENCES vendors (id)
        ON DELETE SET NULL
        ON UPDATE CASCADE,
    CONSTRAINT fk_expenses_approved_by
        FOREIGN KEY (approved_by) REFERENCES users (id)
        ON DELETE SET NULL
        ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;




-- 구매 발주 정보
CREATE TABLE IF NOT EXISTS purchase_orders (
    id BIGINT NOT NULL AUTO_INCREMENT,
    purchase_order_no VARCHAR(50) NOT NULL,
    vendor_id BIGINT NOT NULL,
    requested_by BIGINT NOT NULL,
    order_date DATE NOT NULL,
    expected_date DATE NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'REQUESTED',
    total_amount DECIMAL(15,2) NOT NULL DEFAULT 0.00,
    note VARCHAR(255) NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_purchase_orders_no (purchase_order_no),
    KEY idx_purchase_orders_vendor_id (vendor_id),
    KEY idx_purchase_orders_requested_by (requested_by),
    CONSTRAINT chk_purchase_orders_total_amount_non_negative CHECK (total_amount >= 0),
    CONSTRAINT chk_purchase_orders_status CHECK (status IN ('REQUESTED', 'ORDERED', 'PARTIALLY_RECEIVED', 'RECEIVED', 'CANCELLED')),
    CONSTRAINT fk_purchase_orders_vendor
        FOREIGN KEY (vendor_id) REFERENCES vendors (id)
        ON DELETE RESTRICT
        ON UPDATE CASCADE,
    CONSTRAINT fk_purchase_orders_requested_by
        FOREIGN KEY (requested_by) REFERENCES users (id)
        ON DELETE RESTRICT
        ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 구매 발주 상세 항목
CREATE TABLE IF NOT EXISTS purchase_order_items (
    id BIGINT NOT NULL AUTO_INCREMENT,
    purchase_order_id BIGINT NOT NULL,
    item_id BIGINT NOT NULL,
    quantity INT NOT NULL,
    unit VARCHAR(30) NOT NULL,
    unit_price DECIMAL(15,2) NOT NULL DEFAULT 0.00,
    supply_amount DECIMAL(15,2) NOT NULL DEFAULT 0.00,
    tax_amount DECIMAL(15,2) NOT NULL DEFAULT 0.00,
    line_amount DECIMAL(15,2) NOT NULL DEFAULT 0.00,
    expected_date DATE NULL,
    note VARCHAR(255) NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_purchase_order_items_order_id (purchase_order_id),
    KEY idx_purchase_order_items_item_id (item_id),
    CONSTRAINT chk_purchase_order_items_quantity_positive CHECK (quantity > 0),
    CONSTRAINT chk_purchase_order_items_unit_price_non_negative CHECK (unit_price >= 0),
    CONSTRAINT chk_purchase_order_items_supply_amount_non_negative CHECK (supply_amount >= 0),
    CONSTRAINT chk_purchase_order_items_tax_amount_non_negative CHECK (tax_amount >= 0),
    CONSTRAINT chk_purchase_order_items_line_amount_non_negative CHECK (line_amount >= 0),
    CONSTRAINT chk_purchase_order_items_line_amount_matches CHECK (line_amount = supply_amount + tax_amount),
    CONSTRAINT fk_purchase_order_items_order
        FOREIGN KEY (purchase_order_id) REFERENCES purchase_orders (id)
        ON DELETE CASCADE
        ON UPDATE CASCADE,
    CONSTRAINT fk_purchase_order_items_item
        FOREIGN KEY (item_id) REFERENCES items (id)
        ON DELETE RESTRICT
        ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 재고 이동 기록
CREATE TABLE IF NOT EXISTS stock_movements (
    id BIGINT NOT NULL AUTO_INCREMENT,
    item_id BIGINT NOT NULL,
    from_inventory_id BIGINT NULL,
    to_inventory_id BIGINT NULL,
    movement_type VARCHAR(30) NOT NULL,
    quantity INT NOT NULL,
    unit VARCHAR(30) NOT NULL,
    from_before_quantity INT NULL,
    from_after_quantity INT NULL,
    to_before_quantity INT NULL,
    to_after_quantity INT NULL,
    reference_type VARCHAR(50) NULL,
    reference_id BIGINT NULL,
    purchase_order_item_id BIGINT NULL,
    moved_at DATETIME NOT NULL,
    reason VARCHAR(100) NULL,
    note VARCHAR(255) NULL,
    created_by BIGINT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_stock_movements_item_id (item_id),
    KEY idx_stock_movements_from_inventory_id (from_inventory_id),
    KEY idx_stock_movements_to_inventory_id (to_inventory_id),
    KEY idx_stock_movements_reference (reference_type, reference_id),
    KEY idx_stock_movements_purchase_order_item_id (purchase_order_item_id),
    KEY idx_stock_movements_moved_at (moved_at),
    KEY idx_stock_movements_item_moved_at (item_id, moved_at),
    KEY idx_stock_movements_movement_type (movement_type),
    KEY idx_stock_movements_created_by (created_by),
    CONSTRAINT chk_stock_movements_quantity_positive CHECK (quantity > 0),
    CONSTRAINT chk_stock_movements_from_before_quantity_non_negative CHECK (from_before_quantity IS NULL OR from_before_quantity >= 0),
    CONSTRAINT chk_stock_movements_from_after_quantity_non_negative CHECK (from_after_quantity IS NULL OR from_after_quantity >= 0),
    CONSTRAINT chk_stock_movements_to_before_quantity_non_negative CHECK (to_before_quantity IS NULL OR to_before_quantity >= 0),
    CONSTRAINT chk_stock_movements_to_after_quantity_non_negative CHECK (to_after_quantity IS NULL OR to_after_quantity >= 0),
    CONSTRAINT chk_stock_movements_inventory_exists CHECK (from_inventory_id IS NOT NULL OR to_inventory_id IS NOT NULL),
    CONSTRAINT chk_stock_movements_movement_type CHECK (movement_type IN ('INBOUND', 'OUTBOUND', 'ADJUSTMENT_IN', 'ADJUSTMENT_OUT', 'TRANSFER')),
    CONSTRAINT fk_stock_movements_item
        FOREIGN KEY (item_id) REFERENCES items (id)
        ON DELETE RESTRICT
        ON UPDATE CASCADE,
    CONSTRAINT fk_stock_movements_from_inventory
        FOREIGN KEY (from_inventory_id) REFERENCES inventories (id)
        ON DELETE RESTRICT
        ON UPDATE CASCADE,
    CONSTRAINT fk_stock_movements_to_inventory
        FOREIGN KEY (to_inventory_id) REFERENCES inventories (id)
        ON DELETE RESTRICT
        ON UPDATE CASCADE,
    CONSTRAINT fk_stock_movements_purchase_order_item
        FOREIGN KEY (purchase_order_item_id) REFERENCES purchase_order_items (id)
        ON DELETE SET NULL
        ON UPDATE CASCADE,
    CONSTRAINT fk_stock_movements_created_by
        FOREIGN KEY (created_by) REFERENCES users (id)
        ON DELETE RESTRICT
        ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

INSERT INTO roles (code, name, description)
VALUES
    ('ADMIN', '관리자', '시스템 전체 권한'),
    ('HR_MANAGER', '인사/회계 담당', '인사 및 회계 업무 권한'),
    ('INVENTORY_MANAGER', '재고/발주 담당', '재고 및 발주 업무 권한')
ON DUPLICATE KEY UPDATE
    name = VALUES(name),
    description = VALUES(description);
