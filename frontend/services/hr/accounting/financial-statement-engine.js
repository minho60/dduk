/**
 * Financial Statement Generation Engine
 */
import { generalLedgerService } from './general-ledger-service.js';
import { ACCOUNT_TYPE } from './accounting-master-service.js';

export class FinancialStatementEngine {
    /**
     * Generate Balance Sheet (B/S)
     */
    generateBalanceSheet() {
        const tb = generalLedgerService.getTrialBalance();
        
        const assets = tb.filter(a => a.type === ACCOUNT_TYPE.ASSET);
        const liabilities = tb.filter(a => a.type === ACCOUNT_TYPE.LIABILITY);
        const equity = tb.filter(a => a.type === ACCOUNT_TYPE.EQUITY);

        const totalAssets = assets.reduce((acc, a) => acc + a.balance, 0);
        const totalLiabilities = liabilities.reduce((acc, a) => acc + a.balance, 0);
        const totalEquity = equity.reduce((acc, a) => acc + a.balance, 0);

        return {
            title: '대차대조표 (Balance Sheet)',
            sections: [
                { name: '자산 (Assets)', items: assets, total: totalAssets },
                { name: '부채 (Liabilities)', items: liabilities, total: totalLiabilities },
                { name: '자본 (Equity)', items: equity, total: totalEquity }
            ],
            isBalanced: totalAssets === (totalLiabilities + totalEquity)
        };
    }

    /**
     * Generate Profit & Loss (P/L)
     */
    generateProfitAndLoss() {
        const tb = generalLedgerService.getTrialBalance();
        
        const revenue = tb.filter(a => a.type === ACCOUNT_TYPE.REVENUE);
        const expense = tb.filter(a => a.type === ACCOUNT_TYPE.EXPENSE);

        const totalRevenue = revenue.reduce((acc, a) => acc + a.balance, 0);
        const totalExpense = expense.reduce((acc, a) => acc + a.balance, 0);
        const netIncome = totalRevenue - totalExpense;

        return {
            title: '손익계산서 (Profit & Loss)',
            sections: [
                { name: '수익 (Revenue)', items: revenue, total: totalRevenue },
                { name: '비용 (Expense)', items: expense, total: totalExpense }
            ],
            netIncome
        };
    }
}

export const financialStatementEngine = new FinancialStatementEngine();
