# Common Docs

DDUK ERP 공통 문서 영역이다. API 규격, 에러 처리, 시스템 구조처럼 여러 도메인에서 같이 쓰는 문서를 둔다.

## 문서 목록

| 문서 | 상태 | 설명 |
| --- | --- | --- |
| `SYSTEM_ARCHITECTURE.md` | 기존 문서 이동 | 전체 구조, 도메인 책임, 백엔드/프론트/AI/RPA 경로 기준 |
| `API_STANDARD.md` | 기존 문서 이동 | API 공통 규칙과 MVP 엔드포인트 초안 |
| `ERROR_HANDLING.md` | 기존 규칙 분리 | 공통 응답 구조, 상태코드, 에러 노출 금지 기준 |

## Archive

| 문서 | 처리 사유 |
| --- | --- |
| `ARCHIVE/DB_DRAFT.md` | DB 초안 문서. 현재 도메인 문서 체계에서는 초안 보존 문서로 분리 |
| `ARCHIVE/ENV_GUIDE.md` | 실행 환경 안내 문서. 도메인 기준 문서가 아니므로 보존 |
| `ARCHIVE/RUN_TEST_GUIDE.md` | 실행/테스트 안내 문서. 도메인 기준 문서가 아니므로 보존 |

## Root Reference

- `docs/AI_HARNESS.md`와 `docs/CONVENTION.md`는 AGENTS 지시에서 직접 참조하므로 루트에 유지한다.
- 공통 문서 작성 기준은 위 두 문서를 우선한다.
