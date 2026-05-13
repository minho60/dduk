/**
 * Payroll Application Service (Orchestration Layer)
 */
import { payrollService } from './payroll-service.js';
import { PayrollStateMachine } from './payroll-state-machine.js';
import { PAYROLL_STATUS, REVISION_TYPE } from './payroll-models.js';
import { PayrollRevisionManager } from './payroll-revision-manager.js';
import { payrollAccountingBridge } from './payroll-accounting-bridge.js';
import { UIUtils } from '../../utils/ui-utils.js';

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
    /**
     * Finalize and Post Payroll to Accounting (Backend-centric)
     */
    async finalizeAndPost(payrollId, userId) {
        try {
            UIUtils.setLoading('btn-finalize', true);
            
            // In a backend-centric model, we might call a specific transition API
            // For now, we use the bridge to post to accounting
            const record = (await payrollService.getPayrolls()).data.find(r => r.id === payrollId);
            if (!record) throw new Error("급여 기록을 찾을 수 없습니다.");

            // 1. Post to Accounting via Bridge
            const postResult = await payrollAccountingBridge.postPayrollRun(record);
            
            if (postResult.success) {
                // 2. Transition Status in Backend
                await payrollService.transitionStatus(payrollId, 'POSTED', userId, '급여 확정 및 회계 반영');
                UIUtils.showToast("급여가 확정되어 회계에 반영되었습니다.", 'success');
                return { success: true };
            } else {
                throw new Error(postResult.message);
            }
        } catch (err) {
            UIUtils.showToast(err.message, 'error');
            return { success: false, message: err.message };
        } finally {
            UIUtils.setLoading('btn-finalize', false);
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
