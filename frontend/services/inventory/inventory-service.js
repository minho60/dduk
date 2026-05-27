const InventoryService = {
    getDashboardStats: async () => {
        const response = await fetch('/api/v1/inventory/dashboard/stats');
        return response.json();
    },
    getStocks: async (params = {}) => {
        const queryString = new URLSearchParams(params).toString();
        const response = await fetch(`/api/v1/inventories?${queryString}`);
        return response.json();
    },
    getMovements: async (params = {}) => {
        const queryString = new URLSearchParams(params).toString();
        const response = await fetch(`/api/v1/inventories/stock-movements?${queryString}`);
        return response.json();
    },
    transfer: async (data) => {
        const response = await fetch('/api/v1/inventories/transfer', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(data)
        });
        return response.json();
    },
    getReorderRecommendations: async () => {
        const response = await fetch('/api/v1/inventories/reorder-recommendations');
        return response.json();
    },
    getWarehouses: async () => {
        const response = await fetch('/api/v1/warehouses');
        return response.json();
    },
    getTransfers: async (params = {}) => {
        const queryString = new URLSearchParams(params).toString();
        const response = await fetch(`/api/v1/warehouse-transfers?${queryString}`);
        return response.json();
    },
    getTransferDetail: async (id) => {
        const response = await fetch(`/api/v1/warehouse-transfers/${id}`);
        return response.json();
    },
    requestTransfer: async (data) => {
        const response = await fetch('/api/v1/warehouse-transfers', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(data)
        });
        return response.json();
    },
    approveTransfer: async (id) => {
        const response = await fetch(`/api/v1/warehouse-transfers/${id}/approve`, {
            method: 'POST'
        });
        return response.json();
    },
    completeTransfer: async (id) => {
        const response = await fetch(`/api/v1/warehouse-transfers/${id}/complete`, {
            method: 'POST'
        });
        return response.json();
    },
    cancelTransfer: async (id) => {
        const response = await fetch(`/api/v1/warehouse-transfers/${id}/cancel`, {
            method: 'POST'
        });
        return response.json();
    }
};
