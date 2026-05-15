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

CREATE TABLE IF NOT EXISTS employees (
    id BIGINT NOT NULL AUTO_INCREMENT,
    member_id BIGINT NULL,
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
    UNIQUE KEY uk_employees_member_id (member_id),
    UNIQUE KEY uk_employees_email (email),
    CONSTRAINT fk_employees_member
        FOREIGN KEY (member_id) REFERENCES members (id)
        ON DELETE SET NULL
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
    memo VARCHAR(255) NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_vendors_vendor_code (vendor_code),
    UNIQUE KEY uk_vendors_business_registration_no (business_registration_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS items (
    id BIGINT NOT NULL AUTO_INCREMENT,
    item_code VARCHAR(50) NOT NULL,
    name VARCHAR(100) NOT NULL,
    category VARCHAR(100) NULL,
    spec VARCHAR(100) NULL,
    barcode VARCHAR(100) NULL,
    unit VARCHAR(30) NOT NULL,
    default_vendor_id BIGINT NULL,
    unit_price DECIMAL(15,2) NOT NULL DEFAULT 0.00,
    safety_stock INT NOT NULL DEFAULT 0,
    is_active TINYINT(1) NOT NULL DEFAULT 1,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_items_item_code (item_code),
    UNIQUE KEY uk_items_barcode (barcode),
    KEY idx_items_default_vendor_id (default_vendor_id),
    CONSTRAINT fk_items_default_vendor
        FOREIGN KEY (default_vendor_id) REFERENCES vendors (id)
        ON DELETE SET NULL
        ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS inventories (
    id BIGINT NOT NULL AUTO_INCREMENT,
    item_id BIGINT NOT NULL,
    location VARCHAR(100) NOT NULL,
    quantity INT NOT NULL DEFAULT 0,
    allocated_quantity INT NOT NULL DEFAULT 0,
    safety_stock INT NOT NULL DEFAULT 0,
    lot_no VARCHAR(100) NOT NULL DEFAULT '',
    expiration_date DATE NOT NULL DEFAULT '9999-12-31',
    status VARCHAR(30) NOT NULL DEFAULT 'AVAILABLE',
    last_adjusted_at DATETIME NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_inventories_item_location_lot_expiration (item_id, location, lot_no, expiration_date),
    KEY idx_inventories_item_id (item_id),
    CONSTRAINT chk_inventories_quantity_non_negative CHECK (quantity >= 0),
    CONSTRAINT chk_inventories_allocated_quantity_non_negative CHECK (allocated_quantity >= 0),
    CONSTRAINT chk_inventories_allocated_quantity_available CHECK (allocated_quantity <= quantity),
    CONSTRAINT fk_inventories_item
        FOREIGN KEY (item_id) REFERENCES items (id)
        ON DELETE RESTRICT
        ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS purchase_orders (
    id BIGINT NOT NULL AUTO_INCREMENT,
    purchase_order_no VARCHAR(50) NOT NULL,
    vendor_id BIGINT NOT NULL,
    requested_by_member_id BIGINT NOT NULL,
    approved_by_member_id BIGINT NULL,
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
    KEY idx_purchase_orders_requested_by (requested_by_member_id),
    KEY idx_purchase_orders_approved_by (approved_by_member_id),
    CONSTRAINT fk_purchase_orders_vendor
        FOREIGN KEY (vendor_id) REFERENCES vendors (id)
        ON DELETE RESTRICT
        ON UPDATE CASCADE,
    CONSTRAINT fk_purchase_orders_requested_by
        FOREIGN KEY (requested_by_member_id) REFERENCES members (id)
        ON DELETE RESTRICT
        ON UPDATE CASCADE,
    CONSTRAINT fk_purchase_orders_approved_by
        FOREIGN KEY (approved_by_member_id) REFERENCES members (id)
        ON DELETE SET NULL
        ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS purchase_order_items (
    id BIGINT NOT NULL AUTO_INCREMENT,
    purchase_order_id BIGINT NOT NULL,
    item_id BIGINT NOT NULL,
    quantity INT NOT NULL,
    unit VARCHAR(30) NOT NULL,
    unit_price DECIMAL(15,2) NOT NULL,
    supply_amount DECIMAL(15,2) NOT NULL DEFAULT 0.00,
    tax_amount DECIMAL(15,2) NOT NULL DEFAULT 0.00,
    line_amount DECIMAL(15,2) NOT NULL,
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

CREATE TABLE IF NOT EXISTS audit_logs (
    id BIGINT NOT NULL AUTO_INCREMENT,
    member_id BIGINT NULL,
    action VARCHAR(100) NOT NULL,
    target_type VARCHAR(50) NOT NULL,
    target_id BIGINT NULL,
    details TEXT NULL,
    ip_address VARCHAR(64) NULL,
    user_agent VARCHAR(255) NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_audit_logs_member_id (member_id),
    KEY idx_audit_logs_target (target_type, target_id),
    CONSTRAINT fk_audit_logs_member
        FOREIGN KEY (member_id) REFERENCES members (id)
        ON DELETE SET NULL
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

CREATE TABLE IF NOT EXISTS expenses (
    id BIGINT NOT NULL AUTO_INCREMENT,
    employee_id BIGINT NULL,
    expense_date DATE NOT NULL,
    category VARCHAR(50) NOT NULL,
    amount DECIMAL(15,2) NOT NULL,
    description VARCHAR(255) NOT NULL,
    receipt_file_path VARCHAR(255) NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'SUBMITTED',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_expenses_employee_id (employee_id),
    KEY idx_expenses_expense_date (expense_date),
    CONSTRAINT fk_expenses_employee
        FOREIGN KEY (employee_id) REFERENCES employees (id)
        ON DELETE SET NULL
        ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS stock_movements (
    id BIGINT NOT NULL AUTO_INCREMENT,
    item_id BIGINT NOT NULL,
    inventory_id BIGINT NOT NULL,
    movement_type VARCHAR(30) NOT NULL,
    quantity INT NOT NULL,
    reference_type VARCHAR(50) NULL,
    reference_id BIGINT NULL,
    moved_at DATETIME NOT NULL,
    note VARCHAR(255) NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_stock_movements_item_id (item_id),
    KEY idx_stock_movements_inventory_id (inventory_id),
    KEY idx_stock_movements_reference (reference_type, reference_id),
    CONSTRAINT chk_stock_movements_quantity_positive CHECK (quantity > 0),
    CONSTRAINT fk_stock_movements_item
        FOREIGN KEY (item_id) REFERENCES items (id)
        ON DELETE RESTRICT
        ON UPDATE CASCADE,
    CONSTRAINT fk_stock_movements_inventory
        FOREIGN KEY (inventory_id) REFERENCES inventories (id)
        ON DELETE RESTRICT
        ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Payroll & Accounting Extensions
CREATE TABLE IF NOT EXISTS payroll_contracts (
    id BIGINT NOT NULL AUTO_INCREMENT,
    employee_id BIGINT NOT NULL,
    contract_no VARCHAR(50) NOT NULL,
    base_salary DECIMAL(15,2) NOT NULL,
    hourly_rate DECIMAL(15,2) NULL,
    contract_date DATE NOT NULL,
    expiry_date DATE NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE',
    bonus_rule TEXT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_payroll_contracts_no (contract_no),
    CONSTRAINT fk_payroll_contracts_employee FOREIGN KEY (employee_id) REFERENCES employees (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS accounts (
    id BIGINT NOT NULL AUTO_INCREMENT,
    code VARCHAR(20) NOT NULL,
    name VARCHAR(100) NOT NULL,
    type VARCHAR(30) NOT NULL,
    normal_balance VARCHAR(10) NOT NULL DEFAULT 'DEBIT',
    level INT NOT NULL DEFAULT 1,
    parent_code VARCHAR(20) NULL,
    is_active TINYINT(1) NOT NULL DEFAULT 1,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_accounts_code (code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS journal_entries (
    id BIGINT NOT NULL AUTO_INCREMENT,
    journal_no VARCHAR(50) NOT NULL,
    transaction_date DATE NOT NULL,
    description VARCHAR(255) NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'DRAFT',
    source_type VARCHAR(50) NULL,
    source_id BIGINT NULL,
    total_debit DECIMAL(15,2) NOT NULL DEFAULT 0.00,
    total_credit DECIMAL(15,2) NOT NULL DEFAULT 0.00,
    created_by VARCHAR(100) NULL,
    fiscal_year INT NULL,
    fiscal_month INT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_journal_entries_no (journal_no),
    KEY idx_journal_entries_source (source_type, source_id),
    KEY idx_journal_entries_fiscal (fiscal_year, fiscal_month),
    KEY idx_journal_entries_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS journal_items (
    id BIGINT NOT NULL AUTO_INCREMENT,
    journal_entry_id BIGINT NOT NULL,
    account_id BIGINT NOT NULL,
    debit_amount DECIMAL(15,2) NOT NULL DEFAULT 0.00,
    credit_amount DECIMAL(15,2) NOT NULL DEFAULT 0.00,
    description VARCHAR(255) NULL,
    reference_type VARCHAR(50) NULL,
    reference_id BIGINT NULL,
    PRIMARY KEY (id),
    KEY idx_journal_items_entry (journal_entry_id),
    KEY idx_journal_items_account (account_id),
    CONSTRAINT fk_journal_items_entry FOREIGN KEY (journal_entry_id) REFERENCES journal_entries (id) ON DELETE CASCADE,
    CONSTRAINT fk_journal_items_account FOREIGN KEY (account_id) REFERENCES accounts (id) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS accounting_periods (
    id BIGINT NOT NULL AUTO_INCREMENT,
    fiscal_year INT NOT NULL,
    fiscal_month INT NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'OPEN',
    closed_at DATETIME NULL,
    closed_by VARCHAR(100) NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_accounting_periods_year_month (fiscal_year, fiscal_month)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Default Chart of Accounts (ERP 湲곕낯 怨꾩젙怨쇰ぉ 泥닿퀎)
INSERT INTO accounts (code, name, type, normal_balance, level, parent_code, is_active, created_at, updated_at) VALUES
('1000', '?먯궛',           'ASSET',     'DEBIT',  1, NULL,   1, NOW(), NOW()),
('1100', '?좊룞?먯궛',        'ASSET',     'DEBIT',  2, '1000', 1, NOW(), NOW()),
('1001', '?꾧툑',           'ASSET',     'DEBIT',  3, '1100', 1, NOW(), NOW()),
('1002', '蹂댄넻?덇툑',        'ASSET',     'DEBIT',  3, '1100', 1, NOW(), NOW()),
('1003', '?ш퀬?먯궛',        'ASSET',     'DEBIT',  3, '1100', 1, NOW(), NOW()),
('1004', '?몄긽留ㅼ텧湲?,      'ASSET',     'DEBIT',  3, '1100', 1, NOW(), NOW()),
('2000', '遺梨?,           'LIABILITY', 'CREDIT', 1, NULL,   1, NOW(), NOW()),
('2100', '?좊룞遺梨?,        'LIABILITY', 'CREDIT', 2, '2000', 1, NOW(), NOW()),
('2001', '?몄긽留ㅼ엯湲?,      'LIABILITY', 'CREDIT', 3, '2100', 1, NOW(), NOW()),
('2002', '誘몄?湲됯툑',        'LIABILITY', 'CREDIT', 3, '2100', 1, NOW(), NOW()),
('2003', '誘몄?湲됰퉬??湲됱뿬)', 'LIABILITY', 'CREDIT', 3, '2100', 1, NOW(), NOW()),
('2004', '?덉닔湲?,         'LIABILITY', 'CREDIT', 3, '2100', 1, NOW(), NOW()),
('3000', '?먮낯',           'EQUITY',    'CREDIT', 1, NULL,   1, NOW(), NOW()),
('3001', '?먮낯湲?,         'EQUITY',    'CREDIT', 2, '3000', 1, NOW(), NOW()),
('3002', '?댁씡?됱뿬湲?,      'EQUITY',    'CREDIT', 2, '3000', 1, NOW(), NOW()),
('4000', '?섏씡',           'REVENUE',   'CREDIT', 1, NULL,   1, NOW(), NOW()),
('4001', '留ㅼ텧??,         'REVENUE',   'CREDIT', 2, '4000', 1, NOW(), NOW()),
('5000', '鍮꾩슜',           'EXPENSE',   'DEBIT',  1, NULL,   1, NOW(), NOW()),
('5001', '湲됱뿬',           'EXPENSE',   'DEBIT',  2, '5000', 1, NOW(), NOW()),
('5002', '留ㅼ텧?먭?',        'EXPENSE',   'DEBIT',  2, '5000', 1, NOW(), NOW()),
('5003', '蹂듬━?꾩깮鍮?,      'EXPENSE',   'DEBIT',  2, '5000', 1, NOW(), NOW())
ON DUPLICATE KEY UPDATE
    name = VALUES(name),
    type = VALUES(type),
    normal_balance = VALUES(normal_balance),
    level = VALUES(level),
    parent_code = VALUES(parent_code),
    updated_at = NOW();
