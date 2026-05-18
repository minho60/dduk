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
    // ── 재무제표 ─────────────────────────────────────────────────
    getBalanceSheet() {
        return request('/api/v1/accounting/reports/balance-sheet');
    },

    getProfitLoss() {
        return request('/api/v1/accounting/reports/profit-loss');
    },

    getTrialBalance(fiscalYear, fiscalMonth) {
        const params = new URLSearchParams();
        if (fiscalYear)  params.set('fiscalYear',  fiscalYear);
        if (fiscalMonth) params.set('fiscalMonth', fiscalMonth);
        const qs = params.toString();
        return request(`/api/v1/accounting/reports/trial-balance${qs ? '?' + qs : ''}`);
    },

    // ── 전표 ─────────────────────────────────────────────────────
    getJournals(params = {}) {
        const qs = new URLSearchParams(params).toString();
        return request(`/api/v1/accounting/journals${qs ? '?' + qs : ''}`);
    },

    createJournal(payload) {
        return request('/api/v1/accounting/journals', {
            method: 'POST',
            body: payload
        });
    },

    approveJournal(id) {
        return request(`/api/v1/accounting/journals/${id}/approve`, { method: 'POST' });
    },

    postJournal(id) {
        return request(`/api/v1/accounting/journals/${id}/post`, { method: 'POST' });
    },

    reverseJournal(id) {
        return request(`/api/v1/accounting/journals/${id}/reverse`, { method: 'POST' });
    },

    deleteJournal(id) {
        return request(`/api/v1/accounting/journals/${id}`, { method: 'DELETE' });
    },

    // ── 회계기간 ──────────────────────────────────────────────────
    getPeriods() {
        return request('/api/v1/accounting/periods');
    },

    closePeriod(yearMonth, closedBy) {
        return request(`/api/v1/accounting/periods/${yearMonth}/close`, {
            method: 'POST',
            body: { closedBy: closedBy || 'SYSTEM' }
        });
    },

    reopenPeriod(yearMonth, reopenedBy) {
        return request(`/api/v1/accounting/periods/${yearMonth}/reopen`, {
            method: 'POST',
            body: { reopenedBy: reopenedBy || 'SYSTEM' }
        });
    },

    // ── 급여 ─────────────────────────────────────────────────────
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
