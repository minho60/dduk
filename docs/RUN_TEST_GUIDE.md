# DDUK ERP 실행 및 테스트 가이드

이 문서는 팀원이 현재 프로젝트를 로컬에서 실행하고, 로그인부터 역할별 페이지 확인까지 테스트할 수 있게 정리한 가이드다.

## 1. 준비 환경

아래 프로그램이 먼저 설치되어 있어야 한다.

- Java 21
- MySQL 8.0
- Python 3.10 이상
- Git

현재 `frontend`는 별도 빌드 도구 없이 정적 HTML/CSS/JS로 동작한다.

## 2. 현재 실행 대상

지금 바로 확인 가능한 범위는 아래다.

- backend 실행 시 전체 ERP 스키마 자동 생성
- 초기 계정 3개 자동 seed
- Spring Boot backend 실행
- `frontend/index.html`에서 로그인
- 역할별 메인 페이지 이동 확인
- 관리자 페이지에서 계정 목록/생성/권한 변경/활성 상태 변경 확인

## 3. 프로젝트 경로

- backend: `C:\kmh\dduk\backend`
- frontend: `C:\kmh\dduk\frontend`
- members schema SQL: `C:\kmh\dduk\backend\src\main\resources\db\mysql\dduk_members_schema.sql`
- members seed SQL: `C:\kmh\dduk\backend\src\main\resources\db\mysql\dduk_members_seed.sql`

## 4. DB 준비

### 4-1. MySQL 접속

MySQL 콘솔 또는 Workbench로 접속한다.

예시:

```bash
mysql -u root -p
```

### 4-2. 수동 SQL 실행은 현재 필수 아님

현재 backend는 실행 시 아래를 자동으로 처리하도록 설정되어 있다.

- `dduk_erp` DB 생성 시도
- 전체 ERP 스키마 생성
- 기본 계정 3개 seed

즉, **DB 서버만 살아 있으면 수동으로 schema/seed SQL을 먼저 실행하지 않아도 된다.**

### 4-3. DB 확인

```sql
USE dduk_erp;
SELECT id, login_id, name, role, active FROM members;
```

정상이라면 아래 3개 계정이 보여야 한다.

- `admin`
- `inventory`
- `hr`

## 5. 백엔드 실행

### 5-1. 팀원이 최신 backend 변경 받는 방법

작업 중인 파일이 있으면 먼저 커밋하거나 stash 한 뒤 진행한다.

```bash
git checkout 본인브랜치명
git fetch origin
git merge origin/erp-kmh
```

주의:

- `backend/src/main/resources/application.yml`
- `docs/`
- `backend/src/main/resources/db/`

위 경로를 본인이 수정 중이었다면 merge 전에 먼저 상태를 정리하는 게 좋다.

### 5-2. 환경변수 준비

`backend/src/main/resources/application.yml` 기준으로 아래 환경변수를 사용한다.

- `DB_URL`
- `DB_USERNAME`
- `DB_PASSWORD`
- `JWT_SECRET`
- `JWT_EXPIRATION` 선택
- `JPA_DDL_AUTO` 선택

예시 값:

```powershell
$env:DB_URL="jdbc:mysql://localhost:3306/dduk_erp?useSSL=false&serverTimezone=Asia/Seoul&allowPublicKeyRetrieval=true&createDatabaseIfNotExist=true"
$env:DB_USERNAME="root"
$env:DB_PASSWORD="여기에_DB_비밀번호"
$env:JWT_SECRET="여기에_충분히_긴_JWT_시크릿_문자열"
$env:JPA_DDL_AUTO="validate"
```

주의:

- `JWT_SECRET`은 `.env.example`의 예시 문구를 그대로 쓰면 안 된다.
- 반드시 실제 긴 문자열로 바꿔야 한다.
- `OPENAI_API_KEY`는 AI 기능을 쓰는 경우에만 실제 값이 필요하다.

### 5-3. backend 실행

`backend` 폴더로 이동해서 실행한다.

```bash
cd C:\kmh\dduk\backend
.\gradlew.bat bootRun
```

정상 실행되면 기본 포트는 `http://localhost:8080`이다.

backend 시작 시 기대 동작:

1. `dduk_erp` DB 생성 시도
2. 전체 스키마 자동 생성
3. 기본 계정 `admin`, `inventory`, `hr` 자동 적재
4. 이후 JPA가 현재 엔티티와 정합성 검사

### 5-4. 로그인 API 빠른 확인

로그인이 되는지 먼저 확인하고 싶으면 Postman 또는 curl로 호출한다.

```bash
curl -X POST http://localhost:8080/api/v1/auth/login ^
  -H "Content-Type: application/json" ^
  -d "{\"loginId\":\"admin\",\"password\":\"admin123\"}"
```

정상 응답 예시:

```json
{
  "token": "...",
  "loginId": "admin",
  "name": "System Admin",
  "role": "ADMIN"
}
```

## 6. 프론트 실행

### 6-1. 가장 간단한 방법

파일 탐색기에서 아래 파일을 직접 열어도 된다.

- `C:\kmh\dduk\frontend\index.html`

다만 브라우저 보안 정책이나 경로 문제를 줄이려면 간단한 정적 서버를 권장한다.

### 6-2. Python 정적 서버로 실행

```bash
cd C:\kmh\dduk\frontend
python -m http.server 5500
```

그다음 브라우저에서 아래 주소로 접속한다.

- `http://localhost:5500`

현재 프론트는 `localhost`에서 열리면 자동으로 backend API를 `http://localhost:8080`으로 호출한다.

## 7. 초기 로그인 계정

아래 계정으로 바로 테스트할 수 있다.

| 용도 | loginId | password | role |
| :--- | :--- | :--- | :--- |
| 관리자 | `admin` | `admin123` | `ADMIN` |
| 구매/발주 담당 | `inventory` | `inv123` | `INVENTORY` |
| 인사/회계 담당 | `hr` | `hr123` | `HR` |

## 8. 기본 테스트 시나리오

### 8-1. 관리자 로그인 확인

1. `frontend/index.html` 또는 `http://localhost:5500` 접속
2. `admin` / `admin123` 입력
3. 로그인 성공 후 관리자 대시보드 이동 확인

### 8-2. 관리자 계정 관리 확인

관리자 페이지에서 아래를 확인한다.

1. 계정 목록 조회
2. 새 계정 생성
3. 기존 계정 역할 변경
4. 계정 활성/비활성 변경

### 8-3. 역할별 접근 확인

#### 구매/발주 담당 계정

1. `inventory` / `inv123` 로그인
2. 재고/발주 메인 페이지 이동 확인
3. 관리자 페이지 직접 접근이 막히는지 확인

#### 인사/회계 담당 계정

1. `hr` / `hr123` 로그인
2. 인사/회계 메인 페이지 이동 확인
3. 관리자 페이지 직접 접근이 막히는지 확인

## 9. 관리자 페이지에서 새 계정 만들기

현재 구현 기준으로는 공개 회원가입이 없다.

새 계정은 반드시 관리자 계정으로 로그인한 뒤 관리자 페이지에서 만들어야 한다.

권장 확인 순서:

1. 관리자 로그인
2. 새 계정 생성
3. 역할을 `HR` 또는 `INVENTORY`로 지정
4. 새 계정으로 재로그인
5. 역할에 맞는 페이지로 이동하는지 확인

## 10. 자주 막히는 문제

### 10-1. 로그인 실패

확인할 것:

- backend가 `8080` 포트에서 실행 중인지
- seed SQL이 실제로 들어갔는지
- 비밀번호를 정확히 입력했는지
- 계정 `active` 값이 `1`인지

### 10-2. 스키마가 안 생김

확인할 것:

- DB 서버가 실제로 켜져 있는지
- `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`가 맞는지
- `DB_URL`에 `createDatabaseIfNotExist=true`가 포함돼 있는지
- backend 로그에서 SQL init 에러가 없는지

### 10-3. `JWT_SECRET` 오류

`application.yml`에서 `JWT_SECRET` 환경변수를 사용한다.  
예시 문구 그대로 두면 인증 토큰 생성이 정상 동작하지 않을 수 있다.

### 10-4. 프론트에서 API 호출 실패

확인할 것:

- 프론트를 `localhost` 기반으로 열었는지
- backend가 `http://localhost:8080`에서 실행 중인지
- 브라우저 콘솔에 CORS 에러가 있는지

### 10-5. `gradle` 명령이 안 됨

최근 업데이트를 통해 저장소에 **Gradle Wrapper**가 추가되었습니다. 로컬에 Gradle이 설치되어 있지 않아도 `./gradlew bootRun`(Linux/Mac) 또는 `.\gradlew.bat bootRun`(Windows) 명령어로 실행할 수 있습니다.

## 11. 팀원 체크리스트

작업 시작 전 아래를 체크하면 된다.

- MySQL 실행됨
- schema SQL 실행 완료
- seed SQL 실행 완료
- backend 실행됨
- frontend 접속됨
- 관리자 로그인 성공
- 자기 역할 계정 로그인 성공

## 13. UI 개발용 미리보기 모드 (Bypass Login)

백엔드 서버를 띄우지 않고 프론트엔드 UI 레이아웃만 빠르게 확인하고 싶을 때 사용하는 기능이다.

### 13-1. 설정 방법

1. `frontend/services/common/session.js` 파일을 연다.
2. **46번째 줄**의 주석을 해제한다.

```javascript
// 이 주석을 해제하면 모든 페이지에서 로그인 없이 '관리자' 권한으로 접속됩니다.
return { token: "preview", role: "ADMIN", userName: "미리보기 계정", loginId: "admin_preview" };
```

### 13-2. 주의 사항

- 이 모드에서는 서버와의 실제 데이터 통신이 불가능하므로, 화면에 목록이 비어 있거나 API 호출 에러가 발생할 수 있다.
- UI 디자인 및 컴포넌트 배치 확인 용도로만 사용하고, **실제 테스트 시에는 반드시 다시 주석 처리**해야 한다.

---

## 14. 다음 권장 작업

현재 뼈대 확인이 끝나면 다음 순서로 진행하면 된다.

1. 관리자 페이지에서 실제 계정 추가 테스트
2. `HR`, `INVENTORY` 도메인별 CRUD API 연결
3. 각 메인 페이지에 실제 목록 데이터 연결
