(function () {
    const RECENT_MENU_KEY = 'dduk_dashboard_recent_menu';
    const RECENT_MENU_TTL = 3 * 24 * 60 * 60 * 1000;

    const MENU_GROUPS = [
        {
            id: 'purchase',
            label: '구매/발주',
            items: [
                { label: '구매/발주 대시보드', icon: 'bar-chart-3', href: '#' },
                { label: '발주 요청', icon: 'file-plus', href: '#' },
                { label: '발주 관리', icon: 'clipboard-list', href: '#' },
                { label: '입고 등록', icon: 'package-check', href: '#' },
                { label: '거래처 관리', icon: 'building', href: '#' }
            ]
        },
        {
            id: 'inventory',
            label: '재고관리',
            items: [
                { label: '재고관리 대시보드', icon: 'bar-chart-3', href: '#' },
                { label: '재고 조회', icon: 'search', href: '#' },
                { label: '입출고 이력', icon: 'history', href: '#' },
                { label: '창고 이동', icon: 'truck', href: '#' },
                { label: '자동 발주 추천', icon: 'zap', href: '#' }
            ]
        },
        {
            id: 'accounting',
            label: '회계관리',
            items: [
                { label: '회계 대시보드', icon: 'bar-chart-3', href: 'pages/hr/accounting/dashboard.html' },
                { label: '세금계산서', icon: 'receipt', href: 'pages/hr/accounting/transactions.html' },
                { label: '비용 처리', icon: 'credit-card', href: '#' },
                { label: '매입/매출', icon: 'trending-up', href: 'pages/hr/accounting/trial-balance.html' },
                { label: '월별 정산', icon: 'calendar', href: 'pages/hr/accounting/settlement.html' },
                { label: '급여 계산', icon: 'wallet', href: 'pages/hr/payroll/list.html', match: 'pages/hr/payroll/' },
                { label: '회계 리포트', icon: 'file-bar-chart', href: 'pages/hr/accounting/reports.html' }
            ]
        },
        {
            id: 'docs',
            label: '문서/증빙',
            items: [
                { label: '증빙 업로드', icon: 'upload', href: '#' },
                { label: 'OCR 문서함', icon: 'scan', href: '#' },
                { label: '계약 문서', icon: 'file-signature', href: '#' }
            ]
        },
        {
            id: 'ai',
            label: 'AI 업무지원',
            items: [
                { label: 'AI 챗봇', icon: 'bot', href: '#' },
                { label: '이상 탐지', icon: 'alert-triangle', href: '#' },
                { label: '예측 분석', icon: 'brain', href: '#' }
            ]
        },
        {
            id: 'admin',
            label: '관리자',
            items: [
                { label: '사용자 관리', icon: 'users', href: 'pages/admin/members.html' },
                { label: '권한 관리', icon: 'shield', href: 'pages/admin/roles.html' },
                { label: '시스템 로그', icon: 'scroll', href: 'pages/admin/logs.html' },
                { label: '서버 모니터링', icon: 'server', href: 'pages/admin/monitoring.html' },
                { label: '시스템 설정', icon: 'settings', href: 'pages/admin/settings.html' }
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
        return `
            <a class="menu_item${extraClass ? ` ${extraClass}` : ''}${currentClass}" href="${resolveHref(item.href)}" data-label="${item.label}" title="${item.label}">
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
                    ${renderMenuItem({ label: '대시보드', icon: 'layout-dashboard', href: 'dashboard.html' })}
                    ${renderGroups()}
                </nav>
                <div class="dduk-inline-019">
                    <div class="dduk-inline-020">
                        <div class="dduk-inline-021"><i class="dduk-inline-022" data-lucide="user"></i></div>
                        <div class="sidebar_user_text">
                            <p class="dduk-inline-023">김도연</p>
                            <p class="dduk-inline-024">관리자</p>
                        </div>
                    </div>
                    <div class="dduk-inline-025">
                        <span class="status_dot dduk-inline-026"></span>
                        <span class="dduk-inline-024 sidebar_status_text">서버 정상 · 99.9% uptime</span>
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
        document.querySelectorAll('.menu_item').forEach(menuItem => {
            menuItem.addEventListener('click', () => addRecentMenu(menuItem));
        });
        renderRecentMenus();
        openCurrentMenuGroup();
        checkMobile();
        window.addEventListener('resize', checkMobile);
        if (window.lucide) lucide.createIcons();
    }

    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', initSidebar);
    } else {
        initSidebar();
    }
})();
