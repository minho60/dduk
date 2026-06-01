(function () {
    const RECENT_MENU_KEY = 'dduk_dashboard_recent_menu';
    const RECENT_MENU_TTL = 3 * 24 * 60 * 60 * 1000;

    const MENU_GROUPS = [
        {
            id: 'purchase',
            label: '구매/발주',
            items: [
                { label: '구매/발주 대시보드', icon: 'bar-chart-3', href: 'pages/inventory/purchase-dashboard.html', roles: ['ADMIN', 'INVENTORY'] },
                { label: '구매 요청', icon: 'file-plus', href: 'pages/inventory/purchase-request.html', roles: ['ADMIN', 'INVENTORY'] },
                { label: '발주 관리', icon: 'clipboard-list', href: 'pages/inventory/purchase-orders.html', roles: ['ADMIN', 'INVENTORY'] },
                { label: '발주 현황', icon: 'trending-up', href: 'pages/inventory/purchase-status.html', roles: ['ADMIN', 'INVENTORY'] },
                { label: '입고 등록', icon: 'package-check', href: 'pages/inventory/receiving.html', roles: ['ADMIN', 'INVENTORY'] },
                { label: '거래처 관리', icon: 'building', href: 'pages/inventory/vendors.html', roles: ['ADMIN', 'INVENTORY'] }
            ]
        },
        {
            id: 'inventory',
            label: '재고관리',
            items: [
                { label: '재고관리 대시보드', icon: 'bar-chart-3', href: 'pages/inventory/dashboard.html', roles: ['ADMIN', 'INVENTORY'] },
                { label: '재고 조회', icon: 'search', href: 'pages/inventory/list.html', roles: ['ADMIN', 'INVENTORY'] },
                { label: '입출고 이력', icon: 'history', href: 'pages/inventory/movements.html', roles: ['ADMIN', 'INVENTORY'] },
                { label: '창고 이동', icon: 'truck', href: 'pages/inventory/transfers.html', roles: ['ADMIN', 'INVENTORY'] },
                { label: '자동 발주 추천', icon: 'zap', href: 'pages/inventory/reorder.html', roles: ['ADMIN', 'INVENTORY'] }
            ]
        },
        {
            id: 'accounting',
            label: '회계관리',
            items: [
                { label: '회계 대시보드',    icon: 'bar-chart-3',    href: 'pages/hr/accounting/accounting_dashboard.html', roles: ['ADMIN', 'HR'] },
                { label: '계정과목 관리',    icon: 'folder-tree',    href: 'pages/hr/accounting/accounts.html', roles: ['ADMIN', 'HR'] },
                { label: '전표 관리',        icon: 'receipt',        href: 'pages/hr/accounting/voucher_management.html', roles: ['ADMIN', 'HR'] },
                { label: '합계잔액시산표',   icon: 'trending-up',    href: 'pages/hr/accounting/trial_balance.html', roles: ['ADMIN', 'HR'] },
                { label: '재무제표',         icon: 'file-text',      href: 'pages/hr/accounting/reports.html', roles: ['ADMIN', 'HR'] },
                { label: '회계 분석 리포트', icon: 'file-bar-chart', href: 'pages/hr/accounting/accounting_reports.html', roles: ['ADMIN', 'HR'] },
                { label: '월 마감',          icon: 'calendar-check', href: 'pages/hr/accounting/monthly_closing.html', roles: ['ADMIN', 'HR'] },
                { label: '급여 계산/대장',   icon: 'wallet',         href: 'pages/hr/accounting/payroll_management.html', match: 'pages/hr/accounting/payroll_management.html', roles: ['ADMIN', 'HR'] },
                { label: '세금계산서',       icon: 'file-check',     href: 'pages/hr/accounting/wip.html', disabled: true, roles: ['ADMIN', 'HR'] },
                { label: '비용 처리',        icon: 'credit-card',    href: 'pages/hr/accounting/wip.html', disabled: true, roles: ['ADMIN', 'HR'] }
            ]
        },
        {
            id: 'docs',
            label: '문서/증빙',
            items: [
                { label: '증빙 업로드', icon: 'upload', href: 'pages/ocr/upload.html', roles: ['ADMIN', 'HR', 'INVENTORY'] },
                { label: 'OCR 문서함', icon: 'scan', href: 'pages/ocr/ocr-box.html', roles: ['ADMIN', 'HR', 'INVENTORY'] },
                { label: '계약 문서', icon: 'file-signature', href: '#', roles: ['ADMIN'] }
            ]
        },
        {
            id: 'ai',
            label: 'AI 업무지원',
            items: [
                { label: 'AI 챗봇', icon: 'bot', href: '#', roles: ['ADMIN', 'HR', 'INVENTORY'] },
                { label: '이상 탐지', icon: 'alert-triangle', href: 'pages/admin/anomaly-detection.html', roles: ['ADMIN'] },
                { label: '예측 분석', icon: 'brain', href: '#', roles: ['ADMIN', 'HR', 'INVENTORY'] }
            ]
        },
        {
            id: 'admin',
            label: '관리자',
            items: [
                { label: '계정 및 권한 관리', icon: 'shield-check', href: 'pages/admin/account-security.html', roles: ['ADMIN'] },
                { label: '시스템 운영 관리', icon: 'settings-2', href: 'pages/admin/system-admin.html', roles: ['ADMIN'] },
                { label: '공지사항 관리', icon: 'megaphone', href: 'pages/admin/notice-admin.html', roles: ['ADMIN'] },
                { label: '조직 및 부서 관리', icon: 'network', href: 'pages/admin/org-admin.html', roles: ['ADMIN'] },
                { label: 'AI 챗봇 테스트', icon: 'bot', href: 'pages/admin/chatbot-test.html', roles: ['ADMIN'] },
                { label: 'AI/RPA 작업 이력', icon: 'history', href: 'pages/admin/task-history.html', roles: ['ADMIN'] }
            ]
        }
    ];

    function getRootPath() {
        const path = window.location.pathname.replace(/\\/g, '/');
        const frontendIndex = path.lastIndexOf('/frontend/');

        if (frontendIndex === -1) {
            const pagesIndex = path.lastIndexOf('/pages/');
            if (pagesIndex === -1) return './';
            const depth = path.slice(pagesIndex + '/pages/'.length).split('/').length;
            return '../'.repeat(depth);
        }

        const relativePath = path.slice(frontendIndex + '/frontend/'.length);
        const depth = Math.max(0, relativePath.split('/').length - 1);
        return depth === 0 ? './' : '../'.repeat(depth);
    }

    function resolveHref(href) {
        if (!href || href === '#' || /^(https?:|mailto:|tel:)/.test(href)) {
            return href || '#';
        }
        return getRootPath() + href;
    }

    function isCurrentPage(item) {
        const path = window.location.pathname.replace(/\\/g, '/');
        if (item.match && path.includes('/' + item.match)) return true;
        if (!item.href || item.href === '#') return false;
        return path.endsWith('/frontend/' + item.href) || path.endsWith('/' + item.href);
    }

    function renderMenuItem(item, extraClass) {
        const currentClass = isCurrentPage(item) ? ' active' : '';
        const rolesAttr = item.roles ? ` data-roles="${item.roles.join(',')}"` : '';
        if (item.disabled) {
            return `
                <span class="menu_item menu_item_disabled${extraClass ? ` ${extraClass}` : ''}" title="${item.label}" aria-disabled="true">
                    <i class="dduk-inline-012" data-lucide="${item.icon}"></i>
                    <span class="menu_label">${item.label}</span>
                    <span class="menu_badge_wip">준비중</span>
                </span>
            `;
        }
        return `
            <a class="menu_item${extraClass ? ` ${extraClass}` : ''}${currentClass}" href="${resolveHref(item.href)}"${rolesAttr} data-label="${item.label}" title="${item.label}">
                <i class="dduk-inline-012" data-lucide="${item.icon}"></i>
                <span class="menu_label">${item.label}</span>
            </a>
        `;
    }

    function renderGroups() {
        return MENU_GROUPS.map(group => `
            <div class="menu_group_title dduk-inline-014" onclick="toggleMenu('menu_${group.id}')">
                <span>${group.label}</span> <i class="dduk-inline-015" data-lucide="chevron-down" id="icon_${group.id}"></i>
            </div>
            <div class="submenu collapsed dduk-inline-016" id="menu_${group.id}">
                ${group.items.map(item => renderMenuItem(item)).join('')}
            </div>
        `).join('');
    }

    function renderSidebar() {
        const root = getRootPath();
        const userName = localStorage.getItem('userName') || '게스트';
        const rawRole = localStorage.getItem('role') || '';
        const roleMap = {
            ADMIN: '시스템 관리자',
            HR: '인사 관리자',
            INVENTORY: '재고 관리자'
        };
        const displayRole = roleMap[rawRole] || '사용자';

        return `
            <aside class="sidebar" id="sidebar">
                <div class="dduk-inline-001">
                    <div class="dduk-inline-002">
                        <a class="sidebar_logo_link" href="${root}dashboard.html" aria-label="대시보드로 이동">
                            <h1 data-template-id="logo-text" class="canva-text dduk-inline-003">
                                <img src="${root}assets/logo.png" alt="LOGO">
                            </h1>
                        </a>
                        <button class="sidebar_icon_btn" onclick="toggleSidebar()" aria-label="사이드바 접기">
                            <i class="dduk-inline-004" data-lucide="panel-left-close"></i>
                        </button>
                    </div>
                    <div class="dduk-inline-005">
                        <span data-template-id="workspace-name" class="canva-text dduk-inline-006">(주) 업체명</span>
                        <i class="dduk-inline-007" data-lucide="chevron-down"></i>
                    </div>
                    <div class="dduk-inline-008">
                        <button class="sidebar_icon_btn" aria-label="AI"><i class="dduk-inline-009" data-lucide="bot"></i></button>
                        <button class="sidebar_icon_btn" aria-label="OCR"><i class="dduk-inline-009" data-lucide="scan"></i></button>
                        <button class="sidebar_icon_btn" aria-label="승인"><i class="dduk-inline-009" data-lucide="check-circle"></i></button>
                        <button class="sidebar_icon_btn" aria-label="설정"><i class="dduk-inline-009" data-lucide="settings"></i></button>
                    </div>
                    <div class="recent_group dduk-inline-010">
                        <button class="recent_header dduk-inline-011" type="button" onclick="toggleRecentMenu()" aria-expanded="true">
                            <span>최근 사용</span><i class="dduk-inline-015" data-lucide="chevron-up" id="icon_recent"></i>
                        </button>
                        <div class="recent_items" id="recent_menu_items"></div>
                    </div>
                </div>
                <nav class="dduk-inline-013">
                    ${renderMenuItem({ label: '대시보드', icon: 'layout-dashboard', href: 'dashboard.html', roles: ['ADMIN', 'HR', 'INVENTORY'] })}
                    ${renderGroups()}
                </nav>
                <div class="dduk-inline-019">
                    <div class="dduk-inline-020">
                        <div class="dduk-inline-021"><i class="dduk-inline-022" data-lucide="user"></i></div>
                        <div class="sidebar_user_text">
                            <p class="dduk-inline-023">${userName}</p>
                            <p class="dduk-inline-024">${displayRole}</p>
                        </div>
                    </div>
                    <div class="dduk-inline-025">
                        <span class="status_dot dduk-inline-026"></span>
                        <span class="dduk-inline-024 sidebar_status_text">서버 정상 · 99.9% uptime</span>
                    </div>
                    <div style="margin-top: 1rem; border-top: 1px solid rgba(0,0,0,0.05); padding-top: 0.75rem;">
                        <button onclick="handleLogout()" style="display: flex; align-items: center; gap: 0.5rem; font-size: 0.875rem; color: #6b7280; width: 100%; padding: 0.25rem 0; background: none; border: none; cursor: pointer;">
                            <i data-lucide="log-out" style="width: 1rem; height: 1rem;"></i>
                            <span>로그아웃</span>
                        </button>
                    </div>
                </div>
            </aside>
        `;
    }

    function updateMenuIcon(id, isOpen) {
        const icon = document.getElementById('icon_' + id.replace('menu_', ''));
        if (!icon) return;
        icon.setAttribute('data-lucide', isOpen ? 'chevron-up' : 'chevron-down');
        if (window.lucide) lucide.createIcons();
    }

    window.toggleMenu = function (id) {
        if (document.body.classList.contains('sidebar-collapsed')) return;

        const el = document.getElementById(id);
        if (!el) return;

        const shouldOpen = el.classList.contains('collapsed');
        document.querySelectorAll('.submenu').forEach(submenu => {
            if (submenu.id !== id) {
                submenu.classList.add('collapsed');
                updateMenuIcon(submenu.id, false);
            }
        });
        el.classList.toggle('collapsed', !shouldOpen);
        updateMenuIcon(id, shouldOpen);
    };

    window.toggleRecentMenu = function () {
        const items = document.getElementById('recent_menu_items');
        const icon = document.getElementById('icon_recent');
        const header = document.querySelector('.recent_header');
        if (!items || !icon) return;

        const isCollapsed = items.classList.toggle('collapsed');
        icon.setAttribute('data-lucide', isCollapsed ? 'chevron-down' : 'chevron-up');
        if (header) header.setAttribute('aria-expanded', String(!isCollapsed));
        if (window.lucide) lucide.createIcons();
    };

    window.handleLogout = function() {
        localStorage.clear();
        sessionStorage.clear();
        const root = getRootPath();
        window.location.href = root + 'index.html'; 
    };

    window.toggleSidebar = function () {
        const sidebar = document.getElementById('sidebar');
        if (!sidebar) return;

        if (window.innerWidth <= 1024) {
            sidebar.classList.toggle('open');
            return;
        }

        document.body.classList.toggle('sidebar-collapsed');
        const icon = sidebar.querySelector('[data-lucide="panel-left-close"], [data-lucide="panel-left-open"]');
        if (icon) {
            icon.setAttribute('data-lucide', document.body.classList.contains('sidebar-collapsed') ? 'panel-left-open' : 'panel-left-close');
        }
        if (window.lucide) lucide.createIcons();
    };

    function getMenuLabel(menuItem) {
        const label = menuItem.querySelector('.menu_label');
        return label ? label.textContent.trim() : '';
    }

    function readRecentMenus() {
        const now = Date.now();
        try {
            return JSON.parse(localStorage.getItem(RECENT_MENU_KEY) || '[]')
                .filter(item => item.expiresAt > now)
                .sort((a, b) => b.usedAt - a.usedAt)
                .slice(0, 5);
        } catch (error) {
            localStorage.removeItem(RECENT_MENU_KEY);
            return [];
        }
    }

    function writeRecentMenus(items) {
        localStorage.setItem(RECENT_MENU_KEY, JSON.stringify(items));
    }

    function addRecentMenu(menuItem) {
        if (menuItem.closest('.recent_group')) return;

        const label = getMenuLabel(menuItem);
        if (!label) return;

        const icon = menuItem.querySelector('[data-lucide]');
        const iconName = icon ? icon.getAttribute('data-lucide') : 'circle';
        const now = Date.now();
        const items = readRecentMenus().filter(item => item.label !== label);
        items.unshift({
            label,
            iconName,
            usedAt: now,
            expiresAt: now + RECENT_MENU_TTL
        });
        writeRecentMenus(items.slice(0, 5));
        renderRecentMenus();
    }

    function renderRecentMenus() {
        const container = document.getElementById('recent_menu_items');
        if (!container) return;

        const items = readRecentMenus();
        if (items.length === 0) {
            container.innerHTML = '<div class="recent_empty">최근 사용한 메뉴가 없습니다.</div>';
            return;
        }

        container.innerHTML = items.map(item => `
            <div class="menu_item recent_menu_item" data-label="${item.label}" title="${item.label}">
                <i class="dduk-inline-012" data-lucide="${item.iconName}"></i>
                <span class="menu_label">${item.label}</span>
            </div>
        `).join('');
        if (window.lucide) lucide.createIcons();
    }

    function checkMobile() {
        const btn = document.getElementById('mobile_menu_btn');
        if (btn) btn.style.display = 'flex';

        if (window.innerWidth <= 1024) {
            document.body.classList.remove('sidebar-collapsed');
        }
    }

    function openCurrentMenuGroup() {
        const current = document.querySelector('.menu_item.active');
        const submenu = current ? current.closest('.submenu') : null;
        if (!submenu) return;
        submenu.classList.remove('collapsed');
        updateMenuIcon(submenu.id, true);
    }

    function initSidebar() {
        const host = document.querySelector('[data-dduk-sidebar]');
        if (!host) return;

        host.outerHTML = renderSidebar();
        document.querySelectorAll('.submenu').forEach(submenu => updateMenuIcon(submenu.id, !submenu.classList.contains('collapsed')));
        
        const currentUserRole = localStorage.getItem('role') || '';

        document.querySelectorAll('.menu_item').forEach(menuItem => {
            menuItem.addEventListener('click', (e) => {
                const rolesData = menuItem.getAttribute('data-roles');
                if (rolesData) {
                    const allowedRoles = rolesData.split(',');
                    if (!allowedRoles.includes(currentUserRole)) {
                        e.preventDefault();
                        e.stopPropagation();
                        alert('해당 메뉴에 접근할 권한이 없습니다.');
                        return;
                    }
                }
                addRecentMenu(menuItem);
            });
        });
        renderRecentMenus();
        openCurrentMenuGroup();
        checkMobile();
        window.addEventListener('resize', checkMobile);
        if (window.lucide) lucide.createIcons();
        
        // AI Copilot Portal 위젯 초기화
        initAICopilotPortal();
    }

    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', initSidebar);
    } else {
        initSidebar();
    }
    // ==========================================
    // AI Copilot Portal Widget & Styles
    // ==========================================
    const ANOMALY_DATA = [
        {
            type: 'danger',
            label: '위험',
            time: '10분 전',
            title: '장기 미회수 의심 패턴 감지',
            desc: '특정 원부자재 거래처 물품 대금 지연 가능성 및 장기 미회수가 우려됩니다.',
            query: '장기 미회수 의심 거래처와 미회수 금액 통계를 상세히 분석해 줘.'
        },
        {
            type: 'warning',
            label: '경고',
            time: '1시간 전',
            title: '안전 재고 급감 리스크',
            desc: '원두 원부자재 품목 중 안전 재고 임계값을 하회하여 발주가 시급합니다.',
            query: '현재 안전 재고 이하로 내려간 품목 목록과 리드타임을 고려한 적정 발주 권장 수량을 알려줘.'
        }
    ];

    const PREDICTION_DATA = [
        {
            title: '다음 달 예상 발주 금액',
            stat: '₩12,450,000',
            desc: '이동평균 기반 다음 달 예상 원부자재 구매 비용입니다.',
            color: 'blue',
            linkText: '발주 추천 페이지 이동',
            linkUrl: 'pages/inventory/reorder.html'
        },
        {
            title: '이번 주 예상 재고 소진율',
            stat: '87.4%',
            desc: '평균 출고량 기준 이번 주 예상 원두 재고 소진 추이입니다.',
            color: 'green',
            linkText: '재고 조회 이동',
            linkUrl: 'pages/inventory/list.html'
        },
        {
            title: '월 마감 회계 정산 예측',
            stat: '정상 정산 예상',
            desc: '이전 거래 패턴 대비 불일치 가능성이 낮아 마감이 원활할 것으로 예측됩니다.',
            color: 'purple',
            linkText: '월 마감 페이지 이동',
            linkUrl: 'pages/hr/accounting/monthly_closing.html'
        }
    ];

    let chatbotHistory = [];

    window.toggleFloatingChatbot = function(forceOpen) {
        const panel = document.getElementById('dduk-floating-chatbot-panel');
        if (!panel) return;
        
        if (forceOpen === true) {
            panel.classList.add('active');
        } else if (forceOpen === false) {
            panel.classList.remove('active');
        } else {
            panel.classList.toggle('active');
        }
        
        if (panel.classList.contains('active')) {
            const activeTab = panel.querySelector('.dduk-portal-tab.active');
            if (activeTab && activeTab.getAttribute('data-tab') === 'chatbot') {
                const input = document.getElementById('dduk-floating-chatbot-input');
                if (input) input.focus();
            }
        }
    };
    
    window.openFloatingChatbotWithQuery = function(query) {
        window.toggleFloatingChatbot(true);
        switchPortalTab('chatbot');
        const input = document.getElementById('dduk-floating-chatbot-input');
        if (input) {
            input.value = query;
        }
        sendFloatingChatMessage(query);
    };

    function switchPortalTab(tabName) {
        document.querySelectorAll('.dduk-portal-tab').forEach(btn => {
            if (btn.getAttribute('data-tab') === tabName) {
                btn.classList.add('active');
            } else {
                btn.classList.remove('active');
            }
        });
        
        document.querySelectorAll('.dduk-portal-content').forEach(content => {
            if (content.id === `dduk-portal-content-${tabName}`) {
                content.classList.add('active');
            } else {
                content.classList.remove('active');
            }
        });
        
        if (tabName === 'chatbot') {
            const input = document.getElementById('dduk-floating-chatbot-input');
            if (input) input.focus();
        } else if (tabName === 'rpa') {
            renderDynamicRpaWidget();
        }
        
        if (window.lucide) lucide.createIcons();
    }
    
    async function sendFloatingChatMessage(message) {
        if (!message) return;
        
        const messagesContainer = document.getElementById('dduk-floating-chatbot-messages');
        const input = document.getElementById('dduk-floating-chatbot-input');
        if (input) input.value = '';
        
        appendFloatingMessage('user', message);
        
        const loadingDiv = document.createElement("div");
        loadingDiv.className = "dduk-chat-bubble bot loading";
        loadingDiv.innerHTML = `
            <svg class="animate-spin" style="width: 1rem; height: 1rem; color: #4f46e5;" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24">
                <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle>
                <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path>
            </svg>
            <span>분석 중...</span>
        `;
        messagesContainer.appendChild(loadingDiv);
        messagesContainer.scrollTop = messagesContainer.scrollHeight;
        
        try {
            const response = await fetch(`${getApiBaseUrl()}/api/v1/ai/chat`, {
                method: "POST",
                headers: getHeaders({
                    "Content-Type": "application/json"
                }),
                body: JSON.stringify({
                    message: message,
                    history: chatbotHistory
                })
            });
            
            const text = await response.text();
            let payload;
            try {
                payload = text ? JSON.parse(text) : null;
            } catch (e) {
                throw new Error("응답 형식 오류가 발생했습니다.");
            }
            
            loadingDiv.remove();
            
            if (!response.ok || !payload) {
                const errMsg = payload && payload.message ? payload.message : `실패 (HTTP ${response.status})`;
                throw new Error(errMsg);
            }
            
            if (payload.status === "success" && payload.data && payload.data.response) {
                const reply = payload.data.response;
                appendFloatingMessage("bot", reply);
                chatbotHistory.push({ role: "user", content: message });
                chatbotHistory.push({ role: "model", content: reply });
            } else {
                throw new Error(payload.message || "답변을 받아오지 못했습니다.");
            }
        } catch (error) {
            loadingDiv.remove();
            appendFloatingMessage("bot", `오류: ${error.message}`);
        }
    }
    
    function appendFloatingMessage(role, text) {
        const container = document.getElementById('dduk-floating-chatbot-messages');
        if (!container) return;
        
        const messageDiv = document.createElement("div");
        messageDiv.className = `dduk-chat-bubble ${role}`;
        
        if (text.startsWith('오류:')) {
            messageDiv.style.backgroundColor = '#fef2f2';
            messageDiv.style.color = '#991b1b';
            messageDiv.style.borderColor = '#fee2e2';
        }
        
        messageDiv.textContent = text;
        container.appendChild(messageDiv);
        container.scrollTop = container.scrollHeight;
    }

    function renderAnomalyList() {
        const container = document.getElementById('dduk-floating-anomaly-list');
        if (!container) return;
        
        const detailUrl = resolveHref('pages/admin/anomaly-detection.html');
        
        let html = ANOMALY_DATA.map(item => `
            <div class="dduk-anomaly-card">
                <div class="dduk-anomaly-card-header">
                    <span class="dduk-anomaly-badge ${item.type === 'danger' ? 'danger' : 'warning'}">${item.label}</span>
                    <span class="dduk-anomaly-time">${item.time}</span>
                </div>
                <div class="dduk-anomaly-title">${item.title}</div>
                <div class="dduk-anomaly-desc">${item.desc}</div>
                <div class="dduk-anomaly-action" style="display: flex; gap: 0.5rem; justify-content: flex-end; margin-top: 0.625rem;">
                    <a href="${detailUrl}" class="dduk-prediction-link" style="background-color: #f1f5f9; color: #475569; border-radius: 0.5rem; font-size: 0.75rem; text-decoration: none; padding: 0.3rem 0.75rem; display: inline-flex; align-items: center; font-weight: 700;">
                        이동 ↗
                    </a>
                    <button class="dduk-anomaly-btn" onclick="window.openFloatingChatbotWithQuery('${item.query}')">AI 분석</button>
                </div>
            </div>
        `).join('');
        
        html += `
            <div style="margin-top: 1.25rem; text-align: center; padding-bottom: 0.5rem;">
                <a href="${detailUrl}" style="color: #4f46e5; text-decoration: none; font-size: 0.785rem; font-weight: 700; display: inline-flex; align-items: center; gap: 0.25rem;">
                    <span>이상 탐지 상세 모니터링 페이지로 이동</span>
                    <i data-lucide="arrow-right" style="width: 0.85rem; height: 0.85rem;"></i>
                </a>
            </div>
        `;
        
        container.innerHTML = html;
        if (window.lucide) lucide.createIcons();
    }

    function renderPredictionList() {
        const container = document.getElementById('dduk-floating-prediction-list');
        if (!container) return;
        
        container.innerHTML = PREDICTION_DATA.map(item => `
            <div class="dduk-prediction-card">
                <div class="dduk-prediction-header">
                    <div class="dduk-prediction-indicator ${item.color}"></div>
                    <div class="dduk-prediction-title">${item.title}</div>
                </div>
                <div class="dduk-prediction-stat">${item.stat}</div>
                <div class="dduk-prediction-desc">${item.desc}</div>
                <div class="dduk-prediction-action">
                    <a href="${resolveHref(item.linkUrl)}" class="dduk-prediction-link">
                        <span>${item.linkText}</span>
                        <i data-lucide="external-link" style="width: 0.75rem; height: 0.75rem;"></i>
                    </a>
                </div>
            </div>
        `).join('');
        
        if (window.lucide) lucide.createIcons();
    }

    function initAICopilotPortal() {
        if (document.getElementById('dduk-floating-chatbot-container')) return;

        // 사이드바 "AI 챗봇"과 "예측 분석" 메뉴 클릭 시 플로팅 위젯 강제 오픈 및 탭 이동 연동
        document.querySelectorAll('.menu_item').forEach(btn => {
            const labelEl = btn.querySelector('span');
            if (labelEl) {
                const label = labelEl.textContent.trim();
                if (label === 'AI 챗봇') {
                    btn.addEventListener('click', (e) => {
                        e.preventDefault();
                        window.toggleFloatingChatbot(true);
                        switchPortalTab('chatbot');
                    });
                } else if (label === '예측 분석') {
                    btn.addEventListener('click', (e) => {
                        e.preventDefault();
                        window.toggleFloatingChatbot(true);
                        switchPortalTab('prediction');
                    });
                }
            }
        });
        
        const css = `
            .dduk-chat-trigger {
                position: fixed;
                top: 5.5rem;
                right: 1.5rem;
                width: 3.5rem;
                height: 3.5rem;
                background-color: #4f46e5;
                color: #ffffff;
                border-radius: 9999px;
                display: flex;
                align-items: center;
                justify-content: center;
                box-shadow: 0 10px 25px -5px rgba(79, 70, 229, 0.4), 0 8px 10px -6px rgba(79, 70, 229, 0.4);
                cursor: pointer;
                transition: all 0.2s cubic-bezier(0.4, 0, 0.2, 1);
                z-index: 9999;
            }
            .dduk-chat-trigger:hover {
                transform: scale(1.08);
                background-color: #4338ca;
            }
            .dduk-chat-trigger:active {
                transform: scale(0.95);
            }
            .dduk-chat-panel {
                position: fixed;
                top: 9.5rem;
                right: 1.5rem;
                width: 450px;
                height: 600px;
                min-width: 320px;
                min-height: 400px;
                max-width: 95vw;
                max-height: 80vh;
                background-color: #ffffff;
                border-radius: 1.25rem;
                border: 1px solid #f1f5f9;
                box-shadow: 0 20px 25px -5px rgba(0, 0, 0, 0.1), 0 8px 10px -6px rgba(0, 0, 0, 0.1);
                display: flex;
                flex-direction: column;
                overflow: hidden;
                resize: none !important;
                transition: transform 0.3s cubic-bezier(0.34, 1.56, 0.64, 1), opacity 0.3s ease;
                transform: translateY(20px) scale(0.9);
                opacity: 0;
                pointer-events: none;
                z-index: 9999;
            }
            .dduk-chat-panel::-webkit-resizer {
                display: none !important;
            }
            .dduk-chat-panel.active {
                transform: translateY(0) scale(1);
                opacity: 1;
                pointer-events: auto;
            }
            .dduk-chat-header {
                padding: 1rem 1.25rem;
                background: linear-gradient(135deg, #4f46e5 0%, #3730a3 100%);
                color: #ffffff;
                display: flex;
                align-items: center;
                justify-content: space-between;
            }
            .dduk-chat-header-info {
                display: flex;
                align-items: center;
                gap: 0.75rem;
            }
            .dduk-chat-header-dot {
                width: 0.5rem;
                height: 0.5rem;
                background-color: #34d399;
                border-radius: 9999px;
                box-shadow: 0 0 8px #34d399;
                animation: dduk-pulse 2s infinite;
            }
            @keyframes dduk-pulse {
                0% { opacity: 0.4; }
                50% { opacity: 1; }
                100% { opacity: 0.4; }
            }
            .dduk-chat-header-title {
                font-size: 0.875rem;
                font-weight: 700;
            }
            .dduk-chat-header-close {
                background: none;
                border: none;
                color: rgba(255, 255, 255, 0.8);
                cursor: pointer;
                padding: 0.25rem;
                display: flex;
                align-items: center;
                justify-content: center;
                border-radius: 0.375rem;
                transition: all 0.2s;
            }
            .dduk-chat-header-close:hover {
                color: #ffffff;
                background-color: rgba(255, 255, 255, 0.1);
            }
            
            /* Portal Tab Styling */
            .dduk-portal-tabs {
                display: flex;
                background-color: #f1f5f9;
                border-bottom: 1px solid #e2e8f0;
                padding: 0.25rem;
                gap: 0.25rem;
            }
            .dduk-portal-tab {
                flex: 1;
                background: none;
                border: none;
                padding: 0.5rem 0;
                font-size: 0.8rem;
                font-weight: 700;
                color: #64748b;
                cursor: pointer;
                border-radius: 0.5rem;
                transition: all 0.2s;
                text-align: center;
            }
            .dduk-portal-tab.active {
                background-color: #ffffff;
                color: #4f46e5;
                box-shadow: 0 1px 3px rgba(0,0,0,0.05);
            }
            .dduk-portal-content {
                display: none;
                flex-direction: column;
                flex: 1;
                overflow: hidden;
            }
            .dduk-portal-content.active {
                display: flex;
            }
            .dduk-portal-scroll-area {
                flex: 1;
                padding: 1rem;
                overflow-y: auto;
                background-color: #f8fafc;
            }
            
            /* Anomaly Card Styling */
            .dduk-anomaly-card {
                background: #ffffff;
                border: 1px solid #e2e8f0;
                border-radius: 0.75rem;
                padding: 0.875rem;
                margin-bottom: 0.75rem;
                box-shadow: 0 1px 2px rgba(0,0,0,0.02);
            }
            .dduk-anomaly-card-header {
                display: flex;
                align-items: center;
                justify-content: space-between;
                margin-bottom: 0.375rem;
            }
            .dduk-anomaly-badge {
                font-size: 0.7rem;
                font-weight: 800;
                padding: 0.15rem 0.4rem;
                border-radius: 9999px;
            }
            .dduk-anomaly-badge.danger {
                background-color: #fee2e2;
                color: #ef4444;
            }
            .dduk-anomaly-badge.warning {
                background-color: #fef3c7;
                color: #d97706;
            }
            .dduk-anomaly-time {
                font-size: 0.7rem;
                color: #94a3b8;
                font-weight: 500;
            }
            .dduk-anomaly-title {
                font-size: 0.825rem;
                font-weight: 700;
                color: #1e293b;
                line-height: 1.3;
            }
            .dduk-anomaly-desc {
                font-size: 0.75rem;
                color: #64748b;
                margin-top: 0.25rem;
                line-height: 1.4;
            }
            .dduk-anomaly-action {
                display: flex;
                justify-content: flex-end;
            }
            .dduk-anomaly-btn {
                background-color: #f1f5f9;
                color: #4f46e5;
                border: none;
                border-radius: 0.5rem;
                padding: 0.3rem 0.75rem;
                font-size: 0.75rem;
                font-weight: 700;
                cursor: pointer;
                transition: all 0.2s;
            }
            .dduk-anomaly-btn:hover {
                background-color: #e2e8f0;
            }
            
            /* Prediction Card Styling */
            .dduk-prediction-card {
                background: #ffffff;
                border: 1px solid #e2e8f0;
                border-radius: 0.75rem;
                padding: 0.875rem;
                margin-bottom: 0.75rem;
                box-shadow: 0 1px 2px rgba(0,0,0,0.02);
            }
            .dduk-prediction-header {
                display: flex;
                align-items: center;
                gap: 0.5rem;
                margin-bottom: 0.5rem;
            }
            .dduk-prediction-indicator {
                width: 0.5rem;
                height: 0.5rem;
                border-radius: 9999px;
            }
            .dduk-prediction-indicator.blue { background-color: #3b82f6; }
            .dduk-prediction-indicator.green { background-color: #10b981; }
            .dduk-prediction-indicator.purple { background-color: #8b5cf6; }
            
            .dduk-prediction-title {
                font-size: 0.825rem;
                font-weight: 700;
                color: #1e293b;
                line-height: 1.3;
            }
            .dduk-prediction-stat {
                font-size: 1.15rem;
                font-weight: 800;
                color: #4f46e5;
                margin: 0.25rem 0;
            }
            .dduk-prediction-desc {
                font-size: 0.75rem;
                color: #64748b;
                line-height: 1.4;
            }
            .dduk-prediction-action {
                display: flex;
                justify-content: flex-end;
                margin-top: 0.625rem;
            }
            .dduk-prediction-link {
                background-color: #e0e7ff;
                color: #4f46e5;
                text-decoration: none;
                border-radius: 0.5rem;
                padding: 0.3rem 0.75rem;
                font-size: 0.75rem;
                font-weight: 700;
                cursor: pointer;
                transition: all 0.2s;
                display: inline-flex;
                align-items: center;
                gap: 0.25rem;
            }
            .dduk-prediction-link:hover {
                background-color: #c7d2fe;
            }
            
            .dduk-chat-messages {
                flex: 1;
                padding: 1.25rem;
                overflow-y: auto;
                background-color: #f8fafc;
                display: flex;
                flex-direction: column;
                gap: 1rem;
                scrollbar-width: thin;
            }
            .dduk-chat-messages::-webkit-scrollbar {
                width: 6px;
            }
            .dduk-chat-messages::-webkit-scrollbar-thumb {
                background-color: #cbd5e1;
                border-radius: 3px;
            }
            .dduk-chat-input-area {
                padding: 0.75rem 1rem;
                border-top: 1px solid #f1f5f9;
                background-color: #ffffff;
                display: flex;
                align-items: center;
                gap: 0.5rem;
            }
            .dduk-chat-input {
                flex: 1;
                border: 1px solid #e2e8f0;
                border-radius: 0.75rem;
                padding: 0.625rem 0.875rem;
                font-size: 0.875rem;
                outline: none;
                transition: border-color 0.2s;
                background-color: #ffffff;
                color: #1e293b;
            }
            .dduk-chat-input:focus {
                border-color: #4f46e5;
            }
            .dduk-chat-send-btn {
                background-color: #4f46e5;
                color: #ffffff;
                border: none;
                border-radius: 0.75rem;
                width: 2.25rem;
                height: 2.25rem;
                display: flex;
                align-items: center;
                justify-content: center;
                cursor: pointer;
                transition: background-color 0.2s;
            }
            .dduk-chat-send-btn:hover {
                background-color: #4338ca;
            }
            .dduk-chat-bubble {
                max-width: 85%;
                padding: 0.625rem 0.875rem;
                font-size: 0.875rem;
                line-height: 1.4;
                word-break: break-all;
                white-space: pre-wrap;
            }
            .dduk-chat-bubble.user {
                align-self: flex-end;
                background-color: #4f46e5;
                color: #ffffff;
                border-radius: 1rem 1rem 0 1rem;
                box-shadow: 0 2px 4px rgba(79, 70, 229, 0.15);
            }
            .dduk-chat-bubble.bot {
                align-self: flex-start;
                background-color: #ffffff;
                color: #1e293b;
                border-radius: 1rem 1rem 1rem 0;
                border: 1px solid #e2e8f0;
                box-shadow: 0 2px 4px rgba(0, 0, 0, 0.02);
            }
            .dduk-chat-bubble.loading {
                display: flex;
                align-items: center;
                gap: 0.5rem;
                color: #64748b;
                background-color: #f1f5f9;
            }

            .dduk-portal-resizer-left {
                width: 14px;
                height: 14px;
                position: absolute;
                left: 4px;
                bottom: 4px;
                cursor: sw-resize;
                z-index: 10000;
                background: linear-gradient(225deg, transparent 30%, #94a3b8 30%, #94a3b8 50%, transparent 50%, transparent 70%, #94a3b8 70%);
                border-radius: 0 0 0 1.25rem;
            }
        `;
        const style = document.createElement('style');
        style.textContent = css;
        document.head.appendChild(style);
        
        const container = document.createElement('div');
        container.id = 'dduk-floating-chatbot-container';
        container.innerHTML = `
            <div class="dduk-chat-trigger" id="dduk-floating-chatbot-trigger" title="AI 업무지원 포털">
                <i data-lucide="bot" style="width: 1.5rem; height: 1.5rem;"></i>
            </div>
            <div class="dduk-chat-panel" id="dduk-floating-chatbot-panel">
                <div class="dduk-chat-header">
                    <div class="dduk-chat-header-info">
                        <div class="dduk-chat-header-dot"></div>
                        <span class="dduk-chat-header-title">DDUK AI Copilot</span>
                    </div>
                    <button class="dduk-chat-header-close" id="dduk-floating-chatbot-close" aria-label="닫기">
                        <i data-lucide="x" style="width: 1.1rem; height: 1.1rem;"></i>
                    </button>
                </div>
                
                <div class="dduk-portal-tabs">
                    <button class="dduk-portal-tab active" data-tab="chatbot">AI 챗봇</button>
                    <button class="dduk-portal-tab" data-tab="anomaly">이상 탐지</button>
                    <button class="dduk-portal-tab" data-tab="prediction">예측 분석</button>
                    <button class="dduk-portal-tab" data-tab="rpa">RPA 제어</button>
                </div>
                
                <!-- AI 챗봇 컨텐츠 -->
                <div class="dduk-portal-content active" id="dduk-portal-content-chatbot">
                    <div class="dduk-chat-messages" id="dduk-floating-chatbot-messages">
                        <div class="dduk-chat-bubble bot">안녕하세요! 뚝 ERP AI 어시스턴트입니다. 무엇이든 물어보세요!</div>
                    </div>
                    <div class="dduk-chat-input-area">
                        <input type="text" class="dduk-chat-input" id="dduk-floating-chatbot-input" placeholder="메시지를 입력하세요..." autocomplete="off">
                        <button class="dduk-chat-send-btn" id="dduk-floating-chatbot-send" aria-label="보내기">
                            <i data-lucide="send" style="width: 1rem; height: 1rem;"></i>
                        </button>
                    </div>
                </div>
                
                <!-- 이상 탐지 컨텐츠 -->
                <div class="dduk-portal-content" id="dduk-portal-content-anomaly">
                    <div class="dduk-portal-scroll-area">
                        <div class="dduk-anomaly-list" id="dduk-floating-anomaly-list"></div>
                    </div>
                </div>
                
                <!-- 예측 분석 컨텐츠 -->
                <div class="dduk-portal-content" id="dduk-portal-content-prediction">
                    <div class="dduk-portal-scroll-area">
                        <div class="dduk-prediction-list" id="dduk-floating-prediction-list"></div>
                    </div>
                </div>

                <!-- RPA 제어 컨텐츠 -->
                <div class="dduk-portal-content" id="dduk-portal-content-rpa">
                    <div class="dduk-portal-scroll-area" id="dduk-floating-rpa-scroll-area">
                        <div id="dduk-floating-rpa-container"></div>
                    </div>
                </div>
                
                <!-- 좌측 하단 크기 조절 핸들 -->
                <div class="dduk-portal-resizer-left" id="dduk-portal-resizer-left"></div>
            </div>
        `;
        document.body.appendChild(container);
        
        document.getElementById('dduk-floating-chatbot-trigger').addEventListener('click', () => window.toggleFloatingChatbot());
        document.getElementById('dduk-floating-chatbot-close').addEventListener('click', () => window.toggleFloatingChatbot(false));
        
        document.querySelectorAll('.dduk-portal-tab').forEach(tabBtn => {
            tabBtn.addEventListener('click', () => {
                const tabName = tabBtn.getAttribute('data-tab');
                switchPortalTab(tabName);
            });
        });
        
        const sendBtn = document.getElementById('dduk-floating-chatbot-send');
        const inputField = document.getElementById('dduk-floating-chatbot-input');
        
        sendBtn.addEventListener('click', () => {
            const query = inputField.value.trim();
            sendFloatingChatMessage(query);
        });
        
        inputField.addEventListener('keydown', (e) => {
            if (e.key === 'Enter') {
                const query = inputField.value.trim();
                sendFloatingChatMessage(query);
            }
        });
        
        // Custom 드래그 리사이저 바인딩 (좌측 구석 단방향 지원)
        const resizerLeft = document.getElementById('dduk-portal-resizer-left');
        const panel = document.getElementById('dduk-floating-chatbot-panel');
        
        if (resizerLeft && panel) {
            let startWidth, startHeight, startX, startY;
            
            resizerLeft.addEventListener('mousedown', (e) => {
                e.preventDefault();
                startWidth = parseInt(document.defaultView.getComputedStyle(panel).width, 10);
                startHeight = parseInt(document.defaultView.getComputedStyle(panel).height, 10);
                startX = e.clientX;
                startY = e.clientY;
                
                document.documentElement.addEventListener('mousemove', doResize);
                document.documentElement.addEventListener('mouseup', stopResize);
            });
            
            function doResize(e) {
                e.preventDefault();
                const newWidth = Math.max(320, Math.min(window.innerWidth * 0.95, startWidth - (e.clientX - startX)));
                const newHeight = Math.max(400, Math.min(window.innerHeight * 0.8, startHeight + (e.clientY - startY)));
                
                panel.style.width = newWidth + 'px';
                panel.style.height = newHeight + 'px';
            }
            
            function stopResize() {
                document.documentElement.removeEventListener('mousemove', doResize);
                document.documentElement.removeEventListener('mouseup', stopResize);
            }
        }
        
        renderAnomalyList();
        renderPredictionList();
        
        if (window.lucide) lucide.createIcons();
    }

    // ==========================================
    // RPA Control Tab Logic & API Integration
    // ==========================================
    const rpaState = {
        taskId: null,
        actionName: null,
        pollingHandle: null,
        status: 'IDLE',
        lastUpdated: '-'
    };

    const rpaTranslation = {
        taskTypes: {
            'PURCHASE_PRICE': '단가 수집',
            'INVENTORY_ALERT': '재고 부족'
        },
        actions: {
            'COLLECT_EXTERNAL_PRICE': '외부 단가 수집',
            'CHECK_LOW_INVENTORY': '부족 재고 점검',
            'CHECK_INVENTORY_SHORTAGE': '부족 재고 점검'
        }
    };

    function translateRpaType(type) {
        return rpaTranslation.taskTypes[type] || type || '-';
    }

    function translateRpaAction(action) {
        return rpaTranslation.actions[action] || action || '-';
    }

    const terminalStatuses = new Set(["SUCCESS", "FAILED"]);
    const activeStatuses = new Set(["REQUESTED", "RUNNING"]);

    function getApiBaseUrl() {
        return window.ddukSession && typeof window.ddukSession.getApiBaseUrl === "function"
            ? window.ddukSession.getApiBaseUrl()
            : "http://localhost:8080";
    }

    function getHeaders(extraHeaders) {
        if (window.ddukSession && typeof window.ddukSession.getAuthHeaders === "function") {
            return window.ddukSession.getAuthHeaders({
                "Content-Type": "application/json",
                ...(extraHeaders || {})
            });
        }
        return {
            "Content-Type": "application/json",
            ...(extraHeaders || {})
        };
    }

    async function requestRpaApi(path, options) {
        const response = await fetch(`${getApiBaseUrl()}${path}`, {
            ...options,
            headers: getHeaders(options && options.headers)
        });

        const text = await response.text();
        let payload;
        try {
            payload = text ? JSON.parse(text) : null;
        } catch (e) {
            throw new Error("응답 형식 오류가 발생했습니다.");
        }

        if (!response.ok || !payload || payload.status !== "success") {
            throw new Error(payload && payload.message ? payload.message : "RPA 요청 처리 중 오류가 발생했습니다.");
        }

        return payload.data;
    }

    function getRpaContext() {
        const path = window.location.pathname.replace(/\\/g, '/');
        if (path.includes('/purchase-dashboard.html')) {
            return {
                taskType: 'PURCHASE_PRICE',
                title: '외부 원부자재 최신 단가 수집',
                description: '외부 거래처 사이트에서 원부자재 단가를 조회해 오는 RPA 봇입니다.',
                actionName: 'COLLECT_EXTERNAL_PRICE'
            };
        } else if (path.includes('/dashboard.html')) {
            return {
                taskType: 'INVENTORY_ALERT',
                title: '안전 재고 하회 품목 조회',
                description: '현재 창고 재고를 점검하여 안전 재고 이하 품목을 식별하는 RPA 봇입니다.',
                actionName: 'CHECK_LOW_INVENTORY'
            };
        } else {
            return {
                taskType: 'PURCHASE_PRICE',
                title: 'RPA 연동 상태 점검',
                description: 'RPA 서버 및 콜백 기능 연결을 점검하기 위한 테스트용 트리거입니다.',
                actionName: 'COLLECT_EXTERNAL_PRICE'
            };
        }
    }

    function renderDynamicRpaWidget() {
        const container = document.getElementById('dduk-floating-rpa-container');
        if (!container) return;

        const ctx = getRpaContext();
        
        container.innerHTML = `
            <div class="space-y-4" style="font-family: inherit;">
                <div class="rounded-2xl border border-slate-100 bg-white p-5 shadow-sm">
                    <div class="flex items-center justify-between gap-3" style="display: flex; align-items: center; justify-content: space-between; gap: 0.75rem;">
                        <div class="min-w-0" style="flex: 1; min-width: 0;">
                            <h3 class="text-sm font-bold text-gray-900" style="margin:0; overflow: hidden; text-overflow: ellipsis; white-space: nowrap;" title="${ctx.title}">${ctx.title}</h3>
                            <p class="mt-1 text-[11px] text-gray-400" style="margin: 0.25rem 0 0 0; line-height: 1.3; display: -webkit-box; -webkit-line-clamp: 2; -webkit-box-orient: vertical; overflow: hidden;" title="${ctx.description}">${ctx.description}</p>
                        </div>
                        <div style="flex-shrink: 0;">
                            <span id="rpaWidgetStatusBadge" class="rounded-full bg-slate-100 px-2.5 py-0.5 text-[10px] font-bold text-slate-600" style="white-space: nowrap;">대기</span>
                        </div>
                    </div>

                    <div class="mt-4 grid gap-2.5 grid-cols-3" style="display: grid; grid-template-columns: repeat(3, 1fr); gap: 0.5rem;">
                        <div class="rounded-xl border border-gray-100 bg-gray-50/50 p-2 text-center">
                            <p class="text-[9px] font-bold uppercase tracking-wider text-gray-400" style="margin:0;">업무 유형</p>
                            <p class="mt-0.5 text-[11px] font-semibold text-gray-800" style="margin:0;">${translateRpaType(ctx.taskType)}</p>
                        </div>
                        <div class="rounded-xl border border-gray-100 bg-gray-50/50 p-2 text-center">
                            <p class="text-[9px] font-bold uppercase tracking-wider text-gray-400" style="margin:0;">수행 작업</p>
                            <p id="rpaWidgetActionName" class="mt-0.5 text-[11px] font-semibold text-gray-800" style="margin:0;">${translateRpaAction(rpaState.actionName || ctx.actionName)}</p>
                        </div>
                        <div class="rounded-xl border border-gray-100 bg-gray-50/50 p-2 text-center">
                            <p class="text-[9px] font-bold uppercase tracking-wider text-gray-400" style="margin:0;">작업 ID</p>
                            <p id="rpaWidgetTaskId" class="mt-0.5 text-[11px] font-semibold text-gray-800" style="margin:0; overflow:hidden; text-overflow:ellipsis; white-space:nowrap;" title="${rpaState.taskId || '-'}">${rpaState.taskId || '-'}</p>
                        </div>
                    </div>

                    <div id="rpaWidgetMessage" class="mt-4 rounded-xl border border-slate-100 bg-slate-50/50 px-4 py-3 text-xs text-slate-500 leading-normal">
                        준비되었습니다. 'RPA 봇 구동' 버튼을 누르면 자동 업무 프로세스를 시작합니다.
                    </div>

                    <div class="mt-5 flex items-center justify-between gap-3">
                        <span id="rpaWidgetLastUpdated" class="text-[10px] text-slate-400 font-medium">최근 상태 확인: -</span>
                        <button id="rpaWidgetTriggerButton" type="button" class="rounded-xl bg-indigo-600 hover:bg-indigo-700 px-4 py-2 text-xs font-bold text-white shadow-sm shadow-indigo-100 transition-all">RPA 봇 구동</button>
                    </div>
                </div>

                <div id="rpaWidgetResultContainer"></div>
            </div>
        `;

        const triggerButton = document.getElementById("rpaWidgetTriggerButton");
        triggerButton.addEventListener("click", triggerRpa);

        if (rpaState.status !== 'IDLE') {
            updateSummary();
        } else {
            checkAndRenderPreExistingResult(ctx.taskType);
        }
    }

    function renderStatus(status) {
        const statusBadge = document.getElementById("rpaWidgetStatusBadge");
        const triggerButton = document.getElementById("rpaWidgetTriggerButton");
        if (!statusBadge || !triggerButton) return;

        const palette = {
            IDLE: "bg-slate-100 text-slate-600",
            REQUESTED: "bg-amber-100 text-amber-700",
            RUNNING: "bg-sky-100 text-sky-700",
            SUCCESS: "bg-emerald-100 text-emerald-700",
            FAILED: "bg-rose-100 text-rose-700"
        };
        const labels = {
            IDLE: "대기",
            REQUESTED: "요청됨",
            RUNNING: "실행 중",
            SUCCESS: "성공",
            FAILED: "실패"
        };

        statusBadge.className = `rounded-full px-2.5 py-0.5 text-[10px] font-bold ${palette[status] || palette.IDLE}`;
        statusBadge.textContent = labels[status] || status;
        
        const isProcessing = activeStatuses.has(status);
        triggerButton.disabled = isProcessing;
        triggerButton.classList.toggle("opacity-60", isProcessing);
        triggerButton.classList.toggle("cursor-not-allowed", isProcessing);
        triggerButton.textContent = isProcessing ? "실행 중..." : "RPA 봇 구동";
    }

    function setMessage(text, tone) {
        const messageElement = document.getElementById("rpaWidgetMessage");
        if (!messageElement) return;

        const tones = {
            neutral: "border-slate-100 bg-slate-50/50 text-slate-500",
            loading: "border-sky-100 bg-sky-50/50 text-sky-700",
            success: "border-emerald-100 bg-emerald-50/50 text-emerald-700",
            error: "border-rose-100 bg-rose-50/50 text-rose-700"
        };
        messageElement.className = `mt-4 rounded-xl border px-4 py-3 text-xs leading-normal ${tones[tone] || tones.neutral}`;
        messageElement.textContent = text;
    }

    function updateSummary(detail) {
        const status = detail && detail.status ? detail.status : rpaState.status;
        rpaState.status = status;

        const actionNameEl = document.getElementById("rpaWidgetActionName");
        const taskIdEl = document.getElementById("rpaWidgetTaskId");
        const lastUpdatedEl = document.getElementById("rpaWidgetLastUpdated");

        if (actionNameEl) {
            const rawAction = detail && detail.actionName ? detail.actionName : rpaState.actionName;
            actionNameEl.textContent = translateRpaAction(rawAction);
        }
        if (taskIdEl) taskIdEl.textContent = detail && detail.taskId ? detail.taskId : rpaState.taskId || "-";
        if (lastUpdatedEl) lastUpdatedEl.textContent = `최근 상태 확인: ${formatDateTime(new Date().toISOString())}`;
        
        renderStatus(status);

        if (!detail) return;

        if (detail.status === "SUCCESS") {
            setMessage("RPA 작업이 성공적으로 끝났습니다. 아래 실시간 요약 데이터를 확인해 보세요.", "success");
            fetchAndRenderRpaResult(getRpaContext().taskType);
        } else if (detail.status === "FAILED") {
            setMessage(detail.errorMessage || "RPA 실행 중 오류가 발생하여 작업이 실패했습니다.", "error");
        } else if (detail.status === "RUNNING") {
            setMessage("RPA 작업이 현재 대상 시스템에서 실행 중입니다. 잠시만 대기해 주세요.", "loading");
        } else if (detail.status === "REQUESTED") {
            setMessage("RPA 요청이 백엔드에 접수되어 실행을 준비 중입니다.", "loading");
        }
    }

    function stopPolling() {
        if (rpaState.pollingHandle) {
            window.clearTimeout(rpaState.pollingHandle);
            rpaState.pollingHandle = null;
        }
    }

    async function loadTaskDetail(taskId) {
        const detail = await requestRpaApi(`/api/v1/admin/tasks/${encodeURIComponent(taskId)}`, {
            method: "GET"
        });
        updateSummary(detail);
        return detail;
    }

    async function pollTask(taskId, attempt) {
        try {
            const detail = await loadTaskDetail(taskId);
            if (detail && !terminalStatuses.has(detail.status) && attempt < 30) {
                rpaState.pollingHandle = window.setTimeout(function () {
                    pollTask(taskId, attempt + 1);
                }, 2000);
                return;
            }

            stopPolling();
            if (detail && !terminalStatuses.has(detail.status)) {
                setMessage("최종 결과 처리가 지연되고 있습니다. 작업이 완료되면 결과 데이터가 표시됩니다.", "loading");
            }
        } catch (error) {
            stopPolling();
            renderStatus("FAILED");
            setMessage(error.message || "작업 상태를 확인하지 못했습니다.", "error");
        }
    }

    async function triggerRpa() {
        stopPolling();
        renderStatus("REQUESTED");
        setMessage("RPA trigger 요청을 보내는 중입니다...", "loading");

        const ctx = getRpaContext();

        try {
            const data = await requestRpaApi("/api/v1/admin/rpa/trigger", {
                method: "POST",
                body: JSON.stringify({ taskType: ctx.taskType })
            });

            rpaState.taskId = data.taskId || null;
            rpaState.actionName = data.actionName || null;
            rpaState.status = "REQUESTED";
            
            updateSummary({ taskId: rpaState.taskId, actionName: rpaState.actionName, status: "REQUESTED" });

            if (rpaState.taskId) {
                pollTask(rpaState.taskId, 0);
            }
        } catch (error) {
            renderStatus("FAILED");
            setMessage(error.message || "RPA trigger 호출에 실패했습니다.", "error");
        }
    }

    async function checkAndRenderPreExistingResult(taskType) {
        fetchAndRenderRpaResult(taskType, true);
    }

    async function fetchAndRenderRpaResult(taskType, silent = false) {
        const resultContainer = document.getElementById('rpaWidgetResultContainer');
        if (!resultContainer) return;

        try {
            if (taskType === 'INVENTORY_ALERT') {
                const stats = await requestRpaApi('/api/v1/inventory/dashboard/stats', { method: 'GET' });
                const block = stats.inventoryShortageRpa;
                if (!block || !block.items || !block.items.length) {
                    if (!silent) resultContainer.innerHTML = '';
                    return;
                }

                let itemsHtml = block.items.slice(0, 3).map(item => `
                    <div class="rounded-xl border border-slate-100 bg-white p-3 text-xs shadow-sm" style="margin-bottom: 0.5rem;">
                        <div class="flex items-center justify-between font-bold text-gray-900 mb-1" style="display: flex; justify-content: space-between;">
                            <span>${escapeHtml(item.itemName)}</span>
                            <span class="text-rose-600">${escapeHtml(item.rpaStockStatus || '부족')}</span>
                        </div>
                        <div class="flex justify-between text-[11px] text-gray-500" style="display: flex; justify-content: space-between;">
                            <span>위치: ${escapeHtml(item.warehouseName || '-')}</span>
                            <span>보유/안전: ${item.availableStock ?? 0} / ${item.safetyStock ?? 0}</span>
                        </div>
                        <div class="mt-2 rounded-lg bg-rose-50/50 p-2 text-[10px] font-medium text-rose-700 leading-normal" style="margin-top: 0.5rem; padding: 0.5rem; border-radius: 0.5rem;">
                            추천 행동: ${escapeHtml(item.recommendedAction || '-')}
                        </div>
                    </div>
                `).join('');

                resultContainer.innerHTML = `
                    <div class="rounded-2xl border border-slate-100 bg-slate-50/40 p-4 space-y-3" style="margin-top: 1rem; border: 1px solid #f1f5f9; padding: 1rem; border-radius: 1rem; background-color: #f8fafc;">
                        <div class="flex items-center justify-between" style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 0.75rem;">
                            <h4 class="text-xs font-bold text-slate-800" style="margin: 0;">재고 부족 감지 결과</h4>
                            <span class="text-[10px] font-medium text-slate-400">총 ${block.matchedLowStockCount || 0}건</span>
                        </div>
                        <div class="grid gap-2">
                            ${itemsHtml}
                        </div>
                        ${block.matchedLowStockCount > 3 ? `
                            <div class="text-[10px] text-center text-slate-400 font-semibold pt-1" style="text-align: center; margin-top: 0.5rem;">
                                그 외 ${block.matchedLowStockCount - 3}건의 부족 품목이 더 존재합니다.
                            </div>
                        ` : ''}
                    </div>
                `;
            } else if (taskType === 'PURCHASE_PRICE') {
                const stats = await requestRpaApi('/api/v1/inventory/purchase-dashboard/stats', { method: 'GET' });
                const block = stats.rpaComparison || stats.comparison;
                if (!block || !block.items || !block.items.length) {
                    if (!silent) resultContainer.innerHTML = '';
                    return;
                }

                const delta = block.priceDelta || 0;
                const deltaColor = delta > 0 ? 'text-rose-600' : delta < 0 ? 'text-emerald-600' : 'text-slate-600';
                const deltaSign = delta > 0 ? '+' : '';

                let itemsHtml = block.items.slice(0, 3).map(item => {
                    const itemDelta = item.priceDelta || 0;
                    const itemDeltaColor = itemDelta > 0 ? 'text-rose-600 font-semibold' : itemDelta < 0 ? 'text-emerald-600 font-semibold' : 'text-slate-400';
                    const itemDeltaSign = itemDelta > 0 ? '+' : '';
                    const itemDeltaText = itemDelta === 0 ? '동일' : `${itemDeltaSign}${itemDelta.toLocaleString()}원`;

                    return `
                        <tr class="border-b border-slate-100">
                            <td class="py-2 text-[11px] font-medium text-gray-800" style="text-align: left; padding: 6px 0; max-width: 90px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap;">${escapeHtml(item.productName || item.itemName)}</td>
                            <td class="py-2 text-right text-[11px] text-gray-500" style="text-align: right; padding: 6px 0;">${(item.collectedPrice || 0).toLocaleString()}원</td>
                            <td class="py-2 text-right text-[11px] text-gray-400" style="text-align: right; padding: 6px 0;">${(item.erpPrice || 0).toLocaleString()}원</td>
                            <td class="py-2 text-right text-[11px] ${itemDeltaColor}" style="text-align: right; padding: 6px 0;">${itemDeltaText}</td>
                        </tr>
                    `;
                }).join('');

                resultContainer.innerHTML = `
                    <div class="rounded-2xl border border-slate-100 bg-slate-50/40 p-4 space-y-3" style="margin-top: 1rem; border: 1px solid #f1f5f9; padding: 1rem; border-radius: 1rem; background-color: #f8fafc;">
                        <h4 class="text-xs font-bold text-slate-800" style="margin: 0; margin-bottom: 0.75rem;">단가 수집 및 격차 통계</h4>
                        
                        <div class="grid grid-cols-2 gap-2" style="display: grid; grid-template-columns: 1fr 1fr; gap: 0.5rem; margin-bottom: 0.75rem;">
                            <div class="rounded-xl border border-slate-100 bg-white p-2.5 text-center shadow-xs" style="border: 1px solid #f1f5f9; border-radius: 0.75rem; text-align: center; padding: 0.625rem; background-color: #ffffff;">
                                <p class="text-[9px] font-bold text-slate-400 uppercase" style="margin: 0;">수집 평균가</p>
                                <p class="mt-0.5 text-xs font-extrabold text-slate-800" style="margin: 0.25rem 0 0 0;">${(block.latestCollectedAveragePrice || 0).toLocaleString()}원</p>
                            </div>
                            <div class="rounded-xl border border-slate-100 bg-white p-2.5 text-center shadow-xs" style="border: 1px solid #f1f5f9; border-radius: 0.75rem; text-align: center; padding: 0.625rem; background-color: #ffffff;">
                                <p class="text-[9px] font-bold text-slate-400 uppercase" style="margin: 0;">단가 차이</p>
                                <p class="mt-0.5 text-xs font-extrabold ${deltaColor}" style="margin: 0.25rem 0 0 0;">${deltaSign}${delta.toLocaleString()}원</p>
                            </div>
                        </div>

                        <div class="rounded-xl border border-slate-100 bg-white p-3 shadow-xs" style="border: 1px solid #f1f5f9; border-radius: 0.75rem; padding: 0.75rem; background-color: #ffffff;">
                            <table class="w-full" style="width: 100%; border-collapse: collapse;">
                                <thead>
                                    <tr class="border-b text-[10px] font-bold text-slate-400" style="border-bottom: 1px solid #e2e8f0;">
                                        <th class="pb-1.5 text-left" style="text-align: left; padding-bottom: 0.375rem; width: 35%;">품목명</th>
                                        <th class="pb-1.5 text-right" style="text-align: right; padding-bottom: 0.375rem; width: 22%;">수집가</th>
                                        <th class="pb-1.5 text-right" style="text-align: right; padding-bottom: 0.375rem; width: 22%;">ERP가</th>
                                        <th class="pb-1.5 text-right" style="text-align: right; padding-bottom: 0.375rem; width: 21%;">차이</th>
                                    </tr>
                                </thead>
                                <tbody>
                                    ${itemsHtml}
                                </tbody>
                            </table>
                            ${block.items.length > 3 ? `
                                <p class="mt-2 text-center text-[9px] text-slate-400 font-semibold" style="margin-top: 0.5rem; text-align: center;">그 외 ${block.items.length - 3}개 품목이 더 있습니다.</p>
                            ` : ''}
                        </div>
                    </div>
                `;
            } else {
                resultContainer.innerHTML = `
                    <div class="rounded-2xl border border-emerald-100 bg-emerald-50/30 p-4 text-center" style="margin-top: 1rem; border: 1px solid #d1fae5; border-radius: 1rem; background-color: rgba(209, 250, 229, 0.3); text-align: center; padding: 1rem;">
                        <div class="inline-flex rounded-full bg-emerald-100 p-2 text-emerald-600 mb-2" style="margin-bottom: 0.5rem; display: inline-flex; border-radius: 9999px; background-color: #d1fae5; padding: 0.5rem;">
                            <svg style="width:1.25rem; height:1.25rem;" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="3">
                                <path stroke-linecap="round" stroke-linejoin="round" d="M5 13l4 4L19 7" />
                            </svg>
                        </div>
                        <h4 class="text-xs font-bold text-emerald-950" style="margin: 0;">테스트 성공!</h4>
                        <p class="text-[11px] font-semibold text-emerald-800 mt-1" style="margin: 0.25rem 0 0 0;">RPA 서버 및 콜백 연동이 완벽하게 확인되었습니다.</p>
                    </div>
                `;
            }
        } catch (error) {
            console.error('결과 데이터를 요약 렌더링하지 못함:', error);
        }
    }
})();
