# 🚀 DDUK (떡) ERP Project
> **기업의 모든 자원을 한 곳에서, 효율적이고 지능적으로 관리하는 차세대 ERP 솔루션**

---

## 🛠 기술 스택 (Tech Stack)

### **1. 프론트엔드 (Frontend)**
*   **기본 언어**: HTML5, CSS3, JavaScript (ES6+)
*   **UI 프레임워크 및 스타일링**:
    *   **Vanilla CSS**: 커스텀 속성(Variable)을 활용한 테마 관리 및 Glassmorphism 디자인 적용
    *   **Flexbox & Grid**: 반응형 레이아웃 구현 및 복잡한 ERP 대시보드 구조 설계
*   **비동기 통신 및 실시간성**:
    *   **Fetch API**: RESTful API와의 비동기 데이터 통신 (Async/Await 활용)
    *   **WebSocket**: 실시간 알림 시스템 및 실시간 재고/현황 업데이트 반영
*   **외부 라이브러리**: Font Awesome (아이콘), Google Fonts (Typography)

### **2. 백엔드 (Backend - ERP Core)**
*   **프레임워크**: **Spring Boot 3.x**
*   **사용 언어**: **Java 21** (최신 LTS 버전의 가상 스레드 및 최신 문법 활용)
*   **데이터베이스 관리**:
    *   **Spring Data JPA**: Hibernate를 이용한 객체 지향적 데이터 관리
    *   **QueryDSL**: 타입 세이프한 동적 쿼리 작성으로 복잡한 통계 및 조회 로직 구현
*   **보안 (Security)**:
    *   **Spring Security**: 다중 권한 기반의 접근 제어
    *   **JWT (JSON Web Token)**: Stateless 기반의 인증 및 인가 시스템 구축
*   **빌드 도구**: Gradle (의존성 관리 및 빌드 자동화)

### **3. AI & RPA 서버**
*   **프레임워크**: **Flask** (경량 Python 웹 프레임워크)
*   **사용 언어**: **Python 3.10+**
*   **AI 모델 연동**:
    *   **OpenAI GPT-4o**: 급여 명세 요약, 회계 리포트 생성, 비즈니스 챗봇 구현
*   **업무 자동화 (RPA)**:
    *   **Playwright**: 발주 이메일 자동 처리 및 웹 기반 데이터 수집 자동화
    *   **OCR (Optical Character Recognition)**: Tesseract 또는 Cloud Vision을 이용한 발주서/영수증 자동 인식

### **4. 데이터베이스 및 인프라**
*   **RDBMS**: **MySQL 8.0** (고성능 관계형 데이터베이스로 ERP 핵심 데이터 관리)
*   **배포 및 패키징**:
    *   **Docker**: 컨테이너화를 통한 일관된 개발/운영 환경 보장
    *   **PyInstaller**: Flask 및 Python 환경을 독립적인 **.exe** 실행 파일로 패키징하여 배포 편의성 증대

---

## 👥 Team & Roles

| 이름 | 역할 | 담당 파트 | 주요 기능 |
| :--- | :--- | :--- | :--- |
| **김민호 (조장)** | **관리자 / AI** | 공통 시스템 & 인프라 | 로그인/JWT, 권한 관리, 대시보드, AI 챗봇, API Gateway, 배포 |
| **서우리** | **회계 / 인사** | 인사 및 재무 관리 | 직원/근태 관리, 급여 계산, 비용 처리, 매출/지출 관리, AI 회계 리포트 |
| **김슬기** | **구매 / 발주** | 물류 및 재고 관리 | 재고 관리, 입출고 관리, 거래처 관리, 발주서 OCR, 자동 발주 추천 |

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
