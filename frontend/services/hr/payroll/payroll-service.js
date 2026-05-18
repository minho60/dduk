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
     * Calculate and Save Payroll Draft (Backend-only)
     */
    async calculatePayroll(employee, yearMonth, input = {}) {
        // Step 1: Call Backend API (Single Source of Truth)
        const response = await fetch('/api/v1/hr/payroll/calculate', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({
                employeeId: employee.id,
                payMonth: yearMonth,
                inputs: input
            })
        });

        if (!response.ok) {
            const errorData = await response.json().catch(() => ({}));
            throw new Error(errorData.message || `급여 계산 서버 오류 (${response.status})`);
        }

        const apiData = await response.json();
        console.log("[PayrollService] Backend calculation successful:", apiData);
        
        // Update local records
        const index = this.records.findIndex(r => r.id === apiData.id);
        if (index !== -1) {
            this.records[index] = apiData;
        } else {
            this.records.push(apiData);
        }

        return { success: true, data: apiData };
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

    /**
     * Get all payroll records from Backend
     */
    async getPayrolls(filters = {}) {
        try {
            const response = await fetch('/api/v1/hr/payroll');
            if (response.ok) {
                this.records = await response.json();
                let filtered = this.records;
                if (filters.yearMonth) filtered = filtered.filter(r => r.payMonth === filters.yearMonth);
                if (filters.status) filtered = filtered.filter(r => r.status === filters.status);
                return { success: true, data: filtered };
            }
        } catch (e) {
            console.error("[PayrollService] Failed to fetch payrolls:", e);
        }
        return { success: false, data: [] };
    }

    /**
     * Transition Status via Backend API
     */
    async transitionStatus(id, nextStatus, userId, reason) {
        const response = await fetch(`/api/v1/hr/payroll/${id}/transition`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ nextStatus, userId, reason })
        });

        if (!response.ok) {
            const errorData = await response.json().catch(() => ({}));
            throw new Error(errorData.message || `상태 변경 실패 (${response.status})`);
        }

        const updated = await response.json();
        const index = this.records.findIndex(r => r.id === id);
        if (index !== -1) this.records[index] = updated;
        
        return { success: true, data: updated };
    }
}

export const payrollService = new PayrollService();
