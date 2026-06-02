(function () {
    // 1. 공용 유틸리티 함수 보관 및 노출
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

    function escapeHtml(value) {
        return String(value ?? '').replace(/[&<>"']/g, (char) => ({
            '&': '&amp;',
            '<': '&lt;',
            '>': '&gt;',
            '"': '&quot;',
            "'": '&#039;'
        }[char]));
    }

    function formatDateTime(value) {
        if (!value) return '-';
        try {
            const date = new Date(value);
            if (isNaN(date.getTime())) return value;
            const y = date.getFullYear();
            const m = String(date.getMonth() + 1).padStart(2, '0');
            const d = String(date.getDate()).padStart(2, '0');
            const hh = String(date.getHours()).padStart(2, '0');
            const mm = String(date.getMinutes()).padStart(2, '0');
            return `${y}-${m}-${d} ${hh}:${mm}`;
        } catch (e) {
            return value;
        }
    }

    function getApiBaseUrl() {
        return window.ddukSession && typeof window.ddukSession.getApiBaseUrl === 'function'
            ? window.ddukSession.getApiBaseUrl()
            : 'http://localhost:8080';
    }

    function getHeaders(extraHeaders) {
        if (window.ddukSession && typeof window.ddukSession.getAuthHeaders === 'function') {
            return window.ddukSession.getAuthHeaders({
                'Content-Type': 'application/json',
                ...(extraHeaders || {})
            });
        }
        return {
            'Content-Type': 'application/json',
            ...(extraHeaders || {})
        };
    }

    window.DDUK_COMMON = {
        getRootPath,
        resolveHref,
        escapeHtml,
        formatDateTime,
        getApiBaseUrl,
        getHeaders
    };

    // 2. 기존 전역 계약(브릿지 함수) 유지
    window.toggleMenu = function (id) {
        if (window.DDUK_SIDEBAR_MENU && typeof window.DDUK_SIDEBAR_MENU.toggleMenu === 'function') {
            window.DDUK_SIDEBAR_MENU.toggleMenu(id);
        }
    };

    window.toggleRecentMenu = function () {
        if (window.DDUK_SIDEBAR_MENU && typeof window.DDUK_SIDEBAR_MENU.toggleRecentMenu === 'function') {
            window.DDUK_SIDEBAR_MENU.toggleRecentMenu();
        }
    };

    window.toggleSidebar = function () {
        if (window.DDUK_SIDEBAR_MENU && typeof window.DDUK_SIDEBAR_MENU.toggleSidebar === 'function') {
            window.DDUK_SIDEBAR_MENU.toggleSidebar();
        }
    };

    window.toggleFloatingChatbot = function (forceOpen) {
        if (window.DDUK_FLOATING_PORTAL && typeof window.DDUK_FLOATING_PORTAL.toggleFloatingChatbot === 'function') {
            window.DDUK_FLOATING_PORTAL.toggleFloatingChatbot(forceOpen);
        }
    };

    window.openFloatingChatbotWithQuery = function (query) {
        if (window.DDUK_FLOATING_PORTAL && typeof window.DDUK_FLOATING_PORTAL.openFloatingChatbotWithQuery === 'function') {
            window.DDUK_FLOATING_PORTAL.openFloatingChatbotWithQuery(query);
        }
    };

    window.handleLogout = function () {
        localStorage.clear();
        sessionStorage.clear();
        const root = getRootPath();
        window.location.href = root + 'index.html';
    };

    // 3. 동적 스크립트 로더 및 초기화
    function loadScript(src) {
        return new Promise((resolve, reject) => {
            const script = document.createElement('script');
            script.src = src;
            script.onload = resolve;
            script.onerror = reject;
            document.head.appendChild(script);
        });
    }

    const root = getRootPath();
    const commonPath = root + 'services/common/';

    const scripts = [];
    if (!window.ddukSession) {
        scripts.push(commonPath + 'session.js');
    }
    scripts.push(commonPath + 'sidebar-menu.js');
    scripts.push(commonPath + 'floating-rpa-widget.js');
    scripts.push(commonPath + 'floating-portal.js');

    function loadAllScripts() {
        return scripts.reduce((promise, src) => {
            return promise.then(() => loadScript(src));
        }, Promise.resolve());
    }

    function init() {
        loadAllScripts()
            .then(() => {
                if (window.DDUK_SIDEBAR_MENU && typeof window.DDUK_SIDEBAR_MENU.initSidebar === 'function') {
                    window.DDUK_SIDEBAR_MENU.initSidebar();
                }
                // AI Copilot Portal 위젯 초기화는 sidebar-menu가 init된 후에 이루어져도 되며, 내부에서 DOM 로딩이 완료되면 처리
                if (window.DDUK_FLOATING_PORTAL && typeof window.DDUK_FLOATING_PORTAL.initAICopilotPortal === 'function') {
                    window.DDUK_FLOATING_PORTAL.initAICopilotPortal();
                }
            })
            .catch(err => {
                console.error('사이드바 서비스 스크립트 로드 중 오류 발생:', err);
            });
    }

    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', init);
    } else {
        init();
    }
})();
