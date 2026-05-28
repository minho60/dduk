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
    (1, 'ITM-0001', '원자재 찹쌀가루', 'RAW_MATERIAL', '원재료', '20kg/포대 [공급처: (주)뚝딱물산, 위치: WH-RAW-A1, LOT: LOT-202605-A01]', 'BARCODE-001', 'KG', 1, 1200.00, 1800.00, 500, 1, DATE_SUB(NOW(), INTERVAL 90 DAY), DATE_SUB(NOW(), INTERVAL 90 DAY)),
    (2, 'ITM-0002', '팥 앙금(국산)', 'RAW_MATERIAL', '원재료', '10kg/캔 [공급처: (주)뚝딱물산, 위치: WH-RAW-A2, LOT: LOT-202605-A02]', 'BARCODE-002', 'KG', 1, 3500.00, 4800.00, 200, 1, DATE_SUB(NOW(), INTERVAL 90 DAY), DATE_SUB(NOW(), INTERVAL 90 DAY)),
    (3, 'ITM-0003', '유기농 설탕', 'RAW_MATERIAL', '원재료', '15kg/포대 [공급처: (주)그린테크, 위치: WH-RAW-A3, LOT: LOT-202605-A03]', 'BARCODE-003', 'KG', 2, 1000.00, 1500.00, 300, 1, DATE_SUB(NOW(), INTERVAL 90 DAY), DATE_SUB(NOW(), INTERVAL 90 DAY)),
    (4, 'ITM-0004', '포장용 필름', 'PACKAGING', '부자재', '500m/롤 [공급처: (주)그린테크, 위치: WH-MAIN-B1, LOT: LOT-202605-B01]', 'BARCODE-004', 'EA', 2, 12000.00, 18000.00, 50, 1, DATE_SUB(NOW(), INTERVAL 90 DAY), DATE_SUB(NOW(), INTERVAL 90 DAY)),
    (5, 'ITM-0005', '선물세트 케이스', 'PACKAGING', '부자재', '100개입/묶음 [공급처: (주)글로벌네트웍스, 위치: WH-MAIN-B2, LOT: LOT-202605-B02]', 'BARCODE-005', 'EA', 3, 2000.00, 3500.00, 80, 1, DATE_SUB(NOW(), INTERVAL 90 DAY), DATE_SUB(NOW(), INTERVAL 90 DAY)),
    (6, 'ITM-0006', '정통 찹쌀떡', 'FINISHED_GOOD', '완제품', '50g*10개입 [공급처: 자체생산, 위치: WH-FINISHED-C1, LOT: LOT-202605-C01]', 'BARCODE-006', 'BOX', 3, 4500.00, 8000.00, 100, 1, DATE_SUB(NOW(), INTERVAL 90 DAY), DATE_SUB(NOW(), INTERVAL 90 DAY)),
    (7, 'ITM-0007', '모듬 경단 세트', 'FINISHED_GOOD', '완제품', '400g/팩 [공급처: 자체생산, 위치: WH-FINISHED-C2, LOT: LOT-202605-C02]', 'BARCODE-007', 'EA', 3, 3500.00, 6500.00, 80, 1, DATE_SUB(NOW(), INTERVAL 90 DAY), DATE_SUB(NOW(), INTERVAL 90 DAY)),
    (8, 'ITM-0008', '쑥 찹쌀 반죽', 'WORK_IN_PROGRESS', '반제품', '10kg/배치 [공급처: 자체생산, 위치: WH-COLD-D1, LOT: LOT-202605-D01]', 'BARCODE-008', 'KG', 1, 2000.00, 3000.00, 50, 1, DATE_SUB(NOW(), INTERVAL 90 DAY), DATE_SUB(NOW(), INTERVAL 90 DAY)),
    (9, 'ITM-0009', '시향 샘플킷', 'FINISHED_GOOD', '샘플상품', '1세트 [공급처: 뚝딱세무법인, 위치: WH-MAIN-B3, LOT: LOT-202605-E01]', 'BARCODE-009', 'EA', 4, 1000.00, 2000.00, 10, 1, DATE_SUB(NOW(), INTERVAL 90 DAY), DATE_SUB(NOW(), INTERVAL 90 DAY)),
    (10, 'ITM-0010', '국화차 패키지', 'FINISHED_GOOD', '단종예정상품', '10팩입 [공급처: 뚝딱세무법인, 위치: WH-RETURN-E2, LOT: LOT-202605-E02]', 'BARCODE-010', 'BOX', 4, 3000.00, 6000.00, 5, 1, DATE_SUB(NOW(), INTERVAL 90 DAY), DATE_SUB(NOW(), INTERVAL 90 DAY)),
    (11, 'ITM-0011', '프리미엄 흑임자 경단', 'FINISHED_GOOD', '완제품', '350g/팩 [공급처: 자체생산, 위치: WH-FINISHED-C3, LOT: LOT-202605-F01]', 'BARCODE-011', 'EA', 3, 4000.00, 7500.00, 60, 1, DATE_SUB(NOW(), INTERVAL 60 DAY), DATE_SUB(NOW(), INTERVAL 60 DAY)),
    (12, 'ITM-0012', '오곡 꿀떡', 'FINISHED_GOOD', '완제품', '300g/팩 [공급처: 자체생산, 위치: WH-FINISHED-C4, LOT: LOT-202605-F02]', 'BARCODE-012', 'EA', 3, 2800.00, 5000.00, 70, 1, DATE_SUB(NOW(), INTERVAL 60 DAY), DATE_SUB(NOW(), INTERVAL 60 DAY)),
    (13, 'ITM-0013', '포장 보자기(명절용)', 'PACKAGING', '부자재', '50개입/묶음 [공급처: (주)글로벌네트웍스, 위치: WH-MAIN-B4, LOT: LOT-202605-F03]', 'BARCODE-013', 'EA', 3, 15000.00, 25000.00, 30, 1, DATE_SUB(NOW(), INTERVAL 60 DAY), DATE_SUB(NOW(), INTERVAL 60 DAY)),
    (14, 'ITM-0014', '인절미 쑥가루', 'RAW_MATERIAL', '원재료', '10kg/포대 [공급처: (주)뚝딱물산, 위치: WH-RAW-A4, LOT: LOT-202605-F04]', 'BARCODE-014', 'KG', 1, 4500.00, 6500.00, 100, 1, DATE_SUB(NOW(), INTERVAL 60 DAY), DATE_SUB(NOW(), INTERVAL 60 DAY)),
    (15, 'ITM-0015', '천연 호박 가루', 'RAW_MATERIAL', '원재료', '5kg/캔 [공급처: (주)그린테크, 위치: WH-RAW-A5, LOT: LOT-202605-F05]', 'BARCODE-015', 'KG', 2, 8000.00, 12000.00, 50, 1, DATE_SUB(NOW(), INTERVAL 60 DAY), DATE_SUB(NOW(), INTERVAL 60 DAY)),
    (16, 'ITM-0016', '호박 인절미(완제)', 'FINISHED_GOOD', '완제품', '400g/팩 [공급처: 자체생산, 위치: WH-FINISHED-C5, LOT: LOT-202605-G01]', 'BARCODE-016', 'EA', 3, 3800.00, 7000.00, 80, 1, DATE_SUB(NOW(), INTERVAL 45 DAY), DATE_SUB(NOW(), INTERVAL 45 DAY)),
    (17, 'ITM-0017', '조청 시럽', 'RAW_MATERIAL', '원재료', '10L/통 [공급처: (주)뚝딱물산, 위치: WH-RAW-A6, LOT: LOT-202605-G02]', 'BARCODE-017', 'KG', 1, 20000.00, 28000.00, 20, 1, DATE_SUB(NOW(), INTERVAL 45 DAY), DATE_SUB(NOW(), INTERVAL 45 DAY)),
    (18, 'ITM-0018', '포장 완충재', 'PACKAGING', '부자재', '200개입/BOX [공급처: (주)그린테크, 위치: WH-MAIN-B5, LOT: LOT-202605-G03]', 'BARCODE-018', 'EA', 2, 5000.00, 8000.00, 40, 1, DATE_SUB(NOW(), INTERVAL 45 DAY), DATE_SUB(NOW(), INTERVAL 45 DAY)),
    (19, 'ITM-0019', '자체 조제 팥소', 'WORK_IN_PROGRESS', '반제품', '20kg/배치 [공급처: 자체생산, 위치: WH-COLD-D2, LOT: LOT-202605-H01]', 'BARCODE-019', 'KG', 1, 3000.00, 4000.00, 40, 1, DATE_SUB(NOW(), INTERVAL 30 DAY), DATE_SUB(NOW(), INTERVAL 30 DAY)),
    (20, 'ITM-0020', '프리미엄 찹쌀 떡 선물세트', 'FINISHED_GOOD', '완제품', '20개입/BOX [공급처: 자체생산, 위치: WH-FINISHED-C6, LOT: LOT-202605-H02]', 'BARCODE-020', 'BOX', 3, 18000.00, 32000.00, 50, 1, DATE_SUB(NOW(), INTERVAL 30 DAY), DATE_SUB(NOW(), INTERVAL 30 DAY))
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
    (1, 1, 1, 'WH-MAIN', 1200, 100, 500, 1200.0000, 1440000.0000, 0, NOW(), NOW()),
    (2, 1, 2, 'WH-RAW', 4500, 0, 500, 1180.0000, 5310000.0000, 0, NOW(), NOW()),
    (3, 2, 1, 'WH-MAIN', 150, 20, 200, 3500.0000, 525000.0000, 0, NOW(), NOW()),
    (4, 2, 2, 'WH-RAW', 800, 0, 200, 3450.0000, 2760000.0000, 0, NOW(), NOW()),
    (5, 3, 2, 'WH-RAW', 1200, 0, 300, 1000.0000, 1200000.0000, 0, NOW(), NOW()),
    (6, 4, 1, 'WH-MAIN', 0, 0, 50, 12000.0000, 0.0000, 0, NOW(), NOW()),
    (7, 4, 4, 'WH-COLD', 180, 20, 50, 12100.0000, 2178000.0000, 0, NOW(), NOW()),
    (8, 5, 1, 'WH-MAIN', 600, 80, 80, 2000.0000, 1200000.0000, 0, NOW(), NOW()),
    (9, 6, 3, 'WH-FINISHED', 1500, 120, 100, 4500.0000, 6750000.0000, 0, NOW(), NOW()),
    (10, 6, 4, 'WH-COLD', 800, 0, 100, 4550.0000, 3640000.0000, 0, NOW(), NOW()),
    (11, 7, 3, 'WH-FINISHED', 75, 15, 80, 3500.0000, 262500.0000, 0, NOW(), NOW()),
    (12, 8, 4, 'WH-COLD', 350, 0, 50, 2000.0000, 700000.0000, 0, NOW(), NOW()),
    (13, 9, 1, 'WH-MAIN', 25, 0, 10, 1000.0000, 25000.0000, 0, NOW(), NOW()),
    (14, 10, 5, 'WH-RETURN', 4, 0, 5, 3000.0000, 12000.0000, 0, NOW(), NOW()),
    (15, 11, 3, 'WH-FINISHED', 400, 40, 60, 4000.0000, 1600000.0000, 0, NOW(), NOW()),
    (16, 12, 3, 'WH-FINISHED', 620, 0, 70, 2800.0000, 1736000.0000, 0, NOW(), NOW()),
    (17, 13, 1, 'WH-MAIN', 150, 20, 30, 15000.0000, 2250000.0000, 0, NOW(), NOW()),
    (18, 14, 2, 'WH-RAW', 900, 0, 100, 4500.0000, 4050000.0000, 0, NOW(), NOW()),
    (19, 15, 2, 'WH-RAW', 400, 0, 50, 8000.0000, 3200000.0000, 0, NOW(), NOW()),
    (20, 16, 3, 'WH-FINISHED', 500, 50, 80, 3800.0000, 1900000.0000, 0, NOW(), NOW()),
    (21, 17, 2, 'WH-RAW', 80, 10, 20, 20000.0000, 1600000.0000, 0, NOW(), NOW()),
    (22, 18, 1, 'WH-MAIN', 200, 0, 40, 5000.0000, 1000000.0000, 0, NOW(), NOW()),
    (23, 19, 4, 'WH-COLD', 120, 0, 40, 3000.0000, 360000.0000, 0, NOW(), NOW()),
    (24, 20, 3, 'WH-FINISHED', 15, 5, 50, 18000.0000, 270000.0000, 0, NOW(), NOW())
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
    (1, 'PO-202605-0001', 1, 1, 1, 1, '2026-05-01', '2026-05-05', 'COMPLETED', 15000000.00, '원자재 찹쌀가루 정기 구매 건', DATE_SUB(NOW(), INTERVAL 27 DAY), DATE_SUB(NOW(), INTERVAL 27 DAY)),
    (2, 'PO-202605-0002', 2, 1, 1, 1, '2026-05-03', '2026-05-07', 'COMPLETED', 8500000.00, '포장용 필름 신제품 도입용 발주', DATE_SUB(NOW(), INTERVAL 25 DAY), DATE_SUB(NOW(), INTERVAL 25 DAY)),
    (3, 'PO-202605-0003', 1, 1, 1, 1, '2026-05-08', '2026-05-12', 'COMPLETED', 12000000.00, '루이보스 블렌드 물량 보충', DATE_SUB(NOW(), INTERVAL 20 DAY), DATE_SUB(NOW(), INTERVAL 20 DAY)),
    (4, 'PO-202605-0004', 3, 3, 1, 1, '2026-05-10', '2026-05-15', 'COMPLETED', 24000000.00, '경단 완제품 외부 위탁 생산 수량', DATE_SUB(NOW(), INTERVAL 18 DAY), DATE_SUB(NOW(), INTERVAL 18 DAY)),
    (5, 'PO-202605-0005', 2, 1, 1, 1, '2026-05-12', '2026-05-17', 'COMPLETED', 6200000.00, '선물세트 케이스 보충 물량', DATE_SUB(NOW(), INTERVAL 16 DAY), DATE_SUB(NOW(), INTERVAL 16 DAY)),
    (6, 'PO-202605-0006', 1, 1, 2, 1, '2026-05-15', '2026-05-20', 'RECEIVING', 18000000.00, '[입고중] 찹쌀가루 긴급 보충분', DATE_SUB(NOW(), INTERVAL 13 DAY), DATE_SUB(NOW(), INTERVAL 13 DAY)),
    (7, 'PO-202605-0007', 2, 1, 2, 1, '2026-05-17', '2026-05-22', 'INBOUND_DELAY', 9500000.00, '[지연] 포장재 일부 품목 통관 지연 건', DATE_SUB(NOW(), INTERVAL 11 DAY), DATE_SUB(NOW(), INTERVAL 11 DAY)),
    (8, 'PO-202605-0008', 3, 3, 1, NULL, '2026-05-22', '2026-05-27', 'ORDERED', 14500000.00, '경단 명절 선물세트 대량 발주', DATE_SUB(NOW(), INTERVAL 6 DAY), DATE_SUB(NOW(), INTERVAL 6 DAY)),
    (9, 'PO-202605-0009', 1, 3, 1, 1, '2026-05-24', '2026-05-28', 'APPROVED', 22000000.00, '완제품 찹쌀떡 단체 주문 건 발주', DATE_SUB(NOW(), INTERVAL 4 DAY), DATE_SUB(NOW(), INTERVAL 4 DAY)),
    (10, 'PO-202605-0010', 3, 1, 2, 1, '2026-05-25', '2026-05-29', 'APPROVED', 7500000.00, '선물박스 포장 부자재 승인 완료', DATE_SUB(NOW(), INTERVAL 3 DAY), DATE_SUB(NOW(), INTERVAL 3 DAY)),
    (11, 'PO-202605-0011', 1, 2, 2, NULL, '2026-05-26', '2026-05-31', 'REQUESTED', 11200000.00, '팥앙금 원재료 추가 요청 건', DATE_SUB(NOW(), INTERVAL 2 DAY), DATE_SUB(NOW(), INTERVAL 2 DAY)),
    (12, 'PO-202605-0012', 2, 1, 1, 1, '2026-05-27', '2026-06-01', 'SENT_TO_VENDOR', 5000000.00, '완충재 긴급 추가 발송 건', DATE_SUB(NOW(), INTERVAL 1 DAY), DATE_SUB(NOW(), INTERVAL 1 DAY))
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
    (1, 1, 1, 12500, 'KG', 1200.00, 15000000.00, 0.00, 15000000.00, '2026-05-05', '찹쌀가루 정기 공급', DATE_SUB(NOW(), INTERVAL 27 DAY), DATE_SUB(NOW(), INTERVAL 27 DAY)),
    (2, 2, 4, 708, 'EA', 12000.00, 8500000.00, 0.00, 8500000.00, '2026-05-07', '포장 필름 긴급 보강', DATE_SUB(NOW(), INTERVAL 25 DAY), DATE_SUB(NOW(), INTERVAL 25 DAY)),
    (3, 3, 2, 3428, 'KG', 3500.00, 12000000.00, 0.00, 12000000.00, '2026-05-12', '팥앙금 공급 보충', DATE_SUB(NOW(), INTERVAL 20 DAY), DATE_SUB(NOW(), INTERVAL 20 DAY)),
    (4, 4, 6, 3000, 'BOX', 4500.00, 13500000.00, 0.00, 13500000.00, '2026-05-15', '정통 찹쌀떡 생산분 입고', DATE_SUB(NOW(), INTERVAL 18 DAY), DATE_SUB(NOW(), INTERVAL 18 DAY)),
    (5, 5, 5, 3100, 'EA', 2000.00, 6200000.00, 0.00, 6200000.00, '2026-05-17', '세트 케이스 공급', DATE_SUB(NOW(), INTERVAL 16 DAY), DATE_SUB(NOW(), INTERVAL 16 DAY)),
    (6, 6, 1, 15000, 'KG', 1200.00, 18000000.00, 0.00, 18000000.00, '2026-05-20', '찹쌀가루 대량 입고', DATE_SUB(NOW(), INTERVAL 13 DAY), DATE_SUB(NOW(), INTERVAL 13 DAY)),
    (7, 7, 4, 791, 'EA', 12000.00, 9500000.00, 0.00, 9500000.00, '2026-05-22', '포장재 통관 지연 건', DATE_SUB(NOW(), INTERVAL 11 DAY), DATE_SUB(NOW(), INTERVAL 11 DAY)),
    (8, 8, 7, 4142, 'EA', 3500.00, 14500000.00, 0.00, 14500000.00, '2026-05-27', '경단 세트 대량 발주', DATE_SUB(NOW(), INTERVAL 6 DAY), DATE_SUB(NOW(), INTERVAL 6 DAY)),
    (9, 9, 6, 4888, 'BOX', 4500.00, 22000000.00, 0.00, 22000000.00, '2026-05-28', '찹쌀떡 단체 주문 건', DATE_SUB(NOW(), INTERVAL 4 DAY), DATE_SUB(NOW(), INTERVAL 4 DAY)),
    (10, 10, 13, 500, 'EA', 15000.00, 7500000.00, 0.00, 7500000.00, '2026-05-29', '명절 보자기 부자재', DATE_SUB(NOW(), INTERVAL 3 DAY), DATE_SUB(NOW(), INTERVAL 3 DAY)),
    (11, 11, 2, 3200, 'KG', 3500.00, 11200000.00, 0.00, 11200000.00, '2026-05-31', '팥앙금 추가 공급', DATE_SUB(NOW(), INTERVAL 2 DAY), DATE_SUB(NOW(), INTERVAL 2 DAY)),
    (12, 12, 18, 1000, 'EA', 5000.00, 5000000.00, 0.00, 5000000.00, '2026-06-01', '포장 완충재 공급', DATE_SUB(NOW(), INTERVAL 1 DAY), DATE_SUB(NOW(), INTERVAL 1 DAY))
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
    (1, 'TRF-202605-0001', 2, 1, 'COMPLETED', '[주간보충] 원재료 창고에서 본사창고로 얼그레이 이동 완료', 1, 1, DATE_SUB(NOW(), INTERVAL 25 DAY), DATE_SUB(NOW(), INTERVAL 25 DAY), DATE_SUB(NOW(), INTERVAL 25 DAY), DATE_SUB(NOW(), INTERVAL 25 DAY)),
    (2, 'TRF-202605-0002', 1, 3, 'PENDING', '[요청대기] 본사창고에서 완제품창고로 찹쌀떡 공급 (결재 요망)', 1, NULL, DATE_SUB(NOW(), INTERVAL 1 DAY), DATE_SUB(NOW(), INTERVAL 1 DAY), NULL, NULL),
    (3, 'TRF-202605-0003', 3, 4, 'APPROVED', '[이동중] 완제품창고에서 냉장창고로 경단 세트 승인 완료', 1, 1, DATE_SUB(NOW(), INTERVAL 2 DAY), DATE_SUB(NOW(), INTERVAL 2 DAY), DATE_SUB(NOW(), INTERVAL 2 DAY), NULL),
    (4, 'TRF-202605-0004', 1, 5, 'CANCELLED', '[취소] 사유: 사용자의 단순 변심 및 중복 신청으로 인한 요청 취소.', 1, 1, DATE_SUB(NOW(), INTERVAL 5 DAY), DATE_SUB(NOW(), INTERVAL 5 DAY), NULL, NULL),
    (5, 'TRF-202605-0005', 2, 1, 'COMPLETED', '[생산보충] 원자재창고 찹쌀가루 생산라인 공급 이동 완료', 1, 1, DATE_SUB(NOW(), INTERVAL 15 DAY), DATE_SUB(NOW(), INTERVAL 15 DAY), DATE_SUB(NOW(), INTERVAL 15 DAY), DATE_SUB(NOW(), INTERVAL 15 DAY)),
    (6, 'TRF-202605-0006', 1, 3, 'COMPLETED', '[완제이동] 본사 생산 완제 찹쌀떡 완제품창고로 적재 완료', 2, 1, DATE_SUB(NOW(), INTERVAL 12 DAY), DATE_SUB(NOW(), INTERVAL 12 DAY), DATE_SUB(NOW(), INTERVAL 12 DAY), DATE_SUB(NOW(), INTERVAL 12 DAY)),
    (7, 'TRF-202605-0007', 3, 4, 'COMPLETED', '[냉장보관] 신제품 흑임자 경단 냉장창고 이동 입고 완료', 2, 1, DATE_SUB(NOW(), INTERVAL 10 DAY), DATE_SUB(NOW(), INTERVAL 10 DAY), DATE_SUB(NOW(), INTERVAL 10 DAY), DATE_SUB(NOW(), INTERVAL 10 DAY)),
    (8, 'TRF-202605-0008', 1, 3, 'PENDING', '[정기보충] 본사창고에서 완제품창고로 꿀떡 세트 이동 요청', 1, NULL, DATE_SUB(NOW(), INTERVAL 4 HOUR), DATE_SUB(NOW(), INTERVAL 4 HOUR), NULL, NULL),
    (9, 'TRF-202605-0009', 2, 4, 'APPROVED', '[원료이동] 원재료창고 팥앙금 조리실(냉장창고) 공급 승인', 1, 1, DATE_SUB(NOW(), INTERVAL 12 HOUR), DATE_SUB(NOW(), INTERVAL 12 HOUR), DATE_SUB(NOW(), INTERVAL 12 HOUR), NULL),
    (10, 'TRF-202605-0010', 1, 5, 'CANCELLED', '[반려] 사유: 요청 수량이 가용 한도를 초과하여 취소 후 재상신 요망.', 2, 1, DATE_SUB(NOW(), INTERVAL 3 DAY), DATE_SUB(NOW(), INTERVAL 3 DAY), NULL, NULL)
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
    (4, 4, 5, 50),
    (5, 5, 1, 2000),
    (6, 6, 6, 500),
    (7, 7, 11, 200),
    (8, 8, 12, 150),
    (9, 9, 2, 300),
    (10, 10, 5, 10)
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
    -- 1. 발주 입고 이력 (INBOUND)
    (1, 1, 1, 'INBOUND', 'PURCHASE_RECEIVED', 'PO-202605-0001', 5000, 1200.0000, 6000000.0000, 0, 5000, 'PURCHASE', '1', DATE_SUB(NOW(), INTERVAL 25 DAY)),
    (2, 2, 1, 'INBOUND', 'PURCHASE_RECEIVED', 'PO-202605-0003', 1000, 3500.0000, 3500000.0000, 0, 1000, 'PURCHASE', '3', DATE_SUB(NOW(), INTERVAL 20 DAY)),
    (3, 4, 1, 'INBOUND', 'PURCHASE_RECEIVED', 'PO-202605-0002', 1500, 12000.0000, 18000000.0000, 0, 1500, 'PURCHASE', '2', DATE_SUB(NOW(), INTERVAL 22 DAY)),
    (4, 5, 1, 'INBOUND', 'PURCHASE_RECEIVED', 'PO-202605-0005', 800, 2000.0000, 1600000.0000, 0, 800, 'PURCHASE', '5', DATE_SUB(NOW(), INTERVAL 16 DAY)),
    (5, 6, 3, 'INBOUND', 'PURCHASE_RECEIVED', 'PO-202605-0004', 2000, 4500.0000, 9000000.0000, 0, 2000, 'PURCHASE', '4', DATE_SUB(NOW(), INTERVAL 14 DAY)),
    
    -- 2. 생산 출고 이력 (OUTBOUND)
    (6, 1, 1, 'OUTBOUND', 'SALES_SHIPPED', 'MOV-202605-A01', 1000, 1200.0000, 1200000.0000, 5000, 4000, 'PRODUCTION', 'WIP-101', DATE_SUB(NOW(), INTERVAL 24 DAY)),
    (7, 2, 1, 'OUTBOUND', 'SALES_SHIPPED', 'MOV-202605-A02', 400, 3500.0000, 1400000.0000, 1000, 600, 'PRODUCTION', 'WIP-102', DATE_SUB(NOW(), INTERVAL 23 DAY)),
    (8, 3, 2, 'OUTBOUND', 'SALES_SHIPPED', 'MOV-202605-A03', 200, 1000.0000, 200000.0000, 1400, 1200, 'PRODUCTION', 'WIP-103', DATE_SUB(NOW(), INTERVAL 21 DAY)),
    (9, 8, 4, 'OUTBOUND', 'SALES_SHIPPED', 'MOV-202605-A04', 150, 2000.0000, 300000.0000, 500, 350, 'PRODUCTION', 'WIP-104', DATE_SUB(NOW(), INTERVAL 19 DAY)),
    
    -- 3. 납품 출고 이력 (OUTBOUND)
    (10, 6, 3, 'OUTBOUND', 'SALES_SHIPPED', 'MOV-202605-B01', 400, 4500.0000, 1800000.0000, 2000, 1600, 'SALES', 'SO-202605-001', DATE_SUB(NOW(), INTERVAL 18 DAY)),
    (11, 7, 3, 'OUTBOUND', 'SALES_SHIPPED', 'MOV-202605-B02', 120, 3500.0000, 420000.0000, 300, 180, 'SALES', 'SO-202605-002', DATE_SUB(NOW(), INTERVAL 17 DAY)),
    (12, 11, 3, 'OUTBOUND', 'SALES_SHIPPED', 'MOV-202605-B03', 50, 4000.0000, 200000.0000, 450, 400, 'SALES', 'SO-202605-003', DATE_SUB(NOW(), INTERVAL 15 DAY)),
    (13, 12, 3, 'OUTBOUND', 'SALES_SHIPPED', 'MOV-202605-B04', 80, 2800.0000, 224000.0000, 700, 620, 'SALES', 'SO-202605-004', DATE_SUB(NOW(), INTERVAL 13 DAY)),
    
    -- 4. 창고 이동 이력 (TRANSFER)
    (14, 1, 2, 'TRANSFER_OUT', 'TRANSFER', 'TRF-202605-0005', 2000, 1180.0000, 2360000.0000, 6500, 4500, 'TRANSFER', '5', DATE_SUB(NOW(), INTERVAL 15 DAY)),
    (15, 1, 1, 'TRANSFER_IN', 'TRANSFER', 'TRF-202605-0005', 2000, 1200.0000, 2400000.0000, 4000, 6000, 'TRANSFER', '5', DATE_SUB(NOW(), INTERVAL 15 DAY)),
    (16, 6, 1, 'TRANSFER_OUT', 'TRANSFER', 'TRF-202605-0006', 500, 4500.0000, 2250000.0000, 2000, 1500, 'TRANSFER', '6', DATE_SUB(NOW(), INTERVAL 12 DAY)),
    (17, 6, 3, 'TRANSFER_IN', 'TRANSFER', 'TRF-202605-0006', 500, 4500.0000, 2250000.0000, 1000, 1500, 'TRANSFER', '6', DATE_SUB(NOW(), INTERVAL 12 DAY)),
    (18, 11, 3, 'TRANSFER_OUT', 'TRANSFER', 'TRF-202605-0007', 200, 4000.0000, 800000.0000, 600, 400, 'TRANSFER', '7', DATE_SUB(NOW(), INTERVAL 10 DAY)),
    (19, 11, 4, 'TRANSFER_IN', 'TRANSFER', 'TRF-202605-0007', 200, 4000.0000, 800000.0000, 0, 200, 'TRANSFER', '7', DATE_SUB(NOW(), INTERVAL 10 DAY)),

    -- 5. 긴급 입고 & 구매 입고 이력 (INBOUND)
    (20, 14, 2, 'INBOUND', 'PURCHASE_RECEIVED', 'MOV-202605-C01', 500, 4500.0000, 2250000.0000, 400, 900, 'PURCHASE', 'PO-202605-0001', DATE_SUB(NOW(), INTERVAL 9 DAY)),
    (21, 15, 2, 'INBOUND', 'PURCHASE_RECEIVED', 'MOV-202605-C02', 300, 8000.0000, 2400000.0000, 100, 400, 'PURCHASE', 'PO-202605-0003', DATE_SUB(NOW(), INTERVAL 8 DAY)),
    (22, 17, 2, 'INBOUND', 'PURCHASE_RECEIVED', 'MOV-202605-C03', 50, 20000.0000, 1000000.0000, 30, 80, 'PURCHASE', 'PO-202605-0005', DATE_SUB(NOW(), INTERVAL 7 DAY)),

    -- 6. 반품 입고 이력 (RETURN_IN)
    (23, 6, 3, 'RETURN_IN', 'RETURNED_FROM_CUSTOMER', 'RET-202605-001', 10, 4500.0000, 45000.0000, 1500, 1510, 'RETURN', 'RET-01', DATE_SUB(NOW(), INTERVAL 6 DAY)),
    (24, 7, 3, 'RETURN_IN', 'RETURNED_FROM_CUSTOMER', 'RET-202605-002', 5, 3500.0000, 17500.0000, 70, 75, 'RETURN', 'RET-02', DATE_SUB(NOW(), INTERVAL 6 DAY)),

    -- 7. 폐기 출고 이력 (OUTBOUND - ADJUSTMENT)
    (25, 10, 5, 'OUTBOUND', 'RETURNED_TO_VENDOR', 'RET-202605-003', 2, 3000.0000, 6000.0000, 6, 4, 'RETURN', 'RET-03', DATE_SUB(NOW(), INTERVAL 5 DAY)),
    (26, 4, 1, 'OUTBOUND', 'RETURNED_TO_VENDOR', 'RET-202605-004', 10, 12000.0000, 120000.0000, 10, 0, 'RETURN', 'RET-04', DATE_SUB(NOW(), INTERVAL 5 DAY)),

    -- 8. 재고 조정 이력 (ADJUSTMENT_IN / ADJUSTMENT_OUT)
    (27, 2, 1, 'ADJUSTMENT_OUT', 'MANUAL_ADJUST', 'ADJ-202605-001', 50, 3500.0000, 175000.0000, 200, 150, 'ADJUST', 'ADJ-01', DATE_SUB(NOW(), INTERVAL 4 DAY)),
    (28, 11, 3, 'ADJUSTMENT_IN', 'MANUAL_ADJUST', 'ADJ-202605-002', 20, 4000.0000, 80000.0000, 380, 400, 'ADJUST', 'ADJ-02', DATE_SUB(NOW(), INTERVAL 4 DAY)),
    (29, 20, 3, 'ADJUSTMENT_OUT', 'MANUAL_ADJUST', 'ADJ-202605-003', 35, 18000.0000, 630000.0000, 50, 15, 'ADJUST', 'ADJ-03', DATE_SUB(NOW(), INTERVAL 3 DAY)),
    (30, 18, 1, 'ADJUSTMENT_IN', 'MANUAL_ADJUST', 'ADJ-202605-004', 10, 5000.0000, 50000.0000, 190, 200, 'ADJUST', 'ADJ-04', DATE_SUB(NOW(), INTERVAL 3 DAY)),
    (31, 14, 2, 'ADJUSTMENT_OUT', 'MANUAL_ADJUST', 'ADJ-202605-005', 20, 4500.0000, 90000.0000, 920, 900, 'ADJUST', 'ADJ-05', DATE_SUB(NOW(), INTERVAL 2 DAY)),
    (32, 17, 2, 'ADJUSTMENT_IN', 'REBUILD_ADJUSTMENT', 'ADJ-202605-006', 5, 20000.0000, 100000.0000, 75, 80, 'ADJUST', 'ADJ-06', DATE_SUB(NOW(), INTERVAL 2 DAY)),

    -- 9. 실시간 입출고 트렌드 라인과 차트를 풍부하게 할 최근 거래 데이터들
    (33, 6, 3, 'OUTBOUND', 'SALES_SHIPPED', 'MOV-202605-D01', 50, 4500.0000, 225000.0000, 1510, 1460, 'SALES', 'SO-005', DATE_SUB(NOW(), INTERVAL 1 DAY)),
    (34, 1, 1, 'OUTBOUND', 'SALES_SHIPPED', 'MOV-202605-D02', 80, 1200.0000, 96000.0000, 1280, 1200, 'PRODUCTION', 'WIP-105', DATE_SUB(NOW(), INTERVAL 12 HOUR)),
    (35, 12, 3, 'OUTBOUND', 'SALES_SHIPPED', 'MOV-202605-D03', 40, 2800.0000, 112000.0000, 660, 620, 'SALES', 'SO-006', DATE_SUB(NOW(), INTERVAL 10 HOUR)),
    (36, 16, 3, 'OUTBOUND', 'SALES_SHIPPED', 'MOV-202605-D04', 30, 3800.0000, 114000.0000, 530, 500, 'SALES', 'SO-007', DATE_SUB(NOW(), INTERVAL 8 HOUR)),
    (37, 20, 3, 'OUTBOUND', 'SALES_SHIPPED', 'MOV-202605-D05', 5, 18000.0000, 90000.0000, 20, 15, 'SALES', 'SO-008', DATE_SUB(NOW(), INTERVAL 6 HOUR)),
    (38, 1, 2, 'TRANSFER_OUT', 'TRANSFER', 'TRF-202605-0009', 300, 1180.0000, 354000.0000, 4800, 4500, 'TRANSFER', '9', DATE_SUB(NOW(), INTERVAL 12 HOUR)),
    (39, 2, 1, 'TRANSFER_IN', 'TRANSFER', 'TRF-202605-0009', 300, 3500.0000, 1050000.0000, 0, 300, 'TRANSFER', '9', DATE_SUB(NOW(), INTERVAL 12 HOUR)),
    (40, 13, 1, 'TRANSFER_OUT', 'TRANSFER', 'TRF-202605-0008', 150, 15000.0000, 2250000.0000, 300, 150, 'TRANSFER', '8', DATE_SUB(NOW(), INTERVAL 4 HOUR)),
    (41, 10, 5, 'INBOUND', 'PURCHASE_RECEIVED', 'MOV-202605-E01', 2, 3000.0000, 6000.0000, 2, 4, 'PURCHASE', 'PO-202605-0002', DATE_SUB(NOW(), INTERVAL 5 DAY)),
    (42, 1, 1, 'INBOUND', 'PURCHASE_RECEIVED', 'MOV-202605-E02', 1000, 1200.0000, 1200000.0000, 200, 1200, 'PURCHASE', 'PO-202605-0006', DATE_SUB(NOW(), INTERVAL 2 DAY)),
    (43, 6, 3, 'INBOUND', 'PURCHASE_RECEIVED', 'MOV-202605-E03', 40, 4500.0000, 18000.0000, 1460, 1500, 'PURCHASE', 'PO-202605-0009', DATE_SUB(NOW(), INTERVAL 1 DAY)),
    (44, 2, 1, 'INBOUND', 'PURCHASE_RECEIVED', 'MOV-202605-E04', 150, 3500.0000, 525000.0000, 0, 150, 'PURCHASE', 'PO-202605-0011', DATE_SUB(NOW(), INTERVAL 2 HOUR)),
    (45, 18, 1, 'INBOUND', 'PURCHASE_RECEIVED', 'MOV-202605-E05', 200, 5000.0000, 1000000.0000, 0, 200, 'PURCHASE', 'PO-202605-0012', DATE_SUB(NOW(), INTERVAL 1 HOUR))
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
