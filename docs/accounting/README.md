# Accounting Docs

DDUK ERP 회계관리(Accounting) 도메인의 확정 문서 모음이다.

## 문서 목록

| 문서 | 상태 | 설명 |
| --- | --- | --- |
| `ACCOUNTING_TRANSACTION.md` | 구현 완료 / 고도화 진행 | 전표 입력, 계정과목 검색, Voucher/Journal 저장 흐름 |
| `ACCOUNTING_DASHBOARD.md` | 구현 완료 | 회계 대시보드 KPI, 최근 활동, 집계 API |
| `ACCOUNTING_REPORT.md` | 구현 완료 | 회계 리포트, 월별 손익, 주요 계정 통계 |
| `ACCOUNTING_MONTH_END.md` | 구현 완료 / 보강 | 월 마감, 회계기간, 검증, 기본 OPEN UX |
| `ACCOUNTING_TRIAL_BALANCE.md` | 구현 완료 | 합계잔액시산표 조회 및 집계 |
| `ACCOUNTING_PAYROLL.md` | 구현 완료 | 급여대장, 급여 집계, PayrollLedger 연동 |
| `CHART_OF_ACCOUNTS.md` | 구현 완료 | 계정과목 체계와 기본 CoA Seed |
| `ARCHIVE/ACCOUNTING_ENHANCEMENT_2026-05-22.md` | Archive | 2026-05-22 회계관리 고도화 변경 요약 |

## 구현 상태

- 전표 저장: `Voucher`, `VoucherLine`, `JournalEntry`, `JournalLine` DB 저장 연동 완료
- 계정 검색: `/api/accounting/accounts/search`, `/api/v1/accounting/accounts/search` 제공
- 대시보드/시산표/월마감/리포트/급여: 실제 API 조회 기반 기본값 및 Empty State 제공
- 미구현 또는 확장 예정: 일반전표, 수정전표, 반제전표, 부가세 신고 확정 검증, 승인자 이력 분리
