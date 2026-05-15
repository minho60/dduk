import { hrBackendApi } from './hr-backend-api.js';

const LAST_PAYROLL_KEY = 'dduk_last_backend_payroll';

const page = document.body.dataset.hrPage;

function money(value) {
    const number = Number(value || 0);
    return new Intl.NumberFormat('ko-KR', {
        style: 'currency',
        currency: 'KRW',
        maximumFractionDigits: 0
    }).format(number);
}

function byId(id) {
    return document.getElementById(id);
}

function setText(id, value) {
    const element = byId(id);
    if (element) element.textContent = value;
}

function setStatus(id, message, type = '') {
    const element = byId(id);
    if (!element) return;
    element.textContent = message;
    element.className = `hr_status ${type}`.trim();
}

function showJson(id, data) {
    const element = byId(id);
    if (element) element.textContent = JSON.stringify(data, null, 2);
}

function initShell() {
    const session = window.ddukSession?.requireRole?.(['ADMIN', 'HR', 'FINANCE']) || { loginId: 'admin_preview' };
    if (!session) return null;
    window.ddukSession?.bindShell?.(session);
    window.ddukAppShell?.hydratePage?.({});
    return session;
}

async function safeRun(statusId, task) {
    try {
        setStatus(statusId, '백엔드 API를 호출하는 중입니다.');
        await task();
        setStatus(statusId, '완료되었습니다.', 'ok');
    } catch (error) {
        setStatus(statusId, error.message || '요청 처리 중 오류가 발생했습니다.', 'error');
    }
}

function renderAccountRows(id, rows = []) {
    const tbody = byId(id);
    if (!tbody) return;

    if (!rows.length) {
        tbody.innerHTML = '<tr><td colspan="3">표시할 계정 잔액이 없습니다.</td></tr>';
        return;
    }

    tbody.innerHTML = rows.map(row => `
        <tr>
            <td>${row.code || '-'}</td>
            <td>${row.name || '-'}</td>
            <td class="amount">${money(row.balance)}</td>
        </tr>
    `).join('');
}

function combineReportAccounts(balanceSheet, profitLoss) {
    return [
        ...(balanceSheet.assets || []).map(item => ({ ...item, type: '자산' })),
        ...(balanceSheet.liabilities || []).map(item => ({ ...item, type: '부채' })),
        ...(balanceSheet.equity || []).map(item => ({ ...item, type: '자본' })),
        ...(profitLoss.revenue || []).map(item => ({ ...item, type: '수익' })),
        ...(profitLoss.expenses || []).map(item => ({ ...item, type: '비용' }))
    ];
}

async function loadReports() {
    const [balanceSheet, profitLoss] = await Promise.all([
        hrBackendApi.getBalanceSheet(),
        hrBackendApi.getProfitLoss()
    ]);
    return { balanceSheet, profitLoss };
}

function wireRefresh(buttonId, handler) {
    const button = byId(buttonId);
    if (button) button.addEventListener('click', handler);
}

async function initAccountingDashboard() {
    await safeRun('page_status', async () => {
        const { balanceSheet, profitLoss } = await loadReports();
        setText('kpi_assets', money(balanceSheet.totalAssets));
        setText('kpi_liabilities', money(balanceSheet.totalLiabilities));
        setText('kpi_equity', money(balanceSheet.totalEquity));
        setText('kpi_net_income', money(profitLoss.netIncome));
        setText('balance_state', balanceSheet.isBalanced ? '대차 일치' : '대차 불일치');
        renderAccountRows('asset_rows', balanceSheet.assets);
        renderAccountRows('expense_rows', profitLoss.expenses);
    });
}

async function initReports() {
    async function render(type) {
        await safeRun('page_status', async () => {
            const report = type === 'pl' ? await hrBackendApi.getProfitLoss() : await hrBackendApi.getBalanceSheet();
            setText('report_title', report.title || (type === 'pl' ? '손익계산서' : '재무상태표'));

            if (type === 'pl') {
                setText('report_summary', `수익 ${money(report.totalRevenue)} / 비용 ${money(report.totalExpenses)} / 순손익 ${money(report.netIncome)}`);
                renderAccountRows('report_rows', [
                    ...(report.revenue || []),
                    ...(report.expenses || [])
                ]);
            } else {
                setText('report_summary', `자산 ${money(report.totalAssets)} / 부채 ${money(report.totalLiabilities)} / 자본 ${money(report.totalEquity)}`);
                renderAccountRows('report_rows', [
                    ...(report.assets || []),
                    ...(report.liabilities || []),
                    ...(report.equity || [])
                ]);
            }
        });
    }

    byId('btn_balance_sheet')?.addEventListener('click', () => render('bs'));
    byId('btn_profit_loss')?.addEventListener('click', () => render('pl'));
    await render('bs');
}

function readJournalItems() {
    return Array.from(document.querySelectorAll('.journal_item_row')).map(row => ({
        accountCode: row.querySelector('[data-field="accountCode"]').value.trim(),
        side: row.querySelector('[data-field="side"]').value,
        amount: Number(row.querySelector('[data-field="amount"]').value || 0)
    })).filter(item => item.accountCode && item.amount > 0);
}

function addJournalRow() {
    const container = byId('journal_items');
    const row = document.createElement('div');
    row.className = 'journal_item_row';
    row.innerHTML = `
        <div class="hr_field">
            <label>계정 코드</label>
            <input class="hr_input" data-field="accountCode" placeholder="예: 5100">
        </div>
        <div class="hr_field">
            <label>차대 구분</label>
            <select class="hr_select" data-field="side">
                <option value="DEBIT">차변</option>
                <option value="CREDIT">대변</option>
            </select>
        </div>
        <div class="hr_field">
            <label>금액</label>
            <input class="hr_input" data-field="amount" type="number" min="0" step="1">
        </div>
        <button class="hr_button" type="button" data-remove-row>삭제</button>
    `;
    row.querySelector('[data-remove-row]').addEventListener('click', () => row.remove());
    container.appendChild(row);
}

function updateJournalBalances() {
    const items = readJournalItems();
    let totalDebit = 0;
    let totalCredit = 0;

    items.forEach(item => {
        if (item.side === 'DEBIT') totalDebit += item.amount;
        else totalCredit += item.amount;
    });

    const sumDebitEl = byId('sum_debit');
    const sumCreditEl = byId('sum_credit');
    const badgeEl = byId('balance_badge');
    const msgEl = byId('validation_msg');
    const btnSubmit = byId('btn_submit_journal');

    if (sumDebitEl) sumDebitEl.textContent = money(totalDebit);
    if (sumCreditEl) sumCreditEl.textContent = money(totalCredit);

    const isBalanced = totalDebit > 0 && totalDebit === totalCredit;
    
    if (badgeEl) {
        badgeEl.textContent = isBalanced ? '대차 일치' : '차대 불일치';
        badgeEl.className = `hr_badge ${isBalanced ? 'ok' : 'warn'}`;
    }

    if (msgEl) {
        msgEl.textContent = isBalanced ? '전표를 생성할 수 있습니다.' : '차대 금액이 일치하지 않거나 0입니다.';
        msgEl.className = isBalanced ? 'hr_text_ok' : 'hr_text_error';
    }

    if (btnSubmit) btnSubmit.disabled = !isBalanced;
}

async function renderJournalList() {
    const statusId = 'page_status';
    try {
        const response = await hrBackendApi.getJournals();
        const journals = response.data || [];
        const tbody = byId('journal_list_rows');
        if (!tbody) return;

        if (!journals.length) {
            tbody.innerHTML = '<tr><td colspan="7">표시할 전표가 없습니다.</td></tr>';
            return;
        }

        tbody.innerHTML = journals.map(j => {
            const statusClass = {
                'DRAFT': '',
                'APPROVED': 'info',
                'POSTED': 'ok',
                'REVERSED': 'warn'
            }[j.status] || '';

            const canApprove = j.status === 'DRAFT';
            const canPost = j.status === 'APPROVED';
            const canReverse = j.status === 'POSTED';
            const canDelete = j.status === 'DRAFT';

            return `
                <tr style="cursor: pointer;" onclick="handleJournalAction(${j.id}, 'view')">
                    <td>${j.transactionDate || '-'}</td>
                    <td><code>${j.journalNo || '-'}</code></td>
                    <td>${j.description || '-'}</td>
                    <td class="amount">${money(j.totalDebit)}</td>
                    <td class="amount">${money(j.totalCredit)}</td>
                    <td><span class="hr_badge ${statusClass}">${j.status}</span></td>
                    <td>
                        <div class="hr_actions" style="gap: 4px;" onclick="event.stopPropagation()">
                            ${canApprove ? `<button class="hr_button small" onclick="handleJournalAction(${j.id}, 'approve')">승인</button>` : ''}
                            ${canPost ? `<button class="hr_button small primary" onclick="handleJournalAction(${j.id}, 'post')">기표</button>` : ''}
                            ${canReverse ? `<button class="hr_button small warn" onclick="handleJournalAction(${j.id}, 'reverse')">역분개</button>` : ''}
                            ${canDelete ? `<button class="hr_button small error" onclick="handleJournalAction(${j.id}, 'delete')">삭제</button>` : ''}
                        </div>
                    </td>
                </tr>
            `;
        }).join('');
        if (window.lucide) lucide.createIcons();
    } catch (error) {
        console.error('Failed to load journals:', error);
    }
}

window.handleJournalAction = async function(id, action) {
    if (action === 'view') {
        await openJournalModal(id);
        return;
    }

    const messages = {
        approve: '이 전표를 승인하시겠습니까?',
        post: '이 전표를 기표하시겠습니까? 기표 후에는 수정할 수 없습니다.',
        reverse: '이 전표를 역분개하시겠습니까?',
        delete: '이 전표를 삭제하시겠습니까?'
    };

    if (!confirm(messages[action] || '계속하시겠습니까?')) return;

    await safeRun('page_status', async () => {
        if (action === 'approve') await hrBackendApi.approveJournal(id);
        else if (action === 'post') await hrBackendApi.postJournal(id);
        else if (action === 'reverse') await hrBackendApi.reverseJournal(id);
        else if (action === 'delete') await hrBackendApi.deleteJournal(id);
        
        await renderJournalList();
        // 리포트도 갱신될 수 있으므로 대시보드라면 갱신 고려 (여기선 목록만)
    });
};

async function openJournalModal(id) {
    await safeRun('page_status', async () => {
        const response = await hrBackendApi.getJournals();
        const journal = (response.data || []).find(j => j.id === id);
        if (!journal) throw new Error('전표 정보를 찾을 수 없습니다.');

        setText('detail_journal_no', journal.journalNo);
        setText('detail_date', journal.transactionDate);
        setText('detail_desc', journal.description);
        
        const statusEl = byId('detail_status');
        if (statusEl) {
            statusEl.textContent = journal.status;
            const statusClass = { 'DRAFT': '', 'APPROVED': 'info', 'POSTED': 'ok', 'REVERSED': 'warn' }[journal.status] || '';
            statusEl.className = `hr_badge ${statusClass}`;
        }

        const tbody = byId('detail_line_rows');
        if (tbody) {
            tbody.innerHTML = (journal.lines || []).map(l => `
                <tr>
                    <td>${l.account?.code || '-'}</td>
                    <td>${l.account?.name || '-'}</td>
                    <td class="amount">${money(l.debitAmount)}</td>
                    <td class="amount">${money(l.creditAmount)}</td>
                    <td>${l.description || '-'}</td>
                </tr>
            `).join('');
        }

        setText('detail_sum_debit', money(journal.totalDebit));
        setText('detail_sum_credit', money(journal.totalCredit));

        const modal = byId('journal_detail_modal');
        if (modal) modal.style.display = 'flex';
    });
}

window.closeJournalModal = function() {
    const modal = byId('journal_detail_modal');
    if (modal) modal.style.display = 'none';
};

function initJournalPage() {
    byId('btn_add_journal_row')?.addEventListener('click', () => {
        addJournalRow();
        updateJournalBalances();
    });

    byId('journal_items')?.addEventListener('keyup', (e) => {
        if (e.target.dataset.field === 'amount') {
            updateJournalBalances();
        }
    });

    byId('journal_items')?.addEventListener('change', (e) => {
        if (e.target.dataset.field === 'side' || e.target.dataset.field === 'amount') {
            updateJournalBalances();
        }
    });

    byId('journal_form')?.addEventListener('submit', event => {
        event.preventDefault();
        safeRun('page_status', async () => {
            const payload = {
                date: byId('journal_date').value,
                description: byId('journal_description').value.trim(),
                lines: readJournalItems().map(item => ({
                    accountCode: item.accountCode,
                    debitAmount: item.side === 'DEBIT' ? item.amount : 0,
                    creditAmount: item.side === 'CREDIT' ? item.amount : 0,
                    description: byId('journal_description').value.trim()
                }))
            };

            if (!payload.date || !payload.description || payload.lines.length < 2) {
                throw new Error('일자, 설명, 최소 2개 이상의 전표 항목을 입력하세요.');
            }

            await hrBackendApi.createJournal(payload);
            await renderJournalList();
            byId('journal_form').reset();
            byId('journal_items').innerHTML = '';
            addJournalRow();
            addJournalRow();
            updateJournalBalances();
        });
    });

    byId('btn_refresh_journals')?.addEventListener('click', renderJournalList);

    byId('journal_date').value = new Date().toISOString().slice(0, 10);
    addJournalRow();
    addJournalRow();
    updateJournalBalances();
    renderJournalList();
}

async function initTrialBalance() {
    await safeRun('page_status', async () => {
        const { balanceSheet, profitLoss } = await loadReports();
        const rows = combineReportAccounts(balanceSheet, profitLoss);
        const tbody = byId('trial_rows');
        tbody.innerHTML = rows.map(row => `
            <tr>
                <td>${row.code || '-'}</td>
                <td>${row.name || '-'}</td>
                <td><span class="hr_badge">${row.type}</span></td>
                <td class="amount">${money(row.balance)}</td>
            </tr>
        `).join('') || '<tr><td colspan="4">표시할 시산표 데이터가 없습니다.</td></tr>';
        setText('trial_total', money(rows.reduce((sum, row) => sum + Number(row.balance || 0), 0)));
    });
}

async function renderPeriods() {
    const periods = await hrBackendApi.getPeriods();
    const tbody = byId('period_rows');
    if (!tbody) return;

    if (!periods.length) {
        tbody.innerHTML = '<tr><td colspan="5">마감된 회계기간이 없습니다.</td></tr>';
        return;
    }

    tbody.innerHTML = periods.map(p => {
        const month = `${p.fiscalYear}-${String(p.fiscalMonth).padStart(2, '0')}`;
        const isClosed = p.status === 'CLOSED';
        return `
            <tr>
                <td>${month}</td>
                <td><span class="hr_badge ${isClosed ? 'warn' : 'ok'}">${p.status}</span></td>
                <td>${p.closedAt ? p.closedAt.slice(0, 16).replace('T', ' ') : '-'}</td>
                <td>${p.closedBy || '-'}</td>
                <td>
                    ${isClosed ? `<button class="hr_button" type="button" onclick="window.handleReopenPeriod('${month}')">마감 취소</button>` : '-'}
                </td>
            </tr>
        `;
    }).join('');
    if (window.lucide) lucide.createIcons();
}

window.handleReopenPeriod = async function(yearMonth) {
    if (!confirm(`${yearMonth} 회계기간 마감을 취소하시겠습니까?`)) return;
    await safeRun('page_status', async () => {
        await hrBackendApi.reopenPeriod(yearMonth, window.ddukSession?.getSession?.().loginId);
        await renderPeriods();
    });
};

async function initSettlement() {
    await safeRun('page_status', async () => {
        const { balanceSheet, profitLoss } = await loadReports();
        setText('settlement_income', money(profitLoss.netIncome));
        setText('settlement_assets', money(balanceSheet.totalAssets));
        setText('settlement_balance', balanceSheet.isBalanced ? '대차 일치' : '대차 불일치');
        
        await renderPeriods();
    });

    const monthInput = byId('period_month');
    if (monthInput) monthInput.value = new Date().toISOString().slice(0, 7);

    byId('settlement_form')?.addEventListener('submit', event => {
        event.preventDefault();
        const yearMonth = byId('period_month').value;
        if (!yearMonth) return;

        safeRun('page_status', async () => {
            await hrBackendApi.closePeriod(yearMonth, window.ddukSession?.getSession?.().loginId);
            await renderPeriods();
        });
    });
}

function normalizePayroll(record) {
    const trace = parseTrace(record.calculationTrace);
    return {
        id: record.id,
        employeeName: record.employee?.name || `직원 #${record.employee?.id || '-'}`,
        employeeNo: record.employee?.employeeNo || '-',
        payMonth: record.payMonth,
        baseSalary: Number(record.baseSalary || 0),
        allowanceAmount: Number(record.allowanceAmount || 0),
        deductionAmount: Number(record.deductionAmount || 0),
        netSalary: Number(record.netSalary || 0),
        status: record.status || '-',
        trace
    };
}

function parseTrace(value) {
    if (!value) return {};
    try {
        return JSON.parse(value);
    } catch (error) {
        return { raw: value };
    }
}

function saveLastPayroll(record) {
    localStorage.setItem(LAST_PAYROLL_KEY, JSON.stringify(record));
}

function getLastPayroll() {
    try {
        return JSON.parse(localStorage.getItem(LAST_PAYROLL_KEY) || 'null');
    } catch (error) {
        return null;
    }
}

function renderPayrollResult(record) {
    const payroll = normalizePayroll(record);
    setText('payroll_id', payroll.id || '-');
    setText('payroll_employee', payroll.employeeName);
    setText('payroll_month', payroll.payMonth || '-');
    setText('payroll_base', money(payroll.baseSalary));
    setText('payroll_allowance', money(payroll.allowanceAmount));
    setText('payroll_deduction', money(payroll.deductionAmount));
    setText('payroll_net', money(payroll.netSalary));
    setText('payroll_status', payroll.status);
    showJson('payroll_trace', payroll.trace);
}

function initPayrollCalculate() {
    const month = byId('pay_month');
    if (month) month.value = new Date().toISOString().slice(0, 7);

    const last = getLastPayroll();
    if (last) renderPayrollResult(last);

    byId('payroll_form')?.addEventListener('submit', event => {
        event.preventDefault();
        safeRun('page_status', async () => {
            const payload = {
                employeeId: Number(byId('employee_id').value),
                payMonth: byId('pay_month').value,
                inputs: {
                    overtimeHours: Number(byId('overtime_hours').value || 0),
                    bonus: Number(byId('bonus').value || 0),
                    allowance: Number(byId('allowance').value || 0)
                }
            };

            if (!payload.employeeId || !payload.payMonth) {
                throw new Error('직원 ID와 지급월을 입력하세요.');
            }

            const result = await hrBackendApi.calculatePayroll(payload);
            saveLastPayroll(result);
            renderPayrollResult(result);
        });
    });
}

function initPayrollDetail() {
    const queryId = new URLSearchParams(window.location.search).get('id');
    const last = getLastPayroll();
    if (last) {
        renderPayrollResult(last);
        const id = byId('transition_payroll_id');
        if (id && !id.value) id.value = last.id || '';
    }
    if (queryId && byId('transition_payroll_id')) {
        byId('transition_payroll_id').value = queryId;
    }

    byId('transition_form')?.addEventListener('submit', event => {
        event.preventDefault();
        safeRun('page_status', async () => {
            const id = byId('transition_payroll_id').value;
            const payload = {
                nextStatus: byId('next_status').value,
                userId: byId('transition_user_id').value || window.ddukSession?.getSession?.().loginId || 'admin_preview',
                reason: byId('transition_reason').value
            };

            if (!id || !payload.nextStatus) {
                throw new Error('급여 ID와 다음 상태를 입력하세요.');
            }

            const result = await hrBackendApi.transitionPayroll(id, payload);
            saveLastPayroll(result);
            renderPayrollResult(result);
        });
    });
}

async function initReconciliation() {
    const last = getLastPayroll();
    if (last) {
        const payroll = normalizePayroll(last);
        setText('recon_payroll_gross', money(payroll.baseSalary + payroll.allowanceAmount));
        setText('recon_payroll_deduction', money(payroll.deductionAmount));
        setText('recon_payroll_net', money(payroll.netSalary));
        setText('recon_payroll_status', payroll.status);
    }

    await safeRun('page_status', async () => {
        const profitLoss = await hrBackendApi.getProfitLoss();
        setText('recon_expenses', money(profitLoss.totalExpenses));
        setText('recon_revenue', money(profitLoss.totalRevenue));
        setText('recon_net_income', money(profitLoss.netIncome));
    });
}

function bindCommonRefresh() {
    wireRefresh('btn_refresh_dashboard', initAccountingDashboard);
    wireRefresh('btn_refresh_reports', () => byId('btn_balance_sheet')?.click());
    wireRefresh('btn_refresh_trial', initTrialBalance);
    wireRefresh('btn_refresh_settlement', initSettlement);
    wireRefresh('btn_refresh_recon', initReconciliation);
}

async function init() {
    if (!initShell()) return;
    bindCommonRefresh();

    if (page === 'accounting-dashboard') await initAccountingDashboard();
    if (page === 'accounting-reports') await initReports();
    if (page === 'accounting-transactions') initJournalPage();
    if (page === 'accounting-trial-balance') await initTrialBalance();
    if (page === 'accounting-settlement') await initSettlement();
    if (page === 'payroll-list') initPayrollCalculate();
    if (page === 'payroll-detail') initPayrollDetail();
    if (page === 'payroll-reconciliation') await initReconciliation();
}

init();
