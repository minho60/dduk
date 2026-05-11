# 🚀 DDUK (떡) ERP Project
> **기업의 모든 자원을 한 곳에서, 효율적이고 지능적으로 관리하는 차세대 ERP 솔루션**

---

## 🛠 기술 스택 (Tech Stack)

### **1. 프론트엔드 (Frontend)**
*   **핵심 아키텍처**: 순수 HTML/JS를 기반으로 한 **Component-Based UI** 설계 (React 없이 모듈화 구현)
*   **스타일링 및 디자인**:
    *   **Vanilla CSS (Modern)**: CSS 변수(Custom Properties)를 활용한 다크/라이트 모드 테마 스위칭, `Glassmorphism` 효과 구현
    *   **Layout**: `CSS Grid`를 활용한 대시보드 위젯 배치 및 `Flexbox`를 이용한 반응형 네비게이션
    *   **애니메이션**: `Web Animations API` 및 `CSS Transitions`를 활용한 부드러운 UI 인터랙션
*   **데이터 통신 및 상태 관리**:
    *   **Fetch API (Async/Await)**: 중앙 집중형 API 모듈 구성을 통한 HTTP 요청 관리
    *   **State Management**: 브라우저의 `LocalStorage` 및 `SessionStorage`를 활용한 사용자 세션 및 상태 유지
    *   **WebSocket**: 실시간 알림 시스템 및 전사 실시간 대시보드 데이터 동기화
*   **시각화 라이브러리**: **Chart.js** 또는 **ApexCharts**를 활용한 회계/매출 통계 데이터 시각화

### **2. 백엔드 (Backend - ERP Core)**
*   **기반 기술**: **Spring Boot 3.2+** & **Java 21**
*   **핵심 프레임워크 기능**:
    *   **Spring Data JPA (Hibernate)**: 엔티티 매핑 및 성능 최적화를 위한 `Batch Fetching`, `Lazy Loading` 전략 적용
    *   **QueryDSL**: 컴파일 타임에 타입 체크가 가능한 동적 쿼리를 작성하여 복잡한 ERP 검색 및 통계 필터링 구현
    *   **Spring Security & JWT**: `Access Token` 및 `Refresh Token`을 이용한 보안 인증, `RBAC(Role-Based Access Control)` 기반의 메뉴 권한 제어
*   **비즈니스 로직 및 유틸리티**:
    *   **MapStruct**: Entity와 DTO 간의 고성능 객체 매핑 수행
    *   **Validation API**: 입력 데이터의 정합성 검증 자동화
    *   **HikariCP**: 고성능 데이터베이스 커넥션 풀링 관리
*   **빌드 및 관리**: Gradle 기반의 멀티 모듈 및 의존성 관리

### **3. AI & RPA 서버**
*   **프레임워크**: **Flask** (AI 연동용 REST API 서버)
*   **핵심 라이브러리**:
    *   **OpenAI SDK (GPT-4o)**: 프롬프트 엔지니어링을 통한 회계 데이터 분석, 자연어 기반의 ERP 챗봇(Agent) 구현
    *   **Playwright**: 자동화 브라우저를 이용한 거래처 사이트 로그인, 발주서/영수증 자동 다운로드 및 이메일 자동 발송
    *   **PyTesseract / EasyOCR**: 이미지 파일(영수증, 발주서)에서 텍스트를 추출하는 OCR 파이프라인 구축
    *   **Pandas**: AI 분석 전 데이터 전처리 및 통계 리포트 생성 보조
*   **배포 환경**: **Python 3.10+** (Virtualenv 기반 가상환경 관리)

### **4. 데이터베이스 및 인프라**
*   **데이터베이스**: **MySQL 8.0**
    *   ERP의 핵심 도메인(인사, 회계, 재고)을 위한 정규화된 DB 스키마 설계
    *   대용량 데이터 조회를 위한 인덱스 최적화 및 `Stored Procedure` 최소화
*   **인프라 및 배포**:
    *   **Docker**: 백엔드, DB, AI 서버의 컨테이너화를 통한 환경 일관성 유지
    *   **PyInstaller**: Flask 및 Python 환경을 단일 실행 파일(**.exe**)로 빌드하여 사용자 PC 배포 지원
    *   **Postman**: API 명세 관리 및 통합 테스트 수행

---

## 👥 Team & Roles

| 이름 | 역할 | 담당 파트 | 주요 기여 기능 |
| :--- | :--- | :--- | :--- |
| **김민호 (조장)** | **관리자 / AI** | 공통 시스템 & 인프라 | 로그인/JWT, 대시보드, AI 챗봇, 인프라 배포 |
| **서우리** | **회계 / 인사** | 인사 및 재무 관리 | 근태/급여 정산, 비용 처리, AI 회계 분석 리포트 |
| **김슬기** | **구매 / 발주** | 물류 및 재고 관리 | 재고/거래처 관리, 발주서 OCR, AI 자동 발주 |

---

## ✨ 상세 기능 및 로드맵

### 1️⃣ 회계 / 인사 / 급여 (담당: 서우리) ⭐⭐⭐⭐
*   **직원/근태 관리**: 사원 정보 통합 관리 및 실시간 근태 체크
*   **급여 시스템**: 자동 급여 계산 및 명세서 발행
*   **재무 관리**: 비용 처리 및 매출/지출 현황 분석
*   **AI 특화**: 급여 명세 요약, 비용 이상 탐지, 월간 회계 리포트 생성

### 2️⃣ 구매 / 발주 / 재고 (담당: 김슬기) ⭐⭐⭐
*   **재고 최적화**: 실시간 재고 추적 및 입출고 자동화
*   **공급망 관리**: 거래처 정보 및 주문 상태 실시간 모니터링
*   **RPA/AI 특화**: **Playwright** 기반 이메일 자동 처리, **OCR**을 통한 발주서 자동 인식, 데이터 기반 자동 발주 추천

### 3️⃣ 공통 시스템 / AI (담당: 김민호) ⭐⭐⭐
*   **보안 및 인증**: JWT 기반의 안전한 로그인 및 세밀한 권한 제어
*   **통합 관리**: 전체 시스템 현황을 한눈에 보는 관리자 대시보드
*   **지능형 지원**: **OpenAI API** 연동을 통한 전사 업무 지원 AI 챗봇
*   **인프라**: Flask AI 서버 구축 및 API Gateway를 통한 유연한 아키텍처

---

## 🚀 시작하기

```bash
# 1. 저장소 클론
git clone https://github.com/minho60/dduk.git

# 2. Backend 실행 (Spring Boot)
./gradlew bootRun

# 3. AI Server 실행 (Flask)
cd ai-server
python app.py
```

---
© 2026 Team DDUK. All rights reserved.
