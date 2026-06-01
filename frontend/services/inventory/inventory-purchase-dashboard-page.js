        let session = null;
        let rpaTriggerPending = false;

        document.addEventListener('DOMContentLoaded', () => {
            session = window.ddukSession?.requireRole?.(['ADMIN', 'INVENTORY', 'HR'], { redirectToLogin: true });
            if (!session) return;

            document.getElementById('rpa-trigger-button').addEventListener('click', triggerPurchasePriceRpa);
            document.getElementById('rpa-history-refresh').addEventListener('click', loadRpaHistory);
            if (session.role !== 'ADMIN') {
                const button = document.getElementById('rpa-trigger-button');
                button.disabled = true;
                button.title = 'RPA 실행은 관리자만 가능해.';
                document.getElementById('rpa-history-list').innerHTML = '<div class="rpa-empty">최근 RPA 실행 이력은 관리자만 볼 수 있어.</div>';
            }

            if (window.lucide) lucide.createIcons();
            loadDashboard();
            setInterval(loadDashboard, 10000);
            document.addEventListener('visibilitychange', () => {
                if (!document.hidden) loadDashboard();
            });
        });

        async function requestData(path, options = {}) {
            return window.ddukApi.requestData(path, options);
        }

        async function requestList(path, options = {}) {
            return window.ddukApi.requestList(path, options);
        }

        function money(value) {
            if (value === null || value === undefined || value === '') return '-';
            return Number(value || 0).toLocaleString('ko-KR', { style: 'currency', currency: 'KRW', maximumFractionDigits: 0 });
        }

        function number(value) {
            return Number(value || 0).toLocaleString('ko-KR');
        }

        function percent(value) {
            return `${Number(value || 0)}%`;
        }

        function escapeHtml(value) {
            return String(value ?? '').replace(/[&<>"']/g, char => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#039;' }[char]));
        }

        function statusText(status) {
            return {
                DRAFT: '초안',
                ORDERED: '발주 요청',
                PENDING: '발주 요청',
                REQUESTED: '발주 요청',
                APPROVED: '승인 완료',
                SENT_TO_VENDOR: '거래처 발송',
                INBOUND_DELAY: '입고 지연',
                RECEIVING: '입고 중',
                RECEIVED: '입고 완료',
                COMPLETED: '발주 완료',
                CANCELLED: '취소'
            }[status] || status || '-';
        }

        function setMetric(id, value) {
            document.getElementById(id).textContent = value;
        }

        function clampPercent(value) {
            return Math.max(0, Math.min(100, Number(value || 0)));
        }

        function statusVisual(status) {
            return {
                DRAFT: { color: '#64748b', bg: '#f1f5f9' },
                ORDERED: { color: '#4f46e5', bg: '#eef2ff' },
                PENDING: { color: '#4f46e5', bg: '#eef2ff' },
                REQUESTED: { color: '#4f46e5', bg: '#eef2ff' },
                APPROVED: { color: '#059669', bg: '#ecfdf5' },
                SENT_TO_VENDOR: { color: '#0d9488', bg: '#f0fdfa' },
                INBOUND_DELAY: { color: '#e11d48', bg: '#fff1f2' },
                RECEIVING: { color: '#ea580c', bg: '#fff7ed' },
                RECEIVED: { color: '#0284c7', bg: '#eff6ff' },
                COMPLETED: { color: '#16a34a', bg: '#f0fdf4' },
                CANCELLED: { color: '#b91c1c', bg: '#fef2f2' }
            }[status] || { color: '#475569', bg: '#f1f5f9' };
        }

        function rpaChipMeta(status) {
            switch (status) {
                case 'READY':
                    return { label: '비교 가능', className: 'rpa-chip ready' };
                case 'ERP_BASELINE_ONLY':
                    return { label: '부분 비교', className: 'rpa-chip waiting' };
                case 'NO_ERP_BASELINE':
                case 'MISSING_FILE':
                    return { label: '기준 부족', className: 'rpa-chip missing' };
                case 'EMPTY_RESULT':
                    return { label: '결과 비어 있음', className: 'rpa-chip missing' };
                default:
                    return { label: rpaTriggerPending ? '실행 요청 중' : '대기', className: 'rpa-chip waiting' };
            }
        }

        function renderProgress(stats) {
            const items = [
                ['발주 완료율', stats.orderCompletionRate || 0, '#16a34a', `${number(stats.completedOrderCount)}건 완료`],
                ['입고 완료율', stats.receivingCompletionRate || 0, '#0284c7', `${number(stats.receivedCount)}건 완료`],
                ['입고 지연율', stats.delayRate || 0, '#e11d48', `${number(stats.delayedReceivingCount)}건 지연`]
            ];

            document.getElementById('progress-list').innerHTML = items.map(([label, value, color, note]) => {
                const rate = clampPercent(value);
                return `
                    <div class="progress-gauge-row">
                        <div class="progress-gauge" style="--gauge-color:${color};--gauge-stop:${rate}%">
                            <strong>${rate}%</strong>
                        </div>
                        <div>
                            <div class="progress-title">${label}</div>
                            <div class="progress-note">${note}</div>
                        </div>
                    </div>
                `;
            }).join('');
        }

        function buildDisplayMonths(rows) {
            const realRows = Array.isArray(rows) ? rows : [];
            const rowMap = new Map(realRows.map(row => [row.month, { ...row, demo: false }]));
            const today = new Date();
            const currentCount = Math.max(...realRows.map(row => Number(row.count || 0)), 6);
            const currentAmount = Math.max(...realRows.map(row => Number(row.amount || 0)), 12000000);

            return Array.from({ length: 6 }, (_, index) => {
                const date = new Date(today.getFullYear(), today.getMonth() - 5 + index, 1);
                const month = `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}`;
                if (rowMap.has(month)) return rowMap.get(month);

                const wave = [0.58, 0.72, 0.65, 0.84, 0.76, 0.92][index];
                return {
                    month,
                    count: Math.max(1, Math.round(currentCount * wave)),
                    amount: Math.round(currentAmount * wave),
                    demo: true
                };
            });
        }

        function renderMonthlyChart(rows) {
            rows = buildDisplayMonths(rows);
            const chart = document.getElementById('monthly-chart');
            if (!rows.length) {
                chart.innerHTML = '<div class="self-center w-full text-center text-sm font-bold text-slate-400">표시할 발주 데이터가 없어.</div>';
                return;
            }

            const max = Math.max(...rows.map(row => Number(row.count || 0)), 1);
            chart.innerHTML = rows.map(row => {
                const height = Math.max(8, Number(row.count || 0) / max * 210);
                return `
                    <div class="flex-1 h-full flex flex-col justify-end items-center gap-2 min-w-[4rem]">
                        <div class="text-xs font-bold text-slate-500">${number(row.count)}건</div>
                        <div class="chart-bar w-full bg-indigo-500" style="height:${height}px"></div>
                        <div class="text-xs font-extrabold text-slate-600">${escapeHtml(row.month)}</div>
                        <div class="text-[11px] font-bold text-slate-400">${money(row.amount)}${row.demo ? ' · 예시' : ''}</div>
                    </div>
                `;
            }).join('');
        }

        function renderStatusList(statusCounts, total) {
            const entries = Object.entries(statusCounts || {});
            if (!entries.length) {
                document.getElementById('status-list').innerHTML = '<p class="text-sm font-bold text-slate-400">상태 데이터가 없어.</p>';
                return;
            }

            const stack = entries.map(([status, count]) => {
                const rate = total ? Math.round(count * 1000 / total) / 10 : 0;
                const visual = statusVisual(status);
                return `<div class="status-stack-segment" title="${escapeHtml(statusText(status))} ${rate}%" style="width:${Math.max(rate, 1)}%;background:${visual.color}"></div>`;
            }).join('');

            const legend = entries.map(([status, count]) => {
                const rate = total ? Math.round(count * 1000 / total) / 10 : 0;
                const visual = statusVisual(status);
                return `
                    <div class="status-legend-row">
                        <span class="status-dot-chip" style="background:${visual.color};--status-bg:${visual.bg}"></span>
                        <div>
                            <div class="status-label-text">${escapeHtml(statusText(status))}</div>
                            <div class="status-mini-track"><div class="status-mini-fill" style="width:${Math.min(100, rate)}%;background:${visual.color}"></div></div>
                        </div>
                        <div class="status-count-text">${number(count)}건 · ${rate}%</div>
                    </div>
                `;
            }).join('');

            document.getElementById('status-list').innerHTML = `
                <div class="status-stack">${stack}</div>
                <div class="status-legend">${legend}</div>
            `;
        }

        function renderRecentOrders(rows) {
            const target = document.getElementById('recent-orders');
            if (!Array.isArray(rows) || !rows.length) {
                target.innerHTML = '<tr><td colspan="5" class="p-6 text-center text-sm font-bold text-slate-400">최근 발주가 아직 없어.</td></tr>';
                return;
            }

            target.innerHTML = rows.map(order => `
                <tr class="border-t border-slate-100">
                    <td class="p-3 font-extrabold text-indigo-700">${escapeHtml(order.purchaseOrderNo)}</td>
                    <td class="p-3 font-bold text-slate-700">${escapeHtml(order.vendorName)}</td>
                    <td class="p-3"><span class="status-pill">${escapeHtml(statusText(order.status))}</span></td>
                    <td class="p-3 text-sm text-slate-500">${escapeHtml(order.expectedDate || '-')}</td>
                    <td class="p-3 text-right font-extrabold text-slate-900">${money(order.totalAmount)}</td>
                </tr>
            `).join('');
        }

        function renderRpaComparison(block) {
            const comparison = block || {};
            const chip = document.getElementById('rpa-status-chip');
            const chipMeta = rpaChipMeta(comparison.status);
            chip.className = chipMeta.className;
            chip.textContent = chipMeta.label;

            document.getElementById('rpa-message').textContent = comparison.message || '아직 비교할 결과가 없어.';
            document.getElementById('rpa-vendor-name').textContent = comparison.vendorName || '-';
            document.getElementById('rpa-collected-at').textContent = formatDateTime(comparison.latestCollectedAt);
            document.getElementById('rpa-compared-count').textContent = number(comparison.comparedItemsCount || comparison.collectedItemsCount || 0);
            document.getElementById('rpa-collected-price').textContent = money(comparison.latestCollectedAveragePrice);
            document.getElementById('rpa-erp-price').textContent = money(comparison.erpBaselineAveragePrice);
            document.getElementById('rpa-delta-price').textContent = signedMoney(comparison.priceDelta);
            document.getElementById('rpa-collected-note').textContent = `${number(comparison.collectedItemsCount || 0)}건 수집 기준`;
            document.getElementById('rpa-erp-note').textContent = comparison.erpBaselineAveragePrice == null ? '기준 단가 없음' : '기본 공급처 단가 평균';
            document.getElementById('rpa-delta-note').textContent = comparison.priceDelta == null ? '차이 계산 대기' : '수집 평균 - ERP 평균';

            const body = document.getElementById('rpa-items-body');
            const items = Array.isArray(comparison.items) ? comparison.items : [];
            if (!items.length) {
                body.innerHTML = `
                    <tr>
                        <td colspan="5">
                            <div class="rpa-empty">최근 수집 결과가 아직 없거나 ERP 기준 단가가 부족해.</div>
                        </td>
                    </tr>
                `;
                return;
            }

            body.innerHTML = items.map(item => `
                <tr>
                    <td class="font-bold text-slate-900">${escapeHtml(item.productName || '-')}</td>
                    <td class="font-black text-slate-900">${money(item.collectedPrice)}</td>
                    <td class="font-bold text-slate-700">${money(item.erpPrice)}</td>
                    <td class="font-black ${Number(item.priceDelta || 0) > 0 ? 'text-rose-600' : 'text-emerald-700'}">${signedMoney(item.priceDelta)}</td>
                    <td class="font-bold text-slate-600">${escapeHtml(matchTypeText(item.matchType))}</td>
                </tr>
            `).join('');
        }

        function formatDateTime(value) {
            if (!value) return '-';
            const date = new Date(value);
            if (Number.isNaN(date.getTime())) return value;
            return date.toLocaleString('ko-KR');
        }

        function signedMoney(value) {
            if (value === null || value === undefined || value === '') return '-';
            const numeric = Number(value || 0);
            const prefix = numeric > 0 ? '+' : '';
            return `${prefix}${money(numeric)}`;
        }

        function matchTypeText(value) {
            return {
                EXACT: '정확 매칭',
                LOOSE: '유사 매칭',
                COLLECTED_ONLY: '수집만 있음'
            }[value] || '비교'
        }

        function renderRpaHistory(items) {
            const target = document.getElementById('rpa-history-list');
            if (!Array.isArray(items) || !items.length) {
                target.innerHTML = '<div class="rpa-empty">아직 최근 실행 이력이 없어.</div>';
                return;
            }

            target.innerHTML = items.map(item => `
                <div class="rounded-xl border border-slate-100 bg-white px-3 py-3">
                    <div class="flex items-start justify-between gap-3">
                        <div class="min-w-0">
                            <div class="font-mono text-xs font-black text-slate-700">${escapeHtml(item.taskId || '-')}</div>
                            <div class="mt-1 text-xs font-bold text-slate-500">${escapeHtml(item.status || '-')} · ${escapeHtml(formatDateTime(item.requestedAt))}</div>
                            <div class="mt-1 text-xs font-semibold text-rose-600">${escapeHtml(item.errorMessage || '')}</div>
                        </div>
                        <button type="button" class="rounded-lg border border-slate-200 px-2.5 py-1 text-[11px] font-black text-slate-700 hover:bg-slate-50" onclick="triggerPurchasePriceRpa()">재실행</button>
                    </div>
                </div>
            `).join('');
        }

        async function loadRpaHistory() {
            if (!session || session.role !== 'ADMIN') {
                document.getElementById('rpa-history-list').innerHTML = '<div class="rpa-empty">최근 RPA 실행 이력은 관리자만 볼 수 있어.</div>';
                return;
            }

            try {
                const url = new URL(`${apiOrigin}/api/v1/admin/tasks`);
                url.searchParams.set('taskType', 'RPA');
                url.searchParams.set('actionName', 'collect_purchase_orders');
                url.searchParams.set('size', '5');
                url.searchParams.set('sort', 'requestedAt,desc');
                const response = await fetch(url, { headers: headers(), cache: 'no-store' });
                if (!response.ok) throw new Error(`HTTP ${response.status}`);
                const payload = await response.json();
                renderRpaHistory(payload.data?.content || []);
            } catch (error) {
                document.getElementById('rpa-history-list').innerHTML = `<div class="rpa-empty">최근 실행 이력을 불러오지 못했어: ${escapeHtml(error.message)}</div>`;
            }
        }

        async function triggerPurchasePriceRpa() {
            const message = document.getElementById('message');
            if (!session || session.role !== 'ADMIN') {
                message.textContent = 'RPA 실행은 관리자만 가능해.';
                message.className = 'text-sm text-slate-600 mt-1 font-semibold';
                return;
            }

            const button = document.getElementById('rpa-trigger-button');
            rpaTriggerPending = true;
            button.disabled = true;
            button.innerHTML = '<i data-lucide="loader-circle" class="w-4 h-4"></i> 실행 요청 중';
            if (window.lucide) lucide.createIcons();

            try {
                const response = await fetch(`${apiOrigin}/api/v1/admin/rpa/trigger`, {
                    method: 'POST',
                    headers: headers({ 'Content-Type': 'application/json' }),
                    body: JSON.stringify({ taskType: 'PURCHASE_PRICE' })
                });
                const payload = await response.json().catch(() => ({}));
                if (!response.ok || payload.status !== 'success') {
                    throw new Error(payload.message || `HTTP ${response.status}`);
                }

                message.textContent = `구매 단가 수집을 요청했어. Task ID: ${payload.data?.taskId || '-'}`;
                message.className = 'text-sm text-slate-600 mt-1 font-semibold';
                renderRpaComparison({
                    status: 'WAITING',
                    vendorName: '아망티',
                    message: 'RPA 실행을 요청했어. 완료되면 자동으로 비교 카드가 갱신돼.',
                    items: []
                });
                window.setTimeout(loadDashboard, 2000);
            } catch (error) {
                message.textContent = `RPA 실행 요청 실패: ${error.message}`;
                message.className = 'text-sm text-red-600 mt-1 font-semibold';
            } finally {
                rpaTriggerPending = false;
                button.disabled = false;
                button.innerHTML = '<i data-lucide="bot" class="w-4 h-4"></i> 단가 수집 실행';
                if (window.lucide) lucide.createIcons();
            }
        }

        async function loadDashboard() {
            const message = document.getElementById('message');
            try {
                const url = new URL(`${apiOrigin}/api/v1/inventory/purchase-dashboard/stats`);
                url.searchParams.set('_', Date.now());
                const response = await fetch(url, { headers: headers(), cache: 'no-store' });
                if (!response.ok) throw new Error(`HTTP ${response.status}`);
                const payload = await response.json();
                const stats = payload.data || {};

                setMetric('order-count', number(stats.orderCount));
                setMetric('completed-order-count', number(stats.completedOrderCount));
                setMetric('receiving-count', number(stats.receivingCount));
                setMetric('delay-count', number(stats.delayedReceivingCount));
                setMetric('received-count', number(stats.receivedCount));
                setMetric('order-rate', `완료율 ${percent(stats.orderCompletionRate)}`);
                setMetric('delay-rate', `지연율 ${percent(stats.delayRate)}`);
                setMetric('received-rate', `완료율 ${percent(stats.receivingCompletionRate)}`);
                document.getElementById('base-date').textContent = `기준일 ${stats.baseDate || '-'}`;

                renderProgress(stats);
                renderMonthlyChart(stats.monthlyOrders);
                renderStatusList(stats.statusCounts, Number(stats.orderCount || 0));
                renderRecentOrders(stats.recentOrders);
                renderRpaComparison(stats.rpaComparison);
                if (session.role === 'ADMIN') {
                    loadRpaHistory();
                }

                message.textContent = '구매/발주 지표와 RPA 비교 상태를 최신 데이터로 갱신했어.';
                message.className = 'text-sm text-emerald-600 mt-1 font-semibold';
            } catch (error) {
                message.textContent = `대시보드 조회 실패: ${error.message}`;
                message.className = 'text-sm text-red-600 mt-1 font-semibold';
            } finally {
                if (window.lucide) lucide.createIcons();
            }
        }
    
