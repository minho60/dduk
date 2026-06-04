document.addEventListener('DOMContentLoaded', () => {
    const session = window.ddukSession?.requireRole?.(['ADMIN', 'INVENTORY', 'HR'], { redirectToLogin: true });
    if (!session) return;

    if (window.lucide) {
        window.lucide.createIcons();
    }

    const apiBase = '/api/v1/inventory/vendors';
    const list = document.getElementById('vendor-list');
    const listStatus = document.getElementById('list-status');
    const detailStatus = document.getElementById('detail-status');
    const form = document.getElementById('detail-form');
    const editButton = document.getElementById('btn-edit');
    const cancelButton = document.getElementById('btn-cancel');
    const saveButton = document.getElementById('btn-save');
    const fields = [
        'vendorCode', 'status', 'name', 'representativeName', 'businessRegistrationNo',
        'businessType', 'businessItem', 'contactName', 'contactPhone', 'email',
        'address', 'bankName', 'bankAccountNo', 'bankAccountHolder', 'memo'
    ];

    let vendors = [];
    let current = null;

    function text(value) {
        return value == null || value === '' ? '-' : value;
    }

    async function requestList(path) {
        return window.ddukApi.requestList(path, { method: 'GET' });
    }

    async function requestData(path, options) {
        return window.ddukApi.requestData(path, options);
    }

    function renderList(emptyText = 'Click search to load vendors.') {
        if (!vendors.length) {
            list.innerHTML = `<tr><td colspan="7" class="text-center text-gray-400 font-semibold py-8">${emptyText}</td></tr>`;
            return;
        }

        list.innerHTML = vendors.map((vendor) => `
            <tr data-id="${vendor.id}" class="${current && current.id === vendor.id ? 'selected' : ''}">
                <td class="font-bold">${text(vendor.vendorCode)}</td>
                <td>${text(vendor.name)}</td>
                <td>${text(vendor.representativeName)}</td>
                <td>${text(vendor.businessRegistrationNo)}</td>
                <td>${text(vendor.contactName)}</td>
                <td>${text(vendor.contactPhone)}</td>
                <td>${text(vendor.status)}</td>
            </tr>
        `).join('');

        list.querySelectorAll('tr[data-id]').forEach((row) => {
            row.addEventListener('click', () => loadVendor(row.dataset.id));
        });
        listStatus.textContent = `${vendors.length} vendor(s) loaded.`;
    }

    function setMode(editMode) {
        form.querySelectorAll('input,textarea').forEach((input) => {
            input.disabled = input.dataset.immutable !== undefined || ['vendorCode', 'status'].includes(input.id) || !editMode;
        });
        editButton.classList.toggle('hidden', editMode || !current);
        cancelButton.classList.toggle('hidden', !editMode);
        saveButton.classList.toggle('hidden', !editMode);
    }

    function fill(vendor) {
        current = vendor;
        fields.forEach((field) => {
            document.getElementById(field).value = vendor[field] || '';
        });
        form.classList.remove('hidden');
        detailStatus.textContent = `Loaded ${text(vendor.vendorCode)}.`;
        setMode(false);
        renderList();
    }

    function buildSearchPath() {
        const keyword = document.getElementById('search-keyword').value.trim();
        return keyword
            ? `${apiBase}/search?keyword=${encodeURIComponent(keyword)}`
            : `${apiBase}/search`;
    }

    async function search() {
        const keyword = document.getElementById('search-keyword').value.trim();
        listStatus.textContent = 'Loading vendors...';
        try {
            const path = keyword 
                ? `${apiBase}/search?keyword=${encodeURIComponent(keyword)}`
                : apiBase;
            vendors = await requestList(path);
            renderList('No vendors found.');
        } catch (error) {
            vendors = [];
            renderList('Search failed.');
            listStatus.textContent = `Vendor search failed: ${error.message}`;
        }
    }

    async function loadVendor(id) {
        try {
            const vendor = await requestData(`${apiBase}/${encodeURIComponent(id)}`, { method: 'GET' });
            fill(vendor);
        } catch (error) {
            detailStatus.textContent = `Load failed: ${error.message}`;
        }
    }

    async function save() {
        if (!form.checkValidity()) {
            form.reportValidity();
            return;
        }

        const payload = {
            businessRegistrationNo: document.getElementById('businessRegistrationNo').value.trim(),
            name: document.getElementById('name').value.trim(),
            representativeName: document.getElementById('representativeName').value.trim(),
            businessType: document.getElementById('businessType').value.trim(),
            businessItem: document.getElementById('businessItem').value.trim(),
            contactName: document.getElementById('contactName').value.trim(),
            contactPhone: document.getElementById('contactPhone').value.trim(),
            email: document.getElementById('email').value.trim(),
            address: document.getElementById('address').value.trim(),
            bankName: document.getElementById('bankName').value.trim(),
            bankAccountNo: document.getElementById('bankAccountNo').value.trim(),
            bankAccountHolder: document.getElementById('bankAccountHolder').value.trim(),
            memo: document.getElementById('memo').value.trim()
        };

        try {
            const vendor = await requestData(`${apiBase}/${current.id}`, {
                method: 'PATCH',
                body: payload
            });
            vendors = vendors.map((candidate) => (candidate.id === vendor.id ? vendor : candidate));
            fill(vendor);
            detailStatus.textContent = 'Vendor updated.';
        } catch (error) {
            detailStatus.textContent = `Update failed: ${error.message}`;
        }
    }

    async function loadAllVendors() {
        listStatus.textContent = 'Loading all vendors...';
        try {
            vendors = await requestList(apiBase);
            renderList('No vendors found.');
        } catch (error) {
            vendors = [];
            renderList('Failed to load vendors.');
            listStatus.textContent = `Failed to load all vendors: ${error.message}`;
        }
    }

    function clear() {
        document.getElementById('search-keyword').value = '';
        current = null;
        form.classList.add('hidden');
        editButton.classList.add('hidden');
        cancelButton.classList.add('hidden');
        saveButton.classList.add('hidden');
        detailStatus.textContent = 'Select a vendor from the list.';
        loadAllVendors();
    }

    document.getElementById('btn-search')?.addEventListener('click', search);
    document.getElementById('btn-reset')?.addEventListener('click', clear);
    document.getElementById('search-keyword')?.addEventListener('keydown', (event) => {
        if (event.key === 'Enter') {
            search();
        }
    });
    editButton?.addEventListener('click', () => setMode(true));
    cancelButton?.addEventListener('click', () => fill(current));
    saveButton?.addEventListener('click', save);

    const initialVendorId = new URLSearchParams(window.location.search).get('vendorId');
    if (initialVendorId) {
        loadAllVendors().then(() => {
            loadVendor(initialVendorId);
        });
    } else {
        loadAllVendors();
    }
});
