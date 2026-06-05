# 변경 이력 — 2026-06-05

> **작성 기준:** 2026-06-05 (배포 환경: Render / DB: TiDB Cloud)

---

## 📋 요약

| # | 구분 | 파일 | 내용 |
|---|------|------|------|
| 1 | 🐛 Fix | `Dockerfile` | 배포 시 frontend 파일 누락 문제 수정 |
| 2 | 🐛 Fix | `ApiExceptionHandler.java` | 정적 리소스 미존재 시 500 대신 404 반환 |
| 3 | 🐛 Fix | `frontend/services/admin/auth.js` | localhost 하드코딩 제거 및 인코딩 깨짐 완전 수정 |
| 4 | 🐛 Fix | `frontend/services/common/session.js` | API base URL 동적 분기 처리 |

---

## 🔍 발생 증상

```
{"status":"error","data":null,"message":"서버 내부 오류가 발생했습니다.","code":"INTERNAL_ERROR","errors":null}
```

- `ddukerp.onrender.com` 접속 시 위 에러 응답 반환
- 로그인 버튼 클릭 시 URL에 `?companyCode=...&loginId=admin&password=...` 가 붙으며 페이지가 새로고침만 되고 대시보드로 이동 안 됨

---

## 📝 상세 수정 내역

### 1. `Dockerfile` — frontend 파일 JAR 포함 누락

**원인:** Spring Boot JAR 빌드 시 `frontend/` 폴더가 복사되지 않아 정적 파일을 서빙할 수 없었음

```dockerfile
# 수정 전
COPY backend /backend

# 수정 후
COPY backend /backend
COPY frontend /frontend   # ← 추가
```

---

### 2. `backend/.../config/ApiExceptionHandler.java` — 정적 리소스 없을 때 500 오류

**원인:** `NoResourceFoundException`, `NoHandlerFoundException` 에 대한 핸들러가 없어 Spring 기본 500 응답이 반환됨

```java
// 추가된 핸들러
@ExceptionHandler({NoResourceFoundException.class, NoHandlerFoundException.class})
@ResponseStatus(HttpStatus.NOT_FOUND)
public ApiResponse<Void> handleNotFound(Exception ex) {
    return ApiResponse.error("NOT_FOUND", "요청한 리소스를 찾을 수 없습니다.");
}
```

---

### 3. `frontend/services/admin/auth.js` — localhost 하드코딩 및 인코딩 깨짐

**원인 1 — localhost 하드코딩:**
개발 환경 주소(`http://localhost:8080`)가 그대로 코드에 남아 있어 Render 배포 환경에서 API 호출 실패

**원인 2 — 인코딩 깨짐 (가장 큰 원인):**
수정 과정에서 PowerShell이 파일을 저장할 때 한국어 문자열이 깨지며 JS 문법 오류 발생
→ `setLoginButtonText("로그인")` 의 닫는 따옴표 누락 → 스크립트 전체 실행 불가
→ 폼이 JS 없이 브라우저 기본 동작(GET)으로 제출됨 → URL에 파라미터가 노출되고 대시보드 이동 안 됨

**최종 수정 내용:**
```javascript
// 운영/로컬 환경 자동 분기
const API_BASE_URL = ["localhost", "127.0.0.1"].includes(window.location.hostname)
    ? "http://localhost:8080"
    : "";

// fetch 호출 (상대 경로 사용)
const response = await fetch(API_BASE_URL + "/api/v1/auth/login", { ... });
```

- 파일 전체를 UTF-8(BOM 없음, LF) 로 재작성하여 인코딩 문제 근본 해결
- 템플릿 리터럴 내 한국어 제거 → ASCII 문자열로 대체하여 인코딩 재발 방지

---

### 4. `frontend/services/common/session.js` — API base URL 동적 처리

**원인:** `getApiBaseUrl()` 함수가 항상 `http://localhost:8080` 을 반환하도록 고정되어 있었음

```javascript
// 수정 전
function getApiBaseUrl() {
    return "http://localhost:8080";
}

// 수정 후
function getApiBaseUrl() {
    return ["localhost", "127.0.0.1"].includes(window.location.hostname)
        ? "http://localhost:8080"
        : "";
}
```

---

## 🔐 테스트 계정 정보

| 역할 | 아이디 | 비밀번호 |
|------|--------|----------|
| 관리자 | `admin` | `admin123` |
| 인사 | `hr` | `hr123` |
| 재고 | `inventory` | `inv123` |

> 비밀번호는 BCrypt 해시로 저장됨 (`dduk_bootstrap_data.sql`)

---

## 🚀 Git 커밋 이력

| 커밋 해시 | 메시지 |
|-----------|--------|
| `847f4a7` | first commit |
| `2f83932` | Fix: Render 배포 시 frontend 누락 및 localhost API 주소 하드코딩 문제 수정 |
| `311cdfe` | Fix: auth.js 인코딩 깨짐 및 문법 오류 수정 (1차) |
| `1f8cf1f` | Fix: auth.js 인코딩 완전 재작성 - 문법 오류 최종 수정 |

---

## ⚙️ 배포 환경

| 항목 | 값 |
|------|----|
| 배포 플랫폼 | Render (Docker 기반) |
| DB | TiDB Cloud (MySQL 호환) |
| 프론트 서빙 방식 | Spring Boot 정적 리소스 (`processResources` → JAR 포함) |
| API 베이스 URL | 운영: 상대경로 `""` / 로컬: `http://localhost:8080` |
