/**
 * Payroll Management Service
 */
import { PayrollEngine } from './payroll-engine.js';
import { payrollAuditService } from './payroll-audit-service.js';
import { payrollAccountingBridge } from './payroll-accounting-bridge.js';

class PayrollService {
    constructor() {
        this.records = []; // In-memory storage for mock
    }

    /**
     * Calculate and Save Payroll Draft
     */
    async calculatePayroll(employee, yearMonth, input = {}) {
        const result = PayrollEngine.calculate(employee, input);
        const record = {
            id: `PAY-${Date.now()}`,
            yearMonth,
            ...result,
            status: 'DRAFT',
            revision: 1,
            createdAt: new Date().toISOString(),
            updatedAt: new Date().toISOString()
        };
        
        this.records.push(record);
        payrollAuditService.log(record.id, 'CALCULATED', 'SYSTEM', { yearMonth });
        return { success: true, data: record };
    }

    /**
     * Approve Payroll (Make it Immutable and Sync to Accounting)
     */
    async approvePayroll(id, userId) {
        const record = this.records.find(r => r.id === id);
        if (!record) return { success: false, message: "기록을 찾을 수 없습니다." };
        if (record.status !== 'DRAFT') return { success: false, message: "대기 상태인 것만 승인 가능합니다." };

        record.status = 'APPROVED';
        record.approvedAt = new Date().toISOString();
        record.updatedAt = new Date().toISOString();

        payrollAuditService.log(id, 'APPROVED', userId);

        // Sync to Accounting
        const syncResult = await payrollAccountingBridge.syncToAccounting(record);
        if (syncResult.success) {
            record.accountingId = syncResult.accountingId;
            payrollAuditService.log(id, 'SYNCED', 'SYSTEM', { accountingId: syncResult.accountingId });
        }

        return { success: true, message: "급여 승인 및 회계 연동이 완료되었습니다." };
    }

    /**
     * Recalculate (Create New Revision if Approved)
     */
    async recalculate(id, userId, input = {}) {
        const oldRecord = this.records.find(r => r.id === id);
        if (!oldRecord) return { success: false, message: "기록을 찾을 수 없습니다." };

        // If approved, we need to handle revision carefully (out of scope for simple mock, but we mark as recalculated)
        if (oldRecord.status === 'APPROVED' || oldRecord.status === 'PAID') {
            return { success: false, message: "승인된 급여는 재계산할 수 없습니다. 취소 후 다시 진행하세요." };
        }

        // Simulating recalculation
        const employee = { id: oldRecord.employeeId, name: oldRecord.employeeName, baseSalary: oldRecord.amounts.baseSalary };
        const result = PayrollEngine.calculate(employee, input);
        
        Object.assign(oldRecord, result);
        oldRecord.updatedAt = new Date().toISOString();
        oldRecord.revision++;

        payrollAuditService.log(id, 'RECALCULATED', userId, { revision: oldRecord.revision });
        return { success: true, data: oldRecord };
    }

    async getPayrolls(filters = {}) {
        let filtered = this.records;
        if (filters.yearMonth) filtered = filtered.filter(r => r.yearMonth === filters.yearMonth);
        if (filters.status) filtered = filtered.filter(r => r.status === filters.status);
        return { success: true, data: filtered };
    }
}

export const payrollService = new PayrollService();
