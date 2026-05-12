# DDUK ERP 환경변수 가이드

이 문서는 팀원이 각 서비스의 실행 환경을 맞출 때 참고하는 기준 문서다.

## 1. 목적

현재 프로젝트는 클라우드 네이티브 환경을 위해 **TiDB**를 기본 데이터베이스로 사용한다. 로컬 테스트나 소규모 개발 시에는 MySQL로 대체 가능하지만, 팀 공용 설정은 TiDB를 기준으로 한다.

- 통합 sample: [/.env.example](file:///c:/kmh/dduk/.env.example) (프로젝트 최상위)

## 3. 사용 원칙

1. `.env.example`는 샘플 파일이다.
2. 실제 비밀번호, secret, API key는 넣지 않는다.
3. 각 팀원은 `.env.example`를 참고해서 로컬 `.env` 또는 로컬 환경변수를 직접 만든다.
4. 실제 `.env` 파일은 Git에 올리지 않는다.

## 4. Backend 변수 설명

`backend`는 현재 `application.yml`에서 아래 환경변수를 읽는다.

### 필수

- `DB_URL`: 현재 DB 접속 문자열
- `DB_USERNAME`: DB 계정
- `DB_PASSWORD`: DB 비밀번호
- `JWT_SECRET`: JWT 서명용 secret

### 선택

- `JPA_DDL_AUTO`: 로컬은 보통 `update`
- `JWT_EXPIRATION`: 토큰 만료 시간
- `DB_DIALECT`: 현재는 참고용, 추후 DB 전환 메모용
- `DB_POOL_SIZE`: 추후 커넥션 튜닝 참고용

### TiDB 연결 예시 (기본)

```env
DB_URL=jdbc:mysql://{TIDB_HOST}:4000/dduk_erp?useSSL=true&serverTimezone=Asia/Seoul
DB_USERNAME={TIDB_USER}
DB_PASSWORD={TIDB_PASSWORD}
JPA_DDL_AUTO=update
JWT_SECRET=replace_with_a_long_random_secret
JWT_EXPIRATION=86400000
```

### 로컬 MySQL 연결 예시 (대체)

로컬에 MySQL이 설치된 경우 아래와 같이 설정할 수 있다.

```env
DB_URL=jdbc:mysql://localhost:3306/dduk_erp?useSSL=false&serverTimezone=Asia/Seoul&allowPublicKeyRetrieval=true
DB_USERNAME=root
DB_PASSWORD=change_me
JPA_DDL_AUTO=update
```

## 5. AI Server 변수 설명

`ai-server`는 아직 본격 구현 전이지만, 아래 구조를 기준으로 맞춘다.

### 필수 후보

- `AI_SERVER_HOST`
- `AI_SERVER_PORT`
- `OPENAI_API_KEY`
- `OPENAI_MODEL`

### 선택 후보

- `OCR_PROVIDER`
- `OCR_API_KEY`
- `EXTERNAL_API_BASE_URL`
- `EXTERNAL_API_KEY`
- `BACKEND_API_BASE_URL`

### 예시

```env
FLASK_ENV=development
FLASK_DEBUG=true
AI_SERVER_HOST=127.0.0.1
AI_SERVER_PORT=5000
OPENAI_API_KEY=replace_with_api_key
OPENAI_MODEL=gpt-5.4-mini
BACKEND_API_BASE_URL=http://localhost:8080/api/v1
DEFAULT_TIMEZONE=Asia/Seoul
```

## 6. RPA 변수 설명

`rpa`는 현재 Playwright 자동화 기준으로 아래 값을 샘플로 둔다.

### 주요 값

- `RPA_BROWSER`
- `RPA_HEADLESS`
- `RPA_BASE_URL`
- `RPA_LOGIN_ID`
- `RPA_LOGIN_PASSWORD`
- `PLAYWRIGHT_TIMEOUT_MS`

### 예시

```env
RPA_ENV=development
RPA_BROWSER=chromium
RPA_HEADLESS=true
RPA_BASE_URL=http://localhost:5500
RPA_LOGIN_ID=admin
RPA_LOGIN_PASSWORD=admin123
PLAYWRIGHT_TIMEOUT_MS=30000
```

## 7. 팀 배포 방식

권장 방식은 아래다.

1. 저장소에는 최상위 루트에 `.env.example`만 포함
2. 팀원은 루트의 `.env.example`을 참고해 각 서비스 폴더에 `.env` 생성 (또는 필요한 값만 복사)
3. 실제 실행은 로컬 환경변수 또는 각자 사용하는 실행 도구에서 주입
4. secret 변경이 생기면 통합 샘플 파일의 키 이름만 업데이트하여 공유

## 8. 주의사항

- 테스트용 계정과 비밀번호는 로컬 개발 기준이다.
- 운영용 비밀번호나 API key를 샘플 파일에 넣으면 안 된다.
- backend는 현재 `.env` 파일을 자동으로 읽는 구조가 아니라, 실행 환경에 값이 들어가야 한다.
- 나중에 TiDB를 붙이더라도 변수 이름은 `DB_*` 형태를 유지하는 게 좋다.

## 9. 추천 다음 작업

다음 단계로 이어지면 좋은 건 아래다.

1. `.gitignore`에 서비스별 `.env` 패턴 확인 또는 보강
2. backend 실행 스크립트에서 env 주입 방식 통일
3. ai-server와 rpa 실제 엔트리 파일이 생기면 해당 서비스 실행 예시 추가
