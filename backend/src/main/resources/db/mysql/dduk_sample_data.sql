-- =================================================================
-- DDUK ERP Sample Seed: Amanti tea brand shared demo data
-- =================================================================
-- Purpose
-- - fill inventory / purchase / admin / HR / accounting dashboards with Amanti-based shared demo data
-- - provide enough demand + lead-time history for purchase recommendation
-- - provide anomaly rows for admin monitoring
-- - include stable AI/RPA and accounting demo history rows in this single file
--
-- Notes
-- - this file is startup-loaded sample data for fixed shared demos
-- - it assumes bootstrap schema + task_history_schema + anomaly_log_schema exist
-- - it is written to be re-runnable for the sample keys below
-- - it keeps all demo rows in one startup-managed file for easier maintenance

-- -----------------------------------------------------------------
-- cleanup for re-run
-- -----------------------------------------------------------------
ALTER TABLE items DROP COLUMN IF EXISTS active;
ALTER TABLE stock_movements DROP FOREIGN KEY fk_stock_movements_inventory;
ALTER TABLE stock_movements DROP COLUMN inventory_id;

SET FOREIGN_KEY_CHECKS = 0;

DELETE FROM purchase_order_items
WHERE purchase_order_id IN (
    SELECT id
    FROM purchase_orders
    WHERE purchase_order_no LIKE 'PO-AMANTE-AI-%' OR purchase_order_no LIKE 'PO-AMANTI-%'
);

DELETE FROM purchase_orders
WHERE purchase_order_no LIKE 'PO-AMANTE-AI-%' OR purchase_order_no LIKE 'PO-AMANTI-%';

DELETE FROM stock_movements
WHERE reference_type = 'SAMPLE_AMANTE' OR reference_type = 'SAMPLE_AMANTI';

DELETE FROM task_history
WHERE task_id LIKE 'sample-amante-%' OR task_id LIKE 'sample-amanti-%' OR task_id LIKE 'sample-demo-%';

DELETE FROM anomaly_logs
WHERE anomaly_key LIKE 'SAMPLE_AMANTE:%' OR anomaly_key LIKE 'SAMPLE_AMANTI:%';

DELETE FROM vouchers
WHERE voucher_no LIKE 'DEMO-VCH-%';

DELETE FROM journal_items
WHERE journal_entry_id IN (
    SELECT id FROM journal_entries WHERE journal_no LIKE 'DEMO-JE-%'
);

DELETE FROM journal_entries
WHERE journal_no LIKE 'DEMO-JE-%';

DELETE FROM accounting_payroll_ledgers
WHERE created_by = 'demo-seed';

DELETE FROM accounting_closing_logs
WHERE actor = 'demo-seed';

DELETE FROM warehouse_transfer_items
WHERE transfer_id IN (
    SELECT id FROM warehouse_transfers WHERE transfer_no LIKE 'TR-DEMO-%'
);

DELETE FROM warehouse_transfers
WHERE transfer_no LIKE 'TR-DEMO-%';

DELETE FROM notice
WHERE author_id = 'admin';

DELETE FROM expenses
WHERE employee_id IN (
    SELECT id FROM employees WHERE employee_no LIKE 'EMP-%'
);

DELETE FROM attendances
WHERE employee_id IN (
    SELECT id FROM employees WHERE employee_no LIKE 'EMP-%'
);

DELETE FROM payroll_contracts
WHERE employee_id IN (
    SELECT id FROM employees WHERE employee_no LIKE 'EMP-%'
);

DELETE FROM inventories
WHERE item_id IN (
    SELECT id FROM items WHERE item_code LIKE 'AMANTE-ITEM-%' OR item_code LIKE 'AMANTI-%'
);

DELETE FROM items
WHERE item_code LIKE 'AMANTE-ITEM-%' OR item_code LIKE 'AMANTI-%';

DELETE FROM employees
WHERE employee_no LIKE 'EMP-%';

DELETE FROM vendors
WHERE vendor_code LIKE 'V-AMANTE%' OR vendor_code LIKE 'V-AMANTI%' OR vendor_code IN ('V-TEA-PACK', 'V-GLASS-WARE', 'V-JEJU-FARM', 'V-HERB-IMPORT', 'V-CAFE-MALL', 'V-LOGIS');

SET FOREIGN_KEY_CHECKS = 1;

-- -----------------------------------------------------------------
-- vendors (거래처)
-- -----------------------------------------------------------------
INSERT INTO vendors (
    vendor_code,
    business_registration_no,
    name,
    representative_name,
    business_type,
    business_item,
    contact_name,
    contact_phone,
    email,
    address,
    status,
    memo,
    created_at,
    updated_at
)
VALUES
    ('V-AMANTI', '120-88-12345', '(주)아망티', '이서준', '제조/도소매', '홍차/허브차/식음료', '박철민 과장', '02-333-4455', 'contact@amantea.co.kr', '서울 마포구 창전로 5', 'ACTIVE', '차(Tea) 전문 수입 제조 및 카페 도소매 유통사', NOW(), NOW()),
    ('V-TEA-PACK', '214-85-98765', '대한다업 패키징', '김대현', '제조', '포장재/틴캔/박스', '최준호 대리', '031-777-8899', 'sales@daehanteapack.co.kr', '경기 안산시 단원구 산단로 45', 'ACTIVE', '티백용 필터 및 패키지 전문 제조업체', NOW(), NOW()),
    ('V-GLASS-WARE', '113-81-54321', '삼우글라스', '정삼우', '제조/도소매', '다기/내열유리용기', '이은영 팀장', '02-555-6677', 'info@samwooglass.co.kr', '서울 송파구 송파대로 120', 'ACTIVE', '티포트 및 텀블러/유리식기 수입유통사', NOW(), NOW()),
    ('V-JEJU-FARM', '609-92-11223', '제주 오가닉 다원', '강성민', '농업법인', '유기농녹차/말차원료', '강현우', '064-789-0123', 'farm@jejuorganic.co.kr', '제주 서귀포시 안덕면 녹차분재로 15', 'ACTIVE', '제주 다원 직송 유기농 차 원료 재배 농가', NOW(), NOW()),
    ('V-HERB-IMPORT', '105-84-00123', '글로벌허브 트레이딩', '마이클 스미스', '무역/도매', '허브원료/수입차', '김지연 대리', '02-222-3344', 'import@globalherb.co.kr', '서울 종로구 율곡로 33', 'ACTIVE', '유럽/아프리카 허브원료 직수입 무역회사', NOW(), NOW()),
    ('V-CAFE-MALL', '220-87-55667', '메가카페 식자재', '박성하', '도소매', '프랜차이즈 납품', '이동욱 주임', '02-999-8888', 'buyer@megacafe.co.kr', '서울 강남구 역삼로 88', 'ACTIVE', '대형 프랜차이즈 및 온오프라인 납품 유통망', NOW(), NOW()),
    ('V-LOGIS', '118-85-33445', '한진종합물류', '조현민', '서비스', '물류/택배/보관', '윤승재 과장', '02-1588-4600', 'logistics@hanjin.co.kr', '서울 중구 남대문로 63', 'ACTIVE', '전사 물류 배송 및 창고 3PL 계약 파트너', NOW(), NOW())
ON DUPLICATE KEY UPDATE
    name = VALUES(name),
    representative_name = VALUES(representative_name),
    business_type = VALUES(business_type),
    business_item = VALUES(business_item),
    contact_name = VALUES(contact_name),
    contact_phone = VALUES(contact_phone),
    email = VALUES(email),
    address = VALUES(address),
    status = VALUES(status),
    memo = VALUES(memo),
    updated_at = NOW();

-- -----------------------------------------------------------------
-- warehouses (창고)
-- -----------------------------------------------------------------
INSERT INTO warehouses (
    warehouse_code,
    warehouse_name,
    location,
    manager_name,
    status,
    created_at,
    updated_at
)
VALUES
    ('WH-RAW', '자재류창고', '김포 1센터', '이재훈', 'ACTIVE', NOW(), NOW()),
    ('WH-SEASON', '시즌상품창고', '고양 2센터', '박세진', 'ACTIVE', NOW(), NOW())
ON DUPLICATE KEY UPDATE
    warehouse_name = VALUES(warehouse_name),
    location = VALUES(location),
    manager_name = VALUES(manager_name),
    status = VALUES(status),
    updated_at = NOW();

-- -----------------------------------------------------------------
-- employees / contracts (임직원 및 계약)
-- -----------------------------------------------------------------
INSERT INTO employees (
    member_id,
    employee_no,
    name,
    department,
    position,
    employment_status,
    hire_date,
    email,
    phone,
    created_at,
    updated_at
)
VALUES
    (2, 'EMP-INV-001', '창고담당 김민수', 'inventory', 'Manager', 'ACTIVE', '2023-03-04', 'inventory.manager@dduk.local', '010-2000-3000', NOW(), NOW()),
    (3, 'EMP-HR-001', '인사담당 박지원', 'hr', 'Lead', 'ACTIVE', '2022-09-01', 'hr.lead@dduk.local', '010-2000-4000', NOW(), NOW()),
    (NULL, 'EMP-OPS-001', '운영담당 최유진', 'inventory', 'Staff', 'ACTIVE', '2024-01-08', 'ops.staff@dduk.local', '010-2000-5000', NOW(), NOW()),
    (NULL, 'EMP-ACC-001', '회계담당 이은지', 'hr', 'Manager', 'ACTIVE', '2023-07-15', 'accounting.manager@dduk.local', '010-2000-6000', NOW(), NOW()),
    (NULL, 'EMP-SAL-001', '영업담당 정우성', 'sales', 'Lead', 'ACTIVE', '2021-11-20', 'sales.lead@dduk.local', '010-2000-7000', NOW(), NOW()),
    (NULL, 'EMP-DEV-001', '개발담당 홍길동', 'development', 'Senior Engineer', 'ACTIVE', '2020-05-10', 'dev.senior@dduk.local', '010-2000-8000', NOW(), NOW()),
    (NULL, 'EMP-MFG-001', '생산담당 강철수', 'manufacturing', 'Staff', 'ACTIVE', '2024-02-15', 'mfg.staff@dduk.local', '010-2000-9000', NOW(), NOW())
ON DUPLICATE KEY UPDATE
    name = VALUES(name),
    department = VALUES(department),
    position = VALUES(position),
    employment_status = VALUES(employment_status),
    hire_date = VALUES(hire_date),
    phone = VALUES(phone),
    updated_at = NOW();

INSERT INTO payroll_contracts (
    employee_id,
    contract_no,
    base_salary,
    hourly_rate,
    contract_date,
    expiry_date,
    status,
    bonus_rule,
    created_at,
    updated_at
)
VALUES
    ((SELECT id FROM employees WHERE employee_no = 'EMP-INV-001'), 'CON-DEMO-EMP-INV-001', 4200000.00, NULL, '2025-01-01', NULL, 'ACTIVE', '{"bonusRate":0.08}', NOW(), NOW()),
    ((SELECT id FROM employees WHERE employee_no = 'EMP-HR-001'), 'CON-DEMO-EMP-HR-001', 4600000.00, NULL, '2025-01-01', NULL, 'ACTIVE', '{"bonusRate":0.10}', NOW(), NOW()),
    ((SELECT id FROM employees WHERE employee_no = 'EMP-OPS-001'), 'CON-DEMO-EMP-OPS-001', 3300000.00, NULL, '2025-01-01', NULL, 'ACTIVE', '{"bonusRate":0.05}', NOW(), NOW()),
    ((SELECT id FROM employees WHERE employee_no = 'EMP-ACC-001'), 'CON-DEMO-EMP-ACC-001', 4100000.00, NULL, '2025-01-01', NULL, 'ACTIVE', '{"bonusRate":0.07}', NOW(), NOW()),
    ((SELECT id FROM employees WHERE employee_no = 'EMP-SAL-001'), 'CON-DEMO-EMP-SAL-001', 4800000.00, NULL, '2025-01-01', NULL, 'ACTIVE', '{"bonusRate":0.12}', NOW(), NOW()),
    ((SELECT id FROM employees WHERE employee_no = 'EMP-DEV-001'), 'CON-DEMO-EMP-DEV-001', 5500000.00, NULL, '2025-01-01', NULL, 'ACTIVE', '{"bonusRate":0.15}', NOW(), NOW()),
    ((SELECT id FROM employees WHERE employee_no = 'EMP-MFG-001'), 'CON-DEMO-EMP-MFG-001', 3100000.00, NULL, '2025-01-01', NULL, 'ACTIVE', '{"bonusRate":0.04}', NOW(), NOW())
ON DUPLICATE KEY UPDATE
    employee_id = VALUES(employee_id),
    base_salary = VALUES(base_salary),
    hourly_rate = VALUES(hourly_rate),
    contract_date = VALUES(contract_date),
    expiry_date = VALUES(expiry_date),
    status = VALUES(status),
    bonus_rule = VALUES(bonus_rule),
    updated_at = NOW();

-- -----------------------------------------------------------------
-- items (아망티 차 상품 마스터)
-- -----------------------------------------------------------------
INSERT INTO items (
    item_code,
    name,
    item_type,
    category,
    spec,
    unit,
    default_vendor_id,
    standard_cost,
    unit_price,
    safety_stock,
    is_active,
    created_at,
    updated_at
)
VALUES
    ('AMANTI-ITEM-001', '아망티 스리랑카 실론 홍차 BOP (100g)', 'FINISHED_GOOD', '홍차', '잎차 / BOP / 100g 지퍼백', 'EA', (SELECT id FROM vendors WHERE vendor_code = 'V-AMANTI'), 6500.00, 12000.00, 20, 1, NOW(), NOW()),
    ('AMANTI-ITEM-002', '아망티 얼그레이 클래식 삼각티백 (20입)', 'FINISHED_GOOD', '홍차', '삼각티백 / 1.5g x 20입 지퍼백', 'EA', (SELECT id FROM vendors WHERE vendor_code = 'V-AMANTI'), 4500.00, 8500.00, 25, 1, NOW(), NOW()),
    ('AMANTI-ITEM-003', '아망티 인도 아쌈 CTC 잎차 (250g)', 'FINISHED_GOOD', '홍차', '잎차 / CTC / 250g 벌크', 'EA', (SELECT id FROM vendors WHERE vendor_code = 'V-AMANTI'), 9800.00, 18000.00, 15, 1, NOW(), NOW()),
    ('AMANTI-ITEM-004', '아망티 카모마일 리프레쉬 삼각티백 (20입)', 'FINISHED_GOOD', '허브차', '삼각티백 / 1.2g x 20입 지퍼백', 'EA', (SELECT id FROM vendors WHERE vendor_code = 'V-AMANTI'), 4800.00, 9000.00, 30, 1, NOW(), NOW()),
    ('AMANTI-ITEM-005', '아망티 페퍼민트 허브차 잎차 (80g)', 'FINISHED_GOOD', '허브차', '잎차 / 페퍼민트 100% / 80g', 'EA', (SELECT id FROM vendors WHERE vendor_code = 'V-AMANTI'), 5500.00, 11000.00, 20, 1, NOW(), NOW()),
    ('AMANTI-ITEM-006', '아망티 프리미엄 루이보스 클래식 잎차 (150g)', 'FINISHED_GOOD', '허브차', '잎차 / 루이보스 100% / 150g', 'EA', (SELECT id FROM vendors WHERE vendor_code = 'V-AMANTI'), 7000.00, 13000.00, 18, 1, NOW(), NOW()),
    ('AMANTI-ITEM-007', '아망티 스윗 피치 블랙티 삼각티백 (20입)', 'FINISHED_GOOD', '블렌딩티', '삼각티백 / 복숭아향 블렌딩 / 2g x 20입', 'EA', (SELECT id FROM vendors WHERE vendor_code = 'V-AMANTI'), 5800.00, 10500.00, 15, 1, NOW(), NOW()),
    ('AMANTI-ITEM-008', '아망티 잉글리쉬 브렉퍼스트 블렌드 잎차 (100g)', 'FINISHED_GOOD', '홍차', '잎차 / 아쌈+실론 블렌드 / 100g', 'EA', (SELECT id FROM vendors WHERE vendor_code = 'V-AMANTI'), 6800.00, 12500.00, 12, 1, NOW(), NOW()),
    ('AMANTI-ITEM-009', '아망티 크림 카라멜 루이보스 블렌드 (100g)', 'FINISHED_GOOD', '블렌딩티', '잎차 / 루이보스+카라멜향 / 100g', 'EA', (SELECT id FROM vendors WHERE vendor_code = 'V-AMANTI'), 8200.00, 15000.00, 10, 1, NOW(), NOW()),
    ('AMANTI-ITEM-010', '아망티 제주 유기농 말차 가루 (100g)', 'FINISHED_GOOD', '가루차', '가루차 / 말차분말 100% / 100g캔', 'EA', (SELECT id FROM vendors WHERE vendor_code = 'V-JEJU-FARM'), 11000.00, 20000.00, 15, 1, NOW(), NOW()),
    ('AMANTI-ITEM-011', '아망티 시트러스 블라썸 꽃차 삼각티백 (20입)', 'FINISHED_GOOD', '꽃차', '삼각티백 / 귤꽃+국화 블렌드 / 20입', 'EA', (SELECT id FROM vendors WHERE vendor_code = 'V-AMANTI'), 6000.00, 11500.00, 8, 1, NOW(), NOW()),
    ('AMANTI-ITEM-012', '아망티 내열유리 티포트 (600ml)', 'FINISHED_GOOD', '차도구', '다기 / 내열유리포트 / 600ml', 'EA', (SELECT id FROM vendors WHERE vendor_code = 'V-GLASS-WARE'), 8500.00, 15000.00, 10, 1, NOW(), NOW()),
    ('AMANTI-ITEM-013', '아망티 스테인리스 티 스트레이너 거름망', 'FINISHED_GOOD', '차도구', '다기 / 거름망 / 스테인리스 304', 'EA', (SELECT id FROM vendors WHERE vendor_code = 'V-GLASS-WARE'), 3500.00, 6500.00, 12, 1, NOW(), NOW()),
    ('AMANTI-RAW-001', '아망티 삼각 티백 PLA 필터 (롤)', 'RAW_MATERIAL', '원자재', '부자재 / 생분해 PLA / 롤', 'ROLL', (SELECT id FROM vendors WHERE vendor_code = 'V-TEA-PACK'), 45000.00, 0.00, 10, 1, NOW(), NOW()),
    ('AMANTI-RAW-002', '아망티 건조 카모마일 꽃잎 (원료/kg)', 'RAW_MATERIAL', '원자재', '원재료 / 이집트산 꽃잎 / kg', 'KG', (SELECT id FROM vendors WHERE vendor_code = 'V-HERB-IMPORT'), 15000.00, 0.00, 40, 1, NOW(), NOW()),
    ('AMANTI-RAW-003', '아망티 스리랑카 홍차 원엽 (원료/kg)', 'RAW_MATERIAL', '원자재', '원재료 / 실론 홍차엽 / kg', 'KG', (SELECT id FROM vendors WHERE vendor_code = 'V-AMANTI'), 18000.00, 0.00, 50, 1, NOW(), NOW()),
    ('AMANTI-RAW-004', '아망티 티백용 알루미늄 개별 포장지 (1000매)', 'RAW_MATERIAL', '원자재', '부자재 / 알루미늄 포장재 / 1000매', 'BOX', (SELECT id FROM vendors WHERE vendor_code = 'V-TEA-PACK'), 12000.00, 0.00, 8, 1, NOW(), NOW())
ON DUPLICATE KEY UPDATE
    name = VALUES(name),
    category = VALUES(category),
    spec = VALUES(spec),
    unit = VALUES(unit),
    default_vendor_id = VALUES(default_vendor_id),
    standard_cost = VALUES(standard_cost),
    unit_price = VALUES(unit_price),
    safety_stock = VALUES(safety_stock),
    is_active = VALUES(is_active),
    updated_at = NOW();

-- -----------------------------------------------------------------
-- inventories (재고 수준 설정)
-- -----------------------------------------------------------------
INSERT INTO inventories (
    item_id,
    warehouse_id,
    location,
    current_stock,
    allocated_stock,
    safety_stock,
    average_cost,
    inventory_value,
    version,
    created_at,
    updated_at
)
VALUES
    ((SELECT id FROM items WHERE item_code = 'AMANTI-ITEM-001'), (SELECT id FROM warehouses WHERE warehouse_code = 'WH-SEASON'), 'WH-SEASON', 9, 2, 20, 6500.0000, 58500.0000, 0, NOW(), NOW()),
    ((SELECT id FROM items WHERE item_code = 'AMANTI-ITEM-002'), (SELECT id FROM warehouses WHERE warehouse_code = 'WH-SEASON'), 'WH-SEASON', 6, 1, 25, 4500.0000, 27000.0000, 0, NOW(), NOW()),
    ((SELECT id FROM items WHERE item_code = 'AMANTI-ITEM-003'), (SELECT id FROM warehouses WHERE warehouse_code = 'WH-SEASON'), 'WH-SEASON', 18, 3, 15, 9800.0000, 176400.0000, 0, NOW(), NOW()),
    ((SELECT id FROM items WHERE item_code = 'AMANTI-ITEM-004'), (SELECT id FROM warehouses WHERE warehouse_code = 'WH-SEASON'), 'WH-SEASON', 0, 0, 30, 4800.0000, 0.0000, 0, NOW(), NOW()),
    ((SELECT id FROM items WHERE item_code = 'AMANTI-ITEM-005'), (SELECT id FROM warehouses WHERE warehouse_code = 'WH-SEASON'), 'WH-SEASON', 11, 18, 20, 5500.0000, 60500.0000, 0, NOW(), NOW()),
    ((SELECT id FROM items WHERE item_code = 'AMANTI-ITEM-006'), (SELECT id FROM warehouses WHERE warehouse_code = 'WH-SEASON'), 'WH-SEASON', 24, 2, 18, 7000.0000, 168000.0000, 0, NOW(), NOW()),
    ((SELECT id FROM items WHERE item_code = 'AMANTI-ITEM-007'), (SELECT id FROM warehouses WHERE warehouse_code = 'WH-SEASON'), 'WH-SEASON', 14, 2, 15, 5800.0000, 81200.0000, 0, NOW(), NOW()),
    ((SELECT id FROM items WHERE item_code = 'AMANTI-ITEM-008'), (SELECT id FROM warehouses WHERE warehouse_code = 'WH-SEASON'), 'WH-SEASON', 25, 0, 12, 6800.0000, 170000.0000, 0, NOW(), NOW()),
    ((SELECT id FROM items WHERE item_code = 'AMANTI-ITEM-009'), (SELECT id FROM warehouses WHERE warehouse_code = 'WH-SEASON'), 'WH-SEASON', 5, 1, 10, 8200.0000, 41000.0000, 0, NOW(), NOW()),
    ((SELECT id FROM items WHERE item_code = 'AMANTI-ITEM-010'), (SELECT id FROM warehouses WHERE warehouse_code = 'WH-SEASON'), 'WH-SEASON', 16, 1, 15, 11000.0000, 176000.0000, 0, NOW(), NOW()),
    ((SELECT id FROM items WHERE item_code = 'AMANTI-ITEM-011'), (SELECT id FROM warehouses WHERE warehouse_code = 'WH-SEASON'), 'WH-SEASON', 2, 0, 8, 6000.0000, 12000.0000, 0, NOW(), NOW()),
    ((SELECT id FROM items WHERE item_code = 'AMANTI-ITEM-012'), (SELECT id FROM warehouses WHERE warehouse_code = 'WH-SEASON'), 'WH-SEASON', 12, 2, 10, 8500.0000, 102000.0000, 0, NOW(), NOW()),
    ((SELECT id FROM items WHERE item_code = 'AMANTI-ITEM-013'), (SELECT id FROM warehouses WHERE warehouse_code = 'WH-SEASON'), 'WH-SEASON', 4, 1, 12, 3500.0000, 14000.0000, 0, NOW(), NOW()),
    ((SELECT id FROM items WHERE item_code = 'AMANTI-RAW-001'), (SELECT id FROM warehouses WHERE warehouse_code = 'WH-RAW'), 'WH-RAW', 22, 0, 10, 45000.0000, 990000.0000, 0, NOW(), NOW()),
    ((SELECT id FROM items WHERE item_code = 'AMANTI-RAW-002'), (SELECT id FROM warehouses WHERE warehouse_code = 'WH-RAW'), 'WH-RAW', 120, 0, 40, 15000.0000, 1800000.0000, 0, NOW(), NOW()),
    ((SELECT id FROM items WHERE item_code = 'AMANTI-RAW-003'), (SELECT id FROM warehouses WHERE warehouse_code = 'WH-RAW'), 'WH-RAW', 4, 0, 50, 18000.0000, 72000.0000, 0, NOW(), NOW()),
    ((SELECT id FROM items WHERE item_code = 'AMANTI-RAW-004'), (SELECT id FROM warehouses WHERE warehouse_code = 'WH-RAW'), 'WH-RAW', 15, 0, 8, 12000.0000, 180000.0000, 0, NOW(), NOW())
ON DUPLICATE KEY UPDATE
    current_stock = VALUES(current_stock),
    allocated_stock = VALUES(allocated_stock),
    safety_stock = VALUES(safety_stock),
    average_cost = VALUES(average_cost),
    inventory_value = VALUES(inventory_value),
    version = VALUES(version),
    updated_at = NOW();

-- -----------------------------------------------------------------
-- purchase orders (발주 내역 설정)
-- -----------------------------------------------------------------
INSERT INTO purchase_orders (
    purchase_order_no,
    vendor_id,
    warehouse_id,
    requested_by_member_id,
    approved_by_member_id,
    order_date,
    expected_date,
    status,
    total_amount,
    note,
    created_at,
    updated_at
)
VALUES
    ('PO-AMANTI-AI-001', (SELECT id FROM vendors WHERE vendor_code = 'V-AMANTI'), (SELECT id FROM warehouses WHERE warehouse_code = 'WH-SEASON'), 2, 1, CURRENT_DATE - INTERVAL 150 DAY, CURRENT_DATE - INTERVAL 143 DAY, 'COMPLETED', 170500.00, '실론홍차 및 얼그레이 티백 초도 물량', NOW(), NOW()),
    ('PO-AMANTI-AI-002', (SELECT id FROM vendors WHERE vendor_code = 'V-AMANTI'), (SELECT id FROM warehouses WHERE warehouse_code = 'WH-SEASON'), 2, 1, CURRENT_DATE - INTERVAL 95 DAY, CURRENT_DATE - INTERVAL 88 DAY, 'RECEIVED', 223300.00, '아쌈 원엽 및 루이보스 정기 입고', NOW(), NOW()),
    ('PO-AMANTI-AI-003', (SELECT id FROM vendors WHERE vendor_code = 'V-AMANTI'), (SELECT id FROM warehouses WHERE warehouse_code = 'WH-SEASON'), 2, 1, CURRENT_DATE - INTERVAL 40 DAY, CURRENT_DATE - INTERVAL 33 DAY, 'APPROVED', 146300.00, '실론 홍차 및 페퍼민트 추가 보충', NOW(), NOW()),
    ('PO-AMANTI-AI-004', (SELECT id FROM vendors WHERE vendor_code = 'V-AMANTI'), (SELECT id FROM warehouses WHERE warehouse_code = 'WH-SEASON'), 2, 1, CURRENT_DATE - INTERVAL 18 DAY, CURRENT_DATE - INTERVAL 10 DAY, 'REQUESTED', 105600.00, '카모마일 티백 긴급 발주 요청', NOW(), NOW()),
    ('PO-AMANTI-AI-005', (SELECT id FROM vendors WHERE vendor_code = 'V-JEJU-FARM'), (SELECT id FROM warehouses WHERE warehouse_code = 'WH-SEASON'), 2, 1, CURRENT_DATE - INTERVAL 120 DAY, CURRENT_DATE - INTERVAL 115 DAY, 'COMPLETED', 181500.00, '제주 유기농 말차 가루 신규 매입', NOW(), NOW()),
    ('PO-AMANTI-AI-006', (SELECT id FROM vendors WHERE vendor_code = 'V-TEA-PACK'), (SELECT id FROM warehouses WHERE warehouse_code = 'WH-RAW'), 2, 1, CURRENT_DATE - INTERVAL 80 DAY, CURRENT_DATE - INTERVAL 75 DAY, 'COMPLETED', 379500.00, '티백 필터 및 알루미늄 포장재 보충', NOW(), NOW()),
    ('PO-AMANTI-AI-007', (SELECT id FROM vendors WHERE vendor_code = 'V-GLASS-WARE'), (SELECT id FROM warehouses WHERE warehouse_code = 'WH-SEASON'), 2, 1, CURRENT_DATE - INTERVAL 60 DAY, CURRENT_DATE - INTERVAL 55 DAY, 'COMPLETED', 217250.00, '티포트 및 티 스트레이너 세트 다기류 수입', NOW(), NOW()),
    ('PO-AMANTI-AI-008', (SELECT id FROM vendors WHERE vendor_code = 'V-HERB-IMPORT'), (SELECT id FROM warehouses WHERE warehouse_code = 'WH-RAW'), 2, 1, CURRENT_DATE - INTERVAL 20 DAY, CURRENT_DATE - INTERVAL 13 DAY, 'APPROVED', 330000.00, '이집트산 카모마일 꽃잎 수입 승인건', NOW(), NOW()),
    ('PO-AMANTI-AI-009', (SELECT id FROM vendors WHERE vendor_code = 'V-JEJU-FARM'), (SELECT id FROM warehouses WHERE warehouse_code = 'WH-SEASON'), 2, 1, CURRENT_DATE - INTERVAL 5 DAY, CURRENT_DATE + INTERVAL 2 DAY, 'REQUESTED', 121000.00, '말차 가루 재고 부족 대비 보충 발주', NOW(), NOW()),
    ('PO-AMANTI-AI-010', (SELECT id FROM vendors WHERE vendor_code = 'V-TEA-PACK'), (SELECT id FROM warehouses WHERE warehouse_code = 'WH-RAW'), 2, NULL, CURRENT_DATE - INTERVAL 12 DAY, NULL, 'REJECTED', 198000.00, '규격 미달 및 예산 초과로 인한 반려건', NOW(), NOW())
ON DUPLICATE KEY UPDATE
    vendor_id = VALUES(vendor_id),
    warehouse_id = VALUES(warehouse_id),
    requested_by_member_id = VALUES(requested_by_member_id),
    approved_by_member_id = VALUES(approved_by_member_id),
    order_date = VALUES(order_date),
    expected_date = VALUES(expected_date),
    status = VALUES(status),
    total_amount = VALUES(total_amount),
    note = VALUES(note),
    updated_at = NOW();

INSERT INTO purchase_order_items (
    purchase_order_id,
    item_id,
    quantity,
    unit,
    unit_price,
    supply_amount,
    tax_amount,
    line_amount,
    expected_date,
    note,
    created_at,
    updated_at
)
VALUES
    ((SELECT id FROM purchase_orders WHERE purchase_order_no = 'PO-AMANTI-AI-001'), (SELECT id FROM items WHERE item_code = 'AMANTI-ITEM-001'), 10, 'EA', 6500.00, 65000.00, 6500.00, 71500.00, CURRENT_DATE - INTERVAL 143 DAY, '실론홍차 BOP', NOW(), NOW()),
    ((SELECT id FROM purchase_orders WHERE purchase_order_no = 'PO-AMANTI-AI-001'), (SELECT id FROM items WHERE item_code = 'AMANTI-ITEM-002'), 20, 'EA', 4500.00, 90000.00, 9000.00, 99000.00, CURRENT_DATE - INTERVAL 143 DAY, '얼그레이 삼각티백', NOW(), NOW()),
    ((SELECT id FROM purchase_orders WHERE purchase_order_no = 'PO-AMANTI-AI-002'), (SELECT id FROM items WHERE item_code = 'AMANTI-ITEM-003'), 10, 'EA', 9800.00, 98000.00, 9800.00, 107800.00, CURRENT_DATE - INTERVAL 88 DAY, '아쌈 CTC 잎차', NOW(), NOW()),
    ((SELECT id FROM purchase_orders WHERE purchase_order_no = 'PO-AMANTI-AI-002'), (SELECT id FROM items WHERE item_code = 'AMANTI-ITEM-006'), 15, 'EA', 7000.00, 105000.00, 10500.00, 115500.00, CURRENT_DATE - INTERVAL 88 DAY, '루이보스 클래식', NOW(), NOW()),
    ((SELECT id FROM purchase_orders WHERE purchase_order_no = 'PO-AMANTI-AI-003'), (SELECT id FROM items WHERE item_code = 'AMANTI-ITEM-001'), 12, 'EA', 6500.00, 78000.00, 7800.00, 85800.00, CURRENT_DATE - INTERVAL 33 DAY, '실론홍차 추가분', NOW(), NOW()),
    ((SELECT id FROM purchase_orders WHERE purchase_order_no = 'PO-AMANTI-AI-003'), (SELECT id FROM items WHERE item_code = 'AMANTI-ITEM-005'), 10, 'EA', 5500.00, 55000.00, 5500.00, 60500.00, CURRENT_DATE - INTERVAL 33 DAY, '페퍼민트 잎차', NOW(), NOW()),
    ((SELECT id FROM purchase_orders WHERE purchase_order_no = 'PO-AMANTI-AI-004'), (SELECT id FROM items WHERE item_code = 'AMANTI-ITEM-004'), 20, 'EA', 4800.00, 96000.00, 9600.00, 105600.00, CURRENT_DATE - INTERVAL 10 DAY, '카모마일 티백 긴급', NOW(), NOW()),
    ((SELECT id FROM purchase_orders WHERE purchase_order_no = 'PO-AMANTI-AI-005'), (SELECT id FROM items WHERE item_code = 'AMANTI-ITEM-010'), 15, 'EA', 11000.00, 165000.00, 16500.00, 181500.00, CURRENT_DATE - INTERVAL 115 DAY, '말차가루 초도물량', NOW(), NOW()),
    ((SELECT id FROM purchase_orders WHERE purchase_order_no = 'PO-AMANTI-AI-006'), (SELECT id FROM items WHERE item_code = 'AMANTI-RAW-001'), 5, 'ROLL', 45000.00, 225000.00, 22500.00, 247500.00, CURRENT_DATE - INTERVAL 75 DAY, '삼각 PLA 필터', NOW(), NOW()),
    ((SELECT id FROM purchase_orders WHERE purchase_order_no = 'PO-AMANTI-AI-006'), (SELECT id FROM items WHERE item_code = 'AMANTI-RAW-004'), 10, 'BOX', 12000.00, 120000.00, 12000.00, 132000.00, CURRENT_DATE - INTERVAL 75 DAY, '알루미늄 개별포장지', NOW(), NOW()),
    ((SELECT id FROM purchase_orders WHERE purchase_order_no = 'PO-AMANTI-AI-007'), (SELECT id FROM items WHERE item_code = 'AMANTI-ITEM-012'), 15, 'EA', 8500.00, 127500.00, 12750.00, 140250.00, CURRENT_DATE - INTERVAL 55 DAY, '내열유리 티포트', NOW(), NOW()),
    ((SELECT id FROM purchase_orders WHERE purchase_order_no = 'PO-AMANTI-AI-007'), (SELECT id FROM items WHERE item_code = 'AMANTI-ITEM-013'), 20, 'EA', 3500.00, 70000.00, 7000.00, 77000.00, CURRENT_DATE - INTERVAL 55 DAY, '티 스트레이너 거름망', NOW(), NOW()),
    ((SELECT id FROM purchase_orders WHERE purchase_order_no = 'PO-AMANTI-AI-008'), (SELECT id FROM items WHERE item_code = 'AMANTI-RAW-002'), 20, 'KG', 15000.00, 300000.00, 30000.00, 330000.00, CURRENT_DATE - INTERVAL 13 DAY, '이집트산 카모마일 꽃잎', NOW(), NOW()),
    ((SELECT id FROM purchase_orders WHERE purchase_order_no = 'PO-AMANTI-AI-009'), (SELECT id FROM items WHERE item_code = 'AMANTI-ITEM-010'), 10, 'EA', 11000.00, 110000.00, 11000.00, 121000.00, CURRENT_DATE + INTERVAL 2 DAY, '추가 말차 분말', NOW(), NOW()),
    ((SELECT id FROM purchase_orders WHERE purchase_order_no = 'PO-AMANTI-AI-010'), (SELECT id FROM items WHERE item_code = 'AMANTI-RAW-001'), 4, 'ROLL', 45000.00, 180000.00, 18000.00, 198000.00, NULL, '반려된 예비 필터', NOW(), NOW());

-- -----------------------------------------------------------------
-- stock movements (수불부 기록 설정)
-- -----------------------------------------------------------------
INSERT INTO stock_movements (
    item_id,
    warehouse_id,
    movement_type,
    movement_reason,
    reference_no,
    quantity,
    unit_cost,
    total_amount,
    before_quantity,
    after_quantity,
    reference_type,
    reference_id,
    created_at
)
VALUES
    ((SELECT id FROM items WHERE item_code = 'AMANTI-ITEM-001'), (SELECT id FROM warehouses WHERE warehouse_code = 'WH-SEASON'), 'INBOUND', 'PURCHASE_RECEIVED', 'SM-AMANTI-001', 10, 6500.0000, 65000.0000, 0, 10, 'SAMPLE_AMANTI', 'AMANTI-ITEM-001-1', NOW() - INTERVAL 140 DAY),
    ((SELECT id FROM items WHERE item_code = 'AMANTI-ITEM-001'), (SELECT id FROM warehouses WHERE warehouse_code = 'WH-SEASON'), 'OUTBOUND', 'SALES_SHIPPED', 'SM-AMANTI-002', 3, 6500.0000, 19500.0000, 10, 7, 'SAMPLE_AMANTI', 'AMANTI-ITEM-001-2', NOW() - INTERVAL 28 DAY),
    ((SELECT id FROM items WHERE item_code = 'AMANTI-ITEM-001'), (SELECT id FROM warehouses WHERE warehouse_code = 'WH-SEASON'), 'OUTBOUND', 'SALES_SHIPPED', 'SM-AMANTI-003', 2, 6500.0000, 13000.0000, 7, 5, 'SAMPLE_AMANTI', 'AMANTI-ITEM-001-3', NOW() - INTERVAL 20 DAY),
    ((SELECT id FROM items WHERE item_code = 'AMANTI-ITEM-001'), (SELECT id FROM warehouses WHERE warehouse_code = 'WH-SEASON'), 'INBOUND', 'PURCHASE_RECEIVED', 'SM-AMANTI-004', 8, 6500.0000, 52000.0000, 5, 13, 'SAMPLE_AMANTI', 'AMANTI-ITEM-001-4', NOW() - INTERVAL 15 DAY),
    ((SELECT id FROM items WHERE item_code = 'AMANTI-ITEM-001'), (SELECT id FROM warehouses WHERE warehouse_code = 'WH-SEASON'), 'OUTBOUND', 'SALES_SHIPPED', 'SM-AMANTI-005', 4, 6500.0000, 26000.0000, 13, 9, 'SAMPLE_AMANTI', 'AMANTI-ITEM-001-5', NOW() - INTERVAL 7 DAY),
    ((SELECT id FROM items WHERE item_code = 'AMANTI-ITEM-002'), (SELECT id FROM warehouses WHERE warehouse_code = 'WH-SEASON'), 'INBOUND', 'PURCHASE_RECEIVED', 'SM-AMANTI-006', 20, 4500.0000, 90000.0000, 0, 20, 'SAMPLE_AMANTI', 'AMANTI-ITEM-002-1', NOW() - INTERVAL 140 DAY),
    ((SELECT id FROM items WHERE item_code = 'AMANTI-ITEM-002'), (SELECT id FROM warehouses WHERE warehouse_code = 'WH-SEASON'), 'OUTBOUND', 'SALES_SHIPPED', 'SM-AMANTI-007', 8, 4500.0000, 36000.0000, 20, 12, 'SAMPLE_AMANTI', 'AMANTI-ITEM-002-2', NOW() - INTERVAL 25 DAY),
    ((SELECT id FROM items WHERE item_code = 'AMANTI-ITEM-002'), (SELECT id FROM warehouses WHERE warehouse_code = 'WH-SEASON'), 'OUTBOUND', 'SALES_SHIPPED', 'SM-AMANTI-008', 6, 4500.0000, 27000.0000, 12, 6, 'SAMPLE_AMANTI', 'AMANTI-ITEM-002-3', NOW() - INTERVAL 5 DAY),
    ((SELECT id FROM items WHERE item_code = 'AMANTI-ITEM-005'), (SELECT id FROM warehouses WHERE warehouse_code = 'WH-SEASON'), 'INBOUND', 'PURCHASE_RECEIVED', 'SM-AMANTI-009', 20, 5500.0000, 110000.0000, 0, 20, 'SAMPLE_AMANTI', 'AMANTI-ITEM-005-1', NOW() - INTERVAL 33 DAY),
    ((SELECT id FROM items WHERE item_code = 'AMANTI-ITEM-005'), (SELECT id FROM warehouses WHERE warehouse_code = 'WH-SEASON'), 'OUTBOUND', 'SALES_SHIPPED', 'SM-AMANTI-010', 9, 5500.0000, 49500.0000, 20, 11, 'SAMPLE_AMANTI', 'AMANTI-ITEM-005-2', NOW() - INTERVAL 6 DAY),
    ((SELECT id FROM items WHERE item_code = 'AMANTI-RAW-001'), (SELECT id FROM warehouses WHERE warehouse_code = 'WH-RAW'), 'INBOUND', 'PURCHASE_RECEIVED', 'SM-AMANTI-011', 5, 45000.0000, 225000.0000, 18, 23, 'SAMPLE_AMANTI', 'AMANTI-RAW-001-1', NOW() - INTERVAL 75 DAY),
    ((SELECT id FROM items WHERE item_code = 'AMANTI-RAW-001'), (SELECT id FROM warehouses WHERE warehouse_code = 'WH-RAW'), 'OUTBOUND', 'PRODUCTION_CONSUMED', 'SM-AMANTI-012', 1, 45000.0000, 45000.0000, 23, 22, 'SAMPLE_AMANTI', 'AMANTI-RAW-001-2', NOW() - INTERVAL 10 DAY),
    ((SELECT id FROM items WHERE item_code = 'AMANTI-ITEM-010'), (SELECT id FROM warehouses WHERE warehouse_code = 'WH-SEASON'), 'INBOUND', 'PURCHASE_RECEIVED', 'SM-AMANTI-013', 15, 11000.0000, 165000.0000, 5, 20, 'SAMPLE_AMANTI', 'AMANTI-ITEM-010-1', NOW() - INTERVAL 115 DAY),
    ((SELECT id FROM items WHERE item_code = 'AMANTI-ITEM-010'), (SELECT id FROM warehouses WHERE warehouse_code = 'WH-SEASON'), 'OUTBOUND', 'SALES_SHIPPED', 'SM-AMANTI-014', 4, 11000.0000, 44000.0000, 20, 16, 'SAMPLE_AMANTI', 'AMANTI-ITEM-010-2', NOW() - INTERVAL 15 DAY);

-- -----------------------------------------------------------------
-- warehouse transfers (창고 간 재고 이동)
-- -----------------------------------------------------------------
INSERT INTO warehouse_transfers (
    transfer_no,
    source_warehouse_id,
    target_warehouse_id,
    status,
    remarks,
    requested_by_id,
    approved_by_id,
    created_at,
    updated_at,
    approved_at,
    completed_at
)
VALUES
    ('TR-DEMO-001', (SELECT id FROM warehouses WHERE warehouse_code = 'WH-RAW'), (SELECT id FROM warehouses WHERE warehouse_code = 'WH-SEASON'), 'PENDING', '티백 원단 시즌 대비 긴급 이동 요청', 2, NULL, NOW() - INTERVAL 1 DAY, NOW() - INTERVAL 1 DAY, NULL, NULL),
    ('TR-DEMO-002', (SELECT id FROM warehouses WHERE warehouse_code = 'WH-RAW'), (SELECT id FROM warehouses WHERE warehouse_code = 'WH-SEASON'), 'APPROVED', '패키징 전 대기 박스 부자재 이동', 2, 1, NOW() - INTERVAL 2 DAY, NOW() - INTERVAL 2 DAY, NOW() - INTERVAL 2 DAY + INTERVAL 2 HOUR, NULL),
    ('TR-DEMO-003', (SELECT id FROM warehouses WHERE warehouse_code = 'WH-RAW'), (SELECT id FROM warehouses WHERE warehouse_code = 'WH-SEASON'), 'COMPLETED', '홍차엽 및 허브 원료 시즌창고 정기 이동', 2, 1, NOW() - INTERVAL 5 DAY, NOW() - INTERVAL 4 DAY, NOW() - INTERVAL 5 DAY + INTERVAL 1 HOUR, NOW() - INTERVAL 4 DAY)
ON DUPLICATE KEY UPDATE
    source_warehouse_id = VALUES(source_warehouse_id),
    target_warehouse_id = VALUES(target_warehouse_id),
    status = VALUES(status),
    remarks = VALUES(remarks),
    requested_by_id = VALUES(requested_by_id),
    approved_by_id = VALUES(approved_by_id),
    updated_at = NOW();

INSERT INTO warehouse_transfer_items (
    transfer_id,
    item_id,
    quantity
)
VALUES
    ((SELECT id FROM warehouse_transfers WHERE transfer_no = 'TR-DEMO-001'), (SELECT id FROM items WHERE item_code = 'AMANTI-RAW-001'), 2),
    ((SELECT id FROM warehouse_transfers WHERE transfer_no = 'TR-DEMO-002'), (SELECT id FROM items WHERE item_code = 'AMANTI-RAW-004'), 3),
    ((SELECT id FROM warehouse_transfers WHERE transfer_no = 'TR-DEMO-003'), (SELECT id FROM items WHERE item_code = 'AMANTI-RAW-003'), 5);

-- -----------------------------------------------------------------
-- notices (사내 공지사항 설정)
-- -----------------------------------------------------------------
INSERT INTO notice (
    type,
    title,
    content,
    start_date,
    end_date,
    view_count,
    author_id,
    created_at,
    updated_at
)
VALUES
    ('SYSTEM', 'DDUK ERP 2.0 시스템 정기 업데이트 점검 안내', '안녕하세요. DDUK ERP 운영팀입니다. 시스템 성능 개선 및 보안 업데이트를 위해 2026년 6월 15일(일) 01:00부터 05:00까지 정기 점검이 진행됩니다. 점검 시간 동안은 서비스 접속이 일시 제한되오니 업무에 참고하시기 바랍니다.', CURRENT_DATE - INTERVAL 3 DAY, CURRENT_DATE + INTERVAL 10 DAY, 45, 'admin', NOW(), NOW()),
    ('GENERAL', '[인사] 2026년 하반기 전사 타운홀 미팅 개최 및 참석 요청', '임직원 여러분 안녕하십니까. 경영지원팀입니다. 당해 하반기 목표 달성 전략 공유 및 소통을 위해 전사 타운홀 미팅을 아래와 같이 개최하오니 전 임직원분들은 필히 참석해주시기 바랍니다. 일시: 2026년 6월 10일(수) 15:00, 장소: 대회의실 및 화상회의 줌 스트리밍.', CURRENT_DATE - INTERVAL 2 DAY, CURRENT_DATE + INTERVAL 7 DAY, 120, 'admin', NOW(), NOW()),
    ('EVENT', '임직원 복지몰 아망티 브랜드 특가 제휴 이벤트 안내 (최대 40% 할인)', '복리후생 지원 프로그램의 일환으로 차(Tea) 전문 수입 제조사 아망티와 임직원 전용 특가 제휴를 체결하였습니다. 아망티 공식 쇼핑몰에서 DDUK ERP 사원 인증 번호 입력 시 홍차, 허브차, 선물세트를 최대 40% 할인가에 구매하실 수 있습니다. 상세 가이드는 첨부파일을 참조하세요.', CURRENT_DATE - INTERVAL 5 DAY, CURRENT_DATE + INTERVAL 15 DAY, 310, 'admin', NOW(), NOW())
ON DUPLICATE KEY UPDATE
    title = VALUES(title),
    content = VALUES(content),
    view_count = VALUES(view_count),
    updated_at = NOW();

-- -----------------------------------------------------------------
-- anomaly logs (이상 징후 로그 설정)
-- -----------------------------------------------------------------
INSERT INTO anomaly_logs (
    anomaly_key,
    rule_code,
    severity,
    title,
    summary,
    source_type,
    source_id,
    source_label,
    payload_json,
    active,
    status,
    first_detected_at,
    last_detected_at,
    reviewed_at,
    reviewed_by,
    review_note
)
SELECT
    'SAMPLE_AMANTI:NEGATIVE_AVAILABLE_STOCK',
    'NEGATIVE_AVAILABLE_STOCK',
    'CRITICAL',
    '가용재고가 음수로 내려간 아망티 SKU',
    '예약 수량이 현재고를 초과해 출고 차질 가능성이 높습니다. 즉시 재고 정합성 확인이 필요합니다.',
    'INVENTORY',
    CAST(inv.id AS CHAR),
    CONCAT(i.name, ' / ', w.warehouse_name),
    JSON_OBJECT('currentStock', inv.current_stock, 'allocatedStock', inv.allocated_stock, 'availableStock', inv.current_stock - inv.allocated_stock, 'itemCode', i.item_code),
    b'1',
    'OPEN',
    NOW() - INTERVAL 2 DAY,
    NOW() - INTERVAL 20 MINUTE,
    NULL,
    NULL,
    NULL
FROM inventories inv
JOIN items i ON i.id = inv.item_id
JOIN warehouses w ON w.id = inv.warehouse_id
WHERE i.item_code = 'AMANTI-ITEM-005'
ON DUPLICATE KEY UPDATE
    severity = VALUES(severity),
    title = VALUES(title),
    summary = VALUES(summary),
    source_id = VALUES(source_id),
    source_label = VALUES(source_label),
    payload_json = VALUES(payload_json),
    active = VALUES(active),
    status = VALUES(status),
    last_detected_at = VALUES(last_detected_at),
    updated_at = NOW();

INSERT INTO anomaly_logs (
    anomaly_key,
    rule_code,
    severity,
    title,
    summary,
    source_type,
    source_id,
    source_label,
    payload_json,
    active,
    status,
    first_detected_at,
    last_detected_at,
    reviewed_at,
    reviewed_by,
    review_note
)
SELECT
    'SAMPLE_AMANTI:OUT_OF_STOCK_WITH_SAFETY',
    'OUT_OF_STOCK_WITH_SAFETY',
    'HIGH',
    '안전재고가 남아 있는 품절 위험 SKU',
    '현재 재고가 0인데 안전재고 기준보다 부족합니다. 긴급 보충 또는 판매 중지 판단이 필요합니다.',
    'INVENTORY',
    CAST(inv.id AS CHAR),
    CONCAT(i.name, ' / ', w.warehouse_name),
    JSON_OBJECT('currentStock', inv.current_stock, 'safetyStock', inv.safety_stock, 'availableStock', inv.current_stock - inv.allocated_stock, 'itemCode', i.item_code),
    b'1',
    'OPEN',
    NOW() - INTERVAL 2 DAY,
    NOW() - INTERVAL 40 MINUTE,
    NULL,
    NULL,
    NULL
FROM inventories inv
JOIN items i ON i.id = inv.item_id
JOIN warehouses w ON w.id = inv.warehouse_id
WHERE i.item_code = 'AMANTI-ITEM-004'
ON DUPLICATE KEY UPDATE
    severity = VALUES(severity),
    title = VALUES(title),
    summary = VALUES(summary),
    source_id = VALUES(source_id),
    source_label = VALUES(source_label),
    payload_json = VALUES(payload_json),
    active = VALUES(active),
    status = VALUES(status),
    last_detected_at = VALUES(last_detected_at),
    updated_at = NOW();

INSERT INTO anomaly_logs (
    anomaly_key,
    rule_code,
    severity,
    title,
    summary,
    source_type,
    source_id,
    source_label,
    payload_json,
    active,
    status,
    first_detected_at,
    last_detected_at,
    reviewed_at,
    reviewed_by,
    review_note
)
SELECT
    'SAMPLE_AMANTI:STOCKOUT_BEFORE_LEAD_TIME',
    'STOCKOUT_BEFORE_LEAD_TIME',
    'MEDIUM',
    '리드타임보다 먼저 재고가 소진될 위험이 있는 SKU',
    '최근 출고 속도 기준으로 아망티 실론 홍차는 리드타임 도착 전 품절 가능성이 있어 긴급 발주 검토가 필요합니다.',
    'INVENTORY',
    CAST(inv.id AS CHAR),
    CONCAT(i.name, ' / ', w.warehouse_name),
    JSON_OBJECT('availableStock', inv.current_stock - inv.allocated_stock, 'safetyStock', inv.safety_stock, 'leadTimeDays', 7, 'predictedDaysUntilStockout', 5, 'itemCode', i.item_code),
    b'1',
    'CONFIRMED',
    NOW() - INTERVAL 3 DAY,
    NOW() - INTERVAL 1 HOUR,
    NOW() - INTERVAL 40 MINUTE,
    'System Admin',
    '아망티 실론 홍차 주간 대비 발주 우선순위 상향 검토'
FROM inventories inv
JOIN items i ON i.id = inv.item_id
JOIN warehouses w ON w.id = inv.warehouse_id
WHERE i.item_code = 'AMANTI-ITEM-001'
ON DUPLICATE KEY UPDATE
    severity = VALUES(severity),
    title = VALUES(title),
    summary = VALUES(summary),
    source_id = VALUES(source_id),
    source_label = VALUES(source_label),
    payload_json = VALUES(payload_json),
    active = VALUES(active),
    status = VALUES(status),
    reviewed_at = VALUES(reviewed_at),
    reviewed_by = VALUES(reviewed_by),
    review_note = VALUES(review_note),
    last_detected_at = VALUES(last_detected_at),
    updated_at = NOW();

-- -----------------------------------------------------------------
-- task history (AI/RPA 실행 기록)
-- -----------------------------------------------------------------
INSERT INTO task_history (
    task_id,
    task_type,
    action_name,
    status,
    request_payload,
    response_payload,
    error_message,
    requested_at,
    started_at,
    callback_received_at,
    completed_at,
    created_at,
    updated_at
)
VALUES
    (
        'sample-demo-ai-001',
        'AI',
        'ai_chat_request',
        'SUCCESS',
        JSON_OBJECT('prompt', '주간 재고 위험 요약'),
        JSON_OBJECT('status', 'success', 'data', JSON_OBJECT('summary', '재고 위험 요약 초안 생성 완료')),
        NULL,
        NOW() - INTERVAL 3 DAY,
        NOW() - INTERVAL 3 DAY + INTERVAL 2 MINUTE,
        NOW() - INTERVAL 3 DAY + INTERVAL 3 MINUTE,
        NOW() - INTERVAL 3 DAY + INTERVAL 3 MINUTE,
        NOW() - INTERVAL 3 DAY,
        NOW() - INTERVAL 3 DAY + INTERVAL 3 MINUTE
    ),
    (
        'sample-demo-ai-002',
        'AI',
        'ocr_receipt_parse',
        'RUNNING',
        JSON_OBJECT('documentName', 'demo-receipt-2026-06.pdf'),
        NULL,
        NULL,
        NOW() - INTERVAL 30 MINUTE,
        NOW() - INTERVAL 29 MINUTE,
        NULL,
        NULL,
        NOW() - INTERVAL 30 MINUTE,
        NOW() - INTERVAL 29 MINUTE
    ),
    (
        'sample-demo-rpa-001',
        'RPA',
        'collect_purchase_orders',
        'SUCCESS',
        JSON_OBJECT('taskType', 'PURCHASE_PRICE'),
        JSON_OBJECT(
            'status', 'success',
            'data', JSON_OBJECT(
                'filePath', 'rpa/outputs/demo_purchase_price_sample.json',
                'collectedCount', 3
            )
        ),
        NULL,
        NOW() - INTERVAL 2 DAY,
        NOW() - INTERVAL 2 DAY + INTERVAL 1 MINUTE,
        NOW() - INTERVAL 2 DAY + INTERVAL 4 MINUTE,
        NOW() - INTERVAL 2 DAY + INTERVAL 4 MINUTE,
        NOW() - INTERVAL 2 DAY,
        NOW() - INTERVAL 2 DAY + INTERVAL 4 MINUTE
    ),
    (
        'sample-demo-rpa-002',
        'RPA',
        'check_inventory_shortage',
        'SUCCESS',
        JSON_OBJECT('taskType', 'INVENTORY_SHORTAGE'),
        JSON_OBJECT(
            'status', 'success',
            'data', JSON_OBJECT(
                'filePath', 'rpa/outputs/demo_inventory_shortage_sample.json',
                'alertCount', 3
            )
        ),
        NULL,
        NOW() - INTERVAL 1 DAY,
        NOW() - INTERVAL 1 DAY + INTERVAL 1 MINUTE,
        NOW() - INTERVAL 1 DAY + INTERVAL 2 MINUTE,
        NOW() - INTERVAL 1 DAY + INTERVAL 2 MINUTE,
        NOW() - INTERVAL 1 DAY,
        NOW() - INTERVAL 1 DAY + INTERVAL 2 MINUTE
    ),
    (
        'sample-demo-rpa-003',
        'RPA',
        'collect_hr_reference',
        'FAILED',
        JSON_OBJECT('taskType', 'HR_MIN_WAGE'),
        JSON_OBJECT('status', 'error', 'message', '데모 시드 실패 예시'),
        '데모 시드 실패 예시',
        NOW() - INTERVAL 6 HOUR,
        NOW() - INTERVAL 6 HOUR + INTERVAL 1 MINUTE,
        NOW() - INTERVAL 6 HOUR + INTERVAL 2 MINUTE,
        NOW() - INTERVAL 6 HOUR + INTERVAL 2 MINUTE,
        NOW() - INTERVAL 6 HOUR,
        NOW() - INTERVAL 6 HOUR + INTERVAL 2 MINUTE
    );

-- -----------------------------------------------------------------
-- attendances / expenses (인사 근태 및 경비 청구)
-- -----------------------------------------------------------------
INSERT INTO attendances (
    employee_id,
    work_date,
    check_in_at,
    check_out_at,
    status,
    note,
    created_at,
    updated_at
)
VALUES
    ((SELECT id FROM employees WHERE employee_no = 'EMP-INV-001'), CURRENT_DATE - INTERVAL 4 DAY, '2026-05-29 08:52:10', '2026-05-29 18:15:30', 'PRESENT', '정상 출근', NOW(), NOW()),
    ((SELECT id FROM employees WHERE employee_no = 'EMP-INV-001'), CURRENT_DATE - INTERVAL 3 DAY, '2026-05-30 08:50:00', '2026-05-30 18:05:00', 'PRESENT', '정상 출근', NOW(), NOW()),
    ((SELECT id FROM employees WHERE employee_no = 'EMP-INV-001'), CURRENT_DATE - INTERVAL 2 DAY, '2026-05-31 08:57:40', '2026-05-31 18:10:20', 'PRESENT', '정상 출근', NOW(), NOW()),
    ((SELECT id FROM employees WHERE employee_no = 'EMP-INV-001'), CURRENT_DATE - INTERVAL 1 DAY, '2026-06-01 08:48:15', '2026-06-01 18:22:00', 'PRESENT', '정상 출근', NOW(), NOW()),
    ((SELECT id FROM employees WHERE employee_no = 'EMP-INV-001'), CURRENT_DATE, '2026-06-02 08:53:00', NULL, 'PRESENT', '진행 중', NOW(), NOW()),
    ((SELECT id FROM employees WHERE employee_no = 'EMP-HR-001'), CURRENT_DATE - INTERVAL 4 DAY, '2026-05-29 08:45:00', '2026-05-29 18:01:00', 'PRESENT', '정상 출근', NOW(), NOW()),
    ((SELECT id FROM employees WHERE employee_no = 'EMP-HR-001'), CURRENT_DATE - INTERVAL 3 DAY, '2026-05-30 08:40:00', '2026-05-30 18:02:00', 'PRESENT', '정상 출근', NOW(), NOW()),
    ((SELECT id FROM employees WHERE employee_no = 'EMP-HR-001'), CURRENT_DATE - INTERVAL 2 DAY, '2026-05-31 08:55:00', '2026-05-31 18:05:00', 'PRESENT', '정상 출근', NOW(), NOW()),
    ((SELECT id FROM employees WHERE employee_no = 'EMP-HR-001'), CURRENT_DATE - INTERVAL 1 DAY, '2026-06-01 08:44:00', '2026-06-01 18:00:00', 'PRESENT', '정상 출근', NOW(), NOW()),
    ((SELECT id FROM employees WHERE employee_no = 'EMP-HR-001'), CURRENT_DATE, '2026-06-02 08:42:00', NULL, 'PRESENT', '진행 중', NOW(), NOW()),
    ((SELECT id FROM employees WHERE employee_no = 'EMP-ACC-001'), CURRENT_DATE - INTERVAL 4 DAY, '2026-05-29 09:15:00', '2026-05-29 18:10:00', 'LATE', '늦은 교통사정', NOW(), NOW()),
    ((SELECT id FROM employees WHERE employee_no = 'EMP-ACC-001'), CURRENT_DATE - INTERVAL 3 DAY, '2026-05-30 08:52:00', '2026-05-30 18:08:00', 'PRESENT', '정상 출근', NOW(), NOW()),
    ((SELECT id FROM employees WHERE employee_no = 'EMP-ACC-001'), CURRENT_DATE - INTERVAL 2 DAY, '2026-05-31 08:50:00', '2026-05-31 18:04:00', 'PRESENT', '정상 출근', NOW(), NOW()),
    ((SELECT id FROM employees WHERE employee_no = 'EMP-ACC-001'), CURRENT_DATE - INTERVAL 1 DAY, '2026-06-01 08:55:00', '2026-06-01 18:05:00', 'PRESENT', '정상 출근', NOW(), NOW()),
    ((SELECT id FROM employees WHERE employee_no = 'EMP-ACC-001'), CURRENT_DATE, '2026-06-02 08:51:00', NULL, 'PRESENT', '진행 중', NOW(), NOW())
ON DUPLICATE KEY UPDATE
    check_in_at = VALUES(check_in_at),
    check_out_at = VALUES(check_out_at),
    status = VALUES(status),
    note = VALUES(note),
    updated_at = NOW();

INSERT INTO expenses (
    employee_id,
    expense_date,
    category,
    amount,
    description,
    receipt_file_path,
    status,
    created_at,
    updated_at
)
VALUES
    ((SELECT id FROM employees WHERE employee_no = 'EMP-INV-001'), '2026-05-28', 'MEAL', 15000.00, '물류창고 야근 식대', '/uploads/receipts/inv-001-20260528.jpg', 'APPROVED', NOW(), NOW()),
    ((SELECT id FROM employees WHERE employee_no = 'EMP-HR-001'), '2026-05-29', 'OFFICE_SUPPLIES', 48000.00, '인사팀 사무용 필기구 구매', '/uploads/receipts/hr-001-20260529.jpg', 'APPROVED', NOW(), NOW()),
    ((SELECT id FROM employees WHERE employee_no = 'EMP-ACC-001'), '2026-06-01', 'TRAVEL', 12500.00, '회계팀 은행 업무 출장 택시비', '/uploads/receipts/acc-001-20260601.jpg', 'SUBMITTED', NOW(), NOW())
ON DUPLICATE KEY UPDATE
    employee_id = VALUES(employee_id),
    expense_date = VALUES(expense_date),
    category = VALUES(category),
    amount = VALUES(amount),
    description = VALUES(description),
    status = VALUES(status),
    updated_at = NOW();

-- -----------------------------------------------------------------
-- journal entries (회계 전표 및 분개 설정)
-- -----------------------------------------------------------------
INSERT INTO journal_entries (
    journal_no,
    transaction_date,
    description,
    status,
    source_type,
    source_id,
    total_debit,
    total_credit,
    created_by,
    fiscal_year,
    fiscal_month,
    created_at,
    updated_at
)
VALUES
    ('DEMO-JE-202601-SALES', '2026-01-15', '데모 1월 아망티 차 판매 매출 전표', 'POSTED', 'DEMO_SEED', NULL, 12000000.00, 12000000.00, 'demo-seed', 2026, 1, NOW() - INTERVAL 140 DAY, NOW() - INTERVAL 140 DAY),
    ('DEMO-JE-202602-SALES', '2026-02-15', '데모 2월 아망티 차 판매 매출 전표', 'POSTED', 'DEMO_SEED', NULL, 15000000.00, 15000000.00, 'demo-seed', 2026, 2, NOW() - INTERVAL 110 DAY, NOW() - INTERVAL 110 DAY),
    ('DEMO-JE-202603-SALES', '2026-03-15', '데모 3월 아망티 차 판매 매출 전표', 'POSTED', 'DEMO_SEED', NULL, 18000000.00, 18000000.00, 'demo-seed', 2026, 3, NOW() - INTERVAL 80 DAY, NOW() - INTERVAL 80 DAY),
    ('DEMO-JE-202604-SALES', '2026-04-15', '데모 4월 아망티 차 판매 매출 전표', 'POSTED', 'DEMO_SEED', NULL, 22000000.00, 22000000.00, 'demo-seed', 2026, 4, NOW() - INTERVAL 50 DAY, NOW() - INTERVAL 50 DAY),
    ('DEMO-JE-202605-SALES', '2026-05-15', '데모 5월 아망티 차 판매 매출 전표', 'POSTED', 'DEMO_SEED', NULL, 16000000.00, 16000000.00, 'demo-seed', 2026, 5, NOW() - INTERVAL 18 DAY, NOW() - INTERVAL 18 DAY),
    ('DEMO-JE-202601-EXPENSE', '2026-01-20', '데모 1월 아망티 차 수입 경비 전표', 'POSTED', 'DEMO_SEED', NULL, 3500000.00, 3500000.00, 'demo-seed', 2026, 1, NOW() - INTERVAL 135 DAY, NOW() - INTERVAL 135 DAY),
    ('DEMO-JE-202602-EXPENSE', '2026-02-20', '데모 2월 아망티 차 수입 경비 전표', 'POSTED', 'DEMO_SEED', NULL, 4200000.00, 4200000.00, 'demo-seed', 2026, 2, NOW() - INTERVAL 105 DAY, NOW() - INTERVAL 105 DAY),
    ('DEMO-JE-202603-EXPENSE', '2026-03-20', '데모 3월 아망티 차 수입 경비 전표', 'POSTED', 'DEMO_SEED', NULL, 3800000.00, 3800000.00, 'demo-seed', 2026, 3, NOW() - INTERVAL 75 DAY, NOW() - INTERVAL 75 DAY),
    ('DEMO-JE-202604-EXPENSE', '2026-04-20', '데모 4월 아망티 차 수입 경비 전표', 'POSTED', 'DEMO_SEED', NULL, 5000000.00, 5000000.00, 'demo-seed', 2026, 4, NOW() - INTERVAL 45 DAY, NOW() - INTERVAL 45 DAY),
    ('DEMO-JE-202605-EXPENSE', '2026-05-20', '데모 5월 아망티 차 수입 경비 전표', 'POSTED', 'DEMO_SEED', NULL, 2800000.00, 2800000.00, 'demo-seed', 2026, 5, NOW() - INTERVAL 15 DAY, NOW() - INTERVAL 15 DAY),
    ('DEMO-JE-202605-PAYROLL', '2026-05-25', '데모 5월 임직원 급여 충당 전표', 'POSTED', 'DEMO_SEED', NULL, 6400000.00, 6400000.00, 'demo-seed', 2026, 5, NOW() - INTERVAL 10 DAY, NOW() - INTERVAL 10 DAY)
ON DUPLICATE KEY UPDATE
    transaction_date = VALUES(transaction_date),
    description = VALUES(description),
    status = VALUES(status),
    total_debit = VALUES(total_debit),
    total_credit = VALUES(total_credit),
    updated_at = NOW();

INSERT INTO journal_items (
    journal_entry_id,
    account_id,
    amount,
    debit_amount,
    credit_amount,
    description,
    reference_type,
    reference_id
)
VALUES
    ((SELECT id FROM journal_entries WHERE journal_no = 'DEMO-JE-202601-SALES'), (SELECT id FROM accounts WHERE code = '1002'), 12000000.00, 12000000.00, 0.00, '데모 아망티 입금', 'DEMO_SEED', NULL),
    ((SELECT id FROM journal_entries WHERE journal_no = 'DEMO-JE-202601-SALES'), (SELECT id FROM accounts WHERE code = '4001'), 12000000.00, 0.00, 12000000.00, '데모 아망티 매출', 'DEMO_SEED', NULL),
    ((SELECT id FROM journal_entries WHERE journal_no = 'DEMO-JE-202602-SALES'), (SELECT id FROM accounts WHERE code = '1002'), 15000000.00, 15000000.00, 0.00, '데모 아망티 입금', 'DEMO_SEED', NULL),
    ((SELECT id FROM journal_entries WHERE journal_no = 'DEMO-JE-202602-SALES'), (SELECT id FROM accounts WHERE code = '4001'), 15000000.00, 0.00, 15000000.00, '데모 아망티 매출', 'DEMO_SEED', NULL),
    ((SELECT id FROM journal_entries WHERE journal_no = 'DEMO-JE-202603-SALES'), (SELECT id FROM accounts WHERE code = '1002'), 18000000.00, 18000000.00, 0.00, '데모 아망티 입금', 'DEMO_SEED', NULL),
    ((SELECT id FROM journal_entries WHERE journal_no = 'DEMO-JE-202603-SALES'), (SELECT id FROM accounts WHERE code = '4001'), 18000000.00, 0.00, 18000000.00, '데모 아망티 매출', 'DEMO_SEED', NULL),
    ((SELECT id FROM journal_entries WHERE journal_no = 'DEMO-JE-202604-SALES'), (SELECT id FROM accounts WHERE code = '1002'), 22000000.00, 22000000.00, 0.00, '데모 아망티 입금', 'DEMO_SEED', NULL),
    ((SELECT id FROM journal_entries WHERE journal_no = 'DEMO-JE-202604-SALES'), (SELECT id FROM accounts WHERE code = '4001'), 22000000.00, 0.00, 22000000.00, '데모 아망티 매출', 'DEMO_SEED', NULL),
    ((SELECT id FROM journal_entries WHERE journal_no = 'DEMO-JE-202605-SALES'), (SELECT id FROM accounts WHERE code = '1002'), 16000000.00, 16000000.00, 0.00, '데모 아망티 입금', 'DEMO_SEED', NULL),
    ((SELECT id FROM journal_entries WHERE journal_no = 'DEMO-JE-202605-SALES'), (SELECT id FROM accounts WHERE code = '4001'), 16000000.00, 0.00, 16000000.00, '데모 아망티 매출', 'DEMO_SEED', NULL),
    ((SELECT id FROM journal_entries WHERE journal_no = 'DEMO-JE-202601-EXPENSE'), (SELECT id FROM accounts WHERE code = '5003'), 3500000.00, 3500000.00, 0.00, '데모 아망티 수입관세', 'DEMO_SEED', NULL),
    ((SELECT id FROM journal_entries WHERE journal_no = 'DEMO-JE-202601-EXPENSE'), (SELECT id FROM accounts WHERE code = '1002'), 3500000.00, 0.00, 3500000.00, '데모 아망티 지급', 'DEMO_SEED', NULL),
    ((SELECT id FROM journal_entries WHERE journal_no = 'DEMO-JE-202602-EXPENSE'), (SELECT id FROM accounts WHERE code = '5003'), 4200000.00, 4200000.00, 0.00, '데모 아망티 수입관세', 'DEMO_SEED', NULL),
    ((SELECT id FROM journal_entries WHERE journal_no = 'DEMO-JE-202602-EXPENSE'), (SELECT id FROM accounts WHERE code = '1002'), 4200000.00, 0.00, 4200000.00, '데모 아망티 지급', 'DEMO_SEED', NULL),
    ((SELECT id FROM journal_entries WHERE journal_no = 'DEMO-JE-202603-EXPENSE'), (SELECT id FROM accounts WHERE code = '5003'), 3800000.00, 3800000.00, 0.00, '데모 아망티 수입관세', 'DEMO_SEED', NULL),
    ((SELECT id FROM journal_entries WHERE journal_no = 'DEMO-JE-202603-EXPENSE'), (SELECT id FROM accounts WHERE code = '1002'), 3800000.00, 0.00, 3800000.00, '데모 아망티 지급', 'DEMO_SEED', NULL),
    ((SELECT id FROM journal_entries WHERE journal_no = 'DEMO-JE-202604-EXPENSE'), (SELECT id FROM accounts WHERE code = '5003'), 5000000.00, 5000000.00, 0.00, '데모 아망티 수입관세', 'DEMO_SEED', NULL),
    ((SELECT id FROM journal_entries WHERE journal_no = 'DEMO-JE-202604-EXPENSE'), (SELECT id FROM accounts WHERE code = '1002'), 5000000.00, 0.00, 5000000.00, '데모 아망티 지급', 'DEMO_SEED', NULL),
    ((SELECT id FROM journal_entries WHERE journal_no = 'DEMO-JE-202605-EXPENSE'), (SELECT id FROM accounts WHERE code = '5003'), 2800000.00, 2800000.00, 0.00, '데모 아망티 수입관세', 'DEMO_SEED', NULL),
    ((SELECT id FROM journal_entries WHERE journal_no = 'DEMO-JE-202605-EXPENSE'), (SELECT id FROM accounts WHERE code = '1002'), 2800000.00, 0.00, 2800000.00, '데모 아망티 지급', 'DEMO_SEED', NULL),
    ((SELECT id FROM journal_entries WHERE journal_no = 'DEMO-JE-202605-PAYROLL'), (SELECT id FROM accounts WHERE code = '5002'), 6400000.00, 6400000.00, 0.00, '데모 아망티 임직원급여', 'DEMO_SEED', NULL),
    ((SELECT id FROM journal_entries WHERE journal_no = 'DEMO-JE-202605-PAYROLL'), (SELECT id FROM accounts WHERE code = '2002'), 6400000.00, 0.00, 6400000.00, '데모 급여 미지급금', 'DEMO_SEED', NULL);

INSERT INTO vouchers (
    voucher_no,
    voucher_date,
    voucher_type,
    vat_type,
    vendor_id,
    vendor_name_snapshot,
    status,
    source_type,
    source_reference_id,
    description,
    journal_entry_id,
    created_by,
    created_at,
    updated_at
)
VALUES
    ('DEMO-VCH-202605-001', '2026-05-12', 'SALES', 'TAX_INVOICE', NULL, '메가카페 식자재', 'POSTED', NULL, NULL, '아망티 대량 납품 매출 분개 완료', NULL, 'demo-seed', NOW() - INTERVAL 20 DAY, NOW() - INTERVAL 20 DAY),
    ('DEMO-VCH-202605-002', '2026-05-19', 'PURCHASE', 'TAX_INVOICE', NULL, '(주)아망티', 'REQUESTED', NULL, NULL, '아망티 원엽 수입 승인 대기', NULL, 'demo-seed', NOW() - INTERVAL 13 DAY, NOW() - INTERVAL 13 DAY),
    ('DEMO-VCH-202605-003', '2026-05-27', 'GENERAL', 'ZERO_TAX', NULL, '아망티 복지몰 제휴 비용', 'DRAFT', NULL, NULL, '사내 복지 이벤트 제휴 전표 조정', NULL, 'demo-seed', NOW() - INTERVAL 6 DAY, NOW() - INTERVAL 6 DAY),
    ('DEMO-VCH-202606-001', CURRENT_DATE, 'GENERAL', 'ZERO_TAX', NULL, '월간 결산 마감', 'APPROVED', NULL, NULL, '대시보드용 오늘자 마감 전표', NULL, 'demo-seed', NOW() - INTERVAL 1 HOUR, NOW() - INTERVAL 1 HOUR)
ON DUPLICATE KEY UPDATE
    voucher_date = VALUES(voucher_date),
    vendor_name_snapshot = VALUES(vendor_name_snapshot),
    status = VALUES(status),
    description = VALUES(description),
    updated_at = NOW();

-- -----------------------------------------------------------------
-- payroll ledgers / closing logs (급여대장 및 결산 로그)
-- -----------------------------------------------------------------
INSERT INTO accounting_payroll_ledgers (
    attribution_year_month,
    payroll_type,
    tax_type,
    settlement_cycle,
    target_period_mode,
    payment_date,
    payment_year_month,
    ledger_name,
    settlement_item_selection_mode,
    employee_selection_mode,
    status,
    pre_employee_checked,
    pre_insurance_calculated,
    pre_settlement_validated,
    pre_account_validated,
    head_count,
    gross_amount,
    deduction_amount,
    net_amount,
    bonus_rate_or_amount,
    journal_entry_id,
    created_by,
    created_at,
    updated_at
)
VALUES
    ('2026-03', 'SALARY', 'TAXABLE', 'MONTHLY', 'BULK', '2026-03-25', '2026-03', '데모 급여대장 2026-03', 'ALL', 'ALL', 'POSTED', 1, 1, 1, 1, 3, 12000000.00, 1400000.00, 10600000.00, NULL, NULL, 'demo-seed', NOW() - INTERVAL 70 DAY, NOW() - INTERVAL 70 DAY),
    ('2026-04', 'SALARY', 'TAXABLE', 'MONTHLY', 'BULK', '2026-04-25', '2026-04', '데모 급여대장 2026-04', 'ALL', 'ALL', 'POSTED', 1, 1, 1, 1, 3, 12200000.00, 1420000.00, 10780000.00, NULL, NULL, 'demo-seed', NOW() - INTERVAL 40 DAY, NOW() - INTERVAL 40 DAY),
    ('2026-05', 'SALARY', 'TAXABLE', 'MONTHLY', 'BULK', '2026-06-10', '2026-05', '데모 급여대장 2026-05', 'ALL', 'ALL', 'CALCULATED', 1, 1, 1, 1, 3, 12800000.00, 1500000.00, 11300000.00, NULL, NULL, 'demo-seed', NOW() - INTERVAL 5 DAY, NOW() - INTERVAL 5 DAY)
ON DUPLICATE KEY UPDATE
    ledger_name = VALUES(ledger_name),
    status = VALUES(status),
    head_count = VALUES(head_count),
    gross_amount = VALUES(gross_amount),
    deduction_amount = VALUES(deduction_amount),
    net_amount = VALUES(net_amount),
    updated_at = NOW();

INSERT INTO accounting_closing_logs (
    accounting_period_id,
    action_type,
    from_status,
    to_status,
    actor,
    ip_address,
    message,
    action_at
)
VALUES
    (
        (SELECT id FROM accounting_periods WHERE fiscal_year = 2026 AND fiscal_month = 4),
        'MONTH_CLOSED',
        'OPEN',
        'CLOSED',
        'demo-seed',
        '127.0.0.1',
        '회계 대시보드용 데모 마감 로그',
        NOW() - INTERVAL 30 DAY
    );

SELECT 1;
