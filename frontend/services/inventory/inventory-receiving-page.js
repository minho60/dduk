document.addEventListener('DOMContentLoaded', () => {
    const session = window.ddukSession?.requireRole?.(['ADMIN', 'INVENTORY', 'HR'], { redirectToLogin: true });
    if (!session) return;

    if (window.lucide) {
        window.lucide.createIcons();
    }

    const ordersBody = document.getElementById('orders-body');
    const itemsBody = document.getElementById('items-body');
    const message = document.getElementById('message');
    const selectedOrderText = document.getElementById('selected-order');
    const receiveButton = document.getElementById('btn-receive');
    const cancelButton = document.getElementById('btn-cancel');
    const keywordInput = document.getElementById('keyword');

    let orders = [];
    let selectedOrder = null;

    function escapeHtml(value) {
        return String(value ?? '').replace(/[&<>"']/g, (char) => ({
            '&': '&amp;',
            '<': '&lt;',
            '>': '&gt;',
            '"': '&quot;',
            "'": '&#039;'
        }[char]));
    }

    function money(value) {
        return Number(value || 0).toLocaleString('ko-KR', {
            style: 'currency',
            currency: 'KRW',
            maximumFractionDigits: 0
        });
    }

    function setMessage(text, type = 'info') {
        const color = type === 'error'
            ? 'text-red-600'
            : type === 'ok'
                ? 'text-emerald-600'
                : 'text-gray-500';
        message.className = `text-sm mt-1 font-semibold ${color}`;
        message.textContent = text;
    }

    function memberText(id, name) {
        if (Number(id) === 1) return `Admin (${name || 'admin'}/1)`;
        if (Number(id) === 2) return `Inventory (${name || 'inventory'}/2)`;
        if (Number(id) === 3) return `HR (${name || 'hr'}/3)`;
        return name || `ID ${id || '-'}`;
    }

    function statusText(status) {
        return {
            DRAFT: 'Draft',
            ORDERED: 'Ordered',
            PENDING: 'Pending',
            REQUESTED: 'Requested',
            APPROVED: 'Approved',
            SENT_TO_VENDOR: 'Sent',
            INBOUND_DELAY: 'Inbound delay',
            RECEIVING: 'Receiving',
            RECEIVED: 'Received',
            COMPLETED: 'Completed',
            CANCELLED: 'Cancelled'
        }[status] || status || '-';
    }

    function canSelectOrder(order) {
        return order.status === 'APPROVED' || order.status === 'SENT_TO_VENDOR';
    }

    function clearSelection() {
        selectedOrder = null;
        renderItems();
    }

    async function requestList(path) {
        return window.ddukApi.requestList(path, { method: 'GET' });
    }

    async function requestData(path, options) {
        return window.ddukApi.requestData(path, options);
    }

    function renderOrders() {
        if (orders.length === 0) {
            ordersBody.innerHTML = '<tr><td colspan="5" class="text-center text-gray-400 font-semibold py-8">No receivable purchase orders found.</td></tr>';
            return;
        }

        ordersBody.innerHTML = orders.map((order) => `
            <tr>
                <td>
                    <p class="font-extrabold text-gray-900">${escapeHtml(order.purchaseOrderNo)}</p>
                    <span class="status-badge mt-2">${escapeHtml(statusText(order.status))}</span>
                </td>
                <td>
                    <p class="font-bold text-gray-800">${escapeHtml(order.vendorName || '-')}</p>
                    <p class="text-xs text-gray-500 mt-1">ETA ${escapeHtml((order.expectedDate || '').slice(0, 10) || '-')}</p>
                </td>
                <td>
                    <p class="text-xs text-gray-500">Requested ${escapeHtml(memberText(order.requestedByMemberId, order.requestedByMemberName))}</p>
                    <p class="text-xs text-gray-500 mt-1">Approved ${escapeHtml(memberText(order.approvedByMemberId, order.approvedByMemberName))}</p>
                </td>
                <td class="font-bold text-gray-900">${money(order.totalAmount)}</td>
                <td class="action-cell">
                    <button type="button" class="select-order px-3 py-2 text-xs font-bold rounded-lg bg-indigo-50 text-indigo-700 hover:bg-indigo-100 disabled:hover:bg-gray-100" data-id="${escapeHtml(order.purchaseOrderId)}" ${canSelectOrder(order) ? '' : 'disabled'} title="${canSelectOrder(order) ? 'Select for receiving' : 'Only approved or sent orders can be received'}">
                        Select
                    </button>
                </td>
            </tr>
        `).join('');

        ordersBody.querySelectorAll('.select-order:not(:disabled)').forEach((button) => {
            button.addEventListener('click', () => selectOrder(Number(button.dataset.id)));
        });
    }

    function renderItems() {
        if (!selectedOrder) {
            selectedOrderText.textContent = 'No order selected.';
            itemsBody.innerHTML = '<tr><td colspan="4" class="text-center text-gray-400 font-semibold py-8">Select a purchase order first.</td></tr>';
            receiveButton.disabled = true;
            cancelButton.disabled = true;
            return;
        }

        selectedOrderText.textContent = `${selectedOrder.purchaseOrderNo} / ${selectedOrder.vendorName}`;
        receiveButton.disabled = false;
        cancelButton.disabled = false;
        itemsBody.innerHTML = (selectedOrder.items || []).map((item) => `
            <tr>
                <td>
                    <p class="font-bold text-gray-900">${escapeHtml(item.itemName || '-')}</p>
                    <p class="text-xs text-gray-400 mt-1">Item ID ${escapeHtml(item.itemId)} / ${escapeHtml(item.unit || '-')}</p>
                </td>
                <td class="font-bold text-emerald-700">${Number(item.quantity || 0).toLocaleString()}</td>
                <td>${money(item.unitPrice)}</td>
                <td class="font-bold text-gray-900">${money(item.lineAmount)}</td>
            </tr>
        `).join('');
    }

    function selectOrder(orderId) {
        selectedOrder = orders.find((order) => Number(order.purchaseOrderId) === Number(orderId)) || null;
        renderItems();
    }

    async function loadOrders(keyword) {
        clearSelection();
        ordersBody.innerHTML = '<tr><td colspan="5" class="text-center text-gray-400 font-semibold py-8">Loading receivable orders...</td></tr>';
        setMessage('Loading receivable orders...');

        try {
            const query = keyword ? `?keyword=${encodeURIComponent(keyword)}` : '';
            orders = await requestList(`/api/v1/inventory/purchase-orders/receivable${query}`);
            setMessage(`Loaded ${orders.length} receivable orders.`, 'ok');
            renderOrders();
        } catch (error) {
            orders = [];
            ordersBody.innerHTML = `<tr><td colspan="5" class="text-center text-red-600 font-semibold py-8">Load failed: ${escapeHtml(error.message)}</td></tr>`;
            setMessage(`Load failed: ${error.message}`, 'error');
        }
    }

    async function receiveOrder() {
        if (!selectedOrder) return;
        if (!window.confirm(`Receive ${selectedOrder.purchaseOrderNo}?`)) return;

        const originalReceive = receiveButton.innerHTML;
        receiveButton.disabled = true;
        cancelButton.disabled = true;
        receiveButton.innerHTML = 'Receiving...';
        setMessage(`Receiving ${selectedOrder.purchaseOrderNo}...`);

        try {
            const received = await requestData(`/api/v1/inventory/purchase-orders/${selectedOrder.purchaseOrderId}/receive`, {
                method: 'POST'
            });
            orders = orders.filter((order) => Number(order.purchaseOrderId) !== Number(received.purchaseOrderId));
            clearSelection();
            renderOrders();
            setMessage(`${received.purchaseOrderNo} received.`, 'ok');
        } catch (error) {
            setMessage(`Receive failed: ${error.message}`, 'error');
            receiveButton.disabled = false;
            cancelButton.disabled = false;
        } finally {
            receiveButton.innerHTML = originalReceive;
            if (window.lucide) {
                window.lucide.createIcons();
            }
        }
    }

    async function cancelOrder() {
        if (!selectedOrder) return;
        if (!window.confirm(`Cancel ${selectedOrder.purchaseOrderNo} before receiving?`)) return;

        const originalCancel = cancelButton.innerHTML;
        receiveButton.disabled = true;
        cancelButton.disabled = true;
        cancelButton.innerHTML = 'Cancelling...';
        setMessage(`Cancelling ${selectedOrder.purchaseOrderNo}...`);

        try {
            const cancelled = await requestData(`/api/v1/inventory/purchase-orders/${selectedOrder.purchaseOrderId}/cancel`, {
                method: 'POST'
            });
            orders = orders.filter((order) => Number(order.purchaseOrderId) !== Number(cancelled.purchaseOrderId));
            clearSelection();
            renderOrders();
            setMessage(`${cancelled.purchaseOrderNo} cancelled.`, 'ok');
        } catch (error) {
            setMessage(`Cancel failed: ${error.message}`, 'error');
            receiveButton.disabled = false;
            cancelButton.disabled = false;
        } finally {
            cancelButton.innerHTML = originalCancel;
            if (window.lucide) {
                window.lucide.createIcons();
            }
        }
    }

    document.getElementById('search-form')?.addEventListener('submit', (event) => {
        event.preventDefault();
        const keyword = keywordInput.value.trim();
        if (!keyword) {
            orders = [];
            clearSelection();
            renderOrders();
            setMessage('Enter a keyword or use load all.', 'error');
            keywordInput.focus();
            return;
        }
        loadOrders(keyword);
    });
    document.getElementById('btn-load-all')?.addEventListener('click', () => {
        keywordInput.value = '';
        loadOrders('');
    });
    receiveButton?.addEventListener('click', receiveOrder);
    cancelButton?.addEventListener('click', cancelOrder);
});
