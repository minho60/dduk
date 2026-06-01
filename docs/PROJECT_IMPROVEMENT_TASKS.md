# DDUK ERP 포트폴리오/데모 기준 필수 작업

## 목적

- 운영 대비 확장성보다 포트폴리오 시연 완성도와 재현성에 직접 영향 주는 작업만 남긴다.
- 회계 미완료 항목과 운영 전환용 정리는 이번 우선순위에서 제외한다.

---

## 프론트엔드

### 1. 남아 있는 mock/in-memory/fallback 코드 제거

- 근거 파일:
  - `frontend/services/hr/payroll/payroll-service.js`
  - `frontend/services/hr/mock-accounting-data.js`
  - `frontend/services/common/dashboard.js`
- 왜 지금 해야 하나:
  - 데모에서 실제 API 화면과 mock 결과가 섞이면 시연 흐름이 바로 흔들린다.
  - 실패 시 mock으로 조용히 대체되면 버그가 가려져 포트폴리오 신뢰도가 떨어진다.
- 해야 할 일:
  - mock 데이터 파일 제거 또는 완전 분리
  - fallback to mock 제거
  - 실패는 명확한 empty/error state로 노출

### 2. 대형 서비스 파일 분해

- 근거 파일:
  - `frontend/services/admin/ocr-documents.js`
  - `frontend/services/hr/hr-backend-pages.js`
- 왜 지금 해야 하나:
  - 시연 직전 수정에서 한 파일 영향 범위가 너무 넓다.
  - OCR, AI/RPA, 인사 화면은 데모 포인트가 많아서 빠른 보수성이 중요하다.
- 해야 할 일:
  - API 호출, 상태 관리, 모달/UI 액션 로직 분리
  - 공통 유틸과 화면 전용 로직 분리

### 3. inventory 페이지 inline script 외부화

- 근거 파일:
  - `frontend/pages/inventory/reorder.html`
  - `frontend/pages/inventory/dashboard.html`
  - `frontend/pages/inventory/transfers.html`
  - `frontend/pages/inventory/purchase-order-request.html`
- 왜 지금 해야 하나:
  - 재고/발주 화면은 시연 비중이 큰데, HTML 안에 로직이 섞여 있으면 수정과 검증이 느리다.
  - 추천 발주, 이상 탐지, 발주 요청 흐름을 손볼 때 회귀 확인이 어렵다.
- 해야 할 일:
  - 페이지 초기화 로직을 `frontend/services/...` 로 이동
  - HTML은 구조와 마운트 포인트 중심으로 단순화

### 4. 공통 request/response/error 처리 통일

- 근거 파일:
  - `frontend/services/common/dashboard.js`
  - `frontend/services/admin/ocr-documents.js`
  - 여러 `frontend/services/*`
- 왜 지금 해야 하나:
  - 데모 중 권한 오류, 빈 데이터, API 오류가 화면마다 다르게 보이면 완성도가 떨어진다.
  - AI/RPA/OCR 같이 연결된 기능은 응답 처리 방식이 맞아야 시연 설명도 쉬워진다.
- 해야 할 일:
  - 공통 request wrapper 기준 정리
  - `Authorization`, `ApiResponse`, 에러 메시지 처리 방식 통일

---

## 백엔드

### 1. 시연 핵심 흐름 통합 테스트 보강

- 왜 지금 해야 하나:
  - 이 프로젝트 강점은 CRUD보다 AI/RPA/OCR과 재고 흐름이 연결되는 데 있다.
  - 포트폴리오에서는 "기능이 있다"보다 "끝까지 연결된다"는 증명이 더 중요하다.
- 필요한 시나리오:
  - 관리자 RPA trigger -> callback -> task history 적재
  - anomaly refresh -> 상태 전환 반영
  - recommendation `READY/REVIEW/DISABLED` 분기 확인
  - OCR 승인 후 후속 링크/연결 흐름 확인

---

## 지금 먼저 할 순서

1. 프론트 mock/fallback 제거
2. 프론트 공통 request/response/error 통일
3. 프론트 대형 파일 분해 + inventory inline script 외부화
4. 백엔드 시연 핵심 흐름 통합 테스트 보강
