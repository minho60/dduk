        let assetChartInstance = null;
        let trendChartInstance = null;
        let session = null;
        let rpaTriggerPending = false;

        document.addEventListener('DOMContentLoaded', async () => {
            session = window.ddukSession?.requireRole?.(['ADMIN', 'INVENTORY', 'HR']);
            if (!session) return;

            document.getElementById('rpa-trigger-button').addEventListener('click', triggerInventoryShortageRpa);
            document.getElementById('rpa-history-refresh').addEventListener('click', loadRpaHistory);
            if (session.role !== 'ADMIN') {
                const button = document.getElementById('rpa-trigger-button');
                button.disabled = true;
                button.title = 'RPA 실행은 관리자만 가능해.';
                document.getElementById('rpa-history-list').innerHTML = '<div class="warn-empty">최근 RPA 실행 이력은 관리자만 볼 수 있어.</div>';
            }

            if (window.lucide) lucide.createIcons();
            await loadDashboard();
            await loadPendingTransfers();
            setInterval(loadDashboard, 10000);
        });

        function number(value) {
            return Number(value || 0).toLocaleString('ko-KR');
        }

        function money(value) {
            if (value === null || value === undefined || value === '') return '-';
            return Number(value || 0).toLocaleString('ko-KR', { style: 'currency', currency: 'KRW', maximumFractionDigits: 0 });
        }

        function escapeHtml(value) {
            return String(value ?? '').replace(/[&<>"']/g, char => ({ '&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#039;' }[char]));
        }

        function formatDateTime(value) {
            if (!value) return '-';
            const date = new Date(value);
            return Number.isNaN(date.getTime()) ? value : date.toLocaleString('ko-KR');
        }

        function rpaChipMeta(status) {
            switch (status) {
                case 'READY':
                    return { label: '경고 확인 가능', className: 'rpa-chip ready' };
                case 'ERP_ONLY':
                    return { label: 'ERP 기준만 있음', className: 'rpa-chip waiting' };
                case 'MISSING_FILE':
                case 'EMPTY_RESULT':
                    return { label: '결과 부족', className: 'rpa-chip missing' };
                default:
                    return { label: rpaTriggerPending ? '실행 요청 중' : '대기', className: 'rpa-chip waiting' };
            }
        }

        async function loadDashboard() {
            const pageMessage = document.getElementById('page-message');
            try {
                const response = await InventoryService.getDashboardStats();
                if (!response || response.status !== 'success') {
                    throw new Error(response?.message || '재고 대시보드를 불러오지 못했어.');
                }

                const stats = response.data || {};
                document.getElementById('base-time').textContent = `기준 ${new Date().toLocaleString('ko-KR')}`;
                document.getElementById('total-qty').textContent = number(stats.totalQuantity);
                document.getElementById('total-value').textContent = money(stats.totalValue);
                document.getElementById('low-stock-count').textContent = number(stats.lowStockCount);
                document.getElementById('pending-transfer-count').textContent = number(stats.pendingTransferCount);
                document.getElementById('outbound-volume').textContent = number(stats.outboundVolume30Days);

                renderDistribution(stats.warehouseDistribution || []);
                renderRecentMovements(stats.recentMovements || []);
                renderDashboardCharts(stats);
                renderShortageRpa(stats.inventoryShortageRpa || {});
                if (session.role === 'ADMIN') {
                    loadRpaHistory();
                }

                pageMessage.textContent = '재고 지표와 Stage 3 부족 조회 상태를 최신 데이터로 갱신했어.';
                pageMessage.className = 'mt-1 text-sm font-semibold text-emerald-600';
            } catch (error) {
                pageMessage.textContent = error.message || '재고 대시보드를 불러오지 못했어.';
                pageMessage.className = 'mt-1 text-sm font-semibold text-rose-600';
                ['total-qty','total-value','low-stock-count','pending-transfer-count','outbound-volume'].forEach(id => {
                    const el = document.getElementById(id);
                    if (el) el.textContent = '-';
                });
            } finally {
                if (window.lucide) lucide.createIcons();
            }
        }

        function renderDistribution(items) {
            const target = document.getElementById('distribution-list');
            if (!items.length) {
                target.innerHTML = '<div class="dist-card"><div class="text-sm font-bold text-slate-400">표시할 창고 재고가 없어.</div></div>';
                return;
            }

            target.innerHTML = items.map(item => `
                <div class="dist-card">
                    <div class="flex items-center gap-3">
                        <div class="h-10 w-2.5 rounded-full bg-indigo-500"></div>
                        <div>
                            <p class="text-sm font-black text-slate-800">${escapeHtml(item.warehouseName)}</p>
                            <p class="text-xs font-semibold text-slate-400">자산 ${money(item.totalValue)}</p>
                        </div>
                    </div>
                    <div class="text-right">
                        <p class="text-base font-black text-indigo-600">${number(item.totalStock)}</p>
                        <p class="text-[10px] font-black uppercase text-slate-400">수량</p>
                    </div>
                </div>
            `).join('');
        }

        function renderRecentMovements(items) {
            const target = document.getElementById('recent-movements');
            if (!items.length) {
                target.innerHTML = '<p class="py-4 text-center text-xs font-bold text-slate-400">최근 재고 변경 이력이 없어.</p>';
                return;
            }

            target.innerHTML = items.slice(0, 5).map(item => `
                <div class="movement-card">
                    <div>
                        <p class="text-sm font-black text-slate-800">${escapeHtml(item.itemName)}</p>
                        <p class="text-[11px] font-semibold text-slate-400">${escapeHtml(item.warehouseName)} | ${escapeHtml(item.referenceNo || '-')}</p>
                    </div>
                    <div class="text-right">
                        <p class="text-sm font-black ${String(item.movementType || '').includes('OUT') ? 'text-rose-600' : 'text-emerald-600'}">
                            ${String(item.movementType || '').includes('OUT') ? '-' : '+'}${number(item.quantity)}
                        </p>
                        <p class="text-[10px] font-black uppercase text-slate-400">${escapeHtml(translateType(item.movementType))}</p>
                    </div>
                </div>
            `).join('');
        }

        function renderShortageRpa(block) {
            const chip = document.getElementById('rpa-status-chip');
            const chipMeta = rpaChipMeta(block.status);
            chip.className = chipMeta.className;
            chip.textContent = chipMeta.label;

            document.getElementById('rpa-message').textContent = block.message || '부족 조회 결과가 아직 없어.';
            document.getElementById('rpa-vendor-name').textContent = block.vendorName || '-';
            document.getElementById('rpa-collected-at').textContent = formatDateTime(block.latestCollectedAt);
            document.getElementById('rpa-matched-count').textContent = number(block.matchedLowStockCount || 0);
            document.getElementById('rpa-erp-count').textContent = number(block.erpLowStockCount || 0);
            document.getElementById('rpa-alert-count').textContent = number(block.rpaAlertCount || 0);
            document.getElementById('rpa-match-count-card').textContent = number(block.matchedLowStockCount || 0);

            const target = document.getElementById('rpa-items-body');
            const items = Array.isArray(block.items) ? block.items : [];
            if (!items.length) {
                target.innerHTML = '<tr><td colspan="5"><div class="warn-empty">ERP 부족 재고나 외부 경고 결과가 아직 없어.</div></td></tr>';
                return;
            }

            target.innerHTML = items.map(item => `
                <tr>
                    <td class="font-black text-slate-900">${escapeHtml(item.itemName || '-')}</td>
                    <td class="font-bold text-slate-600">${escapeHtml(item.warehouseName || '-')}</td>
                    <td class="font-bold text-slate-700">${item.availableStock == null ? '-' : `${number(item.availableStock)} / ${number(item.safetyStock)}`}</td>
                    <td class="font-bold ${item.matchType === 'MATCHED' ? 'text-rose-600' : 'text-amber-700'}">${escapeHtml(item.rpaStockStatus || '-')}</td>
                    <td class="font-bold text-slate-700">${escapeHtml(item.recommendedAction || '-')}</td>
                </tr>
            `).join('');
        }

        function renderRpaHistory(items) {
            const target = document.getElementById('rpa-history-list');
            if (!target) return;

            if (!Array.isArray(items) || !items.length) {
                target.innerHTML = '<div class="warn-empty">최근 실행 이력이 아직 없어.</div>';
                return;
            }

            target.innerHTML = items.map(item => `
                <div class="flex items-start justify-between gap-3 rounded-xl border border-amber-100 bg-white/80 px-4 py-3">
                    <div class="min-w-0">
                        <div class="flex flex-wrap items-center gap-2">
                            <span class="text-xs font-black text-slate-900">${escapeHtml(item.status || '-')}</span>
                            <span class="text-[11px] font-bold text-slate-500">${escapeHtml(item.taskId || '-')}</span>
                        </div>
                        <p class="mt-1 text-xs font-semibold text-slate-600">${escapeHtml(formatDateTime(item.requestedAt))}</p>
                        <p class="mt-1 text-xs font-semibold text-rose-600">${escapeHtml(item.errorMessage || '')}</p>
                    </div>
                    <button type="button" onclick="triggerInventoryShortageRpa()" class="rounded-lg border border-amber-200 bg-white px-3 py-1.5 text-xs font-black text-amber-900 hover:bg-amber-50">재실행</button>
                </div>
            `).join('');
        }

        async function loadRpaHistory() {
            const target = document.getElementById('rpa-history-list');
            if (!session || session.role !== 'ADMIN') {
                if (target) {
                    target.innerHTML = '<div class="warn-empty">최근 RPA 실행 이력은 관리자만 볼 수 있어.</div>';
                }
                return;
            }

            if (target) {
                target.innerHTML = '<div class="warn-empty">최근 실행 이력을 불러오는 중이야.</div>';
            }

            try {
                const response = await window.ddukApi.get('/api/v1/admin/tasks?taskType=RPA&actionName=check_inventory_shortage&size=5&sort=requestedAt,desc');
                const page = response?.data;
                const items = Array.isArray(page?.content) ? page.content : [];
                renderRpaHistory(items);
            } catch (error) {
                if (target) {
                    target.innerHTML = `<div class="warn-empty">${escapeHtml(error.message || '최근 실행 이력을 불러오지 못했어.')}</div>`;
                }
            }
        }

        async function triggerInventoryShortageRpa() {
            if (!session || session.role !== 'ADMIN') {
                window.ddukApi?.showToast?.('RPA 실행은 관리자만 가능해.', 'warning');
                return;
            }

            const button = document.getElementById('rpa-trigger-button');
            const pageMessage = document.getElementById('page-message');
            rpaTriggerPending = true;
            button.disabled = true;
            button.innerHTML = '<i data-lucide="loader-circle" class="w-4 h-4"></i> 실행 요청 중';
            if (window.lucide) lucide.createIcons();

            try {
                const response = await window.ddukApi.post('/api/v1/admin/rpa/trigger', { taskType: 'INVENTORY_SHORTAGE' });
                pageMessage.textContent = `재고 부족 조회를 요청했어. Task ID: ${response.data?.taskId || '-'}`;
                pageMessage.className = 'mt-1 text-sm font-semibold text-amber-700';
                renderShortageRpa({
                    status: 'WAITING',
                    vendorName: '아망티',
                    message: 'RPA 실행을 요청했어. 완료되면 자동으로 부족 조회 카드가 갱신돼.',
                    items: []
                });
                loadRpaHistory();
                window.setTimeout(loadDashboard, 2000);
            } catch (error) {
                pageMessage.textContent = error.message || '재고 부족 조회 실행 요청에 실패했어.';
                pageMessage.className = 'mt-1 text-sm font-semibold text-rose-600';
            } finally {
                rpaTriggerPending = false;
                button.disabled = session.role !== 'ADMIN';
                button.innerHTML = '<i data-lucide="radar" class="w-4 h-4"></i> 부족 조회 실행';
                if (window.lucide) lucide.createIcons();
            }
        }

        function renderDashboardCharts(stats) {
            if (assetChartInstance) {
                assetChartInstance.destroy();
                assetChartInstance = null;
            }
            if (trendChartInstance) {
                trendChartInstance.destroy();
                trendChartInstance = null;
            }

            const assetCanvas = document.getElementById('asset-ratio-chart');
            const trendCanvas = document.getElementById('outbound-trend-chart');
            if (!assetCanvas || !trendCanvas) return;

            const distribution = Array.isArray(stats.warehouseDistribution) ? stats.warehouseDistribution : [];
            const whNames = distribution.map(item => item.warehouseName);
            const whValues = distribution.map(item => Number(item.totalValue || 0));

            assetChartInstance = new Chart(assetCanvas.getContext('2d'), {
                type: 'doughnut',
                data: {
                    labels: whNames.length ? whNames : ['재고 없음'],
                    datasets: [{
                        data: whValues.length ? whValues : [0],
                        backgroundColor: [
                            'rgba(79, 70, 229, 0.82)',
                            'rgba(16, 185, 129, 0.82)',
                            'rgba(245, 158, 11, 0.82)',
                            'rgba(239, 68, 68, 0.82)',
                            'rgba(107, 114, 128, 0.82)'
                        ],
                        borderColor: '#ffffff',
                        borderWidth: 2
                    }]
                },
                options: {
                    responsive: true,
                    maintainAspectRatio: false,
                    plugins: {
                        legend: { position: 'bottom', labels: { boxWidth: 12, font: { size: 10 } } }
                    }
                }
            });

            const dateStats = {};
            for (let i = 29; i >= 0; i -= 1) {
                const d = new Date();
                d.setDate(d.getDate() - i);
                const key = d.toISOString().split('T')[0];
                dateStats[key] = { inbound: 0, outbound: 0 };
            }

            (stats.recentMovements || []).forEach(item => {
                const key = String(item.createdAt || '').split('T')[0];
                if (!dateStats[key]) return;
                if (String(item.movementType || '').includes('IN')) {
                    dateStats[key].inbound += Number(item.quantity || 0);
                } else {
                    dateStats[key].outbound += Number(item.quantity || 0);
                }
            });

            const labels = Object.keys(dateStats).map(key => key.slice(5));
            const inboundData = Object.values(dateStats).map(value => value.inbound);
            const outboundData = Object.values(dateStats).map(value => value.outbound);

            trendChartInstance = new Chart(trendCanvas.getContext('2d'), {
                type: 'line',
                data: {
                    labels,
                    datasets: [
                        {
                            label: '입고',
                            data: inboundData,
                            borderColor: 'rgb(16, 185, 129)',
                            backgroundColor: 'rgba(16, 185, 129, 0.06)',
                            tension: 0.3,
                            fill: true,
                            pointRadius: 2
                        },
                        {
                            label: '출고',
                            data: outboundData,
                            borderColor: 'rgb(239, 68, 68)',
                            backgroundColor: 'rgba(239, 68, 68, 0.06)',
                            tension: 0.3,
                            fill: true,
                            pointRadius: 2
                        }
                    ]
                },
                options: {
                    responsive: true,
                    maintainAspectRatio: false,
                    scales: { y: { beginAtZero: true } },
                    plugins: { legend: { position: 'top', labels: { boxWidth: 12 } } }
                }
            });
        }

        async function loadPendingTransfers() {
            const list = document.getElementById('pending-transfer-list');
            const empty = document.getElementById('pending-empty');
            list.innerHTML = '<tr><td colspan="4" class="py-6 text-center text-xs font-bold text-slate-400">로딩 중...</td></tr>';

            try {
                const response = await InventoryService.getTransfers({ status: 'PENDING' });
                const items = Array.isArray(response.data) ? response.data : [];
                if (!items.length) {
                    list.innerHTML = '';
                    empty.classList.remove('hidden');
                    return;
                }

                empty.classList.add('hidden');
                list.innerHTML = items.slice(0, 5).map(item => `
                    <tr class="border-b border-slate-100">
                        <td class="py-3 text-sm font-black text-indigo-600">${escapeHtml(item.transferNo)}</td>
                        <td class="py-3 text-sm text-slate-600">${escapeHtml(item.sourceWarehouseName)} → ${escapeHtml(item.targetWarehouseName)}</td>
                        <td class="py-3 text-sm text-slate-500">${escapeHtml(item.requestedByName)}</td>
                        <td class="py-3 text-center">
                            <button type="button" onclick="approveTransfer(${item.id})" class="rounded-md bg-amber-500 px-2 py-1 text-[11px] font-black text-white hover:bg-amber-600">승인</button>
                        </td>
                    </tr>
                `).join('');
            } catch (error) {
                list.innerHTML = '';
                empty.classList.remove('hidden');
            }
        }

        async function approveTransfer(id) {
            if (!confirm('해당 창고 이동 요청을 승인할까?')) return;
            try {
                const response = await InventoryService.approveTransfer(id);
                if (response.status === 'success') {
                    window.ddukApi?.showToast?.('이동 요청을 승인했어.', 'success');
                    await loadDashboard();
                    await loadPendingTransfers();
                }
            } catch (error) {
                window.ddukApi?.showToast?.(error.message || '이동 승인에 실패했어.', 'error');
            }
        }

        function translateType(type) {
            return {
                INBOUND: '입고',
                OUTBOUND: '출고',
                TRANSFER_IN: '이동 입고',
                TRANSFER_OUT: '이동 출고',
                ADJUSTMENT_IN: '조정 입고',
                ADJUSTMENT_OUT: '조정 출고'
            }[type] || type || '-';
        }
    