(function () {
    // 1. 모든 도메인별 대시보드 데이터 통합 관리
    const dashboardRegistry = {
        ADMIN: {
            allowedRoles: ["ADMIN"],
            pageData: {
                title: "관리자 메인",
                description: "인증, 권한, 공통 설정, 시스템 운영 화면을 연결하는 시작 페이지",
                navItems: [
                    { href: "../admin/dashboard.html", label: "관리자 메인", current: true },
                    { href: "../hr/dashboard.html", label: "인사/회계" },
                    { href: "../inventory/dashboard.html", label: "재고/발주" }
                ],
                cards: [
                    { title: "인증", description: "로그인, JWT, 세션 처리와 보호 API 기준 정리" },
                    { title: "권한", description: "역할별 접근 제어와 메뉴 노출 기준 확정" },
                    { title: "운영", description: "공통 설정, 계정 관리, 감사 로그 연결 준비" }
                ],
                todoItems: [
                    "관리자 계정 생성 및 역할 부여 흐름 점검",
                    "역할별 메뉴 제어 규칙 정리",
                    "공통 응답/에러 처리 형식 연결",
                    "관리자 대시보드 요약 API 정의"
                ],
                tables: ["members", "roles(enum)"],
                apis: [
                    "POST /api/v1/auth/login",
                    "GET /api/v1/admin/members",
                    "PATCH /api/v1/admin/members/{id}/role"
                ]
            }
        },
        HR: {
            allowedRoles: ["ADMIN", "HR"],
            pageData: {
                title: "인사/회계 메인",
                description: "직원, 근태, 급여, 비용 흐름을 구현하기 위한 시작 페이지",
                navItems: [
                    { href: "../hr/dashboard.html", label: "인사/회계 메인", current: true },
                    { href: "../admin/dashboard.html", label: "관리자" },
                    { href: "../inventory/dashboard.html", label: "재고/발주" }
                ],
                cards: [
                    { title: "직원", description: "직원 목록, 등록, 상세 화면 작업 시작" },
                    { title: "근태", description: "출퇴근 기록, 월별 조회, 예외 상태 처리 연결" },
                    { title: "급여/비용", description: "급여 결과, 비용 승인, 월별 회계 데이터 준비" }
                ],
                todoItems: [
                    "직원 목록/등록 화면 우선 구현",
                    "근태 월별 조회 UI와 API 계약 정리",
                    "급여 상태 흐름 반영"
                ],
                tables: ["employees", "attendances", "payrolls", "expenses"],
                apis: [
                    "GET /api/v1/employees",
                    "POST /api/v1/employees",
                    "GET /api/v1/attendances"
                ]
            }
        },
        INVENTORY: {
            allowedRoles: ["ADMIN", "INVENTORY"],
            pageData: {
                title: "재고/발주 메인",
                description: "거래처, 품목, 재고, 발주 흐름을 구현하기 위한 시작 페이지",
                navItems: [
                    { href: "../inventory/dashboard.html", label: "재고/발주 메인", current: true },
                    { href: "../admin/dashboard.html", label: "관리자" },
                    { href: "../hr/dashboard.html", label: "인사/회계" }
                ],
                cards: [
                    { title: "거래처", description: "거래처 목록, 등록, 상태 관리 시작" },
                    { title: "재고", description: "품목, 현재 수량, 안전 재고 화면 작업 시작" },
                    { title: "발주", description: "발주 등록, 상태 변경, 입고 연계 흐름 정리" }
                ],
                todoItems: [
                    "거래처 목록/등록 화면 구현",
                    "품목/재고 목록 API 연결",
                    "발주 등록 폼 설계"
                ],
                tables: ["vendors", "items", "inventories", "purchase_orders"],
                apis: [
                    "GET /api/v1/vendors",
                    "GET /api/v1/items",
                    "POST /api/v1/purchase-orders"
                ]
            }
        }
    };

    /**
     * 현재 페이지의 도메인(ADMIN, HR, INVENTORY)을 URL 경로에서 추론합니다.
     */
    function detectDomain() {
        const path = window.location.pathname;
        if (path.includes("/admin/")) return "ADMIN";
        if (path.includes("/hr/")) return "HR";
        if (path.includes("/inventory/")) return "INVENTORY";
        return null;
    }

    /**
     * 통합 초기화 함수
     */
    function init() {
        const domain = detectDomain();
        if (!domain || !dashboardRegistry[domain]) {
            console.error("DDUK ERP: 알 수 없는 도메인 접근입니다.");
            return;
        }

        const config = dashboardRegistry[domain];
        const session = window.ddukSession.requireRole(config.allowedRoles);
        
        if (session) {
            window.ddukSession.bindShell(session);
            window.ddukAppShell.hydratePage(config.pageData);
            console.log(`DDUK ERP: [${domain}] 통합 대시보드 로드 완료`);
        }
    }

    // 외부 노출
    window.ddukDashboard = { init };

    // DOM 로드 시 자동 실행 (별도 인라인 호출 불필요하게 설정 가능)
    document.addEventListener("DOMContentLoaded", init);
})();
