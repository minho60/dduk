document.addEventListener('DOMContentLoaded', () => {
    const session = window.ddukSession?.requireRole?.(['ADMIN', 'INVENTORY', 'HR'], { redirectToLogin: true });
    if (!session) return;

    if (window.lucide) {
        window.lucide.createIcons();
    }

    const TAX_RATE = 0.1;
    const vendorResult = document.getElementById('vendor-result');
    const itemResult = document.getElementById('item-result');
    const orderBody = document.getElementById('order-items');
    const message = document.getElementById('message');
    const saveButton = document.getElementById('btn-save');
    const selectedBox = document.getElementById('selected-vendor');
    const requestedByInput = document.getElementById('requested-by-member-id');
    const approvedBySelect = document.getElementById('approved-by-member-id');
    const vendorIdInput = document.getElementById('vendor-id');
    const vendorNameInput = document.getElementById('vendor-name');
    const itemNameInput = document.getElementById('item-name');
    const expectedDateInput = document.getElementById('expected-date');
    const orderNoteInput = document.getElementById('order-note');

    let selectedVendor = null;
    let orderItems = [];

    function resolveCurrentMemberId() {
        const loginId = (localStorage.getItem('loginId') || '').toLowerCase();
        const role = (localStorage.getItem('role') || '').toUpperCase();
        if (loginId === 'inventory' || role === 'INVENTORY') return '2';
        if (loginId === 'hr' || role === 'HR') return '3';
        if (loginId === 'admin' || role === 'ADMIN') return '1';
        return localStorage.getItem('memberId') || localStorage.getItem('userId') || '1';
    }

    function text(value) {
        return value == null || value === '' ? '-' : value;
    }

    function money(value) {
        return Number(value || 0).toLocaleString('ko-KR', { maximumFractionDigits: 2 });
    }

    function setMessage(textValue, type = 'info') {
        const color = type === 'error'
            ? 'text-red-600'
            : type === 'ok'
                ? 'text-emerald-600'
                : 'text-gray-500';
        message.className = `text-sm mt-1 font-semibold ${color}`;
        message.textContent = textValue;
    }

    async function requestList(path, fallbackMessage) {
        try {
            return await window.ddukApi.requestList(path, { method: 'GET' });
        } catch (error) {
            throw new Error(error.message || fallbackMessage);
        }
    }

    async function requestData(path, options, fallbackMessage) {
        try {
            return await window.ddukApi.requestData(path, options);
        } catch (error) {
            throw new Error(error.message || fallbackMessage);
        }
    }

    function renderVendors(list) {
        if (!Array.isArray(list) || list.length === 0) {
            vendorResult.innerHTML = '<tr><td colspan="5" class="text-center text-gray-400 font-semibold py-7">No vendors found.</td></tr>';
            return;
        }

        vendorResult.innerHTML = list.map((vendor) => `
            <tr>
                <td class="font-bold">${text(vendor.vendorCode)}</td>
                <td>${text(vendor.name)}</td>
                <td>${text(vendor.representativeName)}</td>
                <td>${text(vendor.contactPhone)}</td>
                <td>
                    <button type="button" class="select-vendor px-3 py-2 text-xs font-bold rounded-lg bg-indigo-50 text-indigo-700" data-id="${vendor.id}">
                        Select
                    </button>
                </td>
            </tr>
        `).join('');

        vendorResult.querySelectorAll('.select-vendor').forEach((button) => {
            button.addEventListener('click', () => {
                selectedVendor = list.find((vendor) => String(vendor.id) === button.dataset.id) || null;
                if (!selectedVendor) {
                    return;
                }
                vendorIdInput.value = selectedVendor.id;
                selectedBox.textContent = `${selectedVendor.vendorCode} / ${selectedVendor.name} / ${selectedVendor.businessRegistrationNo}`;
                setMessage('Vendor selected.', 'ok');
            });
        });
    }

    function addItem(item) {
        const existing = orderItems.find((candidate) => candidate.itemId === item.id);
        if (existing) {
            existing.quantity += 1;
        } else {
            orderItems.push({
                itemId: item.id,
                itemCode: item.itemCode,
                name: item.name,
                unit: item.unit,
                quantity: 1,
                unitPrice: Number(item.unitPrice || 0)
            });
        }
        renderOrder();
        setMessage(`Added item: ${text(item.name)}`, 'ok');
    }

    function openItemPopup() {
        const params = new URLSearchParams({
            vendorId: selectedVendor ? selectedVendor.id : '',
            vendorName: selectedVendor ? selectedVendor.name : '',
            name: itemNameInput.value.trim(),
            registeredById: requestedByInput.value
        });
        const popup = window.open(
            `item-register-popup.html?${params}`,
            'ddukItemRegister',
            'width=760,height=780,menubar=no,toolbar=no,location=no,status=no'
        );
        if (!popup) {
            setMessage('Popup was blocked.', 'error');
        }
    }

    function renderItems(list) {
        if (!Array.isArray(list) || list.length === 0) {
            itemResult.innerHTML = `
                <tr>
                    <td colspan="5" class="text-center py-7">
                        <p class="font-semibold text-gray-400">No items found.</p>
                        <button type="button" id="btn-open-item-register" class="mt-3 px-4 py-2 text-sm font-bold rounded-lg bg-indigo-50 text-indigo-700">
                            Register item
                        </button>
                    </td>
                </tr>
            `;
            document.getElementById('btn-open-item-register')?.addEventListener('click', openItemPopup);
            return;
        }

        itemResult.innerHTML = list.map((item) => `
            <tr>
                <td class="font-bold">${text(item.itemCode)}</td>
                <td>${text(item.name)}</td>
                <td>${text(item.unit)}</td>
                <td>${money(item.unitPrice)}</td>
                <td>
                    <button type="button" class="add-item px-3 py-2 text-xs font-bold rounded-lg bg-indigo-50 text-indigo-700" data-id="${item.id}">
                        Add
                    </button>
                </td>
            </tr>
        `).join('');

        itemResult.querySelectorAll('.add-item').forEach((button) => {
            button.addEventListener('click', () => {
                const item = list.find((candidate) => String(candidate.id) === button.dataset.id);
                if (item) {
                    addItem(item);
                }
            });
        });
    }

    function renderOrder() {
        if (orderItems.length === 0) {
            orderBody.innerHTML = '<tr><td colspan="7" class="text-center text-gray-400 font-semibold py-8">No order items yet.</td></tr>';
            return;
        }

        orderBody.innerHTML = orderItems.map((item, index) => {
            const supply = item.quantity * item.unitPrice;
            const tax = supply * TAX_RATE;
            const total = supply + tax;
            return `
                <tr>
                    <td>
                        <p class="font-bold">${text(item.name)}</p>
                        <p class="text-xs text-gray-400">${text(item.itemCode)} / ${text(item.unit)}</p>
                    </td>
                    <td><input type="number" min="1" step="1" class="form-input qty" data-index="${index}" value="${item.quantity}"></td>
                    <td><input type="number" min="0" step="0.01" class="form-input price" data-index="${index}" value="${item.unitPrice}"></td>
                    <td>${money(supply)}</td>
                    <td>${money(tax)}</td>
                    <td class="font-bold">${money(total)}</td>
                    <td><button type="button" class="remove px-3 py-2 text-xs font-bold rounded-lg bg-red-50 text-red-600" data-index="${index}">Remove</button></td>
                </tr>
            `;
        }).join('');

        orderBody.querySelectorAll('.qty').forEach((input) => {
            input.addEventListener('change', () => {
                orderItems[Number(input.dataset.index)].quantity = Math.max(1, Number(input.value || 1));
                renderOrder();
            });
        });

        orderBody.querySelectorAll('.price').forEach((input) => {
            input.addEventListener('change', () => {
                orderItems[Number(input.dataset.index)].unitPrice = Math.max(0, Number(input.value || 0));
                renderOrder();
            });
        });

        orderBody.querySelectorAll('.remove').forEach((button) => {
            button.addEventListener('click', () => {
                orderItems.splice(Number(button.dataset.index), 1);
                renderOrder();
            });
        });
    }

    async function searchVendors() {
        const name = vendorNameInput.value.trim();
        if (!name) {
            vendorResult.innerHTML = '<tr><td colspan="5" class="text-center text-red-600 font-semibold py-7">Enter a vendor name first.</td></tr>';
            selectedVendor = null;
            vendorIdInput.value = '';
            selectedBox.textContent = 'No vendor selected.';
            vendorNameInput.focus();
            setMessage('Vendor name is required.', 'error');
            return;
        }

        selectedVendor = null;
        vendorIdInput.value = '';
        selectedBox.textContent = 'Choose a vendor from the search result.';
        vendorResult.innerHTML = '<tr><td colspan="5" class="text-center text-gray-400 font-semibold py-7">Loading vendors...</td></tr>';

        try {
            const vendors = await requestList(`/api/v1/inventory/vendors/search?name=${encodeURIComponent(name)}`, 'Failed to load vendors.');
            renderVendors(vendors);
        } catch (error) {
            vendorResult.innerHTML = `<tr><td colspan="5" class="text-center text-red-600 font-semibold py-7">Vendor search failed: ${text(error.message)}</td></tr>`;
        }
    }

    async function searchItems() {
        const name = itemNameInput.value.trim();
        if (!name) {
            itemResult.innerHTML = '<tr><td colspan="5" class="text-center text-red-600 font-semibold py-7">Enter an item name first.</td></tr>';
            itemNameInput.focus();
            setMessage('Item name is required.', 'error');
            return;
        }

        itemResult.innerHTML = '<tr><td colspan="5" class="text-center text-gray-400 font-semibold py-7">Loading items...</td></tr>';

        try {
            const items = await requestList(`/api/v1/inventory/items/search?name=${encodeURIComponent(name)}`, 'Failed to load items.');
            renderItems(items);
        } catch (error) {
            itemResult.innerHTML = `<tr><td colspan="5" class="text-center text-red-600 font-semibold py-7">Item search failed: ${text(error.message)}</td></tr>`;
        }
    }

    async function save() {
        if (!selectedVendor) {
            setMessage('Select a vendor first.', 'error');
            return;
        }
        if (!expectedDateInput.value) {
            setMessage('Expected date is required.', 'error');
            return;
        }
        if (orderItems.length === 0) {
            setMessage('Add at least one item.', 'error');
            return;
        }

        const payload = {
            vendorId: selectedVendor.id,
            requestedByMemberId: Number(requestedByInput.value),
            approvedByMemberId: Number(approvedBySelect.value),
            expectedDate: expectedDateInput.value,
            note: orderNoteInput.value.trim(),
            items: orderItems.map((item) => ({
                itemId: item.itemId,
                quantity: item.quantity,
                unitPrice: item.unitPrice,
                expectedDate: expectedDateInput.value,
                note: ''
            }))
        };

        const original = saveButton.innerHTML;
        saveButton.disabled = true;
        saveButton.innerHTML = 'Saving...';

        try {
            const saved = await requestData('/api/v1/inventory/purchase-orders', {
                method: 'POST',
                body: payload
            }, 'Failed to save purchase order request.');
            setMessage(`Purchase order saved: ${saved.purchaseOrderNo || '-'}`, 'ok');
            orderItems = [];
            renderOrder();
        } catch (error) {
            setMessage(`Save failed: ${error.message}`, 'error');
        } finally {
            saveButton.disabled = false;
            saveButton.innerHTML = original;
            if (window.lucide) {
                window.lucide.createIcons();
            }
        }
    }

    const currentMemberId = resolveCurrentMemberId();
    requestedByInput.value = currentMemberId;
    approvedBySelect.innerHTML = [
        { id: 1, label: 'admin' },
        { id: 2, label: 'inventory' },
        { id: 3, label: 'hr' }
    ]
        .filter((member) => String(member.id) !== String(currentMemberId))
        .map((member) => `<option value="${member.id}">${member.label}</option>`)
        .join('');

    window.ddukPurchaseRequest = { addCreatedItem: addItem };
    window.addEventListener('message', (event) => {
        if (event.origin !== window.location.origin) return;
        if (event.data?.type === 'DDUK_ITEM_CREATED' && event.data.item) {
            addItem(event.data.item);
        }
    });

    // 추천 발주 페이지에서 넘어온 파라미터 처리
    const urlParams = new URLSearchParams(window.location.search);
    const queryItemId = urlParams.get('itemId');
    const queryQty = urlParams.get('qty');
    const queryItemName = urlParams.get('itemName');
    const queryUnit = urlParams.get('unit');

    console.log('[DDUK ERP Query Auto-Fill] Detected params:', { queryItemId, queryQty, queryItemName, queryUnit });

    if (queryItemId && queryItemName) {
        console.log('[DDUK ERP Query Auto-Fill] Adding recommended item to order:', queryItemName);
        addItem({
            id: Number(queryItemId),
            name: queryItemName,
            unit: queryUnit || 'EA',
            unitPrice: 0
        });
        if (queryQty) {
            const added = orderItems.find((candidate) => candidate.itemId === Number(queryItemId));
            if (added) {
                added.quantity = Math.max(1, Number(queryQty));
                renderOrder();
            }
        }
    }

    document.getElementById('btn-vendor-search')?.addEventListener('click', searchVendors);
    document.getElementById('btn-item-search')?.addEventListener('click', searchItems);
    saveButton?.addEventListener('click', save);
    vendorNameInput?.addEventListener('keydown', (event) => {
        if (event.key === 'Enter') {
            event.preventDefault();
            searchVendors();
        }
    });
    itemNameInput?.addEventListener('keydown', (event) => {
        if (event.key === 'Enter') {
            event.preventDefault();
            searchItems();
        }
    });
});
