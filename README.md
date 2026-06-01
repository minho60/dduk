# DDUK ERP Project

기업 운영에 필요한 인사, 회계, 구매, 재고 업무를 한 곳에서 관리하는 통합 지능형 ERP 시스템이다.

---

## 프로젝트 개요

- **목표**: 구매/재고, 회계, AI 업무지원, 관리자 기능을 하나의 유기적인 ERP 흐름으로 통합한다.
- **방향**: 백엔드 중심의 탄탄한 비즈니스 로직을 기반으로 직관적인 Modern UI와 비동기 AI/RPA 자동화 엔진을 결합하여 업무 생산성을 극대화한다.
- **현재 상태**: 핵심 1차, 2차 MVP 설계 범위 및 3차 AI/RPA 연계 기능(OCR 증빙, 최저가 단가 비교, 최저임금 적정성 검증 등)까지 전체 구현 완료되어 안정적인 운영 및 통합 시연이 가능합니다.

---

## 핵심 도메인 및 담당 영역

| 담당자              | 도메인                     | 주요 기능 |
| :------------------ | :------------------------- | :--- |
| **서우리**          | 구매/발주, 문서/증빙, 회계 일부 | 구매/발주 대시보드, 발주 요청, 발주 관리, 입고 등록, 거래처 관리, 세금계산서, 비용 처리, 증빙 업로드, OCR 문서함 (Unlink 매핑 포함) |
| **김슬기**          | 재고관리, 회계관리 | 재고관리 대시보드, 재고 조회, 입출고 이력, 창고 이동, 자동 발주 추천, 회계 대시보드, 거래 내역 등록, 매입/매출 현황, 월 마감, 급여 계산 (최저임금 적정성 검증 포함), 회계 리포트 |
| **김민호**          | AI 업무지원, 관리자 | AI 챗봇, 이상 탐지, 예측 분석, 사용자 관리, 권한 관리 (Spring Security/JWT), 시스템 로그, 서버 모니터링, RPA 비동기 태스크 엔진 제어 |

---

## MVP 범위 및 구현 현황

본 프로젝트는 기획된 시연 가능한 필수 범위부터 차별화 기능까지 모두 성공적으로 구현을 완료했습니다.

### [x] 1차 필수 MVP (구현 완료)
- [x] 로그인 / JWT 기반 인증 및 Spring Security 권한 분기
- [x] 직원 기본 정보 조회 및 등록 (HR)
- [x] 근태 기록 조회
- [x] 거래처 등록 및 목록 조회
- [x] 재고 품목 등록 및 수량 조회
- [x] 발주 등록 및 상태 변경
- [x] 관리자 대시보드 기본 통계 카드

### [x] 2차 확장 기능 (구현 완료)
- [x] 급여 계산 및 명세서 생성
- [x] 매출/지출 통계 시각화
- [x] 발주 승인 흐름
- [x] 재고 부족 알림
- [x] WebSocket 기반 실시간 대시보드 갱신

### [x] 3차 시연/차별화 기능 (구현 완료)
- [x] OpenAI API 기반 ERP 자연어 질의 챗봇
- [x] OCR 기반 영수증/발주서 텍스트 추출 및 증빙 문서함 연계
- [x] Playwright 기반 반복 업무 자동화 (RPA)
  - 아망티 등 공급업체 최신 단가 스크래핑 및 ERP 단가 비교 자동화
  - 법정 최저임금 고시 데이터를 크롤링하여 당해 연도 급여 적정성 검증 자동화
  - 재고 부족 경고 및 예측 RPA 분석
- [x] 권한별 API 접근 제어 강화 및 RPA 결과물 파일 경로 이탈 방지(보안 하드닝)

---

## 기술 스택

| 영역 | 기술 |
| :--- | :--- |
| Frontend | HTML, CSS, Vanilla JS (반응형 모던 UI 테마) |
| Backend | Spring Boot 3.2+, Java 21, JPA, QueryDSL, Spring Security (JWT) |
| AI Server | Python 3.10+, Flask, OpenAI SDK, Pandas |
| RPA | Playwright, OCR (PyTesseract 또는 EasyOCR) |
| Database | TiDB (MySQL Compatible) |
| Infra | Docker, Gradle, Postman |

---

## 저장소 구조

```text
backend/    Spring Boot ERP API (Controller-Service-Repository 계층 구조)
frontend/   HTML/CSS/Vanilla JS UI (페이지 단위 권한 라우팅 및 비동기 렌더링)
ai-server/  Flask 기반 AI API 및 데이터 보조 처리
rpa/        Playwright 자동화 작업
docs/       협업 규칙, 구조 문서, API 초안
```

더 구체적인 구조 기준은 아래 문서를 보면 된다.

- 구조 문서: [docs/common/SYSTEM_ARCHITECTURE.md](/C:/kmh/dduk/docs/common/SYSTEM_ARCHITECTURE.md:1)
- API 기준: [docs/common/API_STANDARD.md](/C:/kmh/dduk/docs/common/API_STANDARD.md:1)
- DB 초안: [docs/common/ARCHIVE/DB_DRAFT.md](/C:/kmh/dduk/docs/common/ARCHIVE/DB_DRAFT.md:1)
- 환경변수 가이드: [docs/common/ARCHIVE/ENV_GUIDE.md](/C:/kmh/dduk/docs/common/ARCHIVE/ENV_GUIDE.md:1) (통합 샘플: [.env.example](/.env.example))
- 실행/테스트 가이드: [docs/common/ARCHIVE/RUN_TEST_GUIDE.md](/C:/kmh/dduk/docs/common/ARCHIVE/RUN_TEST_GUIDE.md:1)
- AI 하네스: [docs/AI_HARNESS.md](/C:/kmh/dduk/docs/AI_HARNESS.md:1)
- 협업 규칙: [docs/CONVENTION.md](/C:/kmh/dduk/docs/CONVENTION.md:1)

문서 바로가기:

- AI/RPA 실행 체크: [docs/ai-rpa/AI_RPA_EXECUTION_CHECK_GUIDE.md](/C:/kmh/dduk/docs/ai-rpa/AI_RPA_EXECUTION_CHECK_GUIDE.md:1)
- AI/RPA 로드맵: [docs/ai-rpa/AI_RPA_ROADMAP.md](/C:/kmh/dduk/docs/ai-rpa/AI_RPA_ROADMAP.md:1)
- 회계 문서: [docs/accounting/ACCOUNTING_DASHBOARD.md](/C:/kmh/dduk/docs/accounting/ACCOUNTING_DASHBOARD.md:1), [ACCOUNTING_TRANSACTION.md](/C:/kmh/dduk/docs/accounting/ACCOUNTING_TRANSACTION.md:1), [CHART_OF_ACCOUNTS.md](/C:/kmh/dduk/docs/accounting/CHART_OF_ACCOUNTS.md:1)
- UI 공통 문서: [docs/ui/UI_GUIDE.md](/C:/kmh/dduk/docs/ui/UI_GUIDE.md:1), [DESIGN_SYSTEM.md](/C:/kmh/dduk/docs/ui/DESIGN_SYSTEM.md:1), [DASHBOARD_LAYOUT.md](/C:/kmh/dduk/docs/ui/DASHBOARD_LAYOUT.md:1)
- 재고 문서: [docs/ui/inventory/INVENTORY_DASHBOARD.md](/C:/kmh/dduk/docs/ui/inventory/INVENTORY_DASHBOARD.md:1), [STOCK_MOVEMENT.md](/C:/kmh/dduk/docs/ui/inventory/STOCK_MOVEMENT.md:1), [WAREHOUSE_MANAGEMENT.md](/C:/kmh/dduk/docs/ui/inventory/WAREHOUSE_MANAGEMENT.md:1)

---

## 빠른 시작

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
- 도메인별 CRUD 및 AI/RPA 연계 REST API 동작 확인

### 3. 프론트 확인

```bash
cd frontend
# 로컬 웹 서버 또는 Live Server를 사용하여 index.html 기준 구동 확인
```

### 4. AI/RPA 서버 실행

```bash
cd ai-server
python app.py
```
- `http://localhost:5000/health` 등을 통해 OCR 및 OpenAI 챗봇 API 대기 상태 확인.
- RPA Playwright 엔진 실행 준비 완료.

---

## 팀 작업 및 협업 가이드

1. **역할 준수**: `admin`, `hr`, `inventory` 계층에 맞는 REST API와 화면 라우팅 기준을 준수한다.
2. **보안 지침**: 비밀번호 등 주요 접속 크레덴셜은 하드코딩하지 않고 `.env` 환경변수로 주입받도록 구성한다.
3. **API 명세**: 신규 API 추가 시 [docs/common/API_STANDARD.md](/C:/kmh/dduk/docs/common/API_STANDARD.md:1) 문서를 먼저 갱신하고 개발에 착수한다.

---

© 2026 Team DDUK
