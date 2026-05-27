INSERT INTO members (
    id,
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
        1,
        'admin',
        '$2a$10$yOCLiBL.jnjWNnTz8H3MBugkf7Nk73W1smvvQSclH3Gj8dgyd1ERG',
        'System Admin',
        'ADMIN',
        1,
        NOW(),
        NOW()
    ),
    (
        2,
        'inventory',
        '$2a$10$jYIYRocKBwTDRrjeOc.I9uhI8Bqzn9gHzrVT0rj/FNk9sea7Qwv.W',
        'Inventory Manager',
        'INVENTORY',
        1,
        NOW(),
        NOW()
    ),
    (
        3,
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

INSERT INTO vendors (
    id,
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
    bank_name,
    bank_account_no,
    bank_account_holder,
    status,
    memo,
    created_at,
    updated_at
)
VALUES
    (
        1,
        'V001',
        '123-45-67890',
        '(주)뚝딱물산',
        '김뚝딱',
        '도소매',
        '사무용품',
        '정대리',
        '010-1111-2222',
        'tt@dduk.com',
        '서울시 강남구 테헤란로 123',
        '국민은행',
        '110-123-456789',
        '김뚝딱',
        'ACTIVE',
        NULL,
        NOW(),
        NOW()
    ),
    (
        2,
        'V002',
        '123-45-67891',
        '(주)그린테크',
        '이그린',
        '제조',
        '컴퓨터기기',
        '김과장',
        '010-1111-2223',
        'gt@green.com',
        '서울시 서초구 반포대로 456',
        '신한은행',
        '110-123-456790',
        '이그린',
        'ACTIVE',
        NULL,
        NOW(),
        NOW()
    ),
    (
        3,
        'V003',
        '123-45-67892',
        '(주)글로벌네트웍스',
        '박네트',
        '서비스',
        '물류대행',
        '최대리',
        '010-1111-2224',
        'gn@global.com',
        '부산시 중구 중앙대로 789',
        '우리은행',
        '110-123-456791',
        '박네트',
        'ACTIVE',
        NULL,
        NOW(),
        NOW()
    ),
    (
        4,
        'V004',
        '123-45-67893',
        '뚝딱세무법인',
        '박세무',
        '전문직',
        '세무대리',
        '박세무',
        '02-123-4567',
        'tax@dduk.com',
        '서울시 영등포구 여의도대로 12',
        '하나은행',
        '110-123-456792',
        '박세무',
        'ACTIVE',
        NULL,
        NOW(),
        NOW()
    )
ON DUPLICATE KEY UPDATE
    business_registration_no = VALUES(business_registration_no),
    name = VALUES(name),
    representative_name = VALUES(representative_name),
    business_type = VALUES(business_type),
    business_item = VALUES(business_item),
    contact_name = VALUES(contact_name),
    contact_phone = VALUES(contact_phone),
    email = VALUES(email),
    address = VALUES(address),
    bank_name = VALUES(bank_name),
    bank_account_no = VALUES(bank_account_no),
    bank_account_holder = VALUES(bank_account_holder),
    status = VALUES(status),
    updated_at = NOW();

INSERT INTO warehouses (
    id,
    warehouse_code,
    warehouse_name,
    location,
    manager_name,
    status,
    created_at,
    updated_at
)
VALUES
    (1, 'WH-MAIN', '본사창고', '서울시 서초구', '김민호', 'ACTIVE', NOW(), NOW()),
    (2, 'WH-RAW', '원재료창고', '경기도 성남시', '김슬기', 'ACTIVE', NOW(), NOW()),
    (3, 'WH-FINISHED', '완제품창고', '경기도 성남시', '이영희', 'ACTIVE', NOW(), NOW()),
    (4, 'WH-COLD', '냉장창고', '인천시 서구', '최대리', 'ACTIVE', NOW(), NOW()),
    (5, 'WH-RETURN', '반품창고', '경기도 용인시', '박민수', 'ACTIVE', NOW(), NOW())
ON DUPLICATE KEY UPDATE
    warehouse_name = VALUES(warehouse_name),
    location = VALUES(location),
    manager_name = VALUES(manager_name),
    status = VALUES(status),
    updated_at = NOW();

INSERT INTO items (
    id,
    item_code,
    name,
    item_type,
    category,
    spec,
    barcode,
    unit,
    default_vendor_id,
    standard_cost,
    unit_price,
    safety_stock,
    active,
    created_at,
    updated_at
)
VALUES
    (1, 'ITM-0001', '얼그레이 티백', 'RAW_MATERIAL', '원재료', '20kg/포대', 'BARCODE-001', 'KG', 1, 5000.00, 8000.00, 500, 1, NOW(), NOW()),
    (2, 'ITM-0002', '루이보스 블렌드', 'RAW_MATERIAL', '원재료', '10kg/캔', 'BARCODE-002', 'KG', 1, 6000.00, 9000.00, 400, 1, NOW(), NOW()),
    (3, 'ITM-0003', '보리차 베이스', 'RAW_MATERIAL', '원재료', '20kg/포대', 'BARCODE-003', 'KG', 2, 3000.00, 5500.00, 300, 1, NOW(), NOW()),
    (4, 'ITM-0004', '패키지 박스', 'PACKAGING', '부자재', '500m/롤', 'BARCODE-004', 'EA', 2, 800.00, 1500.00, 1000, 1, NOW(), NOW()),
    (5, 'ITM-0005', '쇼핑백', 'PACKAGING', '부자재', '100개입/묶음', 'BARCODE-005', 'EA', 3, 400.00, 800.00, 800, 1, NOW(), NOW()),
    (6, 'ITM-0006', '정통 찹쌀떡', 'FINISHED_GOOD', '완제품', '50g*10개입', 'BARCODE-006', 'BOX', 3, 1500.00, 3000.00, 200, 1, NOW(), NOW()),
    (7, 'ITM-0007', '모듬 경단 세트', 'FINISHED_GOOD', '완제품', '400g/팩', 'BARCODE-007', 'EA', 3, 2500.00, 5000.00, 150, 1, NOW(), NOW()),
    (8, 'ITM-0008', '시향 샘플킷', 'FINISHED_GOOD', '샘플상품', '1세트', 'BARCODE-008', 'EA', 4, 1000.00, 2000.00, 50, 1, NOW(), NOW()),
    (9, 'ITM-0009', '국화차 세트', 'FINISHED_GOOD', '단종예정상품', '10팩입', 'BARCODE-009', 'BOX', 4, 4000.00, 7500.00, 10, 1, NOW(), NOW())
ON DUPLICATE KEY UPDATE
    item_code = VALUES(item_code),
    name = VALUES(name),
    item_type = VALUES(item_type),
    category = VALUES(category),
    spec = VALUES(spec),
    barcode = VALUES(barcode),
    unit = VALUES(unit),
    default_vendor_id = VALUES(default_vendor_id),
    standard_cost = VALUES(standard_cost),
    unit_price = VALUES(unit_price),
    safety_stock = VALUES(safety_stock),
    active = VALUES(active),
    updated_at = NOW();

INSERT INTO inventories (
    id,
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
    (1, 1, 1, 'WH-MAIN', 1200, 200, 500, 5000.0000, 6000000.0000, 0, NOW(), NOW()),
    (2, 1, 2, 'WH-RAW', 3000, 0, 500, 4900.0000, 14700000.0000, 0, NOW(), NOW()),
    (3, 2, 1, 'WH-MAIN', 300, 50, 400, 6000.0000, 1800000.0000, 0, NOW(), NOW()),
    (4, 3, 2, 'WH-RAW', 0, 0, 300, 3000.0000, 0.0000, 0, NOW(), NOW()),
    (5, 4, 1, 'WH-MAIN', 5000, 0, 1000, 800.0000, 4000000.0000, 0, NOW(), NOW()),
    (6, 5, 1, 'WH-MAIN', 600, 150, 800, 400.0000, 240000.0000, 0, NOW(), NOW()),
    (7, 6, 3, 'WH-FINISHED', 1500, 100, 200, 1500.0000, 2250000.0000, 0, NOW(), NOW()),
    (8, 6, 4, 'WH-COLD', 800, 0, 200, 1550.0000, 1240000.0000, 0, NOW(), NOW()),
    (9, 7, 3, 'WH-FINISHED', 100, 20, 150, 2500.0000, 250000.0000, 0, NOW(), NOW()),
    (10, 8, 1, 'WH-MAIN', 500, 0, 50, 1000.0000, 500000.0000, 0, NOW(), NOW()),
    (11, 9, 5, 'WH-RETURN', 5, 0, 10, 4000.0000, 20000.0000, 0, NOW(), NOW())
ON DUPLICATE KEY UPDATE
    location = VALUES(location),
    current_stock = VALUES(current_stock),
    allocated_stock = VALUES(allocated_stock),
    safety_stock = VALUES(safety_stock),
    average_cost = VALUES(average_cost),
    inventory_value = VALUES(inventory_value),
    updated_at = NOW();

ALTER TABLE purchase_orders MODIFY COLUMN status VARCHAR(50) NOT NULL;

INSERT INTO purchase_orders (
    id,
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
    (1, 'PO-202512-0001', 1, 1, 1, 1, '2025-12-15', '2025-12-20', 'COMPLETED', 15000000.00, '얼그레이 대량 발주 완료 건', DATE_SUB(NOW(), INTERVAL 163 DAY), DATE_SUB(NOW(), INTERVAL 163 DAY)),
    (2, 'PO-202601-0001', 2, 1, 1, 1, '2026-01-10', '2026-01-15', 'COMPLETED', 24000000.00, '패키지박스 신년 정기 공급 건', DATE_SUB(NOW(), INTERVAL 137 DAY), DATE_SUB(NOW(), INTERVAL 137 DAY)),
    (3, 'PO-202602-0001', 1, 1, 1, 1, '2026-02-12', '2026-02-18', 'COMPLETED', 18500000.00, '루이보스 물량 추가 건', DATE_SUB(NOW(), INTERVAL 104 DAY), DATE_SUB(NOW(), INTERVAL 104 DAY)),
    (4, 'PO-202603-0001', 3, 3, 1, 1, '2026-03-18', '2026-03-25', 'COMPLETED', 32000000.00, '정통 찹쌀떡 공급 계약 건', DATE_SUB(NOW(), INTERVAL 70 DAY), DATE_SUB(NOW(), INTERVAL 70 DAY)),
    (5, 'PO-202604-0001', 2, 1, 1, 1, '2026-04-20', '2026-04-25', 'COMPLETED', 28000000.00, '쇼핑백 봄맞이 정기 발주', DATE_SUB(NOW(), INTERVAL 37 DAY), DATE_SUB(NOW(), INTERVAL 37 DAY)),
    (6, 'PO-202605-0001', 1, 1, 1, 1, '2026-05-05', '2026-05-10', 'RECEIVING', 12000000.00, '얼그레이 차주 추가분', DATE_SUB(NOW(), INTERVAL 22 DAY), DATE_SUB(NOW(), INTERVAL 22 DAY)),
    (7, 'PO-202605-0002', 2, 1, 1, 1, '2026-05-10', '2026-05-20', 'INBOUND_DELAY', 15500000.00, '패키지 박스 통관 지연 건', DATE_SUB(NOW(), INTERVAL 17 DAY), DATE_SUB(NOW(), INTERVAL 17 DAY)),
    (8, 'PO-202605-0003', 3, 3, 1, NULL, '2026-05-25', '2026-05-30', 'ORDERED', 8500000.00, '경단 세트 긴급 발주 요청', DATE_SUB(NOW(), INTERVAL 2 DAY), DATE_SUB(NOW(), INTERVAL 2 DAY)),
    (9, 'PO-202605-0004', 1, 3, 1, 1, '2026-05-26', '2026-05-30', 'APPROVED', 22000000.00, '찹쌀떡 단체 주문용 대량 발주', DATE_SUB(NOW(), INTERVAL 1 DAY), DATE_SUB(NOW(), INTERVAL 1 DAY))
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
    created_at = VALUES(created_at),
    updated_at = NOW();

INSERT INTO purchase_order_items (
    id,
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
    (1, 1, 1, 1500, 'KG', 10000.00, 15000000.00, 0.00, 15000000.00, '2025-12-20', '얼그레이 티백 대량 발주', DATE_SUB(NOW(), INTERVAL 163 DAY), DATE_SUB(NOW(), INTERVAL 163 DAY)),
    (2, 2, 4, 30000, 'EA', 800.00, 24000000.00, 0.00, 24000000.00, '2026-01-15', '패키지 박스 박스 정기 발주', DATE_SUB(NOW(), INTERVAL 137 DAY), DATE_SUB(NOW(), INTERVAL 137 DAY)),
    (3, 3, 2, 3083, 'KG', 6000.00, 18500000.00, 0.00, 18500000.00, '2026-02-18', '루이보스 정기 공급', DATE_SUB(NOW(), INTERVAL 104 DAY), DATE_SUB(NOW(), INTERVAL 104 DAY)),
    (4, 4, 6, 21333, 'BOX', 1500.00, 32000000.00, 0.00, 32000000.00, '2026-03-25', '정통 찹쌀떡 납품 발주', DATE_SUB(NOW(), INTERVAL 70 DAY), DATE_SUB(NOW(), INTERVAL 70 DAY)),
    (5, 5, 5, 70000, 'EA', 400.00, 28000000.00, 0.00, 28000000.00, '2026-04-25', '쇼핑백 정기 발주', DATE_SUB(NOW(), INTERVAL 37 DAY), DATE_SUB(NOW(), INTERVAL 37 DAY)),
    (6, 6, 1, 2400, 'KG', 5000.00, 12000000.00, 0.00, 12000000.00, '2026-05-10', '얼그레이 추가 발주', DATE_SUB(NOW(), INTERVAL 22 DAY), DATE_SUB(NOW(), INTERVAL 22 DAY)),
    (7, 7, 4, 19375, 'EA', 800.00, 15500000.00, 0.00, 15500000.00, '2026-05-20', '패키지박스 추가 발주', DATE_SUB(NOW(), INTERVAL 17 DAY), DATE_SUB(NOW(), INTERVAL 17 DAY)),
    (8, 8, 7, 3400, 'EA', 2500.00, 8500000.00, 0.00, 8500000.00, '2026-05-30', '경단 세트 발주', DATE_SUB(NOW(), INTERVAL 2 DAY), DATE_SUB(NOW(), INTERVAL 2 DAY)),
    (9, 9, 6, 14666, 'BOX', 1500.00, 22000000.00, 0.00, 22000000.00, '2026-05-30', '찹쌀떡 대량 발주', DATE_SUB(NOW(), INTERVAL 1 DAY), DATE_SUB(NOW(), INTERVAL 1 DAY))
ON DUPLICATE KEY UPDATE
    purchase_order_id = VALUES(purchase_order_id),
    item_id = VALUES(item_id),
    quantity = VALUES(quantity),
    unit = VALUES(unit),
    unit_price = VALUES(unit_price),
    supply_amount = VALUES(supply_amount),
    tax_amount = VALUES(tax_amount),
    line_amount = VALUES(line_amount),
    updated_at = NOW();

INSERT INTO warehouse_transfers (
    id,
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
    (1, 'TRF-20260501-0001', 2, 1, 'COMPLETED', '원재료 창고에서 본사창고로 얼그레이 공급 완료', 1, 1, DATE_SUB(NOW(), INTERVAL 26 DAY), DATE_SUB(NOW(), INTERVAL 26 DAY), DATE_SUB(NOW(), INTERVAL 26 DAY), DATE_SUB(NOW(), INTERVAL 26 DAY)),
    (2, 'TRF-20260527-0001', 1, 3, 'PENDING', '본사창고에서 완제품창고로 찹쌀떡 공급 요청 (이동 대기)', 1, NULL, NOW(), NOW(), NULL, NULL),
    (3, 'TRF-20260526-0001', 3, 4, 'APPROVED', '완제품창고에서 냉장창고로 경단 세트 승인 완료', 1, 1, DATE_SUB(NOW(), INTERVAL 1 DAY), DATE_SUB(NOW(), INTERVAL 1 DAY), DATE_SUB(NOW(), INTERVAL 1 DAY), NULL),
    (4, 'TRF-20260525-0001', 1, 5, 'CANCELLED', '불량 쇼핑백 반품창고 이동 취소 건', 1, 1, DATE_SUB(NOW(), INTERVAL 2 DAY), DATE_SUB(NOW(), INTERVAL 2 DAY), NULL, NULL)
ON DUPLICATE KEY UPDATE
    source_warehouse_id = VALUES(source_warehouse_id),
    target_warehouse_id = VALUES(target_warehouse_id),
    status = VALUES(status),
    remarks = VALUES(remarks),
    requested_by_id = VALUES(requested_by_id),
    approved_by_id = VALUES(approved_by_id),
    created_at = VALUES(created_at),
    updated_at = VALUES(updated_at),
    approved_at = VALUES(approved_at),
    completed_at = VALUES(completed_at);

INSERT INTO warehouse_transfer_items (
    id,
    transfer_id,
    item_id,
    quantity
)
VALUES
    (1, 1, 1, 1000),
    (2, 2, 6, 100),
    (3, 3, 7, 20),
    (4, 4, 5, 50)
ON DUPLICATE KEY UPDATE
    transfer_id = VALUES(transfer_id),
    item_id = VALUES(item_id),
    quantity = VALUES(quantity);

ALTER TABLE stock_movements MODIFY COLUMN movement_type VARCHAR(50) NOT NULL;

INSERT INTO stock_movements (
    id,
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
    (1, 1, 1, 'INBOUND', 'PURCHASE_RECEIVED', 'MOV-20260303-0001', 500, 5000.0000, 2500000.0000, 0, 500, 'PURCHASE', 'PO-202602-0001', DATE_SUB(NOW(), INTERVAL 85 DAY)),
    (2, 4, 1, 'INBOUND', 'PURCHASE_RECEIVED', 'MOV-20260308-0001', 2000, 800.0000, 1600000.0000, 0, 2000, 'PURCHASE', 'PO-202601-0001', DATE_SUB(NOW(), INTERVAL 80 DAY)),
    (3, 1, 1, 'TRANSFER_OUT', 'TRANSFER', 'TRF-20260313-0001', 200, 5000.0000, 1000000.0000, 500, 300, 'TRANSFER', '1', DATE_SUB(NOW(), INTERVAL 75 DAY)),
    (4, 1, 2, 'TRANSFER_IN', 'TRANSFER', 'TRF-20260313-0001', 200, 4900.0000, 980000.0000, 0, 200, 'TRANSFER', '1', DATE_SUB(NOW(), INTERVAL 75 DAY)),
    (5, 1, 1, 'OUTBOUND', 'SALES_SHIPPED', 'MOV-20260318-0001', 100, 5000.0000, 500000.0000, 300, 200, 'SALES', 'SAL-001', DATE_SUB(NOW(), INTERVAL 70 DAY)),
    (6, 6, 3, 'OUTBOUND', 'SALES_SHIPPED', 'MOV-20260323-0001', 300, 1500.0000, 450000.0000, 2000, 1700, 'SALES', 'SAL-002', DATE_SUB(NOW(), INTERVAL 65 DAY)),
    (7, 7, 3, 'ADJUSTMENT_IN', 'MANUAL_ADJUST', 'ADJ-20260328-0001', 50, 2500.0000, 125000.0000, 150, 200, 'ADJUST', 'ADJ-001', DATE_SUB(NOW(), INTERVAL 60 DAY)),
    (8, 7, 3, 'OUTBOUND', 'SALES_SHIPPED', 'MOV-20260402-0001', 20, 2500.0000, 50000.0000, 200, 180, 'SALES', 'SAL-003', DATE_SUB(NOW(), INTERVAL 55 DAY)),
    (9, 2, 1, 'INBOUND', 'PURCHASE_RECEIVED', 'MOV-20260407-0001', 400, 6000.0000, 2400000.0000, 0, 400, 'PURCHASE', 'PO-202602-0001', DATE_SUB(NOW(), INTERVAL 50 DAY)),
    (10, 2, 1, 'OUTBOUND', 'SALES_SHIPPED', 'MOV-20260412-0001', 50, 6000.0000, 300000.0000, 400, 350, 'SALES', 'SAL-004', DATE_SUB(NOW(), INTERVAL 45 DAY)),
    (11, 5, 1, 'ADJUSTMENT_OUT', 'MANUAL_ADJUST', 'ADJ-20260417-0001', 10, 400.0000, 4000.0000, 1000, 990, 'ADJUST', 'ADJ-002', DATE_SUB(NOW(), INTERVAL 40 DAY)),
    (12, 6, 3, 'TRANSFER_OUT', 'TRANSFER', 'TRF-20260422-0001', 150, 1500.0000, 2250000.0000, 1700, 1550, 'TRANSFER', '2', DATE_SUB(NOW(), INTERVAL 35 DAY)),
    (13, 6, 4, 'TRANSFER_IN', 'TRANSFER', 'TRF-20260422-0001', 150, 1550.0000, 2325000.0000, 650, 800, 'TRANSFER', '2', DATE_SUB(NOW(), INTERVAL 35 DAY)),
    (14, 1, 1, 'OUTBOUND', 'SALES_SHIPPED', 'MOV-20260429-0001', 150, 5000.0000, 750000.0000, 200, 50, 'SALES', 'SAL-005', DATE_SUB(NOW(), INTERVAL 28 DAY)),
    (15, 6, 3, 'OUTBOUND', 'SALES_SHIPPED', 'MOV-20260502-0001', 450, 1500.0000, 675000.0000, 1550, 1100, 'SALES', 'SAL-006', DATE_SUB(NOW(), INTERVAL 25 DAY)),
    (16, 7, 3, 'OUTBOUND', 'SALES_SHIPPED', 'MOV-20260507-0001', 80, 2500.0000, 200000.0000, 180, 100, 'SALES', 'SAL-007', DATE_SUB(NOW(), INTERVAL 20 DAY)),
    (17, 1, 1, 'INBOUND', 'PURCHASE_RECEIVED', 'MOV-20260512-0001', 1000, 5000.0000, 5000000.0000, 50, 1050, 'PURCHASE', 'PO-202605-0001', DATE_SUB(NOW(), INTERVAL 15 DAY)),
    (18, 1, 2, 'TRANSFER_OUT', 'TRANSFER', 'TRF-20260501-0001', 1000, 4900.0000, 4900000.0000, 4000, 3000, 'TRANSFER', '1', DATE_SUB(NOW(), INTERVAL 26 DAY)),
    (19, 1, 1, 'TRANSFER_IN', 'TRANSFER', 'TRF-20260501-0001', 1000, 5000.0000, 5000000.0000, 1050, 2050, 'TRANSFER', '1', DATE_SUB(NOW(), INTERVAL 26 DAY)),
    (20, 4, 1, 'OUTBOUND', 'SALES_SHIPPED', 'MOV-20260517-0001', 800, 800.0000, 640000.0000, 5800, 5000, 'SALES', 'SAL-008', DATE_SUB(NOW(), INTERVAL 10 DAY)),
    (21, 5, 1, 'OUTBOUND', 'SALES_SHIPPED', 'MOV-20260519-0001', 250, 400.0000, 100000.0000, 990, 740, 'SALES', 'SAL-009', DATE_SUB(NOW(), INTERVAL 8 DAY)),
    (22, 9, 5, 'ADJUSTMENT_IN', 'MANUAL_ADJUST', 'ADJ-20260522-0001', 5, 4000.0000, 20000.0000, 0, 5, 'ADJUST', 'ADJ-003', DATE_SUB(NOW(), INTERVAL 5 DAY)),
    (23, 6, 4, 'OUTBOUND', 'SALES_SHIPPED', 'MOV-20260524-0001', 120, 1550.0000, 186000.0000, 920, 800, 'SALES', 'SAL-010', DATE_SUB(NOW(), INTERVAL 3 DAY)),
    (24, 1, 1, 'OUTBOUND', 'SALES_SHIPPED', 'MOV-20260526-0001', 80, 5000.0000, 400000.0000, 2050, 1970, 'SALES', 'SAL-011', DATE_SUB(NOW(), INTERVAL 1 DAY)),
    (25, 8, 1, 'OUTBOUND', 'SALES_SHIPPED', 'MOV-20260527-0001', 40, 1000.0000, 40000.0000, 540, 500, 'SALES', 'SAL-012', DATE_SUB(NOW(), INTERVAL 4 HOUR))
ON DUPLICATE KEY UPDATE
    item_id = VALUES(item_id),
    warehouse_id = VALUES(warehouse_id),
    movement_type = VALUES(movement_type),
    movement_reason = VALUES(movement_reason),
    reference_no = VALUES(reference_no),
    quantity = VALUES(quantity),
    unit_cost = VALUES(unit_cost),
    total_amount = VALUES(total_amount),
    before_quantity = VALUES(before_quantity),
    after_quantity = VALUES(after_quantity),
    reference_type = VALUES(reference_type),
    reference_id = VALUES(reference_id),
    created_at = VALUES(created_at);

-- ─────────────────────────────────────────────────────────────────────────────
-- 재고 ↔ 회계 자동 연동 시드 데이터
-- 재고 입출고 StockMovement 에 연결된 DRAFT 전표 및 분개 원장 예시
-- ─────────────────────────────────────────────────────────────────────────────

-- [1] accounts seed 확인 (1003 재고자산이 없으면 추가)
INSERT INTO accounts (code, name, type, normal_balance, level, status, allow_posting, system_account, deleted, sort_order)
VALUES
    ('1003', '재고자산', 'ASSET', 'DEBIT', 1, 'ACTIVE', 1, 1, 0, 30),
    ('5003', '재고손실', 'EXPENSE', 'DEBIT', 1, 'ACTIVE', 1, 1, 0, 30)
ON DUPLICATE KEY UPDATE
    name = VALUES(name),
    type = VALUES(type),
    normal_balance = VALUES(normal_balance),
    status = VALUES(status),
    allow_posting = VALUES(allow_posting),
    system_account = VALUES(system_account),
    deleted = VALUES(deleted);

-- [2] journal_entries — 재고 자동분개 원장 (DRAFT 상태)
INSERT INTO journal_entries (
    id, journal_no, transaction_date, description, status,
    source_type, source_id,
    total_debit, total_credit,
    created_by, fiscal_year, fiscal_month,
    created_at, updated_at
)
VALUES
    -- 재고 입고 (MOV-20260303-0001, 얼그레이 500개 입고)
    (101, 'JV20260303-INV001', DATE_SUB(CURDATE(), INTERVAL 85 DAY),
     '[자동] INBOUND 얼그레이 티백 500EA (본사창고)', 'DRAFT',
     'VOUCHER', 101,
     2500000.00, 2500000.00,
     'SYSTEM', YEAR(DATE_SUB(NOW(), INTERVAL 85 DAY)), MONTH(DATE_SUB(NOW(), INTERVAL 85 DAY)),
     DATE_SUB(NOW(), INTERVAL 85 DAY), DATE_SUB(NOW(), INTERVAL 85 DAY)),

    -- 재고 출고 (MOV-20260318-0001, 얼그레이 100개 출고)
    (102, 'JV20260318-INV002', DATE_SUB(CURDATE(), INTERVAL 70 DAY),
     '[자동] OUTBOUND 얼그레이 티백 100EA (본사창고)', 'DRAFT',
     'VOUCHER', 102,
     500000.00, 500000.00,
     'SYSTEM', YEAR(DATE_SUB(NOW(), INTERVAL 70 DAY)), MONTH(DATE_SUB(NOW(), INTERVAL 70 DAY)),
     DATE_SUB(NOW(), INTERVAL 70 DAY), DATE_SUB(NOW(), INTERVAL 70 DAY)),

    -- 재고 조정 증가 (ADJ-20260328-0001, 경단 세트 +50)
    (103, 'JV20260328-INV003', DATE_SUB(CURDATE(), INTERVAL 60 DAY),
     '[자동] ADJUSTMENT_IN 경단 세트 50EA (완제품창고)', 'DRAFT',
     'VOUCHER', 103,
     125000.00, 125000.00,
     'SYSTEM', YEAR(DATE_SUB(NOW(), INTERVAL 60 DAY)), MONTH(DATE_SUB(NOW(), INTERVAL 60 DAY)),
     DATE_SUB(NOW(), INTERVAL 60 DAY), DATE_SUB(NOW(), INTERVAL 60 DAY)),

    -- 재고 조정 감소 (ADJ-20260417-0001, 쇼핑백 -10 손실)
    (104, 'JV20260417-INV004', DATE_SUB(CURDATE(), INTERVAL 40 DAY),
     '[자동] ADJUSTMENT_OUT 소형 쇼핑백 10EA (본사창고) - 재고손실', 'DRAFT',
     'VOUCHER', 104,
     4000.00, 4000.00,
     'SYSTEM', YEAR(DATE_SUB(NOW(), INTERVAL 40 DAY)), MONTH(DATE_SUB(NOW(), INTERVAL 40 DAY)),
     DATE_SUB(NOW(), INTERVAL 40 DAY), DATE_SUB(NOW(), INTERVAL 40 DAY)),

    -- 최근 재고 입고 (MOV-20260512-0001, 얼그레이 1000개 대량 입고)
    (105, 'JV20260512-INV005', DATE_SUB(CURDATE(), INTERVAL 15 DAY),
     '[자동] INBOUND 얼그레이 티백 1000EA (본사창고)', 'DRAFT',
     'VOUCHER', 105,
     5000000.00, 5000000.00,
     'SYSTEM', YEAR(DATE_SUB(NOW(), INTERVAL 15 DAY)), MONTH(DATE_SUB(NOW(), INTERVAL 15 DAY)),
     DATE_SUB(NOW(), INTERVAL 15 DAY), DATE_SUB(NOW(), INTERVAL 15 DAY))
ON DUPLICATE KEY UPDATE
    description = VALUES(description),
    status = VALUES(status),
    total_debit = VALUES(total_debit),
    total_credit = VALUES(total_credit),
    updated_at = VALUES(updated_at);

-- [3] vouchers — INVENTORY 타입 DRAFT 전표 (재고 자동생성)
INSERT INTO vouchers (
    id, voucher_no, voucher_date, voucher_type, vat_type,
    vendor_id, vendor_name_snapshot,
    status, description,
    source_type, source_reference_id,
    journal_entry_id,
    created_by, created_at, updated_at
)
VALUES
    -- 입고 전표 #1: 얼그레이 500개 (stock_movement.id=1 연결)
    (101, 'V20260303-INV001', DATE_SUB(CURDATE(), INTERVAL 85 DAY),
     'INVENTORY', 'TAX_INVOICE',
     1, '(주)뚝딱물산',
     'DRAFT', '[자동] INBOUND 얼그레이 티백 500EA (본사창고) [MOV-20260303-0001]',
     'STOCK_INBOUND', 1,
     101,
     'SYSTEM', DATE_SUB(NOW(), INTERVAL 85 DAY), DATE_SUB(NOW(), INTERVAL 85 DAY)),

    -- 출고 전표 #1: 얼그레이 100개 출고 (stock_movement.id=5 연결)
    (102, 'V20260318-INV002', DATE_SUB(CURDATE(), INTERVAL 70 DAY),
     'INVENTORY', 'TAX_INVOICE',
     NULL, '내부출고',
     'DRAFT', '[자동] OUTBOUND 얼그레이 티백 100EA (본사창고) [MOV-20260318-0001]',
     'STOCK_OUTBOUND', 5,
     102,
     'SYSTEM', DATE_SUB(NOW(), INTERVAL 70 DAY), DATE_SUB(NOW(), INTERVAL 70 DAY)),

    -- 조정 증가 전표: 경단 세트 +50 (stock_movement.id=7 연결)
    (103, 'V20260328-INV003', DATE_SUB(CURDATE(), INTERVAL 60 DAY),
     'INVENTORY', 'TAX_FREE',
     NULL, '재고실사',
     'DRAFT', '[자동] ADJUSTMENT_IN 경단 세트 50EA (완제품창고) [ADJ-20260328-0001]',
     'STOCK_ADJUSTMENT_IN', 7,
     103,
     'SYSTEM', DATE_SUB(NOW(), INTERVAL 60 DAY), DATE_SUB(NOW(), INTERVAL 60 DAY)),

    -- 조정 감소 전표: 쇼핑백 손실 (stock_movement.id=11 연결)
    (104, 'V20260417-INV004', DATE_SUB(CURDATE(), INTERVAL 40 DAY),
     'INVENTORY', 'TAX_FREE',
     NULL, '재고실사',
     'DRAFT', '[자동] ADJUSTMENT_OUT 소형 쇼핑백 10EA (본사창고) - 재고손실 [ADJ-20260417-0001]',
     'STOCK_ADJUSTMENT_OUT', 11,
     104,
     'SYSTEM', DATE_SUB(NOW(), INTERVAL 40 DAY), DATE_SUB(NOW(), INTERVAL 40 DAY)),

    -- 입고 전표 #2: 얼그레이 1000개 대량 (stock_movement.id=17 연결)
    (105, 'V20260512-INV005', DATE_SUB(CURDATE(), INTERVAL 15 DAY),
     'INVENTORY', 'TAX_INVOICE',
     1, '(주)뚝딱물산',
     'DRAFT', '[자동] INBOUND 얼그레이 티백 1000EA (본사창고) [MOV-20260512-0001]',
     'STOCK_INBOUND', 17,
     105,
     'SYSTEM', DATE_SUB(NOW(), INTERVAL 15 DAY), DATE_SUB(NOW(), INTERVAL 15 DAY))
ON DUPLICATE KEY UPDATE
    description = VALUES(description),
    status = VALUES(status),
    source_type = VALUES(source_type),
    source_reference_id = VALUES(source_reference_id),
    updated_at = VALUES(updated_at);

-- [4] voucher_lines — 재고 자동분개 라인 (stock_movement_id 연결 포함)
INSERT INTO voucher_lines (
    id, voucher_id, line_no,
    account_id, account_code, account_name,
    debit_credit,
    supply_amount, vat_amount, total_amount,
    quantity, unit_price, description,
    sort_order,
    stock_movement_id, movement_type, movement_reference_no
)
SELECT
    id, voucher_id, line_no,
    account_id, account_code, account_name,
    debit_credit,
    supply_amount, vat_amount, total_amount,
    quantity, unit_price, description,
    sort_order,
    stock_movement_id, movement_type, movement_reference_no
FROM (
    -- 전표 101: 재고 입고 [차] 재고자산 / [대] 외상매입금
    SELECT 1001 AS id, 101 AS voucher_id, 1 AS line_no,
           (SELECT id FROM accounts WHERE code = '1003') AS account_id,
           '1003' AS account_code, '재고자산' AS account_name,
           'DEBIT' AS debit_credit,
           2500000.00 AS supply_amount, 0.00 AS vat_amount, 2500000.00 AS total_amount,
           500 AS quantity, 5000.0000 AS unit_price,
           '입고 얼그레이 티백 [MOV-20260303-0001]' AS description,
           1 AS sort_order,
           1 AS stock_movement_id, 'INBOUND' AS movement_type, 'MOV-20260303-0001' AS movement_reference_no
    UNION ALL
    SELECT 1002, 101, 2,
           (SELECT id FROM accounts WHERE code = '2001'),
           '2001', '외상매입금', 'CREDIT',
           2500000.00, 0.00, 2500000.00, 500, 5000.0000,
           '입고 얼그레이 티백 - 외상매입금 [MOV-20260303-0001]', 2,
           NULL, NULL, NULL

    UNION ALL
    -- 전표 102: 재고 출고 [차] 매출원가 / [대] 재고자산
    SELECT 1003, 102, 1,
           (SELECT id FROM accounts WHERE code = '5001'),
           '5001', '매출원가', 'DEBIT',
           500000.00, 0.00, 500000.00, 100, 5000.0000,
           '출고 얼그레이 티백 - 매출원가 [MOV-20260318-0001]', 1,
           5, 'OUTBOUND', 'MOV-20260318-0001'
    UNION ALL
    SELECT 1004, 102, 2,
           (SELECT id FROM accounts WHERE code = '1003'),
           '1003', '재고자산', 'CREDIT',
           500000.00, 0.00, 500000.00, 100, 5000.0000,
           '출고 얼그레이 티백 [MOV-20260318-0001]', 2,
           NULL, NULL, NULL

    UNION ALL
    -- 전표 103: 조정 증가 [차] 재고자산 / [대] 외상매입금 (임시)
    SELECT 1005, 103, 1,
           (SELECT id FROM accounts WHERE code = '1003'),
           '1003', '재고자산', 'DEBIT',
           125000.00, 0.00, 125000.00, 50, 2500.0000,
           '재고 조정 증가 경단 세트 [ADJ-20260328-0001]', 1,
           7, 'ADJUSTMENT_IN', 'ADJ-20260328-0001'
    UNION ALL
    SELECT 1006, 103, 2,
           (SELECT id FROM accounts WHERE code = '2001'),
           '2001', '외상매입금', 'CREDIT',
           125000.00, 0.00, 125000.00, 50, 2500.0000,
           '재고 조정 증가 상대계정 [ADJ-20260328-0001]', 2,
           NULL, NULL, NULL

    UNION ALL
    -- 전표 104: 조정 감소 [차] 재고손실 / [대] 재고자산
    SELECT 1007, 104, 1,
           (SELECT id FROM accounts WHERE code = '5003'),
           '5003', '재고손실', 'DEBIT',
           4000.00, 0.00, 4000.00, 10, 400.0000,
           '재고 손실 소형 쇼핑백 [ADJ-20260417-0001]', 1,
           11, 'ADJUSTMENT_OUT', 'ADJ-20260417-0001'
    UNION ALL
    SELECT 1008, 104, 2,
           (SELECT id FROM accounts WHERE code = '1003'),
           '1003', '재고자산', 'CREDIT',
           4000.00, 0.00, 4000.00, 10, 400.0000,
           '재고 손실 처리 소형 쇼핑백 [ADJ-20260417-0001]', 2,
           NULL, NULL, NULL

    UNION ALL
    -- 전표 105: 대량 입고 [차] 재고자산 / [대] 외상매입금
    SELECT 1009, 105, 1,
           (SELECT id FROM accounts WHERE code = '1003'),
           '1003', '재고자산', 'DEBIT',
           5000000.00, 0.00, 5000000.00, 1000, 5000.0000,
           '대량 입고 얼그레이 티백 [MOV-20260512-0001]', 1,
           17, 'INBOUND', 'MOV-20260512-0001'
    UNION ALL
    SELECT 1010, 105, 2,
           (SELECT id FROM accounts WHERE code = '2001'),
           '2001', '외상매입금', 'CREDIT',
           5000000.00, 0.00, 5000000.00, 1000, 5000.0000,
           '대량 입고 얼그레이 티백 - 외상매입금 [MOV-20260512-0001]', 2,
           NULL, NULL, NULL
) AS src
ON DUPLICATE KEY UPDATE
    account_code = VALUES(account_code),
    account_name = VALUES(account_name),
    debit_credit = VALUES(debit_credit),
    total_amount = VALUES(total_amount),
    description = VALUES(description),
    stock_movement_id = VALUES(stock_movement_id),
    movement_type = VALUES(movement_type),
    movement_reference_no = VALUES(movement_reference_no);

-- [5] journal_items — 재고 자동분개 원장 항목
INSERT INTO journal_items (
    id, journal_entry_id, account_id,
    debit_amount, credit_amount, description,
    reference_type, reference_id
)
SELECT
    id, journal_entry_id, account_id,
    debit_amount, credit_amount, description,
    reference_type, reference_id
FROM (
    -- journal_entry 101: 입고
    SELECT 2001 AS id, 101 AS journal_entry_id,
           (SELECT id FROM accounts WHERE code = '1003') AS account_id,
           2500000.00 AS debit_amount, 0.00 AS credit_amount,
           '입고 얼그레이 티백 [MOV-20260303-0001]' AS description,
           'VOUCHER_LINE' AS reference_type, 1001 AS reference_id
    UNION ALL
    SELECT 2002, 101,
           (SELECT id FROM accounts WHERE code = '2001'),
           0.00, 2500000.00,
           '입고 얼그레이 티백 - 외상매입금',
           'VOUCHER_LINE', 1002

    UNION ALL
    -- journal_entry 102: 출고
    SELECT 2003, 102,
           (SELECT id FROM accounts WHERE code = '5001'),
           500000.00, 0.00,
           '출고 얼그레이 티백 - 매출원가',
           'VOUCHER_LINE', 1003
    UNION ALL
    SELECT 2004, 102,
           (SELECT id FROM accounts WHERE code = '1003'),
           0.00, 500000.00,
           '출고 얼그레이 티백 - 재고자산 감소',
           'VOUCHER_LINE', 1004

    UNION ALL
    -- journal_entry 103: 조정 증가
    SELECT 2005, 103,
           (SELECT id FROM accounts WHERE code = '1003'),
           125000.00, 0.00,
           '재고 조정 증가 경단 세트',
           'VOUCHER_LINE', 1005
    UNION ALL
    SELECT 2006, 103,
           (SELECT id FROM accounts WHERE code = '2001'),
           0.00, 125000.00,
           '재고 조정 증가 상대계정',
           'VOUCHER_LINE', 1006

    UNION ALL
    -- journal_entry 104: 조정 감소
    SELECT 2007, 104,
           (SELECT id FROM accounts WHERE code = '5003'),
           4000.00, 0.00,
           '재고 손실 소형 쇼핑백',
           'VOUCHER_LINE', 1007
    UNION ALL
    SELECT 2008, 104,
           (SELECT id FROM accounts WHERE code = '1003'),
           0.00, 4000.00,
           '재고 손실 처리',
           'VOUCHER_LINE', 1008

    UNION ALL
    -- journal_entry 105: 대량 입고
    SELECT 2009, 105,
           (SELECT id FROM accounts WHERE code = '1003'),
           5000000.00, 0.00,
           '대량 입고 얼그레이 티백',
           'VOUCHER_LINE', 1009
    UNION ALL
    SELECT 2010, 105,
           (SELECT id FROM accounts WHERE code = '2001'),
           0.00, 5000000.00,
           '대량 입고 외상매입금',
           'VOUCHER_LINE', 1010
) AS src
ON DUPLICATE KEY UPDATE
    debit_amount = VALUES(debit_amount),
    credit_amount = VALUES(credit_amount),
    description = VALUES(description);
