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
     * Create and Post a Journal Entry (Backend-only)
     */
    async createAndPost(data) {
        // data: { date, description, items: [{accountCode, amount, side}] }
        
        // Step 1: Call Backend API (Single Source of Truth)
        const response = await fetch('/api/v1/accounting/journal', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(data)
        });

        if (!response.ok) {
            const errorData = await response.json().catch(() => ({}));
            throw new Error(errorData.message || `전표 생성 서버 오류 (${response.status})`);
        }

        const apiData = await response.json();
        console.log("[JournalEntryService] Backend posting successful:", apiData);
        this.journals.push(apiData);
        return { success: true, id: apiData.id };
    }

    getJournals() { return this.journals; }
}

export const journalEntryService = new JournalEntryService();
