# DDUK ERP DB 테이블 초안

이 문서는 조장이 팀 전체 기준선으로 공유할 수 있는 DB 초안이다.  
목표는 "지금 당장 구현 가능한 최소 계약"을 만드는 것이고, 최종 DDL 확정본은 아니다.

---

## 1. 설계 원칙

- MySQL 8.0 기준으로 설계한다.
- 테이블명, 컬럼명은 `snake_case`를 사용한다.
- 기본 PK 컬럼명은 `id`를 우선한다.
- 공통 생성/수정 시각은 가능하면 `created_at`, `updated_at`으로 통일한다.
- 상태값은 초기에 문자열 enum 후보로 두되, 확장 가능성을 고려해 코드성 값으로 관리한다.
- 도메인 간 연결은 FK로 명확히 하되, 초반 MVP에서 꼭 필요한 관계만 먼저 둔다.

---

## 2. 도메인 구분

| 도메인 | 설명 |
| :--- | :--- |
| `admin` | 로그인, 권한, 메뉴 권한, 감사 로그 |
| `hr` | 직원, 근태, 급여, 비용 |
| `inventory` | 거래처, 품목, 재고, 발주, 입출고 |

---

## 3. 공통 / 관리자 테이블

### 3.1 `users`
시스템 로그인 계정

| 컬럼 | 타입 예시 | 설명 |
| :--- | :--- | :--- |
| `id` | bigint | PK |
| `username` | varchar(50) | 로그인 ID, unique |
| `password_hash` | varchar(255) | 암호화된 비밀번호 |
| `email` | varchar(100) | 이메일 |
| `status` | varchar(30) | `ACTIVE`, `LOCKED`, `INACTIVE` |
| `last_login_at` | datetime | 마지막 로그인 시각 |
| `created_at` | datetime | 생성 시각 |
| `updated_at` | datetime | 수정 시각 |

비고:

- 직원 계정과 사용자 계정을 1:1로 묶으려면 `employees.user_id` 또는 `users.employee_id` 중 한 방향으로 연결한다.
- 초기에는 `users`와 `employees`를 분리하고, 직원이 아닌 관리자 계정도 수용할 수 있게 두는 게 안전하다.

### 3.2 `roles`
권한 역할 정의

| 컬럼 | 타입 예시 | 설명 |
| :--- | :--- | :--- |
| `id` | bigint | PK |
| `code` | varchar(50) | `ADMIN`, `HR_MANAGER`, `INVENTORY_MANAGER` |
| `name` | varchar(100) | 역할 표시명 |
| `description` | varchar(255) | 역할 설명 |
| `created_at` | datetime | 생성 시각 |
| `updated_at` | datetime | 수정 시각 |

### 3.3 `user_roles`
사용자-역할 매핑

| 컬럼 | 타입 예시 | 설명 |
| :--- | :--- | :--- |
| `id` | bigint | PK |
| `user_id` | bigint | FK -> `users.id` |
| `role_id` | bigint | FK -> `roles.id` |
| `created_at` | datetime | 생성 시각 |

권장 제약:

- `user_id`, `role_id` unique 조합

### 3.4 `menus`
ERP 메뉴 정의

| 컬럼 | 타입 예시 | 설명 |
| :--- | :--- | :--- |
| `id` | bigint | PK |
| `menu_key` | varchar(50) | 내부 키 |
| `name` | varchar(100) | 메뉴명 |
| `path` | varchar(255) | 프론트 라우트 또는 메뉴 경로 |
| `parent_id` | bigint | 상위 메뉴 ID, nullable |
| `sort_order` | int | 정렬 순서 |
| `is_active` | tinyint | 사용 여부 |
| `created_at` | datetime | 생성 시각 |
| `updated_at` | datetime | 수정 시각 |

### 3.5 `menu_permissions`
역할별 메뉴 접근 권한

| 컬럼 | 타입 예시 | 설명 |
| :--- | :--- | :--- |
| `id` | bigint | PK |
| `role_id` | bigint | FK -> `roles.id` |
| `menu_id` | bigint | FK -> `menus.id` |
| `can_view` | tinyint | 조회 가능 |
| `can_create` | tinyint | 생성 가능 |
| `can_update` | tinyint | 수정 가능 |
| `can_delete` | tinyint | 삭제 가능 |
| `created_at` | datetime | 생성 시각 |
| `updated_at` | datetime | 수정 시각 |

### 3.6 `audit_logs`
중요 행위 감사 로그

| 컬럼 | 타입 예시 | 설명 |
| :--- | :--- | :--- |
| `id` | bigint | PK |
| `user_id` | bigint | FK -> `users.id`, nullable |
| `action` | varchar(100) | 예: `LOGIN`, `CREATE_EMPLOYEE` |
| `target_type` | varchar(50) | 대상 리소스 종류 |
| `target_id` | bigint | 대상 리소스 ID, nullable |
| `ip_address` | varchar(64) | 요청 IP |
| `user_agent` | varchar(255) | 클라이언트 정보 |
| `details` | text | 변경 요약 JSON 또는 텍스트 |
| `created_at` | datetime | 발생 시각 |

---

## 4. 인사 / 회계 (`hr`) 테이블

### 4.1 `employees`
직원 기본 정보

| 컬럼 | 타입 예시 | 설명 |
| :--- | :--- | :--- |
| `id` | bigint | PK |
| `user_id` | bigint | FK -> `users.id`, nullable |
| `employee_no` | varchar(50) | 사번, unique |
| `name` | varchar(100) | 직원명 |
| `department` | varchar(100) | 부서명 |
| `position` | varchar(100) | 직책 |
| `employment_status` | varchar(30) | `ACTIVE`, `LEAVE`, `RESIGNED` |
| `hire_date` | date | 입사일 |
| `email` | varchar(100) | 이메일 |
| `phone` | varchar(30) | 연락처 |
| `created_at` | datetime | 생성 시각 |
| `updated_at` | datetime | 수정 시각 |

비고:

- 부서를 별도 테이블로 뺄 수도 있지만 MVP에서는 문자열로 시작해도 된다.
- `users`와 1:1 매핑을 잡으면 ERP 로그인과 직원 정보 연계가 쉬워진다.

### 4.2 `attendances`
근태 기록

| 컬럼 | 타입 예시 | 설명 |
| :--- | :--- | :--- |
| `id` | bigint | PK |
| `employee_id` | bigint | FK -> `employees.id` |
| `work_date` | date | 근무일 |
| `check_in_at` | datetime | 출근 시각 |
| `check_out_at` | datetime | 퇴근 시각 |
| `status` | varchar(30) | `PRESENT`, `LATE`, `ABSENT`, `LEAVE` |
| `note` | varchar(255) | 비고 |
| `created_at` | datetime | 생성 시각 |
| `updated_at` | datetime | 수정 시각 |

권장 제약:

- `employee_id`, `work_date` unique 조합 검토

### 4.3 `payrolls`
월별 급여 결과

| 컬럼 | 타입 예시 | 설명 |
| :--- | :--- | :--- |
| `id` | bigint | PK |
| `employee_id` | bigint | FK -> `employees.id` |
| `pay_month` | char(7) | 예: `2026-05` |
| `base_salary` | decimal(15,2) | 기본급 |
| `allowance_amount` | decimal(15,2) | 수당 |
| `deduction_amount` | decimal(15,2) | 공제 |
| `net_salary` | decimal(15,2) | 실지급액 |
| `status` | varchar(30) | `DRAFT`, `CONFIRMED`, `PAID` |
| `paid_at` | datetime | 지급 시각, nullable |
| `created_at` | datetime | 생성 시각 |
| `updated_at` | datetime | 수정 시각 |

권장 제약:

- `employee_id`, `pay_month` unique 조합

### 4.4 `expenses`
회사 비용 지출 기록

| 컬럼 | 타입 예시 | 설명 |
| :--- | :--- | :--- |
| `id` | bigint | PK |
| `employee_id` | bigint | FK -> `employees.id`, nullable |
| `expense_date` | date | 지출일 |
| `category` | varchar(50) | 비용 분류 |
| `amount` | decimal(15,2) | 금액 |
| `description` | varchar(255) | 내용 |
| `receipt_file_path` | varchar(255) | 영수증 파일 경로, nullable |
| `status` | varchar(30) | `SUBMITTED`, `APPROVED`, `REJECTED` |
| `created_at` | datetime | 생성 시각 |
| `updated_at` | datetime | 수정 시각 |

---

## 5. 구매 / 재고 (`inventory`) 테이블

### 5.1 `vendors`
거래처 정보

| 컬럼 | 타입 예시 | 설명 |
| :--- | :--- | :--- |
| `id` | bigint | PK |
| `vendor_code` | varchar(50) | 거래처 코드, unique |
| `name` | varchar(100) | 거래처명 |
| `contact_name` | varchar(100) | 담당자명 |
| `contact_phone` | varchar(30) | 연락처 |
| `email` | varchar(100) | 이메일 |
| `address` | varchar(255) | 주소 |
| `status` | varchar(30) | `ACTIVE`, `INACTIVE` |
| `created_at` | datetime | 생성 시각 |
| `updated_at` | datetime | 수정 시각 |

### 5.2 `items`
관리 품목

| 컬럼 | 타입 예시 | 설명 |
| :--- | :--- | :--- |
| `id` | bigint | PK |
| `item_code` | varchar(50) | 품목 코드, unique |
| `name` | varchar(100) | 품목명 |
| `category` | varchar(100) | 카테고리 |
| `unit` | varchar(30) | 단위 예: `EA`, `BOX` |
| `default_vendor_id` | bigint | FK -> `vendors.id`, nullable |
| `unit_price` | decimal(15,2) | 기본 단가 |
| `safety_stock` | int | 안전 재고 |
| `is_active` | tinyint | 사용 여부 |
| `created_at` | datetime | 생성 시각 |
| `updated_at` | datetime | 수정 시각 |

### 5.3 `inventories`
현재 재고 수량

| 컬럼 | 타입 예시 | 설명 |
| :--- | :--- | :--- |
| `id` | bigint | PK |
| `item_id` | bigint | FK -> `items.id` |
| `quantity` | int | 현재 수량 |
| `location` | varchar(100) | 창고/보관 위치 |
| `last_adjusted_at` | datetime | 마지막 조정 시각 |
| `created_at` | datetime | 생성 시각 |
| `updated_at` | datetime | 수정 시각 |

권장 방향:

- 단일 창고면 `item_id` unique로 단순화 가능
- 다중 창고면 `location`을 별도 `warehouses` 테이블로 분리 검토

### 5.4 `purchase_orders`
발주 헤더

| 컬럼 | 타입 예시 | 설명 |
| :--- | :--- | :--- |
| `id` | bigint | PK |
| `purchase_order_no` | varchar(50) | 발주번호, unique |
| `vendor_id` | bigint | FK -> `vendors.id` |
| `requested_by` | bigint | FK -> `users.id` |
| `approved_by` | bigint | FK -> `users.id`, nullable |
| `order_date` | date | 발주일 |
| `expected_date` | date | 입고 예정일, nullable |
| `status` | varchar(30) | `REQUESTED`, `APPROVED`, `ORDERED`, `RECEIVED`, `CANCELED` |
| `total_amount` | decimal(15,2) | 총액 |
| `note` | varchar(255) | 비고 |
| `created_at` | datetime | 생성 시각 |
| `updated_at` | datetime | 수정 시각 |

### 5.5 `purchase_order_items`
발주 상세 품목

| 컬럼 | 타입 예시 | 설명 |
| :--- | :--- | :--- |
| `id` | bigint | PK |
| `purchase_order_id` | bigint | FK -> `purchase_orders.id` |
| `item_id` | bigint | FK -> `items.id` |
| `quantity` | int | 발주 수량 |
| `unit_price` | decimal(15,2) | 발주 단가 |
| `line_amount` | decimal(15,2) | 행 금액 |
| `created_at` | datetime | 생성 시각 |
| `updated_at` | datetime | 수정 시각 |

### 5.6 `stock_movements`
입출고 이력

| 컬럼 | 타입 예시 | 설명 |
| :--- | :--- | :--- |
| `id` | bigint | PK |
| `item_id` | bigint | FK -> `items.id` |
| `inventory_id` | bigint | FK -> `inventories.id` |
| `movement_type` | varchar(30) | `IN`, `OUT`, `ADJUST` |
| `quantity` | int | 변동 수량 |
| `reference_type` | varchar(50) | `PURCHASE_ORDER`, `MANUAL` |
| `reference_id` | bigint | 참조 대상 ID, nullable |
| `moved_at` | datetime | 변동 시각 |
| `note` | varchar(255) | 비고 |
| `created_at` | datetime | 생성 시각 |

비고:

- 재고 화면은 `inventories`를 보고, 감사성 추적은 `stock_movements`를 기준으로 본다.

---

## 6. 핵심 관계 요약

```text
users 1---N user_roles N---1 roles
roles 1---N menu_permissions N---1 menus
users 1---N audit_logs

users 1---0..1 employees
employees 1---N attendances
employees 1---N payrolls
employees 1---N expenses

vendors 1---N items
items 1---N inventories
vendors 1---N purchase_orders
purchase_orders 1---N purchase_order_items
items 1---N purchase_order_items
items 1---N stock_movements
inventories 1---N stock_movements
```

주의:

- `users`와 `employees`는 1:1 강제 여부를 팀에서 먼저 정해야 한다.
- `vendors`와 `items`는 기본 공급처만 둘지, 다대다 공급 가능 구조를 만들지 후속 결정이 필요하다.

---

## 7. MVP에서 먼저 만들 테이블

1차 권장 순서:

1. `users`
2. `roles`
3. `user_roles`
4. `employees`
5. `vendors`
6. `items`
7. `inventories`
8. `purchase_orders`
9. `purchase_order_items`
10. `attendances`

2차 확장:

- `menus`
- `menu_permissions`
- `audit_logs`
- `payrolls`
- `expenses`
- `stock_movements`

---

## 8. 조장이 팀원에게 넘길 때 체크할 것

- `employee`와 `user`를 분리할지 통합할지
- 부서를 문자열로 둘지 별도 테이블로 뺄지
- 창고를 단일 위치로 볼지 다중 창고로 갈지
- 발주 승인 흐름을 단일 승인으로 둘지 다단계로 갈지
- 비용/급여 상태값을 얼마나 세분화할지

---

## 9. 다음 단계

- 이 문서를 기준으로 간단한 ERD 스케치 작성
- 팀원별 담당 테이블 컬럼 상세 보강
- API 초안과 필드명 맞춤
- 이후 실제 JPA Entity 또는 SQL DDL로 변환
