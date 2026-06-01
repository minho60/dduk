-- =================================================================
-- DDUK ERP Sample Seed: Amante public-site based shared demo data
-- =================================================================
-- Purpose
-- - fill inventory / purchase / admin dashboards with Amante-based shared demo data
-- - provide enough demand + lead-time history for purchase recommendation
-- - provide anomaly rows for admin monitoring
-- - leave AI/RPA runtime surfaces empty until real bot executions populate them
--
-- Notes
-- - this file is startup-loaded sample data for fixed shared demos
-- - it assumes bootstrap schema + task_history_schema + anomaly_log_schema exist
-- - it is written to be re-runnable for the sample keys below
-- - it intentionally does not insert fake AI/RPA runtime history rows

-- -----------------------------------------------------------------
-- cleanup for re-run
-- -----------------------------------------------------------------
DELETE FROM purchase_order_items
WHERE purchase_order_id IN (
    SELECT id
    FROM purchase_orders
    WHERE purchase_order_no LIKE 'PO-AMANTE-AI-%'
);

DELETE FROM purchase_orders
WHERE purchase_order_no LIKE 'PO-AMANTE-AI-%';

DELETE FROM stock_movements
WHERE reference_type = 'SAMPLE_AMANTE';

DELETE FROM task_history
WHERE task_id LIKE 'sample-amante-%';

DELETE FROM anomaly_logs
WHERE anomaly_key LIKE 'SAMPLE_AMANTE:%';

-- -----------------------------------------------------------------
-- vendor / warehouse / employee
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
    memo
)
VALUES (
    'V-AMANTE',
    '110-86-24007',
    '아망티',
    '공개사이트기준',
    '도소매',
    '침구/홈패브릭',
    '아망티 온라인담당',
    '02-555-1122',
    'support@amante.co.kr',
    '서울 강남구 테헤란로 12',
    'ACTIVE',
    '공개 아망티 상품 페이지 기준 비교/예측 데모 거래처'
)
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

INSERT INTO warehouses (
    warehouse_code,
    warehouse_name,
    location,
    manager_name,
    status
)
VALUES
    ('WH-RAW', '원재료창고', '김포 1센터', '이재성', 'ACTIVE'),
    ('WH-SEASON', '시즌상품창고', '남양주 2센터', '박세진', 'ACTIVE')
ON DUPLICATE KEY UPDATE
    warehouse_name = VALUES(warehouse_name),
    location = VALUES(location),
    manager_name = VALUES(manager_name),
    status = VALUES(status),
    updated_at = NOW();

INSERT INTO employees (
    member_id,
    employee_no,
    name,
    department,
    position,
    employment_status,
    hire_date,
    email,
    phone
)
VALUES
    (2, 'EMP-INV-001', '창고담당 김민수', 'inventory', 'Manager', 'ACTIVE', '2023-03-04', 'inventory.manager@dduk.local', '010-2000-3000'),
    (3, 'EMP-HR-001', '인사담당 박지우', 'hr', 'Lead', 'ACTIVE', '2022-09-01', 'hr.lead@dduk.local', '010-2000-4000'),
    (NULL, 'EMP-OPS-001', '운영담당 최유진', 'inventory', 'Staff', 'ACTIVE', '2024-01-08', 'ops.staff@dduk.local', '010-2000-5000')
ON DUPLICATE KEY UPDATE
    name = VALUES(name),
    department = VALUES(department),
    position = VALUES(position),
    employment_status = VALUES(employment_status),
    hire_date = VALUES(hire_date),
    phone = VALUES(phone),
    updated_at = NOW();

-- -----------------------------------------------------------------
-- amante product master
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
    is_active
)
VALUES
    ('AMANTE-ITEM-001', '브리즈 시어사커 피그먼트 양면 워싱 여름 차렵이불 SS/Q/K 5colors', 'FINISHED_GOOD', '여름침구', '차렵이불 / SS-Q-K / 피그먼트 양면 워싱', 'EA', (SELECT id FROM vendors WHERE vendor_code = 'V-AMANTE'), 45200.00, 64900.00, 18, 1),
    ('AMANTE-ITEM-002', '프로방스 워싱 리플 100% 피그먼트 고정밴드 침대패드 SS/Q/K', 'FINISHED_GOOD', '침대패드', '고정밴드 침대패드 / SS-Q-K / 리플 100%', 'EA', (SELECT id FROM vendors WHERE vendor_code = 'V-AMANTE'), 37200.00, 53900.00, 14, 1),
    ('AMANTE-ITEM-003', '보다 피그먼트 여름 차렵이불 세트 SS/Q/K 5colors', 'FINISHED_GOOD', '여름침구', '세트 / SS-Q-K / 피그먼트 세트', 'EA', (SELECT id FROM vendors WHERE vendor_code = 'V-AMANTE'), 81200.00, 125900.00, 10, 1),
    ('AMANTE-ITEM-004', '보다 피그먼트 고정밴드 베개 SS/Q/K 5colors', 'FINISHED_GOOD', '베개', '고정밴드 베개 / SS-Q-K / 5colors', 'EA', (SELECT id FROM vendors WHERE vendor_code = 'V-AMANTE'), 33400.00, 54900.00, 16, 1),
    ('AMANTE-ITEM-005', '보다 피그먼트 고정밴드 침대패드 SS/Q/K 5colors', 'FINISHED_GOOD', '침대패드', '고정밴드 침대패드 / SS-Q-K / 5colors', 'EA', (SELECT id FROM vendors WHERE vendor_code = 'V-AMANTE'), 21200.00, 34900.00, 22, 1),
    ('AMANTE-ITEM-006', '코코홈 워싱 시어사커 피그먼트 양면 워싱 여름 차렵이불 SS/Q/K 4colors', 'FINISHED_GOOD', '여름침구', '차렵이불 / SS-Q-K / 시어사커 / 4colors', 'EA', (SELECT id FROM vendors WHERE vendor_code = 'V-AMANTE'), 31800.00, 48900.00, 12, 1)
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
-- inventory baseline for dashboard / recommendation / anomaly
-- -----------------------------------------------------------------
INSERT INTO inventories (
    item_id,
    warehouse_id,
    current_stock,
    allocated_stock,
    safety_stock,
    average_cost,
    inventory_value,
    version
)
VALUES
    ((SELECT id FROM items WHERE item_code = 'AMANTE-ITEM-001'), (SELECT id FROM warehouses WHERE warehouse_code = 'WH-SEASON'), 9, 2, 18, 45200.0000, 406800.0000, 0),
    ((SELECT id FROM items WHERE item_code = 'AMANTE-ITEM-002'), (SELECT id FROM warehouses WHERE warehouse_code = 'WH-SEASON'), 6, 1, 14, 37200.0000, 223200.0000, 0),
    ((SELECT id FROM items WHERE item_code = 'AMANTE-ITEM-003'), (SELECT id FROM warehouses WHERE warehouse_code = 'WH-SEASON'), 12, 4, 10, 81200.0000, 974400.0000, 0),
    ((SELECT id FROM items WHERE item_code = 'AMANTE-ITEM-004'), (SELECT id FROM warehouses WHERE warehouse_code = 'WH-RAW'), 0, 0, 16, 33400.0000, 0.0000, 0),
    ((SELECT id FROM items WHERE item_code = 'AMANTE-ITEM-005'), (SELECT id FROM warehouses WHERE warehouse_code = 'WH-RAW'), 11, 18, 22, 21200.0000, 233200.0000, 0),
    ((SELECT id FROM items WHERE item_code = 'AMANTE-ITEM-006'), (SELECT id FROM warehouses WHERE warehouse_code = 'WH-SEASON'), 24, 2, 12, 31800.0000, 763200.0000, 0)
ON DUPLICATE KEY UPDATE
    current_stock = VALUES(current_stock),
    allocated_stock = VALUES(allocated_stock),
    safety_stock = VALUES(safety_stock),
    average_cost = VALUES(average_cost),
    inventory_value = VALUES(inventory_value),
    version = VALUES(version),
    updated_at = NOW();

-- -----------------------------------------------------------------
-- purchase order history to support lead-time / demand prediction
-- -----------------------------------------------------------------
INSERT INTO purchase_orders (
    purchase_order_no,
    vendor_id,
    requested_by_member_id,
    approved_by_member_id,
    order_date,
    expected_date,
    status,
    total_amount,
    note
)
VALUES
    ('PO-AMANTE-AI-001', (SELECT id FROM vendors WHERE vendor_code = 'V-AMANTE'), 2, 1, CURRENT_DATE - INTERVAL 150 DAY, CURRENT_DATE - INTERVAL 143 DAY, 'COMPLETED', 162690.00, 'AI 데모 seed - lead time baseline 1'),
    ('PO-AMANTE-AI-002', (SELECT id FROM vendors WHERE vendor_code = 'V-AMANTE'), 2, 1, CURRENT_DATE - INTERVAL 95 DAY, CURRENT_DATE - INTERVAL 88 DAY, 'RECEIVED', 207900.00, 'AI 데모 seed - lead time baseline 2'),
    ('PO-AMANTE-AI-003', (SELECT id FROM vendors WHERE vendor_code = 'V-AMANTE'), 2, 1, CURRENT_DATE - INTERVAL 40 DAY, CURRENT_DATE - INTERVAL 33 DAY, 'APPROVED', 149490.00, 'AI 데모 seed - lead time baseline 3'),
    ('PO-AMANTE-AI-004', (SELECT id FROM vendors WHERE vendor_code = 'V-AMANTE'), 2, 1, CURRENT_DATE - INTERVAL 18 DAY, CURRENT_DATE - INTERVAL 10 DAY, 'REQUESTED', 157300.00, 'AI 데모 seed - recent replenishment plan')
ON DUPLICATE KEY UPDATE
    vendor_id = VALUES(vendor_id),
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
    note
)
VALUES
    ((SELECT id FROM purchase_orders WHERE purchase_order_no = 'PO-AMANTE-AI-001'), (SELECT id FROM items WHERE item_code = 'AMANTE-ITEM-001'), 2, 'EA', 64900.00, 129800.00, 12980.00, 142780.00, CURRENT_DATE - INTERVAL 143 DAY, 'sample amante history'),
    ((SELECT id FROM purchase_orders WHERE purchase_order_no = 'PO-AMANTE-AI-001'), (SELECT id FROM items WHERE item_code = 'AMANTE-ITEM-005'), 1, 'EA', 34900.00, 34900.00, 3490.00, 38390.00, CURRENT_DATE - INTERVAL 143 DAY, 'sample amante history'),
    ((SELECT id FROM purchase_orders WHERE purchase_order_no = 'PO-AMANTE-AI-002'), (SELECT id FROM items WHERE item_code = 'AMANTE-ITEM-002'), 3, 'EA', 53900.00, 161700.00, 16170.00, 177870.00, CURRENT_DATE - INTERVAL 88 DAY, 'sample amante history'),
    ((SELECT id FROM purchase_orders WHERE purchase_order_no = 'PO-AMANTE-AI-002'), (SELECT id FROM items WHERE item_code = 'AMANTE-ITEM-004'), 1, 'EA', 54900.00, 54900.00, 5490.00, 60390.00, CURRENT_DATE - INTERVAL 88 DAY, 'sample amante history'),
    ((SELECT id FROM purchase_orders WHERE purchase_order_no = 'PO-AMANTE-AI-003'), (SELECT id FROM items WHERE item_code = 'AMANTE-ITEM-001'), 2, 'EA', 64900.00, 129800.00, 12980.00, 142780.00, CURRENT_DATE - INTERVAL 33 DAY, 'sample amante history'),
    ((SELECT id FROM purchase_orders WHERE purchase_order_no = 'PO-AMANTE-AI-004'), (SELECT id FROM items WHERE item_code = 'AMANTE-ITEM-004'), 2, 'EA', 54900.00, 109800.00, 10980.00, 120780.00, CURRENT_DATE - INTERVAL 10 DAY, 'sample amante history'),
    ((SELECT id FROM purchase_orders WHERE purchase_order_no = 'PO-AMANTE-AI-004'), (SELECT id FROM items WHERE item_code = 'AMANTE-ITEM-005'), 1, 'EA', 36500.00, 36500.00, 3650.00, 40150.00, CURRENT_DATE - INTERVAL 10 DAY, 'sample amante history')
;

-- -----------------------------------------------------------------
-- recent stock movement pattern for AI recommendation
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
    ((SELECT id FROM items WHERE item_code = 'AMANTE-ITEM-001'), (SELECT id FROM warehouses WHERE warehouse_code = 'WH-SEASON'), 'OUTBOUND', 'SALE', 'SM-AMANTE-001', 3, 45200.0000, 135600.0000, 18, 15, 'SAMPLE_AMANTE', 'AMANTE-ITEM-001-1', NOW() - INTERVAL 28 DAY),
    ((SELECT id FROM items WHERE item_code = 'AMANTE-ITEM-001'), (SELECT id FROM warehouses WHERE warehouse_code = 'WH-SEASON'), 'OUTBOUND', 'SALE', 'SM-AMANTE-002', 2, 45200.0000, 90400.0000, 15, 13, 'SAMPLE_AMANTE', 'AMANTE-ITEM-001-2', NOW() - INTERVAL 20 DAY),
    ((SELECT id FROM items WHERE item_code = 'AMANTE-ITEM-001'), (SELECT id FROM warehouses WHERE warehouse_code = 'WH-SEASON'), 'OUTBOUND', 'SALE', 'SM-AMANTE-003', 4, 45200.0000, 180800.0000, 13, 9, 'SAMPLE_AMANTE', 'AMANTE-ITEM-001-3', NOW() - INTERVAL 7 DAY),
    ((SELECT id FROM items WHERE item_code = 'AMANTE-ITEM-002'), (SELECT id FROM warehouses WHERE warehouse_code = 'WH-SEASON'), 'OUTBOUND', 'SALE', 'SM-AMANTE-004', 2, 37200.0000, 74400.0000, 12, 10, 'SAMPLE_AMANTE', 'AMANTE-ITEM-002-1', NOW() - INTERVAL 25 DAY),
    ((SELECT id FROM items WHERE item_code = 'AMANTE-ITEM-002'), (SELECT id FROM warehouses WHERE warehouse_code = 'WH-SEASON'), 'OUTBOUND', 'SALE', 'SM-AMANTE-005', 2, 37200.0000, 74400.0000, 10, 8, 'SAMPLE_AMANTE', 'AMANTE-ITEM-002-2', NOW() - INTERVAL 15 DAY),
    ((SELECT id FROM items WHERE item_code = 'AMANTE-ITEM-002'), (SELECT id FROM warehouses WHERE warehouse_code = 'WH-SEASON'), 'OUTBOUND', 'SALE', 'SM-AMANTE-006', 2, 37200.0000, 74400.0000, 8, 6, 'SAMPLE_AMANTE', 'AMANTE-ITEM-002-3', NOW() - INTERVAL 5 DAY),
    ((SELECT id FROM items WHERE item_code = 'AMANTE-ITEM-004'), (SELECT id FROM warehouses WHERE warehouse_code = 'WH-RAW'), 'OUTBOUND', 'SALE', 'SM-AMANTE-007', 2, 33400.0000, 66800.0000, 6, 4, 'SAMPLE_AMANTE', 'AMANTE-ITEM-004-1', NOW() - INTERVAL 24 DAY),
    ((SELECT id FROM items WHERE item_code = 'AMANTE-ITEM-004'), (SELECT id FROM warehouses WHERE warehouse_code = 'WH-RAW'), 'OUTBOUND', 'SALE', 'SM-AMANTE-008', 2, 33400.0000, 66800.0000, 4, 2, 'SAMPLE_AMANTE', 'AMANTE-ITEM-004-2', NOW() - INTERVAL 14 DAY),
    ((SELECT id FROM items WHERE item_code = 'AMANTE-ITEM-004'), (SELECT id FROM warehouses WHERE warehouse_code = 'WH-RAW'), 'OUTBOUND', 'SALE', 'SM-AMANTE-009', 2, 33400.0000, 66800.0000, 2, 0, 'SAMPLE_AMANTE', 'AMANTE-ITEM-004-3', NOW() - INTERVAL 4 DAY),
    ((SELECT id FROM items WHERE item_code = 'AMANTE-ITEM-005'), (SELECT id FROM warehouses WHERE warehouse_code = 'WH-RAW'), 'OUTBOUND', 'SALE', 'SM-AMANTE-010', 4, 21200.0000, 84800.0000, 23, 19, 'SAMPLE_AMANTE', 'AMANTE-ITEM-005-1', NOW() - INTERVAL 27 DAY),
    ((SELECT id FROM items WHERE item_code = 'AMANTE-ITEM-005'), (SELECT id FROM warehouses WHERE warehouse_code = 'WH-RAW'), 'OUTBOUND', 'SALE', 'SM-AMANTE-011', 4, 21200.0000, 84800.0000, 19, 15, 'SAMPLE_AMANTE', 'AMANTE-ITEM-005-2', NOW() - INTERVAL 16 DAY),
    ((SELECT id FROM items WHERE item_code = 'AMANTE-ITEM-005'), (SELECT id FROM warehouses WHERE warehouse_code = 'WH-RAW'), 'OUTBOUND', 'SALE', 'SM-AMANTE-012', 4, 21200.0000, 84800.0000, 15, 11, 'SAMPLE_AMANTE', 'AMANTE-ITEM-005-3', NOW() - INTERVAL 6 DAY)
;

-- -----------------------------------------------------------------
-- anomaly rows for admin anomaly dashboard
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
    'SAMPLE_AMANTE:NEGATIVE_AVAILABLE_STOCK',
    'NEGATIVE_AVAILABLE_STOCK',
    'CRITICAL',
    '가용재고가 음수로 내려간 아망티 SKU',
    '예약 수량이 현재고를 초과하여 출고 차질 가능성이 높아. 즉시 재고 정합성 확인이 필요해.',
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
WHERE i.item_code = 'AMANTE-ITEM-005'
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
    'SAMPLE_AMANTE:OUT_OF_STOCK_WITH_SAFETY',
    'OUT_OF_STOCK_WITH_SAFETY',
    'HIGH',
    '안전재고가 있는데 품절된 아망티 베개 SKU',
    '현재 재고가 0인데 안전재고 기준이 남아 있어서 긴급 보충 또는 판매 중지 판단이 필요해.',
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
WHERE i.item_code = 'AMANTE-ITEM-004'
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
    'SAMPLE_AMANTE:STOCKOUT_BEFORE_LEAD_TIME',
    'STOCKOUT_BEFORE_LEAD_TIME',
    'MEDIUM',
    '리드타임보다 먼저 재고가 소진될 위험이 있는 SKU',
    '최근 출고 속도 기준으로 시리즈 차렵이불은 리드타임 내 도착 전 품절 가능성이 있어 긴급 발주 검토가 필요해.',
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
    '아망티 여름 행사 주간 대비 발주 우선순위 영향 검토'
FROM inventories inv
JOIN items i ON i.id = inv.item_id
JOIN warehouses w ON w.id = inv.warehouse_id
WHERE i.item_code = 'AMANTE-ITEM-001'
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

SELECT 1;
