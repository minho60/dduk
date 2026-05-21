# DDUK ERP - AI & RPA 연동 및 구동 가이드라인

이 문서는 DDUK ERP 로컬 개발 환경에서 AI 서버와 RPA 서버를 함께 띄우고, 연동 상태를 확인하는 실행 가이드다.

---

## 1. 연동 구조

현재 로컬 구조는 아래 흐름을 기준으로 동작한다.

- 프론트: `http://localhost:5500`
- 백엔드: `http://localhost:8080`
- AI 서버: `http://localhost:5000`
- RPA 서버: `http://localhost:5050`

동작 흐름:

1. 프론트는 백엔드를 호출한다.
2. 백엔드는 AI 서버를 프록시 호출한다.
3. 백엔드는 RPA 서버에 비동기 트리거 요청을 보낸다.
4. RPA 작업 완료 후 백엔드 콜백(`/api/v1/callbacks/rpa`)으로 결과를 돌려준다.

---

## 2. 주요 환경변수

프로젝트 루트 `.env` 파일에 아래 값이 준비되어 있어야 한다.

```env
BACKEND_API_BASE_URL=http://localhost:8080/api/v1
BACKEND_CALLBACK_URL=http://localhost:8080/api/v1/callbacks/rpa

AI_SERVER_HOST=127.0.0.1
AI_SERVER_PORT=5000

RPA_SERVER_HOST=127.0.0.1
RPA_SERVER_PORT=5050
RPA_BASE_URL=http://localhost:5050
RPA_CALLBACK_TOKEN=replace_with_secure_token
RPA_VENDOR_USERNAME=vendor_agent
RPA_VENDOR_PASSWORD=replace_with_password
PLAYWRIGHT_TIMEOUT_MS=30000
```

주의:

- `RPA_CALLBACK_TOKEN`은 백엔드와 RPA에서 같은 값을 써야 한다.
- 현재 기준 포트는 AI `5000`, RPA `5050`, 프론트 `5500`, 백엔드 `8080`이다.

---

## 3. 로컬 테스트 통합 구동 가이드

### 🚀 원클릭 일괄 자동 기동 (`ai-all-start.bat`)
번거로운 파이썬 가상환경 구성(venv), 필수 패키지 설치(`pip install`), 브라우저 다운로드(`playwright install`) 및 각 서버 기동/포트 점검 단계를 **전부 하나로 자동 통합**했습니다.

* **가동 대상**: Spring Boot 백엔드(8080) + 프론트엔드(5500) + AI Flask(5000) + RPA Flask(5050)
* **실행 방법**:
  ```powershell
  # 프로젝트 루트에서 일괄 기동 스크립트 실행 (혹은 윈도우 탐색기에서 더블클릭)
  .\ai-all-start.bat
  ```

> [!IMPORTANT]
> **최초 기동 시 소요 시간 안내 (1~3분)**
> 최초 실행 시에는 가상환경(venv) 구축, 종속 패키지 설치, Playwright 웹 브라우저 엔진 다운로드 등의 **환경 설정이 백그라운드에서 완전 자동으로 가동**됩니다. 이 과정에서 검은색 콘솔 창이 잠시 멈춘 것처럼 보일 수 있으나 오류가 아니므로 **절대로 창을 닫지 마시고** 연녹색 성공 요약판이 나올 때까지 대기해 주세요. (2회차 구동부터는 기 구축된 환경을 그대로 재사용하므로 5초 이내로 즉시 켜집니다.)

* **자동 처리 흐름 (E2E Zero-Config)**:
  1. **가상환경 감출 및 생성**: AI 및 RPA 서버 폴더에 파이썬 가상환경(`venv`)이 없는 경우 자동으로 생성합니다.
  2. **의존성 자동 적재**: 각 모듈의 `requirements.txt`에 명시된 필수 패키지들을 자동으로 pip 설치합니다.
  3. **Playwright 엔진 다운로드**: RPA 가동에 필요한 `Chromium` 브라우저 바이너리를 무인으로 다운로드합니다.
  4. **헬스체크 및 포트 오픈 대기**: 4대 서버가 정상적으로 기동되어 가용 상태가 될 때까지 대기(Health Checking)한 후, 최종 연녹색 성공 메시지를 출력하며 ERP 대시보드 로그인 화면(`http://localhost:5500/`)을 자동으로 오픈합니다.

### 3-2. 백엔드 + 프론트만 실행

AI/RPA 없이 기본 ERP 화면만 먼저 띄우려면 아래 배치 파일을 사용한다.

```powershell
.\start-local.bat
```

동일한 별칭:

```powershell
.\local-start.bat
```

---

## 4. AI / RPA 서버가 실제로 열렸는지 확인하는 방법

### 4-1. 브라우저 확인

아래 주소를 직접 열어 본다.

- AI 서버: [http://localhost:5000/health](http://localhost:5000/health)
- RPA 서버: [http://localhost:5050/health](http://localhost:5050/health)

정상일 경우 아래와 비슷한 응답이 보인다.

```json
{"status":"UP"}
```

### 4-2. PowerShell 확인

상태코드만 빠르게 확인:

```powershell
(Invoke-WebRequest http://localhost:5000/health -UseBasicParsing).StatusCode
(Invoke-WebRequest http://localhost:5050/health -UseBasicParsing).StatusCode
```

정상이면 둘 다 `200`이다.

응답 본문까지 확인:

```powershell
Invoke-WebRequest http://localhost:5000/health -UseBasicParsing
Invoke-WebRequest http://localhost:5050/health -UseBasicParsing
```

### 4-3. 포트 리슨 확인

서버가 포트에 바인딩됐는지 보려면:

```powershell
Get-NetTCPConnection -LocalPort 5000 -State Listen
Get-NetTCPConnection -LocalPort 5050 -State Listen
```

리스닝 정보가 조회되면 포트 오픈 상태다.

### 4-4. 로그 확인

실행 로그는 `.local-run` 아래 실행 시각별 폴더로 저장된다.

예시:

```text
C:\kmh\dduk\.local-run\20260520-161943\
```

주요 로그:

- `ai-server.out.log`
- `ai-server.err.log`
- `rpa-server.out.log`
- `rpa-server.err.log`
- `backend.out.log`
- `backend.err.log`

최신 로그 폴더 찾기:

```powershell
Get-ChildItem C:\kmh\dduk\.local-run | Sort-Object LastWriteTime -Descending | Select-Object -First 1
```

예를 들어 AI 에러 로그 마지막 50줄 보기:

```powershell
Get-Content "C:\kmh\dduk\.local-run\최신폴더명\ai-server.err.log" -Tail 50
```

---

## 5. RPA 트리거 기본 테스트

RPA 서버가 정상 기동된 후 아래 요청으로 트리거 엔드포인트를 확인할 수 있다.

```powershell
curl.exe -X POST http://localhost:5050/api/v1/rpa/trigger `
  -H "Content-Type: application/json" `
  -d "{\"taskId\":\"manual-test-001\",\"action\":\"collect_purchase_orders\"}"
```

정상이면 `202 Accepted` 계열 응답과 함께 task id가 반환된다.

동시성 제한 테스트 예시:

```powershell
1..4 | ForEach-Object -Parallel {
    curl.exe -X POST http://localhost:5050/api/v1/rpa/trigger `
      -H "Content-Type: application/json" `
      -d "{\"taskId\":\"load-test-$($_)\",\"action\":\"collect_purchase_orders\"}"
}
```

정상 동작이면 3개까지는 수락되고, 초과 요청은 `429`로 거절될 수 있다.

---

## 6. 핵심 구현 메커니즘 요약

### 6-1. 동시성 제한

RPA 서버는 세마포어로 동시 실행 작업 수를 제한한다.

- 목표: 브라우저 프로세스 과다 생성 방지
- 초과 시: `429 Too Many Requests`

### 6-2. 120초 작업 제한

RPA 작업 프로세스는 최대 120초까지만 허용된다.

- 초과 시 프로세스 강제 종료
- 백엔드로 실패 콜백 전송

### 6-3. 최대 3회 재시도

Playwright 작업은 일시적 페이지 지연에 대비해 최대 3회 재시도한다.

- 실패 시 스크린샷 저장
- 마지막 실패 스크린샷 경로를 콜백 payload에 포함

---

## 7. Gemini API Key 등록 방법

AI 챗봇 기능을 쓰려면 Gemini API Key가 필요하다.

1. [Google AI Studio](https://aistudio.google.com/) 접속
2. Google 계정 로그인
3. `Get API key` 또는 `Create API key` 선택
4. 발급된 키를 루트 `.env`에 등록

예시:

```env
GEMINI_API_KEY=replace_with_real_gemini_api_key
```

AI 서버 재기동 후 적용된다.

---

## 8. 자주 보는 실패 원인

### AI 서버가 안 뜰 때

- `ModuleNotFoundError: No module named 'flask'`
  - `venv`는 있는데 패키지 설치가 중간에 끊긴 상태일 가능성이 크다.
  - `ai-server\venv\Scripts\pip.exe install -r requirements.txt` 재실행

### RPA 서버가 안 뜰 때

- `playwright` 관련 import 에러
  - `rpa\venv\Scripts\pip.exe install -r requirements.txt`
  - `rpa\venv\Scripts\playwright.exe install chromium`

### health 는 뜨는데 기능이 안 될 때

- 프론트가 `5500` 정적 서버로 API를 치는지 확인
- 백엔드 API base URL이 `8080` 기준인지 확인
- `RPA_CALLBACK_TOKEN` 값이 백엔드와 RPA에서 일치하는지 확인

---

## 9. 빠른 체크리스트

1. `.env` 존재 확인
2. `ai-all-start.bat` 실행
3. `http://localhost:5000/health` 확인
4. `http://localhost:5050/health` 확인
5. 로그인 화면 진입 확인
6. 필요 시 `.local-run` 최신 로그 확인

