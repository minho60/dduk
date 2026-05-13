/**
 * Payroll Application Service (Orchestration Layer)
 */
import { payrollService } from './payroll-service.js';
import { PayrollStateMachine } from './payroll-state-machine.js';
import { PAYROLL_STATUS, REVISION_TYPE } from './payroll-models.js';
import { PayrollRevisionManager } from './payroll-revision-manager.js';
import { payrollAccountingBridge } from './payroll-accounting-bridge.js';

export class PayrollAppService {
    /**
     * Run Batch Payroll for a Period
     */
    async runBatchPayroll(yearMonth, employees, userId) {
        console.log(`[AppService] Running batch payroll for ${yearMonth}...`);
        const results = [];
        for (const emp of employees) {
            const res = await payrollService.calculatePayroll(emp, yearMonth, { overtimeHours: 0 }); // Default inputs
            results.push(res.data);
        }
        return results;
    }

    /**
     * Finalize and Post Payroll to Accounting
     */
    async finalizeAndPost(payrollRunId, userId) {
        try {
            const record = (await payrollService.getPayrolls()).data.find(r => r.id === payrollRunId);
            if (!record) throw new Error("Record not found");

            // 1. Approval Step (via dedicated service usually, here simple)
            PayrollStateMachine.transition(record, PAYROLL_STATUS.APPROVED, userId);

            // 2. Posting Step
            const postResult = await payrollAccountingBridge.postPayrollRun(record);
            if (postResult.success) {
                PayrollStateMachine.transition(record, PAYROLL_STATUS.POSTED, userId);
            }

            return { success: true, record };
        } catch (err) {
            return { success: false, message: err.message };
        }
    }

    /**
     * Initiate Revision for an Approved Record
     */
    async initiateRevision(payrollRunId, type, userId, reason) {
        const baseRun = (await payrollService.getPayrolls()).data.find(r => r.id === payrollRunId);
        const newRun = PayrollRevisionManager.createRevision(baseRun, type, userId, reason);
        
        // Save new run
        payrollService.records.push(newRun);
        return newRun;
    }
}

export const payrollAppService = new PayrollAppService();
