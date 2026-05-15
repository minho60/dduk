const API_BASE_URL = (() => {
    if (window.location.protocol === 'file:') {
        return window.ddukSession?.getApiBaseUrl?.() || 'http://localhost:8080';
    }

    if (window.location.port && window.location.port !== '8080') {
        return window.ddukSession?.getApiBaseUrl?.() || 'http://localhost:8080';
    }

    return '';
})();

async function request(path, options = {}) {
    const headers = window.ddukSession?.getAuthHeaders?.({ 'Content-Type': 'application/json' }) || { 'Content-Type': 'application/json' };
    const response = await fetch(`${API_BASE_URL}${path}`, {
        method: options.method || 'GET',
        headers,
        body: options.body ? JSON.stringify(options.body) : undefined
    });

    const text = await response.text();
    let data = null;
    try {
        data = text ? JSON.parse(text) : null;
    } catch (error) {
        data = { message: text };
    }

    if (!response.ok) {
        throw new Error(data?.message || data?.error || `API 요청 실패 (${response.status})`);
    }

    return data;
}

export const hrBackendApi = {
    getBalanceSheet() {
        return request('/api/v1/accounting/report/balance-sheet');
    },

    getProfitLoss() {
        return request('/api/v1/accounting/report/profit-loss');
    },

    createJournal(payload) {
        return request('/api/v1/accounting/journal', {
            method: 'POST',
            body: payload
        });
    },

    calculatePayroll(payload) {
        return request('/api/v1/hr/payroll/calculate', {
            method: 'POST',
            body: payload
        });
    },

    transitionPayroll(id, payload) {
        return request(`/api/v1/hr/payroll/${id}/transition`, {
            method: 'POST',
            body: payload
        });
    }
};
