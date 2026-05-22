# UI Docs

DDUK ERP 프론트엔드 UI 문서 영역이다.

## 문서 목록

| 문서 | 상태 | 설명 |
| --- | --- | --- |
| `UI_GUIDE.md` | 초안 | 화면 구조와 공통 UI 작성 기준 |
| `DESIGN_SYSTEM.md` | 초안 | 색상, typography, spacing, 카드, 버튼, 테이블 기준 |
| `DASHBOARD_LAYOUT.md` | 초안 | sidebar, topbar, content area, KPI grid, section card 구조 |

## 기준

- 기본 디자인 시스템은 `frontend/dashboard.html` 및 `frontend/styles/common/dashboard.css`를 기준으로 한다.
- 회계 화면은 `frontend/styles/accounting/` 하위 CSS를 사용하되 카드, 버튼, 테이블, 그림자, spacing은 공통 dashboard 톤을 따른다.
- 반응형 breakpoint는 `1024px`, `768px`, `576px`를 우선 사용한다.
- Bootstrap은 사용하지 않는다.
