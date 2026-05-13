/**
 * Journal Entry Management Service
 */
import { generalLedgerService } from './general-ledger-service.js';
import { accountingMasterService } from './accounting-master-service.js';

export class JournalEntryService {
    constructor() {
        this.journals = [];
    }

    /**
     * Create and Post a Journal Entry
     */
    async createAndPost(data) {
        // data: { date, description, items: [{accountCode, amount, side}] }
        
        // 1. Balanced Validation
        const debitSum = data.items.filter(i => i.side === 'DEBIT').reduce((acc, i) => acc + i.amount, 0);
        const creditSum = data.items.filter(i => i.side === 'CREDIT').reduce((acc, i) => acc + i.amount, 0);

        if (debitSum !== creditSum) {
            throw new Error("차대 불일치: 차변과 대변의 합계가 일치해야 합니다.");
        }

        // 2. Fiscal Period Check
        const yearMonth = data.date.substring(0, 7);
        if (!accountingMasterService.isPeriodOpen(yearMonth)) {
            throw new Error(`마감된 회계 기간(${yearMonth})에는 전표를 생성할 수 없습니다.`);
        }

        // 3. Post to GL
        for (const item of data.items) {
            await generalLedgerService.postEntry(item.accountCode, item.amount, item.side);
        }

        const journal = {
            id: `JRN-${Date.now()}`,
            ...data,
            status: 'POSTED',
            createdAt: new Date().toISOString()
        };
        this.journals.push(journal);
        return { success: true, id: journal.id };
    }

    getJournals() { return this.journals; }
}

export const journalEntryService = new JournalEntryService();
