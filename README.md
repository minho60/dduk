# 🚀 DDUK (떡) ERP Project
> **기업의 모든 자원을 한 곳에서, 효율적이고 지능적으로 관리하는 차세대 ERP 솔루션**

---

## 🛠 기술 스택 (Tech Stack)

### **프론트엔드 (Frontend)**
*   **핵심 기술**: ![HTML5](https://img.shields.io/badge/html5-%23E34F26.svg?style=flat-square&logo=html5&logoColor=white) ![CSS3](https://img.shields.io/badge/css3-%231572B6.svg?style=flat-square&logo=css3&logoColor=white) ![JavaScript](https://img.shields.io/badge/javascript-%23323330.svg?style=flat-square&logo=javascript&logoColor=%23F7DF1E)
*   **UI/디자인**: Vanilla CSS (Custom Properties), CSS Grid/Flexbox, Font Awesome, Google Fonts
*   **통신**: Fetch API (Async/Await), WebSocket (실시간 알림)

### **백엔드 (Backend - ERP Core)**
*   **프레임워크**: ![Spring Boot](https://img.shields.io/badge/Spring_Boot-3.x-6DB33F?style=flat-square&logo=springboot&logoColor=white)
*   **언어**: ![Java 21](https://img.shields.io/badge/Java-21-ED8B00?style=flat-square&logo=openjdk&logoColor=white)
*   **데이터 접근**: Spring Data JPA (Hibernate), QueryDSL
*   **보안**: Spring Security, JWT (JSON Web Token)
*   **빌드 도구**: Gradle

### **AI & RPA 서버**
*   **프레임워크**: ![Flask](https://img.shields.io/badge/flask-%23000.svg?style=flat-square&logo=flask&logoColor=white)
*   **언어**: ![Python 3.10](https://img.shields.io/badge/Python-3.10-3776AB?style=flat-square&logo=python&logoColor=white)
*   **AI 엔진**: ![OpenAI](https://img.shields.io/badge/OpenAI_GPT--4-412991?style=flat-square&logo=openai&logoColor=white)
*   **자동화**: ![Playwright](https://img.shields.io/badge/Playwright-2EAD33?style=flat-square&logo=playwright&logoColor=white) (RPA), OCR (Tesseract/Cloud Vision)

### **데이터베이스 및 인프라**
*   **데이터베이스**: ![MySQL](https://img.shields.io/badge/mysql-8.0-%2300f.svg?style=flat-square&logo=mysql&logoColor=white)
*   **배포/패키징**: ![Docker](https://img.shields.io/badge/Docker-2496ED?style=flat-square&logo=docker&logoColor=white), PyInstaller (EXE 패키징)

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
