(function () {
    const rolePathMap = {
        ADMIN: "../admin/dashboard.html",
        HR: "../hr/dashboard.html",
        INVENTORY: "../inventory/dashboard.html"
    };

    function getSession() {
        return {
            token: localStorage.getItem("token"),
            loginId: localStorage.getItem("loginId"),
            role: localStorage.getItem("role"),
            userName: localStorage.getItem("userName")
        };
    }

    function getApiBaseUrl() {
        const host = window.location.hostname === "127.0.0.1" ? "127.0.0.1" : "localhost";
        return `http://${host}:8080`;
    }

    function getAuthHeaders(extraHeaders) {
        const session = getSession();
        const headers = {
            Authorization: `Bearer ${session.token}`
        };

        return Object.assign(headers, extraHeaders || {});
    }

    function clearSession() {
        localStorage.removeItem("token");
        localStorage.removeItem("loginId");
        localStorage.removeItem("role");
        localStorage.removeItem("userName");
    }

    function redirectToLogin() {
        window.location.href = "../../index.html";
    }

    function requireRole(allowedRoles) {
        const session = getSession();

        /* =================================================================
           [개발용 미리보기 모드] 
           로그인 없이 UI를 확인하고 싶을 때 아래 한 줄의 주석을 해제하세요.
           ================================================================= */
        // return { token: "preview", role: "ADMIN", userName: "미리보기 계정", loginId: "admin_preview" };

        if (!session.token || !session.role) {
            redirectToLogin();
            return null;
        }

        if (allowedRoles.length > 0 && !allowedRoles.includes(session.role)) {
            const fallbackPath = rolePathMap[session.role];

            if (fallbackPath) {
                window.location.href = fallbackPath;
            } else {
                redirectToLogin();
            }

            return null;
        }

        return session;
    }

    function bindShell(session) {
        const userNameElement = document.querySelector("[data-user-name]");
        const loginIdElement = document.querySelector("[data-login-id]");
        const roleElement = document.querySelector("[data-role]");
        const logoutButton = document.querySelector("[data-action='logout']");

        if (userNameElement) {
            userNameElement.textContent = session.userName || "-";
        }

        if (loginIdElement) {
            loginIdElement.textContent = session.loginId || "-";
        }

        if (roleElement) {
            roleElement.textContent = session.role || "-";
        }

        if (logoutButton) {
            logoutButton.addEventListener("click", function () {
                clearSession();
                redirectToLogin();
            });
        }
    }

    window.ddukSession = {
        bindShell,
        clearSession,
        getApiBaseUrl,
        getAuthHeaders,
        getSession,
        requireRole
    };
})();
