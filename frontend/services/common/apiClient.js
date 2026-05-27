(function () {
    const API_BASE_URL = (() => {
        if (window.location.protocol === 'file:') {
            return 'http://localhost:8080';
        }
        if (window.location.port && window.location.port !== '8080') {
            return 'http://localhost:8080';
        }
        return '';
    })();

    async function request(url, options = {}) {
        const token = localStorage.getItem("token");
        const headers = {
            "Content-Type": "application/json",
            ...options.headers
        };

        if (token) {
            headers["Authorization"] = `Bearer ${token}`;
        }

        const config = {
            ...options,
            headers
        };

        let fullUrl = url;
        if (url.startsWith('/api/') && API_BASE_URL) {
            fullUrl = `${API_BASE_URL}${url}`;
        } else if (!url.startsWith('http') && !url.startsWith('/api/') && API_BASE_URL) {
            fullUrl = `${API_BASE_URL}/api/v1/${url}`;
        }

        try {
            const response = await fetch(fullUrl, config);
            
            if (response.status === 401) {
                console.warn("[API Auth] 401 Unauthorized detected. Redirecting to login...");
                localStorage.clear();
                sessionStorage.clear();
                
                alert("인증 세션이 만료되었거나 권한이 없습니다. 로그인 페이지로 이동합니다.");
                
                const path = window.location.pathname.replace(/\\/g, '/');
                let loginUrl = 'index.html';
                if (path.includes('/pages/')) {
                    const depth = path.split('/pages/')[1].split('/').length;
                    loginUrl = '../'.repeat(depth) + 'index.html';
                }
                window.location.href = loginUrl;
                return null;
            }

            // CSV 내보내기 같은 바이너리 스트림 응답 예외 처리
            const contentType = response.headers.get("content-type");
            if (contentType && (contentType.includes("octet-stream") || contentType.includes("csv"))) {
                return response;
            }

            const payload = await response.json();
            if (!response.ok || payload.status === "error") {
                throw new Error(payload.message || "API 요청 수행 중 오류가 발생했습니다.");
            }
            return payload;
        } catch (error) {
            console.error(`[API Error] Request to ${url} failed:`, error);
            throw error;
        }
    }

    window.ddukApi = {
        get: (url, options) => request(url, { ...options, method: 'GET' }),
        post: (url, body, options) => request(url, { ...options, method: 'POST', body: JSON.stringify(body) }),
        put: (url, body, options) => request(url, { ...options, method: 'PUT', body: JSON.stringify(body) }),
        patch: (url, body, options) => request(url, { ...options, method: 'PATCH', body: JSON.stringify(body) }),
        delete: (url, options) => request(url, { ...options, method: 'DELETE' }),
        getBaseUrl: () => API_BASE_URL
    };
})();
