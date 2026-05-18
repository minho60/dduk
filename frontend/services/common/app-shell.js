(function () {
    const GLOBAL_NAV_ITEMS = [
        { 
            label: "대시보드",
            items: [
                { href: "dashboard.html", label: "통합 대시보드", roles: ["ADMIN", "HR", "INVENTORY"] }
            ]
        },
        { 
            label: "구매/발주",
            items: [
                { href: "pages/inventory/purchase-dashboard.html", label: "구매/발주 대시보드", roles: ["ADMIN", "INVENTORY"] },
                { href: "pages/inventory/purchase-request.html", label: "발주 요청", roles: ["ADMIN", "INVENTORY"] },
                { href: "pages/inventory/purchase-mgmt.html", label: "발주 관리", roles: ["ADMIN", "INVENTORY"] },
                { href: "pages/inventory/receiving.html", label: "입고 등록", roles: ["ADMIN", "INVENTORY"] },
                { href: "pages/inventory/vendors.html", label: "거래처 관리", roles: ["ADMIN", "INVENTORY"] }
            ]
        },
        { 
            label: "재고관리",
            items: [
                { href: "pages/inventory/stock-dashboard.html", label: "재고관리 대시보드", roles: ["ADMIN", "INVENTORY"] },
                { href: "pages/inventory/items.html", label: "재고 조회", roles: ["ADMIN", "INVENTORY"] },
                { href: "pages/inventory/stock-history.html", label: "입출고 이력", roles: ["ADMIN", "INVENTORY"] },
                { href: "pages/inventory/stock-move.html", label: "창고 이동", roles: ["ADMIN", "INVENTORY"] },
                { href: "pages/inventory/auto-order.html", label: "자동 발주 추천", roles: ["ADMIN", "INVENTORY"] }
            ]
        },
        { 
            label: "회계관리",
            items: [
                { href: "pages/accounting/dashboard.html", label: "회계 대시보드", roles: ["ADMIN"] },
                { href: "pages/accounting/tax-invoice.html", label: "세금계산서", roles: ["ADMIN"] },
                { href: "pages/accounting/expenses.html", label: "비용 처리", roles: ["ADMIN"] },
                { href: "pages/accounting/sales-purchase.html", label: "매입/매출", roles: ["ADMIN"] },
                { href: "pages/accounting/monthly-settlement.html", label: "월별 정산", roles: ["ADMIN"] },
                { href: "pages/hr/payroll.html", label: "급여 계산", roles: ["ADMIN", "HR"] },
                { href: "pages/accounting/report.html", label: "회계 리포트", roles: ["ADMIN"] }
            ]
        },
        { 
            label: "문서/증빙",
            items: [
                { href: "pages/docs/upload.html", label: "증빙 업로드", roles: ["ADMIN", "HR", "INVENTORY"] },
                { href: "pages/docs/ocr-box.html", label: "OCR 문서함", roles: ["ADMIN"] },
                { href: "pages/docs/contracts.html", label: "계약 문서", roles: ["ADMIN"] }
            ]
        },
        { 
            label: "AI 업무지원",
            items: [
                { href: "pages/ai/chatbot.html", label: "AI 챗봇", roles: ["ADMIN", "HR", "INVENTORY"] },
                { href: "ai/anomaly-detection.html", label: "이상 탐지", roles: ["ADMIN"] },
                { href: "ai/predictive-analysis.html", label: "예측 분석", roles: ["ADMIN"] }
            ]
        },
        { 
            label: "관리자",
            items: [
                { href: "pages/admin/members.html", label: "사용자 관리", roles: ["ADMIN"] },
                { href: "pages/admin/roles.html", label: "권한 관리", roles: ["ADMIN"] },
                { href: "pages/admin/logs.html", label: "시스템 로그", roles: ["ADMIN"] },
                { href: "pages/admin/monitoring.html", label: "서버 모니터링", roles: ["ADMIN"] },
                { href: "pages/admin/settings.html", label: "시스템 설정", roles: ["ADMIN"] },
                { href: "#", label: "데이터 동기화 (최신화)", roles: ["ADMIN"] }
            ]
        }
    ];

    function getRelativeRoot() {
        const path = window.location.pathname;
        if (path.includes('/pages/')) {
            const depth = path.split('/pages/')[1].split('/').length;
            return '../'.repeat(depth + 1);
        }
        return './';
    }

    function renderNav() {
        const userRole = window.ddukSession.getSession().role || "USER";
        const root = getRelativeRoot();

        return GLOBAL_NAV_ITEMS.map(function (group) {
            const allowedItems = group.items.filter(item => item.roles.includes(userRole));
            if (allowedItems.length === 0) return "";

            const itemHtml = allowedItems.map(function (item) {
                const href = item.href === "#" ? "#" : root + item.href;
                const isCurrent = window.location.pathname.endsWith(item.href) ? ' aria-current="page"' : "";
                
                return `
                    <a href="${href}"${isCurrent}>
                        <span class="nav-label">${item.label}</span>
                    </a>
                `;
            }).join("");

            return `
                <div class="nav-group">
                    <div class="nav-header" onclick="toggleNavGroup(this)">
                        ${group.label}
                    </div>
                    <div class="nav-items">
                        ${itemHtml}
                    </div>
                </div>
            `;
        }).join("");
    }

    window.toggleNavGroup = function(header) {
        const currentGroup = header.parentElement;
        const allGroups = document.querySelectorAll('.nav-group');
        const isExpanded = currentGroup.classList.contains('expanded');
        
        // Close all other groups
        allGroups.forEach(group => group.classList.remove('expanded'));
        
        // Toggle the clicked group
        if (!isExpanded) {
            currentGroup.classList.add('expanded');
        }
    };

    function renderCards(items) {
        return items.map(function (item) {
            if (item.value !== undefined) {
                return `
                    <article class="panel stat-card">
                        <span class="label">${item.title}</span>
                        <span class="value">${item.value}</span>
                        ${item.trend ? `<span class="trend ${item.trend.startsWith('+') ? 'up' : 'down'}">${item.trend}</span>` : ''}
                    </article>
                `;
            }
            return `
                <article class="panel">
                    <h3>${item.title}</h3>
                    <p>${item.description}</p>
                </article>
            `;
        }).join("");
    }

    function renderList(items) {
        return items.map(function (item) {
            return `<li>${item}</li>`;
        }).join("");
    }

    function hydratePage(config) {
        const navElement = document.querySelector("[data-nav]");
        const titleElement = document.querySelector("[data-page-title]");
        const descriptionElement = document.querySelector("[data-page-description]");
        const cardsElement = document.querySelector("[data-summary-cards]");
        const todoElement = document.querySelector("[data-todo-list]");
        const tableElement = document.querySelector("[data-table-list]");
        const apiElement = document.querySelector("[data-api-list]");

        if (navElement) {
            navElement.innerHTML = renderNav();
        }

        if (titleElement && config.title !== undefined) {
            titleElement.textContent = config.title || "";
        }

        if (descriptionElement && config.description !== undefined) {
            descriptionElement.textContent = config.description || "";
        }

        if (cardsElement) {
            cardsElement.innerHTML = renderCards(config.cards || []);
        }

        if (todoElement) {
            todoElement.innerHTML = renderList(config.todoItems || []);
        }

        if (tableElement) {
            tableElement.innerHTML = renderList(config.tables || []);
        }

        if (apiElement) {
            apiElement.innerHTML = renderList(config.apis || []);
        }
    }

    window.ddukAppShell = {
        hydratePage
    };
})();
