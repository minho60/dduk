# DDUK ERP API 초안

이 문서는 프론트엔드, 백엔드, AI 서버가 동시에 작업할 수 있게 핵심 엔드포인트 초안을 정리한 문서다.  
완성 명세가 아니라 MVP 착수 기준이다.

---

## 1. 공통 규칙

- Base path: `/api/v1`
- Content-Type: `application/json`
- 인증 필요 API는 `Authorization: Bearer <token>` 사용
- 응답 포맷은 [docs/CONVENTION.md](/C:/kmh/dduk/docs/CONVENTION.md:102) 규칙을 따른다.

---

## 2. 인증 / 관리자

### 로그인

- `POST /api/v1/auth/login`

요청 예시:

```json
{
  "username": "admin",
  "password": "password"
}
```

응답 예시:

```json
{
  "status": "success",
  "data": {
    "accessToken": "jwt-access-token",
    "refreshToken": "jwt-refresh-token",
    "role": "ADMIN"
  },
  "message": "로그인에 성공했습니다."
}
```

### 내 정보 조회

- `GET /api/v1/auth/me`

### 관리자 대시보드 요약

- `GET /api/v1/admin/dashboard`

예상 항목:

- 총 직원 수
- 오늘 출근 수
- 재고 부족 품목 수
- 대기 중 발주 수

---

## 3. 인사 / 회계 (`hr`)

### 직원 목록 조회

- `GET /api/v1/employees?page=0&size=20&sort=createdAt,desc`

### 직원 등록

- `POST /api/v1/employees`

요청 예시:

```json
{
  "name": "홍길동",
  "department": "HR",
  "position": "Manager",
  "email": "hong@example.com"
}
```

### 직원 상세 조회

- `GET /api/v1/employees/{employeeId}`

### 근태 목록 조회

- `GET /api/v1/attendances?employeeId=1&month=2026-05`

### 근태 등록

- `POST /api/v1/attendances`

### 급여 계산 결과 조회

- `GET /api/v1/payrolls/{employeeId}?month=2026-05`

초기 MVP에서는 조회 중심으로 시작하고, 계산 로직은 2차 확장으로 둔다.

---

## 4. 구매 / 재고 (`inventory`)

### 거래처 목록 조회

- `GET /api/v1/vendors`

### 거래처 등록

- `POST /api/v1/vendors`

### 품목 목록 조회

- `GET /api/v1/items`

### 품목 등록

- `POST /api/v1/items`

### 재고 수량 조회

- `GET /api/v1/inventories`

### 재고 수량 조정

- `PATCH /api/v1/inventories/{inventoryId}`

### 발주 목록 조회

- `GET /api/v1/purchase-orders`

### 발주 등록

- `POST /api/v1/purchase-orders`

### 발주 상태 변경

- `PATCH /api/v1/purchase-orders/{purchaseOrderId}/status`

상태 예시:

- `REQUESTED`
- `APPROVED`
- `ORDERED`
- `RECEIVED`
- `CANCELED`

---

## 5. AI / 보조 기능

AI 서버와 백엔드를 분리할 경우 두 가지 방식 중 하나를 쓴다.

### 방식 A. 백엔드가 AI 서버를 호출

- 프론트 -> 백엔드 -> AI 서버
- 장점: 인증, 감사 로그, 권한 관리를 백엔드에서 통제하기 쉬움

### 방식 B. 프론트가 AI 서버를 직접 호출

- 프론트 -> AI 서버
- 장점: 단순하지만 권한/보안 정책이 흔들리기 쉬움

권장:

- 초기에는 방식 A를 기본으로 잡는다.

예시 엔드포인트:

- `POST /api/v1/ai/chat`
- `POST /api/v1/ai/expenses/analyze`
- `POST /api/v1/ai/ocr/receipt`

---

## 6. 에러 처리 초안

예시:

```json
{
  "status": "error",
  "message": "유효하지 않은 직원 ID입니다.",
  "code": "EMPLOYEE_NOT_FOUND"
}
```

권장 코드:

- `INVALID_REQUEST`
- `UNAUTHORIZED`
- `FORBIDDEN`
- `EMPLOYEE_NOT_FOUND`
- `INVENTORY_NOT_FOUND`
- `PURCHASE_ORDER_NOT_FOUND`

---

## 7. 프론트 작업 시작점

프론트는 아래 화면부터 만들면 된다.

1. 로그인 페이지
2. 관리자 대시보드
3. 직원 목록 / 등록 페이지
4. 거래처 목록 / 등록 페이지
5. 재고 목록 페이지
6. 발주 목록 / 등록 페이지

각 화면은 먼저 mock 데이터로 만들고, API 연결은 백엔드 엔드포인트가 고정된 뒤 붙인다.

---

## 8. 다음에 확정할 것

- 정확한 필드명
- 날짜/금액 포맷
- 페이지네이션 응답 구조
- 검색 조건 표준
- 파일 업로드 방식
- AI 서버 연동 인증 방식
