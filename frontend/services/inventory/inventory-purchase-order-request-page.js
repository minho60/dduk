    const inventorySession = window.ddukSession?.requireRole?.(['ADMIN', 'INVENTORY', 'HR'], { redirectToLogin: true });
    if (!inventorySession) {
        throw new Error('Inventory login required');
    }

    window.ddukAppShell.hydratePage({
        title: "발주 생성",
        description: "거래처와 품목을 입력하고 발주 요청 API로 전송합니다.",
        navItems: [
            {
                label: "발주 관리",
                children: [
                    { href: "pages/inventory/purchase-order-request.html", label: "발주 생성", current: true },
                    { href: "#", label: "발주 상태 변경" },
                    { href: "#", label: "발주서 조회" }
                ]
            },
            {
                label: "거래처 관리",
                children: [
                    { href: "/vendor-create.html", label: "거래처 등록" },
                    { href: "/vendor-list.html", label: "거래처 목록 조회" },
                    { href: "#", label: "거래처 활성/비활성 변경" },
                    { href: "#", label: "거래처별 발주 내역 조회" }
                ]
            },
            { href: "#", label: "입고 등록" }
        ],
        cards: [
            { title: "API", value: "REQUESTED", description: "기본 상태" },
            { title: "세율", value: "10%", description: "자동 계산" }
        ]
    });

    const previewSession = {
        userName: inventorySession.userName || "Inventory Manager",
        loginId: inventorySession.loginId || "inventory",
        role: inventorySession.role || "INVENTORY",
        memberId: (() => {
            const loginId = (localStorage.getItem("loginId") || "").toLowerCase();
            const role = (localStorage.getItem("role") || "").toUpperCase();
            if (loginId === "inventory" || role === "INVENTORY") return "2";
            if (loginId === "hr" || role === "HR") return "3";
            if (loginId === "admin" || role === "ADMIN") return "1";
            return localStorage.getItem("memberId") || localStorage.getItem("userId") || "1";
        })()
    };
    document.querySelector("[data-user-name]").textContent = previewSession.userName;
    document.querySelector("[data-login-id]").textContent = previewSession.loginId;
    document.querySelector("[data-role]").textContent = previewSession.role;
    document.querySelector("[data-action='logout']").addEventListener("click", () => {
        localStorage.removeItem("token");
        localStorage.removeItem("memberId");
        localStorage.removeItem("userId");
        localStorage.removeItem("loginId");
        localStorage.removeItem("role");
        localStorage.removeItem("userName");
        location.reload();
    });

    const TAX_RATE = 0.1;
    const form = document.getElementById("orderForm");
    const itemsBody = document.getElementById("itemsBody");
    const template = document.getElementById("itemRowTemplate");
    const message = document.getElementById("message");
    const expectedDateInput = document.getElementById("expectedDate");
    const vendorResultBody = document.getElementById("vendorResultBody");
    const selectedVendor = document.getElementById("selectedVendor");
    const vendorSearchFields = [
        ["name", "vendorSearchName"],
        ["representativeName", "vendorSearchRepresentativeName"],
        ["contactPhone", "vendorSearchContactPhone"],
        ["businessRegistrationNo", "vendorSearchBusinessRegistrationNo"]
    ];
    let activeItemRow = null;
    const itemPickerModal = document.getElementById("itemPickerModal");
    const itemSearchKeyword = document.getElementById("itemSearchKeyword");
    const itemSearchResults = document.getElementById("itemSearchResults");
    const itemCreateForm = document.getElementById("itemCreateForm");
    const newItemFields = {
        name: document.getElementById("newItemName"),
        category: document.getElementById("newItemCategory"),
        spec: document.getElementById("newItemSpec"),
        unit: document.getElementById("newItemUnit"),
        unitPrice: document.getElementById("newItemUnitPrice")
    };

    const formatMoney = (value) => new Intl.NumberFormat("ko-KR", {
        maximumFractionDigits: 2
    }).format(value);

    function text(value) {
        return value || "-";
    }

    function buildVendorSearchUrl() {
        const params = new URLSearchParams();
        vendorSearchFields.forEach(([paramName, inputId]) => {
            const value = document.getElementById(inputId).value.trim();
            if (value) {
                params.set(paramName, value);
            }
        });

        const query = params.toString();
        return `${window.location.origin}/api/v1/inventory/vendors/search${query ? `?${query}` : ""}`;
    }

    function selectVendor(vendor) {
        document.getElementById("vendorId").value = vendor.id;
        selectedVendor.className = "mt-4 rounded-xl border border-emerald-200 bg-emerald-50 px-4 py-3 text-sm font-semibold text-emerald-700";
        selectedVendor.textContent = `${text(vendor.vendorCode)} / ${text(vendor.name)} / ${text(vendor.businessRegistrationNo)}`;
    }

    function renderVendorRows(vendors) {
        if (!vendors.length) {
            vendorResultBody.innerHTML = `
                <tr>
                    <td class="px-3 py-5 text-center text-sm font-semibold text-slate-400" colspan="6">조회된 거래처가 없습니다.</td>
                </tr>
            `;
            return;
        }

        vendorResultBody.innerHTML = vendors.map((vendor) => `
            <tr class="transition hover:bg-slate-50">
                <td class="px-3 py-2 font-black text-slate-950">${text(vendor.vendorCode)}</td>
                <td class="px-3 py-2 font-bold text-slate-800">${text(vendor.name)}</td>
                <td class="px-3 py-2 text-slate-600">${text(vendor.representativeName)}</td>
                <td class="px-3 py-2 text-slate-600">${text(vendor.contactPhone)}</td>
                <td class="px-3 py-2 text-slate-600">${text(vendor.businessRegistrationNo)}</td>
                <td class="px-3 py-2 text-right">
                    <button type="button" class="select-vendor rounded-xl border border-brand-200 bg-brand-50 px-3 py-2 text-xs font-bold text-brand-600 transition hover:bg-brand-100" data-vendor-id="${vendor.id}">선택</button>
                </td>
            </tr>
        `).join("");

        vendorResultBody.querySelectorAll(".select-vendor").forEach((button) => {
            button.addEventListener("click", () => {
                const vendor = vendors.find((item) => String(item.id) === button.dataset.vendorId);
                if (vendor) {
                    selectVendor(vendor);
                }
            });
        });
    }

    async function searchVendors() {
        const token = localStorage.getItem("token");
        const headers = {};
        if (token) {
            headers.Authorization = `Bearer ${token}`;
        }

        try {
            setMessage("거래처를 조회하는 중입니다.");
            const response = await fetch(buildVendorSearchUrl(), {headers});
            const responseText = await response.text();
            if (!response.ok) {
                throw new Error(responseText || `HTTP ${response.status}`);
            }
            const vendors = responseText ? JSON.parse(responseText) : [];
            renderVendorRows(vendors);
            setMessage("거래처 조회가 완료되었습니다.", "ok");
        } catch (error) {
            renderVendorRows([]);
            setMessage(`거래처 조회 실패: ${error.message}`, "error");
        }
    }

    function buildAuthHeaders(json = false) {
        const token = localStorage.getItem("token");
        const headers = json ? {"Content-Type": "application/json"} : {};
        if (token) {
            headers.Authorization = `Bearer ${token}`;
        }
        return headers;
    }

    function applyItemToRow(row, item) {
        row.querySelector(".item-id").value = item.id || "";
        row.querySelector(".item-code").value = item.itemCode || "";
        row.querySelector(".item-name").value = item.name || "";
        if (item.unitPrice !== null && item.unitPrice !== undefined) {
            row.querySelector(".unit-price").value = item.unitPrice;
        }
        updateView();
    }

    function openItemPicker(row) {
        activeItemRow = row;
        itemSearchKeyword.value = row.querySelector(".item-name").value || "";
        itemSearchResults.textContent = "검색어를 입력한 뒤 검색하세요.";
        itemCreateForm.classList.add("hidden");
        Object.values(newItemFields).forEach((field) => {
            field.value = "";
        });
        newItemFields.name.value = itemSearchKeyword.value;
        itemPickerModal.classList.remove("hidden");
        itemSearchKeyword.focus();
    }

    function closeItemPicker() {
        itemPickerModal.classList.add("hidden");
        activeItemRow = null;
    }

    function renderItemSearchResults(items) {
        if (!items.length) {
            itemSearchResults.innerHTML = `<div class="px-2 py-1 font-semibold text-slate-400">검색 결과가 없습니다. 등록폼을 열어 새 품목을 등록하세요.</div>`;
            newItemFields.name.value = itemSearchKeyword.value.trim();
            return;
        }

        itemSearchResults.innerHTML = items.map((item) => `
            <a href="#" class="select-item flex w-full items-center justify-between rounded-lg px-2 py-2 text-left transition hover:bg-white hover:text-brand-600" data-item-id="${item.id}">
                <span class="font-bold text-slate-800">${text(item.name)}</span>
                <span class="text-slate-400">${text(item.itemCode)} / 항목번호 ${item.id}</span>
            </a>
        `).join("");

        itemSearchResults.querySelectorAll(".select-item").forEach((link) => {
            link.addEventListener("click", (event) => {
                event.preventDefault();
                const item = items.find((entry) => String(entry.id) === link.dataset.itemId);
                if (item && activeItemRow) {
                    applyItemToRow(activeItemRow, item);
                    closeItemPicker();
                }
            });
        });
    }

    async function searchItems() {
        const name = itemSearchKeyword.value.trim();
        const params = new URLSearchParams();
        if (name) {
            params.set("name", name);
        }

        try {
            setMessage("품목을 조회하는 중입니다.");
            itemSearchResults.innerHTML = `<div class="px-2 py-1 font-semibold text-slate-400">검색 중입니다.</div>`;
            const query = params.toString();
            const response = await fetch(`${window.location.origin}/api/v1/inventory/items/search${query ? `?${query}` : ""}`);
            const responseText = await response.text();
            if (!response.ok) {
                throw new Error(responseText || `HTTP ${response.status}`);
            }
            renderItemSearchResults(responseText ? JSON.parse(responseText) : []);
            setMessage("품목 조회가 완료되었습니다.", "ok");
        } catch (error) {
            itemSearchResults.innerHTML = `<div class="px-2 py-1 font-semibold text-rose-600">품목 조회 실패: ${error.message}</div>`;
            setMessage(`품목 조회 실패: ${error.message}`, "error");
        }
    }

    async function createItem() {
        const payload = {
            name: newItemFields.name.value.trim(),
            category: newItemFields.category.value.trim(),
            spec: newItemFields.spec.value.trim(),
            unit: newItemFields.unit.value.trim(),
            unitPrice: newItemFields.unitPrice.value === "" ? null : Number(newItemFields.unitPrice.value),
            vendorId: Number(document.getElementById("vendorId").value),
            registeredById: Number(previewSession.memberId)
        };
        if (!payload.vendorId || payload.vendorId < 1) {
            setMessage("거래처를 먼저 조회해서 선택해야 품목을 등록할 수 있습니다.", "error");
            return;
        }
        if (!payload.name || !payload.category || !payload.spec || !payload.unit || payload.unitPrice === null) {
            setMessage("품목명, 카테고리, 규격, 단위, 단가를 모두 입력해야 합니다.", "error");
            return;
        }

        try {
            setMessage("품목을 등록하는 중입니다.");
            const response = await fetch(`${window.location.origin}/api/v1/inventory/items`, {
                method: "POST",
                headers: {"Content-Type": "application/json"},
                body: JSON.stringify(payload)
            });
            const responseText = await response.text();
            if (!response.ok) {
                throw new Error(responseText || `HTTP ${response.status}`);
            }
            if (activeItemRow) {
                applyItemToRow(activeItemRow, JSON.parse(responseText));
            }
            closeItemPicker();
            setMessage("품목이 등록되었습니다. 같은 이름이 이미 있으면 기존 품목을 사용합니다.", "ok");
        } catch (error) {
            setMessage(`품목 등록 실패: ${error.message}`, "error");
        }
    }

    function todayPlus(days) {
        const date = new Date();
        date.setDate(date.getDate() + days);
        return date.toISOString().slice(0, 10);
    }

    function setMessage(text, type = "") {
        const base = "mt-5 min-h-11 rounded-xl border px-4 py-3 text-sm font-semibold";
        const state = type === "ok"
            ? "border-emerald-200 bg-emerald-50 text-emerald-700"
            : type === "error"
                ? "border-rose-200 bg-rose-50 text-rose-700"
                : "border-slate-200 bg-slate-50 text-slate-500";
        message.className = `${base} ${state}`;
        message.textContent = text;
    }

    function addItemRow(seed = {}) {
        const row = template.content.firstElementChild.cloneNode(true);
        row.querySelector(".item-id").value = seed.itemId || "";
        row.querySelector(".item-code").value = seed.itemCode || "";
        row.querySelector(".quantity").value = seed.quantity || 1;
        row.querySelector(".unit-price").value = seed.unitPrice || 0;
        row.querySelector(".item-date").value = seed.expectedDate || "";
        row.querySelector(".item-name").value = seed.itemName || "";
        row.querySelector(".item-note").value = seed.note || "";
        row.querySelector(".item-selected").checked = seed.selected !== false;

        row.addEventListener("input", updateView);
        row.querySelector(".open-item-picker").addEventListener("click", () => openItemPicker(row));
        row.querySelector(".remove-row").addEventListener("click", () => {
            if (itemsBody.children.length > 1) {
                row.remove();
                updateView();
            } else {
                setMessage("발주 품목은 최소 1개가 필요합니다.", "error");
            }
        });

        itemsBody.appendChild(row);
        updateView();
    }

    function collectRows() {
        return Array.from(itemsBody.querySelectorAll("tr")).map((row) => {
            const itemId = Number(row.querySelector(".item-id").value);
            const quantity = Number(row.querySelector(".quantity").value);
            const unitPrice = Number(row.querySelector(".unit-price").value);
            const expectedDate = row.querySelector(".item-date").value || null;
            const note = row.querySelector(".item-note").value.trim();
            const selected = row.querySelector(".item-selected").checked;
            const supplyAmount = quantity * unitPrice;
            const taxAmount = supplyAmount * TAX_RATE;
            const lineAmount = supplyAmount + taxAmount;

            row.querySelector(".line-supply").textContent = formatMoney(supplyAmount);
            row.querySelector(".line-tax").textContent = formatMoney(taxAmount);
            row.querySelector(".line-total").textContent = formatMoney(lineAmount);

            return {
                itemId,
                quantity,
                unitPrice,
                expectedDate,
                note,
                selected,
                supplyAmount,
                taxAmount,
                lineAmount
            };
        });
    }

    function buildPayload(onlySelected = false) {
        const rows = collectRows().filter((row) => !onlySelected || row.selected);

        return {
            vendorId: Number(document.getElementById("vendorId").value),
            approvedByMemberId: Number(document.getElementById("approvedByMemberId").value),
            expectedDate: expectedDateInput.value || null,
            note: document.getElementById("orderNote").value.trim(),
            items: rows.map((row) => ({
                itemId: row.itemId,
                quantity: row.quantity,
                unitPrice: row.unitPrice,
                expectedDate: row.expectedDate,
                note: row.note
            }))
        };
    }

    function updateSummary(rows) {
        const itemCount = rows.length;
        const totalQuantity = rows.reduce((sum, row) => sum + (row.quantity || 0), 0);
        const supplyAmount = rows.reduce((sum, row) => sum + (row.supplyAmount || 0), 0);
        const taxAmount = rows.reduce((sum, row) => sum + (row.taxAmount || 0), 0);
        const totalAmount = rows.reduce((sum, row) => sum + (row.lineAmount || 0), 0);

        document.getElementById("itemCount").textContent = itemCount;
        document.getElementById("totalQuantity").textContent = formatMoney(totalQuantity);
        document.getElementById("supplyAmount").textContent = formatMoney(supplyAmount);
        document.getElementById("taxAmount").textContent = formatMoney(taxAmount);
        document.getElementById("totalAmount").textContent = formatMoney(totalAmount);
    }

    function updateView() {
        const rows = collectRows();
        updateSummary(rows);
    }

    function validatePayload(payload) {
        if (!payload.vendorId || payload.vendorId < 1) {
            return "거래처를 조회한 뒤 선택해야 합니다.";
        }
        if (!payload.approvedByMemberId || payload.approvedByMemberId < 1) {
            return "승인 담당자를 선택해야 합니다.";
        }
        if (!payload.expectedDate) {
            return "납기 예정일을 입력해야 합니다.";
        }
        if (!payload.items.length) {
            return "발주 품목을 추가해야 합니다.";
        }
        for (const item of payload.items) {
            if (!item.itemId || item.itemId < 1) {
                return "모든 품목에 품목 ID가 필요합니다.";
            }
            if (!item.quantity || item.quantity < 1) {
                return "모든 품목 수량은 1 이상이어야 합니다.";
            }
            if (item.unitPrice < 0) {
                return "단가는 0 이상이어야 합니다.";
            }
        }
        return "";
    }

    async function submitOrder(event, onlySelected = false) {
        event.preventDefault();
        updateView();

        const payload = buildPayload(onlySelected);
        const error = validatePayload(payload);
        if (error) {
            setMessage(error, "error");
            return;
        }

        const apiBase = window.location.origin;
        const token = localStorage.getItem("token");
        const headers = {"Content-Type": "application/json"};
        if (token) {
            headers.Authorization = `Bearer ${token}`;
        }

        try {
            setMessage(onlySelected ? "선택한 품목 발주 요청을 전송하는 중입니다." : "모든 품목 발주 요청을 전송하는 중입니다.");
            const response = await fetch(`${apiBase}/api/v1/inventory/purchase-orders`, {
                method: "POST",
                headers,
                body: JSON.stringify(payload)
            });
            const responseText = await response.text();
            if (!response.ok) {
                throw new Error(responseText || `HTTP ${response.status}`);
            }
            setMessage(onlySelected ? "선택한 품목 발주 요청이 정상 처리되었습니다." : "모든 품목 발주 요청이 정상 처리되었습니다.", "ok");
        } catch (error) {
            setMessage(`요청 실패: ${error.message}`, "error");
        }
    }

    document.getElementById("closeItemPickerBtn").addEventListener("click", closeItemPicker);
    document.getElementById("itemSearchBtn").addEventListener("click", searchItems);
    document.getElementById("showItemCreateFormBtn").addEventListener("click", () => {
        itemCreateForm.classList.remove("hidden");
        if (!newItemFields.name.value.trim()) {
            newItemFields.name.value = itemSearchKeyword.value.trim();
        }
        newItemFields.name.focus();
    });
    document.getElementById("submitNewItemBtn").addEventListener("click", createItem);
    itemSearchKeyword.addEventListener("keydown", (event) => {
        if (event.key === "Enter") {
            event.preventDefault();
            searchItems();
        }
    });
    itemPickerModal.addEventListener("click", (event) => {
        if (event.target === itemPickerModal) {
            closeItemPicker();
        }
    });
    document.getElementById("addItemBtn").addEventListener("click", () => addItemRow());
    document.getElementById("vendorSearchBtn").addEventListener("click", searchVendors);
    document.getElementById("submitSelectedBtn").addEventListener("click", (event) => submitOrder(event, true));
    form.addEventListener("input", updateView);
    form.addEventListener("submit", (event) => submitOrder(event, false));

    expectedDateInput.value = todayPlus(7);
    addItemRow({quantity: 1, unitPrice: 0, expectedDate: expectedDateInput.value});
    searchVendors();
