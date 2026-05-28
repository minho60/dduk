-- =================================================================
-- DDUK ERP Custom Sample Data Seed File
-- =================================================================
-- 이 파일에 추후 커스텀 더미 데이터를 INSERT 쿼리 형태로 작성하시면 
-- Spring Boot 기동 시 자동으로 데이터베이스에 적재됩니다.
--
-- [주의사항]
-- 1. 데이터 정합성(외래키 제약조건)을 지키기 위해 아래 순서대로 작성하시는 것을 권장합니다.
--    - members -> employees -> payroll_contracts -> vendors -> warehouses -> items -> inventories -> vouchers -> voucher_lines ...
-- 2. 중복 기동 시 고유키(Unique Key) 충돌 에러를 방지하기 위해 
--    `INSERT INTO ... ON DUPLICATE KEY UPDATE` 구문을 적극 사용하십시오.
--
-- =================================================================

-- -----------------------------------------------------------------
-- 1. 예시: 임직원 (employees) 추가
-- -----------------------------------------------------------------
-- INSERT INTO employees (employee_no, name, department, position, employment_status, hire_date, email, phone)
-- VALUES ('E9999', '홍길동', '개발부', '사원', 'ACTIVE', '2026-01-01', 'hong@dduk.com', '010-1234-5678')
-- ON DUPLICATE KEY UPDATE 
--     name = VALUES(name), 
--     department = VALUES(department), 
--     position = VALUES(position), 
--     updated_at = NOW();

-- -----------------------------------------------------------------
-- 2. 예시: 거래처 (vendors) 추가
-- -----------------------------------------------------------------
-- INSERT INTO vendors (vendor_code, business_registration_no, name, representative_name, business_type, business_item, contact_name, contact_phone, email, status)
-- VALUES ('V999', '123-45-00000', '(주)예시대상', '김예시', '도소매', '샘플품목', '이담당', '010-9999-8888', 'sample@example.com', 'ACTIVE')
-- ON DUPLICATE KEY UPDATE 
--     name = VALUES(name), 
--     representative_name = VALUES(representative_name), 
--     updated_at = NOW();

-- -----------------------------------------------------------------
-- 3. 예시: 품목 (items) 추가
-- -----------------------------------------------------------------
-- INSERT INTO items (item_code, name, item_type, category, spec, unit, standard_cost, unit_price, is_active)
-- VALUES ('ITM-9999', '예시 찹쌀가루', 'RAW_MATERIAL', '원재료', '10kg/포대', 'KG', 1500.00, 2000.00, 1)
-- ON DUPLICATE KEY UPDATE 
--     name = VALUES(name), 
--     standard_cost = VALUES(standard_cost), 
--     unit_price = VALUES(unit_price), 
--     updated_at = NOW();

-- -----------------------------------------------------------------
-- 빈 스크립트 실행 에러 방지를 위한 더미 쿼리
-- -----------------------------------------------------------------
SELECT 1;
