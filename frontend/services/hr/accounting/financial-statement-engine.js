/**
 * Financial Statement Generation Engine
 */
import { generalLedgerService } from './general-ledger-service.js';
import { ACCOUNT_TYPE } from './accounting-master-service.js';

export class FinancialStatementEngine {
    /**
     * Generate Balance Sheet (B/S) (Backend-only)
     */
    async generateBalanceSheet() {
        // Step 1: Call Backend API (Single Source of Truth)
        const response = await fetch('/api/v1/accounting/report/balance-sheet');
        if (!response.ok) {
            const errorData = await response.json().catch(() => ({}));
            throw new Error(errorData.message || `대차대조표 생성 서버 오류 (${response.status})`);
        }
        return await response.json();
    }

    /**
     * Generate Profit & Loss (P/L) (Backend-only)
     */
    async generateProfitAndLoss() {
        // Step 1: Call Backend API (Single Source of Truth)
        const response = await fetch('/api/v1/accounting/report/profit-loss');
        if (!response.ok) {
            const errorData = await response.json().catch(() => ({}));
            throw new Error(errorData.message || `손익계산서 생성 서버 오류 (${response.status})`);
        }
        return await response.json();
    }
}

export const financialStatementEngine = new FinancialStatementEngine();
