# RPA 상태 유지 기능 구현 계획서

이 문서는 공용 사이드바 RPA 제어 위젯과 역할별 대시보드에서 RPA를 실행한 뒤,
페이지 이동이나 새로고침 이후에도 직전 작업 상태를 이어서 확인할 수 있게 만드는
프론트엔드 상태 유지 계획을 정리한다.

## 1. 배경

- 현재 공용 RPA 제어 위젯은 [sidebar.js](/C:/kmh/dduk/frontend/services/common/sidebar.js:1338)의 메모리 객체 `rpaState`만 사용한다.
- 그래서 같은 페이지 안에서는 상태가 유지되지만, 메뉴 이동이나 새로고침 이후에는 `taskId`, `status`, `actionName`, 안내 메시지가 초기화된다.
- 최근 작업에서 공용 위젯의 `taskType` 매핑은 `PURCHASE_PRICE`, `INVENTORY_SHORTAGE`, `HR_MIN_WAGE` 기준으로 정리했다.
- 최근 작업에서 `/api/v1/admin/rpa/**`, `/api/v1/admin/tasks/**` 접근 권한도 [SecurityConfig.java](/C:/kmh/dduk/backend/src/main/java/com/dduk/config/SecurityConfig.java:61) 기준으로 `ADMIN`, `HR`, `INVENTORY`까지 확장했다.
- 따라서 이제 남은 UX 과제는 "실행 권한"보다 "실행 후 상태 복원"이다.

## 2. 현재 코드 기준 정리

### 2.1 공용 위젯

- 위치: [sidebar.js](/C:/kmh/dduk/frontend/services/common/sidebar.js:1338)
- 현재 기능
  - `getRpaContext()`로 현재 페이지별 `taskType` 결정
  - `triggerRpa()`로 `/api/v1/admin/rpa/trigger` 호출
  - `pollTask()`로 `/api/v1/admin/tasks/{taskId}` 상태 조회
  - `fetchAndRenderRpaResult()`로 구매/재고 결과 요약 렌더
- 현재 한계
  - 메모리 상태만 사용
  - 새로고침 후 마지막 실행 상태 복원 불가
  - 성공/실패 후 마지막 메시지 복원 불가
  - 페이지 이동 후 진행 중 작업 이어받기 불가

### 2.2 역할별 전용 페이지

- inventory 메인: [inventory-dashboard-page.js](/C:/kmh/dduk/frontend/services/inventory/inventory-dashboard-page.js:248)
- inventory 구매: [inventory-purchase-dashboard-page.js](/C:/kmh/dduk/frontend/services/inventory/inventory-purchase-dashboard-page.js:347)
- hr 급여 기준 비교: [hr-backend-pages-payroll.js](/C:/kmh/dduk/frontend/services/hr/hr-backend-pages-payroll.js:119)

전용 페이지들은 각자 최근 이력과 요약 API를 다시 호출하는 구조라 기본 복원력은 공용 위젯보다 낫다.
다만 "직전 버튼 상태", "진행 중 폴링 이어받기", "최근 taskId 표시 유지", "직전 상태 메시지 유지"는 여전히 부족하다.

## 3. 목표

- 다른 메뉴로 이동했다가 돌아와도 마지막 RPA 작업 상태를 다시 보여준다.
- `REQUESTED`, `RUNNING` 상태면 자동 폴링을 재개한다.
- `SUCCESS`, `FAILED` 상태면 마지막 상태 메시지와 가능한 범위의 결과 영역을 복원한다.
- 상태 복원은 현재 페이지와 현재 권한이 허용하는 `taskType` 범위 안에서만 동작한다.

현재 기준 역할 범위:

- 공용 위젯 실행/상태 조회: `ADMIN`, `HR`, `INVENTORY`
- hr 기준 조회 페이지: `ADMIN`, `HR`
- inventory 상세/집계 API: `ADMIN`, `INVENTORY`

## 4. 비목표

- 백엔드 task history 스키마 변경
- 새로운 RPA taskType 추가
- 미구현 placeholder 탭을 실제 기능으로 확장
- 브라우저 종료 이후까지 영구 복원

## 5. 상태 저장 전략

### 5.1 저장소

- `sessionStorage` 사용
- 키 이름: `dduk_rpa_states`

선택 이유:

- 탭 단위 상태 유지 목적에 맞다.
- 시연 흐름에서 "새로고침 또는 메뉴 이동 후 복원" 요구를 충족한다.
- `localStorage`보다 이전 세션 잔존 상태로 인한 오동작 위험이 낮다.

주의:

- `sessionStorage`는 브라우저 탭 단위다.
- 브라우저를 완전히 닫으면 상태는 유지되지 않는다.
- 따라서 이 기능은 "영구 이력 저장"이 아니라 "현재 시연 세션 복원"으로 정의해야 한다.

### 5.2 저장 구조

```json
{
  "PURCHASE_PRICE": {
    "taskId": "rpa-task-12345678",
    "actionName": "collect_purchase_orders",
    "status": "RUNNING",
    "lastUpdated": "2026-06-02T11:30:00.000Z",
    "message": "RPA 요청이 접수되어 실행 중입니다."
  },
  "INVENTORY_SHORTAGE": {
    "taskId": "rpa-task-87654321",
    "actionName": "check_inventory_shortage",
    "status": "SUCCESS",
    "lastUpdated": "2026-06-02T11:31:20.000Z",
    "message": "RPA 작업이 성공적으로 완료되었습니다."
  },
  "HR_MIN_WAGE": {
    "taskId": "rpa-task-11223344",
    "actionName": "collect_hr_reference",
    "status": "FAILED",
    "lastUpdated": "2026-06-02T11:32:10.000Z",
    "message": "RPA 연동 호출이 실패했습니다."
  }
}
```

저장 필드 원칙:

- 저장 허용: `taskId`, `actionName`, `status`, `lastUpdated`, `message`
- 저장 금지: 응답 payload 원문, 결과 상세 원문, 인증 토큰, 사용자 민감정보

## 6. 구현 계획

### 6.1 공용 사이드바 위젯 보강

대상 파일:

- [sidebar.js](/C:/kmh/dduk/frontend/services/common/sidebar.js:1338)

추가 함수:

- `loadRpaStates()`
- `saveRpaState(taskType, patch)`
- `clearRpaState(taskType)`
- `restoreRpaStateForCurrentContext()`

동작 순서:

1. `getRpaContext()`로 현재 페이지의 `taskType`을 구한다.
2. 위젯 렌더 직후 `sessionStorage`에서 현재 `taskType` 상태만 읽는다.
3. 저장된 상태가 `REQUESTED` 또는 `RUNNING`이면 `pollTask(taskId, 0)`을 다시 시작한다.
4. 저장된 상태가 `SUCCESS` 또는 `FAILED`면 `updateSummary()`로 상태를 복원한다.
5. 결과 재조회가 가능한 경우에만 `fetchAndRenderRpaResult()`를 호출한다.
6. 404, 403, 파싱 오류가 나면 저장 상태를 정리하고 기본 UI로 복귀한다.

### 6.2 상태 저장 시점

- `triggerRpa()` 성공 직후
- `loadTaskDetail()` 결과 반영 직후
- `pollTask()` 상태 갱신 직후
- `pollTask()` 예외 처리 직후
- `updateSummary(detail)` 이후

### 6.3 전용 페이지 보강

대상 파일:

- [inventory-dashboard-page.js](/C:/kmh/dduk/frontend/services/inventory/inventory-dashboard-page.js:248)
- [inventory-purchase-dashboard-page.js](/C:/kmh/dduk/frontend/services/inventory/inventory-purchase-dashboard-page.js:347)
- [hr-backend-pages-payroll.js](/C:/kmh/dduk/frontend/services/hr/hr-backend-pages-payroll.js:119)

권장 방식:

- 공용 `sessionStorage` 구조를 재사용한다.
- 전용 페이지는 이미 최근 이력/요약 API가 있으므로 "상세 결과 복원"보다 `taskId`, `status`, `message`, `RUNNING` 재조회에 집중한다.

inventory 페이지:

- 마지막 요청 `taskId`가 있으면 초기 로딩 시 최근 이력 조회를 먼저 시도한다.
- `REQUESTED` 또는 `RUNNING`이면 페이지 자체 폴링을 재개한다.
- 결과 상세 재조회는 현재 세션 역할이 inventory 계열 API 접근 권한을 가질 때만 수행한다.

hr 페이지:

- `HR_MIN_WAGE` 상태를 `sessionStorage`에서 읽어 `hr_reference_message`, `hr_reference_status_badge`에 먼저 반영한다.
- 이후 기존 `loadPayrollReferenceSummary()`로 서버 최신값과 동기화한다.

## 7. 주의사항 및 설계 보완

### 7.1 권한과 결과 복원을 분리한다

- `/api/v1/admin/rpa/**`, `/api/v1/admin/tasks/**`는 `ADMIN`, `HR`, `INVENTORY`가 접근 가능하다.
- 하지만 `/api/v1/inventory/**`는 현재 [SecurityConfig.java](/C:/kmh/dduk/backend/src/main/java/com/dduk/config/SecurityConfig.java:71) 기준으로 `ADMIN`, `INVENTORY`만 접근 가능하다.
- 즉 `HR` 사용자는 공용 위젯에서 작업 실행과 상태 조회는 가능해도 inventory 결과 API 재조회는 403이 날 수 있다.
- 따라서 복원 로직은 "상태 복원"과 "결과 재조회"를 같은 단계로 묶으면 안 된다.

필수 보완:

- 상태 배지와 메시지는 권한과 무관하게 복원 가능
- 결과 상세 렌더는 현재 역할과 현재 페이지 API 권한을 확인한 뒤 선택적으로 수행
- 결과 재조회 실패 시 전체 복원을 실패 처리하지 말고 상태 메시지만 유지

### 7.2 현재 페이지의 taskType만 복원한다

- inventory 사용자가 hr 전용 결과를 직접 보거나
- hr 사용자가 inventory 전용 결과를 직접 보거나
- 다른 페이지의 오래된 상태가 현재 페이지 위젯을 덮어쓰는 교차 복원은 막아야 한다.

필수 보완:

- 복원 시 현재 `getRpaContext()`가 돌려주는 `taskType` 하나만 읽는다.
- 여러 `taskType` 전체를 한 번에 순회하면서 UI에 반영하지 않는다.

### 7.3 오래된 상태 정리 정책이 필요하다

- 시연 중 실패/성공 이후 오래 지난 상태가 남아 있으면 다음 시연에서 혼선이 생길 수 있다.

권장 정책:

- `SUCCESS`, `FAILED`는 1일 또는 다음 브라우저 탭 세션까지 유지
- 404 응답 또는 파싱 실패 시 즉시 제거
- `RUNNING` 상태가 장시간 갱신되지 않으면 "상태 확인 필요" 메시지로 내리고 폴링을 중단

### 7.4 sidebar.js 문자열 정리와 같이 보는 게 안전하다

- 현재 [sidebar.js](/C:/kmh/dduk/frontend/services/common/sidebar.js:1338)에는 콘솔 환경에서 문자열이 깨져 보이는 구간이 남아 있다.
- 상태 유지 로직을 추가하면서 같은 파일을 다시 건드리게 되므로, 구현 시점에는 UTF-8 문자열 정리까지 같이 보는 편이 안전하다.
- 다만 이번 계획 자체는 문자열 복구를 전제조건으로 두지 않고, 상태 저장/복원 로직을 우선 계약으로 고정한다.

### 7.5 민감한 데이터는 저장하지 않는다

- 결과 JSON 원문
- 백엔드 응답 payload 전체
- 사용자 식별 토큰
- 권한 판정에 쓰이는 민감 세션 데이터 원문

저장 대상은 UI 복원에 필요한 최소 메타데이터로 제한한다.

## 8. 예외 처리

- 저장된 `taskId`가 있는데 `/api/v1/admin/tasks/{taskId}`가 404면 상태를 제거한다.
- `SUCCESS`인데 결과 API가 비어 있거나 403/404면 상태는 유지하고 "결과 재조회 불가" 메시지를 보여준다.
- `sessionStorage` 파싱 실패 시 전체 키를 초기화하고 기본 `IDLE` 상태로 복귀한다.
- `RUNNING` 상태 복원 후 첫 조회가 실패하면 무한 폴링 대신 재시도 횟수를 제한한다.

## 9. 검증 계획

### 9.1 정적 검증

- `node --check frontend/services/common/sidebar.js`
- `node --check frontend/services/inventory/inventory-dashboard-page.js`
- `node --check frontend/services/inventory/inventory-purchase-dashboard-page.js`
- `node --check frontend/services/hr/hr-backend-pages-payroll.js`

### 9.2 동작 검증

공용 위젯:

1. inventory 대시보드에서 RPA 실행
2. 상태가 `REQUESTED` 또는 `RUNNING`일 때 다른 페이지로 이동
3. 다시 같은 페이지로 복귀
4. 마지막 `taskId`, 상태, 메시지가 복원되는지 확인
5. 권한이 있으면 결과 카드까지 다시 보이는지 확인

hr 기준 조회:

1. hr 계정으로 기준 조회 실행
2. 다른 hr/accounting 화면 이동
3. 다시 급여 기준 비교 영역으로 복귀
4. 마지막 상태/메시지가 먼저 보이고 이후 서버 최신값으로 동기화되는지 확인

권한 경계:

1. `HR` 세션에서 inventory 관련 공용 위젯 실행
2. 상태 복원은 되지만 inventory 상세 결과 재조회 실패가 전체 위젯 오류로 번지지 않는지 확인
3. `INVENTORY` 세션에서 hr 전용 상태가 교차 복원되지 않는지 확인

## 10. 구현 우선순위

1. 공용 위젯 `sessionStorage` 상태 저장/복원
2. 공용 위젯 `RUNNING` 자동 폴링 재개
3. 권한 분리 기반 결과 재조회 가드 추가
4. inventory 전용 페이지 메시지/상태 복원
5. hr 기준 조회 페이지 메시지/상태 복원
6. 오래된 상태 정리 정책 반영

## 11. 현재 판단

- 이 기능은 현재 코드 구조에서 충분히 구현 가능하다.
- 특히 공용 위젯은 이미 `rpaState`, `triggerRpa()`, `pollTask()`, `loadTaskDetail()`이 있어 `sessionStorage` 계층만 추가하면 된다.
- 다만 구현 시 주의점은 분명하다.
  - 상태 복원과 결과 재조회는 분리해야 한다.
  - 현재 페이지의 `taskType`만 복원해야 한다.
  - `HR`이 inventory 결과 API를 바로 읽는 흐름은 권한상 막힐 수 있다.
  - `sidebar.js` 문자열 정리는 같은 작업 창에서 같이 점검하는 편이 안전하다.
