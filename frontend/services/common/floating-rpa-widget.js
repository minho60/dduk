(function () {
    const RPA_STATE_STORAGE_KEY = 'dduk_rpa_states';
    const terminalStatuses = new Set(['SUCCESS', 'FAILED']);
    const activeStatuses = new Set(['REQUESTED', 'RUNNING']);
    const rpaState = {
        taskId: null,
        actionName: null,
        pollingHandle: null,
        status: 'IDLE',
        message: null,
        tone: 'neutral'
    };

    const rpaTranslation = {
        taskTypes: {
            PURCHASE_PRICE: '구매 단가 수집',
            INVENTORY_SHORTAGE: '재고 위험 분석',
            HR_MIN_WAGE: '급여 기준 조회'
        },
        actions: {
            COLLECT_EXTERNAL_PRICE: '아망티 가격 수집',
            CHECK_INVENTORY_SHORTAGE: '재고 위험 분석',
            COLLECT_HR_REFERENCE: '급여 기준 수집'
        }
    };

    function getRootPath() {
        return window.DDUK_COMMON ? window.DDUK_COMMON.getRootPath() : './';
    }

    function resolveHref(href) {
        return window.DDUK_COMMON ? window.DDUK_COMMON.resolveHref(href) : href;
    }

    function escapeHtml(value) {
        return window.DDUK_COMMON ? window.DDUK_COMMON.escapeHtml(value) : String(value ?? '');
    }

    function formatDateTime(value) {
        return window.DDUK_COMMON ? window.DDUK_COMMON.formatDateTime(value) : value;
    }

    function translateRpaType(type) {
        return rpaTranslation.taskTypes[type] || type || '-';
    }

    function translateRpaAction(action) {
        return rpaTranslation.actions[action] || action || '-';
    }

    function getCurrentRole() {
        if (window.ddukSession && typeof window.ddukSession.getSession === 'function') {
            return window.ddukSession.getSession()?.role || null;
        }
        return null;
    }

    function getApiBaseUrl() {
        return window.DDUK_COMMON ? window.DDUK_COMMON.getApiBaseUrl() : '';
    }

    function getHeaders(extraHeaders) {
        return window.DDUK_COMMON ? window.DDUK_COMMON.getHeaders(extraHeaders) : {
            'Content-Type': 'application/json',
            ...(extraHeaders || {})
        };
    }

    function loadRpaStates() {
        try {
            const raw = window.sessionStorage.getItem(RPA_STATE_STORAGE_KEY);
            if (!raw) {
                return {};
            }
            const parsed = JSON.parse(raw);
            return parsed && typeof parsed === 'object' ? parsed : {};
        } catch (error) {
            window.sessionStorage.removeItem(RPA_STATE_STORAGE_KEY);
            return {};
        }
    }

    function persistRpaStates(states) {
        try {
            window.sessionStorage.setItem(RPA_STATE_STORAGE_KEY, JSON.stringify(states));
        } catch (error) {
            console.warn('RPA 상태 저장에 실패했어.', error);
        }
    }

    function saveRpaState(taskType, patch) {
        if (!taskType) {
            return;
        }
        const states = loadRpaStates();
        const previous = states[taskType] && typeof states[taskType] === 'object' ? states[taskType] : {};
        states[taskType] = {
            ...previous,
            ...patch,
            taskType,
            lastUpdated: new Date().toISOString()
        };
        persistRpaStates(states);
    }

    function clearRpaState(taskType) {
        if (!taskType) {
            return;
        }
        const states = loadRpaStates();
        delete states[taskType];
        if (Object.keys(states).length) {
            persistRpaStates(states);
        } else {
            window.sessionStorage.removeItem(RPA_STATE_STORAGE_KEY);
        }
    }

    function getStoredRpaState(taskType) {
        const states = loadRpaStates();
        return states[taskType] && typeof states[taskType] === 'object' ? states[taskType] : null;
    }

    function canRenderRpaResult(taskType) {
        const role = getCurrentRole();
        if (!role) {
            return false;
        }
        if (taskType === 'PURCHASE_PRICE' || taskType === 'INVENTORY_SHORTAGE') {
            return role === 'ADMIN' || role === 'INVENTORY';
        }
        return role === 'ADMIN';
    }

    function toPersistedDetail(taskType, detail) {
        return {
            taskId: detail?.taskId || rpaState.taskId || null,
            actionName: detail?.actionName || rpaState.actionName || null,
            status: detail?.status || rpaState.status || 'IDLE',
            requestedAt: detail?.requestedAt || null,
            completedAt: detail?.completedAt || null,
            message: rpaState.message || null,
            tone: rpaState.tone || 'neutral',
            errorMessage: detail?.errorMessage || null
        };
    }

    async function requestRpaApi(path, options) {
        const response = await fetch(`${getApiBaseUrl()}${path}`, {
            ...options,
            headers: getHeaders(options && options.headers)
        });

        const text = await response.text();
        let payload = null;

        try {
            payload = text ? JSON.parse(text) : null;
        } catch (error) {
            const parseError = new Error('응답 형식을 해석하지 못했어.');
            parseError.statusCode = response.status;
            throw parseError;
        }

        if (!response.ok || !payload || payload.status !== 'success') {
            const requestError = new Error(payload?.message || 'RPA 요청 처리 중 오류가 발생했어.');
            requestError.statusCode = response.status;
            throw requestError;
        }

        return payload.data;
    }

    function getRpaContext() {
        const path = window.location.pathname.replace(/\\/g, '/');
        if (path.includes('/purchase-dashboard.html')) {
            return {
                taskType: 'PURCHASE_PRICE',
                title: '아망티 가격 수집',
                description: '실시간 비교 대신 백그라운드 수집을 요청하고 최근 저장 결과를 다시 보여줘.',
                actionName: 'COLLECT_EXTERNAL_PRICE'
            };
        }
        if (path.includes('/dashboard.html')) {
            return {
                taskType: 'INVENTORY_SHORTAGE',
                title: '재고 위험 분석',
                description: '재고 부족과 장기 체화 위험을 비동기 분석으로 확인해.',
                actionName: 'CHECK_INVENTORY_SHORTAGE'
            };
        }
        return {
            taskType: 'PURCHASE_PRICE',
            title: 'RPA 상태 점검',
            description: '공통 RPA 위젯에서 최근 작업 상태를 확인해.',
            actionName: 'COLLECT_EXTERNAL_PRICE'
        };
    }

    function restoreRpaStateForCurrentContext() {
        const ctx = getRpaContext();
        const saved = getStoredRpaState(ctx.taskType);
        if (!saved) {
            return null;
        }
        rpaState.taskId = saved.taskId || null;
        rpaState.actionName = saved.actionName || ctx.actionName;
        rpaState.status = saved.status || 'IDLE';
        rpaState.message = saved.message || null;
        rpaState.tone = saved.tone || 'neutral';
        return saved;
    }

    function renderDynamicRpaWidget() {
        const container = document.getElementById('dduk-floating-rpa-container');
        if (!container) {
            return;
        }

        const path = window.location.pathname.replace(/\\/g, '/');
        if (path.includes('/accounting') || path.includes('/monthly_closing') || path.includes('/voucher_management') || path.includes('/trial_balance') || path.includes('/settlement') || path.includes('/accounts') || path.includes('/wip')) {
            container.innerHTML = `
                <div class="flex flex-col items-center justify-center py-16 text-center">
                    <div style="background-color: #f8fafc; border: 1px dashed #cbd5e1; border-radius: 1rem; padding: 2rem; width: 100%;">
                        <i data-lucide="cpu" style="width: 2.5rem; height: 2.5rem; margin: 0 auto 0.75rem auto; color: #94a3b8;"></i>
                        <h4 style="margin: 0 0 0.5rem 0; font-size: 0.875rem; font-weight: 700; color: #1e293b;">RPA 자동 제어</h4>
                        <span style="display: inline-block; background-color: #e2e8f0; color: #475569; border-radius: 9999px; padding: 0.25rem 0.75rem; font-size: 0.75rem; font-weight: 700;">추후 업데이트 예정</span>
                    </div>
                </div>
            `;
            if (window.lucide) {
                window.lucide.createIcons();
            }
            return;
        }

        const ctx = getRpaContext();
        const triggerLabel = ctx.taskType === 'PURCHASE_PRICE' ? '수집 요청' : '분석 실행';
        container.innerHTML = `
            <div class="space-y-4">
                <div class="rounded-2xl border border-slate-100 bg-white p-5 shadow-sm">
                    <div class="flex items-center justify-between gap-3">
                        <div class="min-w-0" style="flex: 1;">
                            <div class="flex items-center gap-2">
                                <h3 class="text-sm font-bold text-gray-900" style="margin:0;">${escapeHtml(ctx.title)}</h3>
                                <span class="rounded bg-indigo-50 px-1.5 py-0.5 text-[9px] font-bold text-indigo-600">${translateRpaType(ctx.taskType)}</span>
                            </div>
                            <p class="mt-1 text-[11px] text-gray-400" style="margin: 0.25rem 0 0 0; line-height: 1.3;">${escapeHtml(ctx.description)}</p>
                        </div>
                        <span id="rpaWidgetStatusBadge" class="rounded-full bg-slate-100 px-2.5 py-0.5 text-[10px] font-bold text-slate-600">대기</span>
                    </div>

                    <div class="mt-4 grid grid-cols-2 gap-3 rounded-xl border border-slate-100 bg-slate-50/50 px-4 py-3 text-xs text-slate-500">
                        <div>
                            <div class="text-[10px] font-bold uppercase tracking-wide text-slate-400">작업 유형</div>
                            <div id="rpaWidgetActionName" class="mt-1 font-semibold text-slate-700">${translateRpaAction(ctx.actionName)}</div>
                        </div>
                        <div>
                            <div class="text-[10px] font-bold uppercase tracking-wide text-slate-400">최근 Task ID</div>
                            <div id="rpaWidgetTaskId" class="mt-1 truncate font-mono text-[11px] text-slate-700">-</div>
                        </div>
                    </div>

                    <div id="rpaWidgetMessage" class="mt-4 rounded-xl border border-slate-100 bg-slate-50/50 px-4 py-3 text-xs text-slate-500 leading-normal">
                        버튼을 누르면 현재 페이지 기준 분석을 요청해. 구매 단가 수집은 완료될 때까지 마지막 저장 결과를 계속 보여줘.
                    </div>

                    <div class="mt-5 flex items-center justify-between gap-3">
                        <span id="rpaWidgetLastUpdated" class="text-[10px] text-slate-400 font-medium">최근 상태 확인: -</span>
                        <div class="flex gap-2">
                            <button id="rpaWidgetRefreshButton" type="button" class="rounded-xl border border-slate-200 hover:bg-slate-50 px-3 py-2 text-xs font-bold text-slate-600 transition-all">새로고침</button>
                            <button id="rpaWidgetTriggerButton" type="button" class="rounded-xl bg-indigo-600 hover:bg-indigo-700 px-4 py-2 text-xs font-bold text-white shadow-sm shadow-indigo-100 transition-all">${triggerLabel}</button>
                        </div>
                    </div>
                </div>

                <div id="rpaWidgetResultContainer"></div>
            </div>
        `;

        const triggerButton = document.getElementById('rpaWidgetTriggerButton');
        triggerButton?.addEventListener('click', triggerRpa);

        const refreshButton = document.getElementById('rpaWidgetRefreshButton');
        refreshButton?.addEventListener('click', refreshRpaState);

        const saved = restoreRpaStateForCurrentContext();
        if (saved && rpaState.status !== 'IDLE') {
            updateSummary(saved);
            if (saved.message) {
                setMessage(saved.message, saved.tone || 'neutral');
            }
            if (saved.taskId && activeStatuses.has(saved.status)) {
                pollTask(saved.taskId, 0);
            } else if (saved.status === 'SUCCESS' && canRenderRpaResult(ctx.taskType)) {
                fetchAndRenderRpaResult(ctx.taskType, true);
            }
            return;
        }

        checkAndRenderPreExistingResult(ctx.taskType);
    }

    function renderStatus(status) {
        const statusBadge = document.getElementById('rpaWidgetStatusBadge');
        const triggerButton = document.getElementById('rpaWidgetTriggerButton');
        if (!statusBadge || !triggerButton) {
            return;
        }

        const palette = {
            IDLE: 'bg-slate-100 text-slate-600',
            REQUESTED: 'bg-amber-100 text-amber-700',
            RUNNING: 'bg-sky-100 text-sky-700',
            SUCCESS: 'bg-emerald-100 text-emerald-700',
            FAILED: 'bg-rose-100 text-rose-700'
        };
        const labels = {
            IDLE: '대기',
            REQUESTED: '요청됨',
            RUNNING: '실행 중',
            SUCCESS: '성공',
            FAILED: '실패'
        };

        const ctx = getRpaContext();
        const triggerLabel = ctx.taskType === 'PURCHASE_PRICE' ? '수집 요청' : '분석 실행';
        const pendingLabel = ctx.taskType === 'PURCHASE_PRICE' ? '요청 중...' : '실행 중...';

        statusBadge.className = `rounded-full px-2.5 py-0.5 text-[10px] font-bold ${palette[status] || palette.IDLE}`;
        statusBadge.textContent = labels[status] || status;

        const isProcessing = activeStatuses.has(status);
        triggerButton.disabled = isProcessing;
        triggerButton.classList.toggle('opacity-60', isProcessing);
        triggerButton.classList.toggle('cursor-not-allowed', isProcessing);
        triggerButton.textContent = isProcessing ? pendingLabel : triggerLabel;
    }

    function setMessage(text, tone) {
        const messageElement = document.getElementById('rpaWidgetMessage');
        if (!messageElement) {
            return;
        }

        const tones = {
            neutral: 'border-slate-100 bg-slate-50/50 text-slate-500',
            loading: 'border-sky-100 bg-sky-50/50 text-sky-700',
            success: 'border-emerald-100 bg-emerald-50/50 text-emerald-700',
            error: 'border-rose-100 bg-rose-50/50 text-rose-700'
        };
        messageElement.className = `mt-4 rounded-xl border px-4 py-3 text-xs leading-normal ${tones[tone] || tones.neutral}`;
        messageElement.textContent = text;
        rpaState.message = text || null;
        rpaState.tone = tone || 'neutral';
    }

    function updateSummary(detail) {
        const ctx = getRpaContext();
        const status = detail?.status || rpaState.status;
        rpaState.status = status;
        rpaState.taskId = detail?.taskId || rpaState.taskId || null;
        rpaState.actionName = detail?.actionName || rpaState.actionName || ctx.actionName;

        const actionNameEl = document.getElementById('rpaWidgetActionName');
        const taskIdEl = document.getElementById('rpaWidgetTaskId');
        const lastUpdatedEl = document.getElementById('rpaWidgetLastUpdated');

        if (actionNameEl) {
            actionNameEl.textContent = translateRpaAction(rpaState.actionName);
        }
        if (taskIdEl) {
            taskIdEl.textContent = rpaState.taskId || '-';
            taskIdEl.title = rpaState.taskId || '-';
        }
        if (lastUpdatedEl) {
            const lastUpdatedSource = detail?.completedAt || detail?.requestedAt || new Date().toISOString();
            lastUpdatedEl.textContent = `최근 상태 확인: ${formatDateTime(lastUpdatedSource)}`;
        }

        renderStatus(status);

        if (!detail) {
            return;
        }

        if (status === 'SUCCESS') {
            setMessage(
                ctx.taskType === 'PURCHASE_PRICE'
                    ? '구매 단가 수집이 끝났어. 최근 저장 결과를 다시 확인해볼게.'
                    : 'RPA 작업이 완료됐어. 최신 결과를 다시 확인해볼게.',
                'success'
            );
            saveRpaState(ctx.taskType, toPersistedDetail(ctx.taskType, detail));
            if (canRenderRpaResult(ctx.taskType)) {
                fetchAndRenderRpaResult(ctx.taskType);
            }
            return;
        }

        if (status === 'FAILED') {
            setMessage(detail.errorMessage || 'RPA 실행 중 오류가 발생해서 작업이 실패했어.', 'error');
            saveRpaState(ctx.taskType, toPersistedDetail(ctx.taskType, detail));
            if (ctx.taskType === 'PURCHASE_PRICE' && canRenderRpaResult(ctx.taskType)) {
                fetchAndRenderRpaResult(ctx.taskType, true);
            }
            return;
        }

        if (status === 'RUNNING') {
            setMessage(
                ctx.taskType === 'PURCHASE_PRICE'
                    ? '아망티 가격을 다시 수집하는 중이야. 완료 전까지는 마지막 저장 결과를 유지할게.'
                    : 'RPA 작업이 아직 진행 중이야. 아래 새로고침 버튼으로 다시 확인해줘.',
                'loading'
            );
            saveRpaState(ctx.taskType, toPersistedDetail(ctx.taskType, detail));
            return;
        }

        if (status === 'REQUESTED') {
            setMessage(
                ctx.taskType === 'PURCHASE_PRICE'
                    ? '구매 단가 수집 요청이 접수됐어. 완료되면 최근 저장 결과가 갱신돼.'
                    : 'RPA 요청이 접수됐어. 실행이 오래 걸리면 아래 새로고침 버튼을 눌러봐.',
                'loading'
            );
            saveRpaState(ctx.taskType, toPersistedDetail(ctx.taskType, detail));
            return;
        }

        saveRpaState(ctx.taskType, toPersistedDetail(ctx.taskType, detail));
    }

    function stopPolling() {
        if (rpaState.pollingHandle) {
            window.clearTimeout(rpaState.pollingHandle);
            rpaState.pollingHandle = null;
        }
    }

    async function loadTaskDetail(taskId) {
        const detail = await requestRpaApi(`/api/v1/admin/tasks/${encodeURIComponent(taskId)}`, {
            method: 'GET'
        });
        updateSummary(detail);
        return detail;
    }

    async function pollTask(taskId, attempt) {
        const ctx = getRpaContext();
        try {
            const detail = await loadTaskDetail(taskId);
            if (detail && !terminalStatuses.has(detail.status) && attempt < 30) {
                rpaState.pollingHandle = window.setTimeout(() => {
                    pollTask(taskId, attempt + 1);
                }, 2000);
                return;
            }

            stopPolling();
            if (detail && !terminalStatuses.has(detail.status)) {
                setMessage('아직 최종 결과가 반영되는 중이야. 실행이 오래 걸리면 새로고침 버튼으로 다시 확인해줘.', 'loading');
                saveRpaState(ctx.taskType, toPersistedDetail(ctx.taskType, detail));
            }
        } catch (error) {
            stopPolling();
            if (error?.statusCode === 404 || error?.statusCode === 403) {
                clearRpaState(ctx.taskType);
                rpaState.taskId = null;
                rpaState.actionName = ctx.actionName;
                rpaState.status = 'IDLE';
                renderStatus('IDLE');
                setMessage(
                    error.statusCode === 403
                        ? '현재 권한으로는 이전 작업 상태를 다시 조회할 수 없어.'
                        : '이전 작업 정보를 찾지 못해 저장된 상태를 정리했어.',
                    error.statusCode === 403 ? 'error' : 'neutral'
                );
                return;
            }

            renderStatus('FAILED');
            setMessage(error.message || '작업 상태를 확인하지 못했어.', 'error');
            saveRpaState(ctx.taskType, toPersistedDetail(ctx.taskType, {
                taskId,
                actionName: rpaState.actionName,
                status: rpaState.status || 'FAILED'
            }));
        }
    }

    async function triggerRpa() {
        const ctx = getRpaContext();
        stopPolling();
        renderStatus('REQUESTED');
        setMessage(
            ctx.taskType === 'PURCHASE_PRICE'
                ? '아망티 가격 수집 요청을 보내는 중이야...'
                : 'RPA 실행 요청을 보내는 중이야...',
            'loading'
        );

        try {
            const data = await requestRpaApi('/api/v1/admin/rpa/trigger', {
                method: 'POST',
                body: JSON.stringify({ taskType: ctx.taskType })
            });

            rpaState.taskId = data.taskId || null;
            rpaState.actionName = data.actionName || ctx.actionName;
            rpaState.status = 'REQUESTED';

            updateSummary({
                taskId: rpaState.taskId,
                actionName: rpaState.actionName,
                status: 'REQUESTED'
            });

            if (rpaState.taskId) {
                pollTask(rpaState.taskId, 0);
            }
        } catch (error) {
            renderStatus('FAILED');
            setMessage(error.message || 'RPA 실행 요청에 실패했어.', 'error');
            saveRpaState(ctx.taskType, toPersistedDetail(ctx.taskType, {
                taskId: rpaState.taskId,
                actionName: rpaState.actionName || ctx.actionName,
                status: 'FAILED'
            }));
            if (ctx.taskType === 'PURCHASE_PRICE' && canRenderRpaResult(ctx.taskType)) {
                fetchAndRenderRpaResult(ctx.taskType, true);
            }
        }
    }

    async function refreshRpaState() {
        const ctx = getRpaContext();
        setMessage('상태를 새로고침하는 중이야...', 'loading');

        try {
            const saved = getStoredRpaState(ctx.taskType);
            if (saved && saved.taskId) {
                const detail = await loadTaskDetail(saved.taskId);
                if (detail) {
                    if (terminalStatuses.has(detail.status) && canRenderRpaResult(ctx.taskType)) {
                        await fetchAndRenderRpaResult(ctx.taskType);
                    }
                    return;
                }
            }

            await fetchAndRenderRpaResult(ctx.taskType);
            setMessage('최신 상태로 갱신했어.', 'success');
        } catch (error) {
            setMessage(error.message || '상태를 새로고침하지 못했어.', 'error');
            console.error('RPA 상태 새로고침 실패:', error);
        }
    }

    async function checkAndRenderPreExistingResult(taskType) {
        fetchAndRenderRpaResult(taskType, true);
    }

    function renderPurchasePriceSummary(block) {
        const resultContainer = document.getElementById('rpaWidgetResultContainer');
        if (!resultContainer) {
            return;
        }

        if (!block) {
            resultContainer.innerHTML = '';
            return;
        }

        const items = Array.isArray(block.items) ? block.items : [];
        const statusLabelMap = {
            EMPTY: '스냅샷 없음',
            MISSING_FILE: '결과 파일 누락',
            EMPTY_RESULT: '수집 결과 비어 있음',
            NO_ERP_BASELINE: 'ERP 기준 단가 없음',
            READY: '비교 가능'
        };
        const taskStatusLabelMap = {
            IDLE: '대기',
            REQUESTED: '요청됨',
            RUNNING: '수집 중',
            SUCCESS: '성공',
            FAILED: '실패'
        };
        const delta = block.priceDelta == null ? null : Number(block.priceDelta || 0);
        const deltaColor = delta == null ? 'text-slate-500' : delta > 0 ? 'text-rose-600' : delta < 0 ? 'text-emerald-600' : 'text-slate-600';
        const deltaText = delta == null ? '계산 대기' : `${delta > 0 ? '+' : ''}${delta.toLocaleString()}원`;
        const latestTaskLabel = taskStatusLabelMap[block.latestTaskStatus || block.status] || (block.latestTaskStatus || block.status || '대기');

        const bodyHtml = items.length
            ? items.slice(0, 5).map((item) => {
                const itemDelta = item.priceDelta == null ? null : Number(item.priceDelta || 0);
                const itemDeltaColor = itemDelta == null ? 'text-slate-400' : itemDelta > 0 ? 'text-rose-600 font-semibold' : itemDelta < 0 ? 'text-emerald-600 font-semibold' : 'text-slate-500';
                const itemDeltaText = itemDelta == null ? '-' : `${itemDelta > 0 ? '+' : ''}${itemDelta.toLocaleString()}원`;
                return `
                    <tr class="border-b border-slate-100">
                        <td class="py-2 text-[11px] font-medium text-gray-800">${escapeHtml(item.productName || item.itemName || '-')}</td>
                        <td class="py-2 text-right text-[11px] text-gray-500">${item.collectedPrice == null ? '-' : `${Number(item.collectedPrice).toLocaleString()}원`}</td>
                        <td class="py-2 text-right text-[11px] text-gray-500">${item.erpPrice == null ? '-' : `${Number(item.erpPrice).toLocaleString()}원`}</td>
                        <td class="py-2 text-right text-[11px] ${itemDeltaColor}">${itemDeltaText}</td>
                    </tr>
                `;
            }).join('')
            : `
                <tr>
                    <td colspan="4" class="py-3">
                        <div class="rounded-xl border border-dashed border-slate-200 bg-slate-50 px-3 py-3 text-[11px] leading-relaxed text-slate-500">
                            ${escapeHtml(block.message || '아직 비교 가능한 스냅샷이 없어.')}
                        </div>
                    </td>
                </tr>
            `;

        const latestTaskMeta = block.latestTaskId
            ? `<div class="mt-2 text-[10px] text-slate-400">최근 요청 ${escapeHtml(block.latestTaskId)} · ${escapeHtml(latestTaskLabel)}${block.latestTaskRequestedAt ? ` · ${escapeHtml(formatDateTime(block.latestTaskRequestedAt))}` : ''}</div>`
            : '';

        resultContainer.innerHTML = `
            <div class="rounded-2xl border border-slate-100 bg-slate-50/40 p-4 space-y-3" style="margin-top: 1rem;">
                <div class="flex items-center justify-between gap-3">
                    <div>
                        <h4 class="text-xs font-bold text-slate-800" style="margin: 0;">구매 단가 비교 스냅샷</h4>
                        <p class="mt-1 text-[11px] leading-relaxed text-slate-500">${escapeHtml(block.message || '최근 수집 결과를 보여줘.')}</p>
                        ${latestTaskMeta}
                    </div>
                    <div class="text-right">
                        <div class="rounded-full bg-white px-3 py-1 text-[10px] font-bold text-slate-600">${escapeHtml(statusLabelMap[block.resultStatus] || '결과 확인')}</div>
                        <div class="mt-2 text-[10px] text-slate-400">${block.latestCollectedAt ? `최근 성공 ${escapeHtml(formatDateTime(block.latestCollectedAt))}` : '성공 스냅샷 없음'}</div>
                    </div>
                </div>

                <div class="grid grid-cols-2 gap-2">
                    <div class="rounded-xl border border-slate-100 bg-white p-3 text-center">
                        <p class="text-[9px] font-bold uppercase text-slate-400" style="margin: 0;">수집 평균 단가</p>
                        <p class="mt-1 text-xs font-extrabold text-slate-800">${block.latestCollectedAveragePrice == null ? '-' : `${Number(block.latestCollectedAveragePrice).toLocaleString()}원`}</p>
                    </div>
                    <div class="rounded-xl border border-slate-100 bg-white p-3 text-center">
                        <p class="text-[9px] font-bold uppercase text-slate-400" style="margin: 0;">평균 단가 차이</p>
                        <p class="mt-1 text-xs font-extrabold ${deltaColor}">${deltaText}</p>
                    </div>
                    <div class="rounded-xl border border-slate-100 bg-white p-3 text-center">
                        <p class="text-[9px] font-bold uppercase text-slate-400" style="margin: 0;">수집 품목 수</p>
                        <p class="mt-1 text-xs font-extrabold text-slate-800">${Number(block.collectedItemsCount || 0).toLocaleString()}건</p>
                    </div>
                    <div class="rounded-xl border border-slate-100 bg-white p-3 text-center">
                        <p class="text-[9px] font-bold uppercase text-slate-400" style="margin: 0;">비교 매칭 수</p>
                        <p class="mt-1 text-xs font-extrabold text-slate-800">${Number(block.comparedItemsCount || 0).toLocaleString()}건</p>
                    </div>
                </div>

                <div class="rounded-xl border border-slate-100 bg-white p-3">
                    <div class="mb-2 text-[10px] font-semibold text-slate-400">
                        ${escapeHtml(block.vendorName || '-')} · 현재 진행 중인 동일 작업 ${Number(block.activeTaskCount || 0).toLocaleString()}건
                    </div>
                    <table class="w-full" style="width: 100%; border-collapse: collapse;">
                        <thead>
                            <tr class="border-b text-[10px] font-bold text-slate-400">
                                <th class="pb-1.5 text-left">품목명</th>
                                <th class="pb-1.5 text-right">수집가</th>
                                <th class="pb-1.5 text-right">ERP가</th>
                                <th class="pb-1.5 text-right">차이</th>
                            </tr>
                        </thead>
                        <tbody>${bodyHtml}</tbody>
                    </table>
                </div>
            </div>
        `;
    }

    function renderPurchasePriceSummaryV2(block) {
        const resultContainer = document.getElementById('rpaWidgetResultContainer');
        if (!resultContainer) {
            return;
        }

        if (!block) {
            resultContainer.innerHTML = '';
            return;
        }

        const snapshotItems = Array.isArray(block.collectedSnapshotItems)
            ? block.collectedSnapshotItems
            : Array.isArray(block.items) ? block.items : [];
        const matchedItems = Array.isArray(block.matchedItems) ? block.matchedItems : [];
        const hasErpBaseline = Number(block.erpItemsCount || 0) > 0;
        const statusLabelMap = {
            EMPTY: '결과 없음',
            MISSING_FILE: '결과 파일 누락',
            EMPTY_RESULT: '수집 결과 비어 있음',
            NO_ERP_BASELINE: 'ERP 기준 단가 없음',
            READY: '비교 가능'
        };
        const taskStatusLabelMap = {
            IDLE: '대기',
            REQUESTED: '요청됨',
            RUNNING: '수집 중',
            SUCCESS: '성공',
            FAILED: '실패'
        };
        const matchTypeLabelMap = {
            EXACT: '정확 매칭',
            LOOSE: '유사 매칭',
            SNAPSHOT: '동일 단가',
            COLLECTED_ONLY: '미매칭'
        };
        const delta = block.priceDelta == null ? null : Number(block.priceDelta || 0);
        const deltaColor = delta == null ? 'text-slate-500' : delta > 0 ? 'text-rose-600' : delta < 0 ? 'text-emerald-600' : 'text-slate-600';
        const deltaText = delta == null ? '계산 대기' : `${delta > 0 ? '+' : ''}${delta.toLocaleString()}원`;
        const latestTaskLabel = taskStatusLabelMap[block.latestTaskStatus || block.status] || (block.latestTaskStatus || block.status || '대기');

        const renderRows = (items, showMatchMeta = false, emptyMessage = null) => {
            if (!items.length) {
                return `
                    <tr>
                        <td colspan="4" class="py-3">
                            <div class="rounded-xl border border-dashed border-slate-200 bg-slate-50 px-3 py-3 text-[11px] leading-relaxed text-slate-500">
                                ${escapeHtml(emptyMessage || block.message || '표시할 수집 결과가 아직 없어.')}
                            </div>
                        </td>
                    </tr>
                `;
            }

            return items.slice(0, 12).map((item) => {
                const itemDelta = item.priceDelta == null ? null : Number(item.priceDelta || 0);
                const itemDeltaColor = itemDelta == null ? 'text-slate-400' : itemDelta > 0 ? 'text-rose-600 font-semibold' : itemDelta < 0 ? 'text-emerald-600 font-semibold' : 'text-slate-500';
                const itemDeltaText = itemDelta == null ? '-' : `${itemDelta > 0 ? '+' : ''}${itemDelta.toLocaleString()}원`;
                const matchMeta = showMatchMeta && item.matchType
                    ? `<div class="mt-1 text-[10px] font-medium text-slate-400">${escapeHtml(matchTypeLabelMap[item.matchType] || item.matchType)}</div>`
                    : '';

                return `
                    <tr class="border-b border-slate-100">
                        <td class="py-2 text-[11px] font-medium text-gray-800">
                            <div>${escapeHtml(item.productName || item.itemName || '-')}</div>
                            ${matchMeta}
                        </td>
                        <td class="py-2 text-right text-[11px] text-gray-500">${item.collectedPrice == null ? '-' : `${Number(item.collectedPrice).toLocaleString()}원`}</td>
                        <td class="py-2 text-right text-[11px] text-gray-500">${item.erpPrice == null ? '-' : `${Number(item.erpPrice).toLocaleString()}원`}</td>
                        <td class="py-2 text-right text-[11px] ${itemDeltaColor}">${itemDeltaText}</td>
                    </tr>
                `;
            }).join('');
        };

        const latestTaskMeta = block.latestTaskId
            ? `<div class="mt-2 text-[10px] text-slate-400">최근 요청 ${escapeHtml(block.latestTaskId)} · ${escapeHtml(latestTaskLabel)}${block.latestTaskRequestedAt ? ` · ${escapeHtml(formatDateTime(block.latestTaskRequestedAt))}` : ''}</div>`
            : '';

        resultContainer.innerHTML = `
            <div class="rounded-2xl border border-slate-100 bg-slate-50/40 p-4 space-y-3" style="margin-top: 1rem;">
                <div class="flex items-center justify-between gap-3">
                    <div>
                        <h4 class="text-xs font-bold text-slate-800" style="margin: 0;">구매 단가 수집 결과</h4>
                        <p class="mt-1 text-[11px] leading-relaxed text-slate-500">${escapeHtml(block.message || '최근 수집 결과를 보여줄게.')}</p>
                        ${latestTaskMeta}
                    </div>
                    <div class="text-right">
                        <div class="rounded-full bg-white px-3 py-1 text-[10px] font-bold text-slate-600">${escapeHtml(statusLabelMap[block.resultStatus] || '결과 확인')}</div>
                        <div class="mt-2 text-[10px] text-slate-400">${block.latestCollectedAt ? `최근 성공 ${escapeHtml(formatDateTime(block.latestCollectedAt))}` : '성공 이력 없음'}</div>
                    </div>
                </div>

                <div class="grid grid-cols-2 gap-2">
                    <div class="rounded-xl border border-slate-100 bg-white p-3 text-center">
                        <p class="text-[9px] font-bold uppercase text-slate-400" style="margin: 0;">수집 평균 단가</p>
                        <p class="mt-1 text-xs font-extrabold text-slate-800">${block.latestCollectedAveragePrice == null ? '-' : `${Number(block.latestCollectedAveragePrice).toLocaleString()}원`}</p>
                    </div>
                    <div class="rounded-xl border border-slate-100 bg-white p-3 text-center">
                        <p class="text-[9px] font-bold uppercase text-slate-400" style="margin: 0;">평균 단가 차이</p>
                        <p class="mt-1 text-xs font-extrabold ${deltaColor}">${deltaText}</p>
                    </div>
                    <div class="rounded-xl border border-slate-100 bg-white p-3 text-center">
                        <p class="text-[9px] font-bold uppercase text-slate-400" style="margin: 0;">수집 품목 수</p>
                        <p class="mt-1 text-xs font-extrabold text-slate-800">${Number(block.collectedItemsCount || 0).toLocaleString()}건</p>
                    </div>
                    <div class="rounded-xl border border-slate-100 bg-white p-3 text-center">
                        <p class="text-[9px] font-bold uppercase text-slate-400" style="margin: 0;">ERP 매칭 수</p>
                        <p class="mt-1 text-xs font-extrabold text-slate-800">${Number(block.comparedItemsCount || 0).toLocaleString()}건</p>
                    </div>
                </div>

                <div class="rounded-xl border border-slate-100 bg-white p-3">
                    <div class="mb-3 flex items-center justify-between gap-2">
                        <div class="text-[10px] font-semibold text-slate-400">
                            ${escapeHtml(block.vendorName || '-')} · 현재 진행 중인 동일 작업 ${Number(block.activeTaskCount || 0).toLocaleString()}건
                        </div>
                        <div class="flex gap-2">
                            <button type="button" id="rpaCollectedTab" class="rounded-full bg-slate-900 px-3 py-1 text-[10px] font-semibold text-white">수집 결과</button>
                            <button type="button" id="rpaMatchedTab" class="rounded-full border border-slate-200 bg-white px-3 py-1 text-[10px] font-semibold text-slate-500" ${hasErpBaseline ? '' : 'disabled'}>ERP 매칭</button>
                        </div>
                    </div>
                    <table class="w-full" style="width: 100%; border-collapse: collapse;">
                        <thead>
                            <tr class="border-b text-[10px] font-bold text-slate-400">
                                <th class="pb-1.5 text-left">품목명</th>
                                <th class="pb-1.5 text-right">수집가</th>
                                <th class="pb-1.5 text-right">ERP가</th>
                                <th class="pb-1.5 text-right">차이</th>
                            </tr>
                        </thead>
                        <tbody id="rpaPurchasePriceTableBody">${renderRows(snapshotItems, false)}</tbody>
                    </table>
                </div>
            </div>
        `;

        const collectedTab = document.getElementById('rpaCollectedTab');
        const matchedTab = document.getElementById('rpaMatchedTab');
        const tableBody = document.getElementById('rpaPurchasePriceTableBody');
        if (!collectedTab || !matchedTab || !tableBody) {
            return;
        }

        const activateCollected = () => {
            collectedTab.className = 'rounded-full bg-slate-900 px-3 py-1 text-[10px] font-semibold text-white';
            matchedTab.className = hasErpBaseline
                ? 'rounded-full border border-slate-200 bg-white px-3 py-1 text-[10px] font-semibold text-slate-500'
                : 'rounded-full border border-slate-100 bg-slate-100 px-3 py-1 text-[10px] font-semibold text-slate-300';
            tableBody.innerHTML = renderRows(snapshotItems, false);
        };

        const activateMatched = () => {
            if (!hasErpBaseline) {
                return;
            }
            collectedTab.className = 'rounded-full border border-slate-200 bg-white px-3 py-1 text-[10px] font-semibold text-slate-500';
            matchedTab.className = 'rounded-full bg-slate-900 px-3 py-1 text-[10px] font-semibold text-white';
            tableBody.innerHTML = renderRows(
                matchedItems,
                true,
                'ERP 기준 품목은 찾았지만 수집 품목명과 바로 매칭된 항목은 아직 없어.'
            );
        };

        collectedTab.addEventListener('click', activateCollected);
        matchedTab.addEventListener('click', activateMatched);
    }

    function renderInventoryShortageSummary(block) {
        const resultContainer = document.getElementById('rpaWidgetResultContainer');
        if (!resultContainer) {
            return;
        }

        if (!block || !Array.isArray(block.items) || !block.items.length) {
            resultContainer.innerHTML = '';
            return;
        }

        const itemsHtml = block.items.slice(0, 3).map((item) => `
            <div class="rounded-xl border border-slate-100 bg-white p-3 text-xs shadow-sm" style="margin-bottom: 0.5rem;">
                <div class="flex items-center justify-between font-bold text-gray-900 mb-1">
                    <span>${escapeHtml(item.itemName)}</span>
                    <span class="text-amber-700">${escapeHtml(item.statusLabel || '위험 재고')}</span>
                </div>
                <div class="flex justify-between text-[11px] text-gray-500">
                    <span>위치: ${escapeHtml(item.warehouseName || '-')}</span>
                    <span>보유 ${item.availableStock ?? 0}</span>
                </div>
                <div class="mt-2 rounded-lg bg-amber-50/50 p-2 text-[10px] font-medium text-amber-800 leading-normal">
                    추천 행동: ${escapeHtml(item.recommendedAction || '-')}
                </div>
            </div>
        `).join('');

        resultContainer.innerHTML = `
            <div class="rounded-2xl border border-slate-100 bg-slate-50/40 p-4 space-y-3" style="margin-top: 1rem;">
                <div class="flex items-center justify-between">
                    <h4 class="text-xs font-bold text-slate-800" style="margin: 0;">재고 위험 분석 결과</h4>
                    <span class="text-[10px] font-medium text-slate-400">${block.latestCollectedAt ? `분석일 ${formatDateTime(block.latestCollectedAt)} | ` : ''}총 ${block.totalRiskCount || 0}건</span>
                </div>
                <div class="grid gap-2">${itemsHtml}</div>
            </div>
        `;
    }

    async function fetchAndRenderRpaResult(taskType, silent = false) {
        const resultContainer = document.getElementById('rpaWidgetResultContainer');
        if (!resultContainer) {
            return;
        }

        if (!canRenderRpaResult(taskType)) {
            if (!silent) {
                resultContainer.innerHTML = '';
            }
            return;
        }

        try {
            if (taskType === 'INVENTORY_SHORTAGE') {
                const stats = await requestRpaApi('/api/v1/inventory/dashboard/stats', { method: 'GET' });
                renderInventoryShortageSummary(stats.inventoryShortageRpa);
                return;
            }

            if (taskType === 'PURCHASE_PRICE') {
                const stats = await requestRpaApi('/api/v1/inventory/purchase-dashboard/stats', { method: 'GET' });
                renderPurchasePriceSummaryV2(stats.rpaComparison || stats.comparison);
                return;
            }

            resultContainer.innerHTML = '';
        } catch (error) {
            if (!silent && (error?.statusCode === 403 || error?.statusCode === 404)) {
                setMessage(
                    error.statusCode === 403
                        ? '현재 권한으로는 결과 상세를 다시 조회할 수 없어.'
                        : '저장된 결과를 다시 불러오지 못했어.',
                    error.statusCode === 403 ? 'success' : 'error'
                );
                return;
            }
            if (!silent) {
                setMessage(error.message || '결과 요약을 다시 불러오지 못했어.', 'error');
            }
            console.error('RPA 결과 요약 로드 실패:', error);
        }
    }

    window.DDUK_RPA_WIDGET = {
        requestRpaApi,
        renderDynamicRpaWidget,
        triggerRpa,
        pollTask,
        fetchAndRenderRpaResult
    };
})();
