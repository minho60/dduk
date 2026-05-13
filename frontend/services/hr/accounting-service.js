/**
 * Accounting Management Service
 */

import { mockTransactions } from './mock-accounting-data.js';

class AccountingService {
    constructor() {
        this.transactions = [...mockTransactions];
    }

    /**
     * Get Transactions with filters and pagination
     */
    async getTransactions(filters = {}) {
        // Simulate network delay
        await new Promise(resolve => setTimeout(resolve, 300));

        let filtered = this.transactions.filter(t => !t.isDeleted);

        // Date filter
        if (filters.startDate) {
            filtered = filtered.filter(t => t.transactionDate >= filters.startDate);
        }
        if (filters.endDate) {
            filtered = filtered.filter(t => t.transactionDate <= filters.endDate);
        }

        // Type filter
        if (filters.type && filters.type !== 'ALL') {
            filtered = filtered.filter(t => t.type === filters.type);
        }

        // Status filter
        if (filters.status && filters.status !== 'ALL') {
            filtered = filtered.filter(t => t.status === filters.status);
        }

        // Search filter (Vendor Name or Note)
        if (filters.keyword) {
            const kw = filters.keyword.toLowerCase();
            filtered = filtered.filter(t => 
                t.vendorName.toLowerCase().includes(kw) || 
                (t.note && t.note.toLowerCase().includes(kw)) ||
                t.voucherNo.toLowerCase().includes(kw)
            );
        }

        // Sorting
        if (filters.sortBy) {
            const order = filters.sortOrder === 'desc' ? -1 : 1;
            filtered.sort((a, b) => {
                if (a[filters.sortBy] < b[filters.sortBy]) return -1 * order;
                if (a[filters.sortBy] > b[filters.sortBy]) return 1 * order;
                return 0;
            });
        } else {
            // Default sort: Date desc
            filtered.sort((a, b) => b.transactionDate.localeCompare(a.transactionDate));
        }

        // Pagination
        const page = parseInt(filters.page) || 1;
        const size = parseInt(filters.size) || 10;
        const total = filtered.length;
        const start = (page - 1) * size;
        const paginated = filtered.slice(start, start + size);

        return {
            success: true,
            data: paginated,
            pagination: {
                page,
                size,
                total,
                totalPages: Math.ceil(total / size)
            },
            message: ""
        };
    }

    /**
     * Save (Create or Update) Transaction
     */
    async saveTransaction(transactionData) {
        await new Promise(resolve => setTimeout(resolve, 500));

        if (transactionData.id) {
            const index = this.transactions.findIndex(t => t.id === transactionData.id);
            if (index !== -1) {
                this.transactions[index] = { 
                    ...this.transactions[index], 
                    ...transactionData,
                    updatedAt: new Date().toISOString()
                };
            }
        } else {
            const newId = Math.max(...this.transactions.map(t => t.id), 0) + 1;
            const newTransaction = {
                ...transactionData,
                id: newId,
                voucherNo: `VOU-${new Date().getFullYear()}-${String(newId).padStart(5, '0')}`,
                isDeleted: false,
                createdAt: new Date().toISOString(),
                updatedAt: new Date().toISOString()
            };
            this.transactions.unshift(newTransaction);
        }

        return { success: true, message: "저장되었습니다." };
    }

    /**
     * Soft Delete Transaction
     */
    async deleteTransaction(id) {
        await new Promise(resolve => setTimeout(resolve, 300));
        const index = this.transactions.findIndex(t => t.id === id);
        if (index !== -1) {
            this.transactions[index].isDeleted = true;
        }
        return { success: true, message: "삭제되었습니다." };
    }

    /**
     * Bulk Soft Delete
     */
    async deleteTransactions(ids) {
        await new Promise(resolve => setTimeout(resolve, 500));
        this.transactions.forEach(t => {
            if (ids.includes(t.id)) {
                t.isDeleted = true;
            }
        });
        return { success: true, message: `${ids.length}건이 삭제되었습니다.` };
    }
}

export const accountingService = new AccountingService();
