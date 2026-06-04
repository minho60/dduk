        document.addEventListener('DOMContentLoaded', () => {
            const session = window.ddukSession?.requireRole?.(['ADMIN', 'INVENTORY', 'HR'], { redirectToLogin: true });
            if (!session) return;
            if (window.lucide) lucide.createIcons();

            const apiOrigin = location.protocol === 'file:' ? 'http://localhost:8080' : location.origin;
            const body = document.getElementById('orders-body');
            const message = document.getElementById('message');
            const keywordInput = document.getElementById('keyword');
            const isSuperAdmin = (session.role || '').toUpperCase() === 'ADMIN'
                && (session.loginId || localStorage.getItem('loginId') || '').toLowerCase() === 'admin';
            const changedOrderIds = new Set();
            let orders = [];

            const statusLabels = new Map([
                ['ORDERED', '발주요청'],
                ['APPROVED', '승인'],
                ['SENT_TO_VENDOR', '거래처 발송'],
                ['INBOUND_DELAY', '입고지연'],
                ['RECEIVING', '입고중'],
                ['RECEIVED', '입고 완료'],
                ['COMPLETED', '발주완료'],
                ['CANCELLED', '취소']
            ]);
            const statuses = [...statusLabels].filter(([value]) => !['ORDERED', 'APPROVED'].includes(value));

            function headers(json) {
                const token = localStorage.getItem('token');
                const result = json ? { 'Content-Type': 'application/json' } : {};
                if (token) result.Authorization = `Bearer ${token}`;
                return result;
            }

            function setMessage(text, type) {
                const color = type === 'error' ? 'text-red-600' : type === 'ok' ? 'text-emerald-600' : 'text-gray-500';
                message.className = `text-sm mt-1 font-semibold ${color}`;
                message.textContent = text;
            }

            async function readError(response) {
                const text = await response.text();
                if (!text) return `HTTP ${response.status}`;
                try {
                    const parsed = JSON.parse(text);
                    return parsed.message || parsed.error || text;
                } catch {
                    return text;
                }
            }

            function escapeHtml(value) {
                return String(value ?? '').replace(/[&<>"']/g, char => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#039;' }[char]));
            }

            function money(value) {
                return Number(value || 0).toLocaleString('ko-KR', { style: 'currency', currency: 'KRW', maximumFractionDigits: 0 });
            }

            function statusText(status) {
                return statusLabels.get(status) || status || '-';
            }

            function normalizeStatus(status) {
                if (!status) return status;
                const normalized = String(status).trim().toUpperCase();
                if (normalized === '거래처 발송') return 'SENT_TO_VENDOR';
                return normalized;
            }

            function canChangeOrderStatus(order) {
                return isSuperAdmin
                    && ['ORDERED', 'PENDING', 'REQUESTED'].includes(order.status)
                    && !changedOrderIds.has(Number(order.purchaseOrderId));
            }

            function disabledReason(order) {
                if (!isSuperAdmin) return '최고관리자(admin)만 상태를 변경할 수 있습니다.';
                if (changedOrderIds.has(Number(order.purchaseOrderId))) return '이미 상태를 한 번 변경한 발주입니다.';
                if (!['ORDERED', 'PENDING', 'REQUESTED'].includes(order.status)) return '발주요청 상태에서만 한 번 변경할 수 있습니다.';
                return '';
            }

            function memberText(id, name) {
                if (Number(id) === 1) return `최고관리자 (${name || 'admin'}/1)`;
                if (Number(id) === 2) return `구매/발주 담당 (${name || 'inventory'}/2)`;
                if (Number(id) === 3) return `회계/직원 관리 담당 (${name || 'hr'}/3)`;
                return name || `ID ${id || '-'}`;
            }

            function itemSummary(order) {
                const items = order.items || [];
                if (items.length === 0) return '-';
                const names = items.slice(0, 2).map(item => item.itemName || `품목 ${item.itemId}`).join(', ');
                return items.length > 2 ? `${names} 외 ${items.length - 2}건` : names;
            }

            function render() {
                if (orders.length === 0) {
                    body.innerHTML = '<tr><td colspan="7" class="text-center text-gray-400 font-semibold py-8">조회된 발주서가 없습니다.</td></tr>';
                    return;
                }

                body.innerHTML = orders.map(order => `
                    <tr>
                        <td>
                            <a class="font-extrabold text-indigo-700 hover:text-indigo-900 hover:underline" href="purchase-order-detail.html?id=${encodeURIComponent(order.purchaseOrderId)}">${escapeHtml(order.purchaseOrderNo || '-')}</a>
                            <p class="text-xs text-gray-400 mt-1">ID ${escapeHtml(order.purchaseOrderId || '-')}</p>
                        </td>
                        <td class="font-bold text-gray-800">${escapeHtml(order.vendorName || '-')}</td>
                        <td>
                            <p class="text-xs text-gray-500">요청 ${escapeHtml(memberText(order.requestedByMemberId, order.requestedByMemberName))}</p>
                            <p class="text-xs text-gray-500 mt-1">승인 ${escapeHtml(memberText(order.approvedByMemberId, order.approvedByMemberName))}</p>
                        </td>
                        <td>${escapeHtml(itemSummary(order))}</td>
                        <td class="font-bold text-gray-900">${money(order.totalAmount)}</td>
                        <td><span class="status-badge status-${escapeHtml(order.status)}">${escapeHtml(statusText(order.status))}</span></td>
                        <td>
                            <select class="form-select status-select min-w-[10rem]" data-id="${escapeHtml(order.purchaseOrderId)}" data-current="${escapeHtml(order.status)}" ${canChangeOrderStatus(order) ? '' : `disabled title="${escapeHtml(disabledReason(order))}"`}>
                                ${['ORDERED', 'PENDING', 'REQUESTED'].includes(order.status) ? `<option value="${escapeHtml(order.status)}" selected disabled>${statusText(order.status)}</option>` : ''}
                                <option value="" ${statuses.some(([value]) => value === order.status) ? '' : 'selected'} disabled>상태 선택</option>
                                ${statuses.map(([value, label]) => `<option value="${value}" ${value === order.status ? 'selected' : ''}>${label}</option>`).join('')}
                            </select>
                            ${canChangeOrderStatus(order) ? '' : `<p class="text-xs text-gray-400 mt-2 font-bold">${escapeHtml(disabledReason(order))}</p>`}
                        </td>
                    </tr>
                `).join('');

                body.querySelectorAll('.status-select').forEach(select => {
                    if (select.disabled) return;
                    select.addEventListener('change', () => changeStatus(select));
                });
            }

            async function loadOrders(keyword, all) {
                body.innerHTML = '<tr><td colspan="7" class="text-center text-gray-400 font-semibold py-8">발주서를 조회 중입니다.</td></tr>';
                setMessage('발주서를 조회 중입니다.');
                try {
                    const url = new URL(`${apiOrigin}/api/v1/inventory/purchase-orders/management`);
                    if (all) url.searchParams.set('all', 'true');
                    if (keyword) url.searchParams.set('keyword', keyword);
                    const response = await fetch(url, { headers: headers(false) });
                    if (!response.ok) throw new Error(await readError(response));
                    orders = await response.json();
                    setMessage(`발주서 ${orders.length}건을 조회했습니다.`, 'ok');
                    render();
                } catch (error) {
                    orders = [];
                    body.innerHTML = `<tr><td colspan="7" class="text-center text-red-600 font-semibold py-8">발주 조회 실패: ${escapeHtml(error.message)}</td></tr>`;
                    setMessage(`발주 조회 실패: ${error.message}`, 'error');
                }
            }

            async function changeStatus(select) {
                const orderId = Number(select.dataset.id);
                const previousStatus = select.dataset.current;
                const nextStatus = select.value;
                const order = orders.find(item => Number(item.purchaseOrderId) === orderId);
                if (!order || nextStatus === previousStatus) return;
                if (!isSuperAdmin) {
                    select.value = previousStatus;
                    setMessage('최고관리자(admin)만 발주 상태를 변경할 수 있습니다.', 'error');
                    return;
                }
                if (changedOrderIds.has(orderId) || !['ORDERED', 'PENDING', 'REQUESTED'].includes(previousStatus)) {
                    select.value = previousStatus;
                    setMessage('발주 상태는 발주요청 상태에서 한 번만 변경할 수 있습니다.', 'error');
                    return;
                }

                const previousStatusText = statusText(previousStatus);
                const nextStatusText = statusText(nextStatus);
                const confirmMessage = `${order.purchaseOrderNo} 상태를 ${previousStatusText}에서 ${nextStatusText}(으)로 변경할까요?\n\n상태 변경은 한 번만 가능하며, 변경 후에는 이 발주의 선택창이 비활성화됩니다.`;
                if (!window.confirm(confirmMessage)) {
                    select.value = previousStatus;
                    return;
                }

                select.disabled = true;
                setMessage(`${order.purchaseOrderNo} 상태를 변경 중입니다.`);
                try {
                    const response = await fetch(`${apiOrigin}/api/v1/inventory/purchase-orders/${orderId}/management-status`, {
                        method: 'PATCH',
                        headers: headers(true),
                        body: JSON.stringify({ status: nextStatus })
                    });
                    if (!response.ok) throw new Error(await readError(response));
                    const updated = await response.json();
                    updated.status = normalizeStatus(updated.status) || nextStatus;
                    changedOrderIds.add(orderId);
                    orders = orders.map(item => Number(item.purchaseOrderId) === orderId ? updated : item);
                    setMessage(`${updated.purchaseOrderNo || order.purchaseOrderNo} 상태가 ${statusText(updated.status)}로 저장되었습니다.`, 'ok');
                    render();
                } catch (error) {
                    select.value = previousStatus;
                    setMessage(`상태 변경 실패: ${error.message}`, 'error');
                    select.disabled = false;
                }
            }

            document.getElementById('search-form').addEventListener('submit', event => {
                event.preventDefault();
                const keyword = keywordInput.value.trim();
                if (!keyword) {
                    orders = [];
                    render();
                    setMessage('검색어를 입력해야 조회할 수 있습니다.', 'error');
                    keywordInput.focus();
                    return;
                }
                loadOrders(keyword, false);
            });

            document.getElementById('btn-all').addEventListener('click', () => {
                keywordInput.value = '';
                loadOrders('', true);
            });
        });
    