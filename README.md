# DDUK ERP Project

기업 운영에 필요한 인사, 회계, 구매, 재고 업무를 한 곳에서 관리하는 팀 프로젝트형 ERP 뼈대다.  
현재 문서는 "무엇을 만들지"보다 "팀원이 어떻게 바로 착수할지"에 초점을 맞춘 배포용 초안이다.

---

## 프로젝트 한눈에 보기

- 목표: 인사/회계, 구매/재고, 관리자 기능을 하나의 ERP 흐름으로 묶는다.
- 방향: 백엔드 중심의 업무 시스템에 프론트 UI, AI 보조 기능, RPA 자동화를 단계적으로 붙인다.
- 현재 상태: 저장소 구조와 협업 규칙은 잡혀 있고, 구현은 각 도메인별로 확장해 나가는 단계다.

### 핵심 도메인

| 도메인 | 설명 | 담당 |
| :--- | :--- | :--- |
| `hr` | 직원, 근태, 급여, 비용 처리 | 서우리 |
| `inventory` | 거래처, 발주, 입출고, 재고 추적 | 김슬기 |
| `admin` | 로그인, 권한, 대시보드, 공통 운영 기능 | 김민호 |

---

## MVP 범위

지금 단계에서 중요한 건 "한 번에 다 만드는 것"보다 "시연 가능한 최소 제품"을 먼저 닫는 거다.

### 1차 필수 MVP

- 로그인 / JWT 기반 인증
- 직원 기본 정보 조회 및 등록
- 근태 기록 조회
- 거래처 등록 및 목록 조회
- 재고 품목 등록 및 수량 조회
- 발주 등록 및 상태 변경
- 관리자 대시보드 기본 통계 카드

### 2차 확장 기능

- 급여 계산 및 명세서 생성
- 매출/지출 통계 시각화
- 발주 승인 흐름
- 재고 부족 알림
- WebSocket 기반 실시간 대시보드 갱신

### 3차 시연/차별화 기능

- OpenAI 기반 ERP 챗봇
- OCR 기반 영수증/발주서 텍스트 추출
- Playwright 기반 반복 업무 자동화
- AI 추천 발주 또는 이상 비용 탐지

---

## 기술 스택

| 영역 | 기술 |
| :--- | :--- |
| Frontend | HTML, CSS, Vanilla JS |
| Backend | Spring Boot 3.2+, Java 21, JPA, QueryDSL, Spring Security |
| AI Server | Python 3.10+, Flask, OpenAI SDK, Pandas |
| RPA | Playwright, OCR(PyTesseract 또는 EasyOCR) |
| Database | TiDB (MySQL Compatible) |
| Infra | Docker, Gradle, Postman |

스택은 넓지만 구현 우선순위는 MVP 기준으로 잘라서 간다.  
AI, OCR, RPA는 초반부터 모든 흐름에 넣지 말고 독립 기능으로 붙이는 걸 권장한다.

---

## 저장소 구조

```text
backend/    Spring Boot ERP API
frontend/   HTML/CSS/Vanilla JS UI
ai-server/  Flask 기반 AI API 및 데이터 보조 처리
rpa/        Playwright 자동화 작업
docs/       협업 규칙, 구조 문서, API 초안
```

더 구체적인 구조 기준은 아래 문서를 보면 된다.

- 구조 문서: [docs/ARCHITECTURE.md](/C:/kmh/dduk/docs/ARCHITECTURE.md:1)
- API 초안: [docs/API_DRAFT.md](/C:/kmh/dduk/docs/API_DRAFT.md:1)
- DB 초안: [docs/DB_DRAFT.md](/C:/kmh/dduk/docs/DB_DRAFT.md:1)
- 환경변수 가이드: [docs/ENV_GUIDE.md](/C:/kmh/dduk/docs/ENV_GUIDE.md:1) (통합 샘플: [.env.example](/.env.example))
- 실행/테스트 가이드: [docs/RUN_TEST_GUIDE.md](/C:/kmh/dduk/docs/RUN_TEST_GUIDE.md:1)
- AI 하네스: [docs/AI_HARNESS.md](/C:/kmh/dduk/docs/AI_HARNESS.md:1)
- 협업 규칙: [docs/CONVENTION.md](/C:/kmh/dduk/docs/CONVENTION.md:1)

---

## 빠른 시작

현재 저장소는 구현 초기 뼈대 단계라 일부 런타임 엔트리 파일은 아직 비어 있거나 미완성일 수 있다.  
아래는 팀 공통 기준으로 맞출 실행 순서다.

### 요구 환경

- Java 21
- Gradle Wrapper 또는 Gradle 실행 환경
- Python 3.10+
- MySQL 8.0
- Node.js

### 1. 저장소 준비

```bash
git clone https://github.com/minho60/dduk.git
cd dduk
```

### 2. 백엔드 실행

```bash
cd backend
./gradlew bootRun
```

기본 목표:

- 인증 API 응답 확인
- 도메인별 CRUD API 확장 준비

### 3. 프론트 확인

```bash
cd frontend
```

현재는 정적 구조 중심이므로 `index.html`을 기준 엔트리로 사용한다.

### 4. AI 서버 준비

```bash
cd ai-server
```

현재 폴더 구조는 준비되어 있지만 실제 앱 진입점 파일과 환경 변수 규칙은 구현하면서 확정해야 한다.

권장 초기 구조:

- `api/`: Flask route
- `services/`: AI/OCR 처리 로직
- `models/`: 요청/응답 모델
- `utils/`: 공통 유틸리티

### 5. RPA 준비

```bash
cd rpa
```

권장 역할:

- `tasks/`: 작업 시나리오
- `engine/`: Playwright 공통 실행기
- `outputs/`: 자동화 산출물

---

## 팀 작업 순서 제안

1. `admin`이 로그인/권한/공통 응답 구조를 먼저 고정한다.
2. `hr`, `inventory`가 각자 도메인 API와 화면을 병렬로 만든다.
3. 기본 CRUD와 대시보드가 붙으면 그 다음에 AI/OCR/RPA를 연결한다.

이 순서로 가면 초반 계약이 덜 흔들리고, 시연 가능한 상태를 빨리 만들 수 있다.

---

## 문서 읽는 순서

팀원에게는 아래 순서로 공유하면 된다.

1. [README.md](/C:/kmh/dduk/README.md:1)
2. [docs/AI_HARNESS.md](/C:/kmh/dduk/docs/AI_HARNESS.md:1)
3. [docs/CONVENTION.md](/C:/kmh/dduk/docs/CONVENTION.md:1)
4. [docs/ARCHITECTURE.md](/C:/kmh/dduk/docs/ARCHITECTURE.md:1)
5. [docs/API_DRAFT.md](/C:/kmh/dduk/docs/API_DRAFT.md:1)
6. [docs/DB_DRAFT.md](/C:/kmh/dduk/docs/DB_DRAFT.md:1)
7. [docs/ENV_GUIDE.md](/C:/kmh/dduk/docs/ENV_GUIDE.md:1)
8. [docs/RUN_TEST_GUIDE.md](/C:/kmh/dduk/docs/RUN_TEST_GUIDE.md:1)

---

## 메모

- 지금 문서는 구현 완료본이 아니라 착수용 기준 문서다.
- 실제 패키지명, DB 스키마, API 필드는 구현하면서 구체화하되, 공용 계약은 문서 먼저 갱신하고 움직인다.

---

© 2026 Team DDUK
