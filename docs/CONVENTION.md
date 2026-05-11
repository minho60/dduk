# 🤝 DDUK ERP 협업 가이드라인 (Convention)

이 문서는 **DDUK (떡) ERP 프로젝트**의 팀원 및 AI가 효율적으로 협업하기 위한 규칙을 담고 있습니다.

---

## 📂 1. 프로젝트 구조 (Project Structure)
모든 개발은 아래 정의된 구조 내에서 진행하며, 새로운 최상위 폴더 생성 시 팀원과 합의해야 합니다.

*   `backend/`: Spring Boot (Core ERP)
*   `frontend/`: HTML, CSS, Vanilla JS
*   `ai-server/`: Flask (AI, RPA)
*   `rpa/`: Playwright Scripts
*   `docs/`: 설계서 및 컨벤션 문서

---

## 🌿 2. Git 커밋 규칙 (Commit Message)
커밋 메시지는 한글 또는 영문을 사용하되, 아래 형식을 엄격히 준수합니다.

> **`타입: 제목`** (예시: `feat: 직원 근태 관리 API 추가`)

| 타입 | 의미 |
| :--- | :--- |
| `feat` | 새로운 기능 추가 |
| `fix` | 버그 수정 |
| `docs` | 문서 수정 (Markdown 등) |
| `style` | 코드 포맷팅, 세미콜론 누락 수정 (로직 변경 없음) |
| `refactor` | 코드 리팩토링 |
| `test` | 테스트 코드 추가 및 리팩토링 |
| `chore` | 빌드 설정, 패키지 매니저 설정 변경 등 |

---

## 💻 3. 코딩 컨벤션 (Coding Standard)

### **공통 (Common)**
*   변수명, 함수명은 직관적인 **영문**으로 작성합니다.
*   들여쓰기는 **Space 4칸**을 권장합니다.
*   복잡한 로직에는 반드시 **주석**을 달아 의도를 설명합니다.

### **Backend (Java/Spring)**
*   `CamelCase` 사용 (클래스는 `PascalCase`)
*   Lombok을 활용하여 보일러플레이트 코드를 줄입니다.
*   비즈니스 로직은 `Service` 레이어에 집중합니다.

### **AI/RPA (Python/Flask)**
*   `snake_case` 사용
*   `requirements.txt`를 통해 의존성을 관리합니다.
*   AI 모델 관련 설정은 별도의 `config.py`나 `.env`로 관리합니다.

### **Database (MySQL)**
*   테이블명/컬럼명은 `snake_case`를 사용합니다.
*   ID 컬럼은 `id` (PK) 형식을 사용합니다.

---

## 🌐 4. API 설계 (API Design)
*   모든 API 경로는 `/api/v1/`으로 시작합니다.
*   **응답 포맷 (JSON)**
    ```json
    {
      "status": "success",
      "data": { ... },
      "message": "요청이 완료되었습니다."
    }
    ```

---

## 🤖 5. AI와 협업할 때
*   AI에게 수정을 요청할 때는 **수정 대상 파일명**과 **수정 의도**를 명확히 전달합니다.
*   AI가 작성한 코드는 반드시 팀원이 검토(Code Review) 후 메인 코드에 합산합니다.

---
**마지막 업데이트**: 2026-05-11
**작성자**: 김민호 (조장)
