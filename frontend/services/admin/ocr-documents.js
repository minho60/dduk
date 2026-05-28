(function () {
    function getApiBaseUrl() {
        return window.ddukSession.getApiBaseUrl();
    }

    function getAuthHeaders(extraHeaders) {
        return window.ddukSession.getAuthHeaders(extraHeaders || {});
    }

    function formatDateTime(value) {
        if (!value) {
            return "-";
        }
        const date = new Date(value);
        if (Number.isNaN(date.getTime())) {
            return value;
        }
        return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, "0")}-${String(date.getDate()).padStart(2, "0")} ${String(date.getHours()).padStart(2, "0")}:${String(date.getMinutes()).padStart(2, "0")}`;
    }

    function formatCurrency(value) {
        if (value === null || value === undefined || value === "") {
            return "-";
        }
        const numericValue = Number(value);
        if (Number.isNaN(numericValue)) {
            return value;
        }
        return new Intl.NumberFormat("ko-KR", {
            style: "currency",
            currency: "KRW",
            maximumFractionDigits: 0
        }).format(numericValue);
    }

    function formatCurrencyWithoutSymbol(value) {
        if (value === null || value === undefined || value === "") {
            return "-";
        }
        const numericValue = Number(value);
        if (Number.isNaN(numericValue)) {
            return value;
        }
        return new Intl.NumberFormat("ko-KR", {
            maximumFractionDigits: 0
        }).format(numericValue);
    }

    function setMessage(element, text, type) {
        if (!element) {
            return;
        }
        if (!text) {
            element.className = "hidden";
            element.textContent = "";
            return;
        }
        const palette = type === "error"
            ? "border-red-200 bg-red-50 text-red-700"
            : "border-emerald-200 bg-emerald-50 text-emerald-700";
        element.className = `mt-5 rounded-2xl border px-4 py-3 text-sm font-medium ${palette}`;
        element.textContent = text;
    }

    const documentTypeMap = {
        "RECEIPT": "영수증",
        "INVOICE": "세금계산서/송장",
        "PURCHASE_ORDER": "발주서",
        "STATEMENT": "거래명세서"
    };

    const processingStatusMap = {
        "UPLOADED": "업로드됨",
        "PROCESSING": "분석 중",
        "PARSED": "분석 완료",
        "FAILED": "분석 실패"
    };

    const reviewStatusMap = {
        "PENDING": "검수 대기",
        "APPROVED": "승인 완료",
        "REJECTED": "반려됨"
    };

    const linkStatusMap = {
        "UNLINKED": "연결 대기",
        "LINKED": "연결 완료",
        "UNLINKED_BY_DELETE": "연결 삭제됨"
    };

    function translateDocumentType(val) {
        return documentTypeMap[val] || val || "-";
    }

    function translateProcessingStatus(val) {
        return processingStatusMap[val] || val || "-";
    }

    function translateReviewStatus(val) {
        return reviewStatusMap[val] || val || "-";
    }

    function translateLinkStatus(val) {
        return linkStatusMap[val] || val || "-";
    }

    function translateLinkedDomainType(val) {
        if (!val) return "";
        switch (val) {
            case "PURCHASE_ORDER": return "구매 발주";
            case "EXPENSE": return "비용";
            case "VOUCHER": return "전표";
            default: return val;
        }
    }

    function badgeClass(value) {
        switch (value) {
            case "APPROVED":
                return "bg-emerald-100 text-emerald-700";
            case "REJECTED":
            case "FAILED":
                return "bg-rose-100 text-rose-700";
            case "PARSED":
                return "bg-sky-100 text-sky-700";
            case "LINKED":
                return "bg-indigo-100 text-indigo-700";
            default:
                return "bg-slate-200 text-slate-700";
        }
    }

    async function parseApiResponse(response, fallbackMessage) {
        const text = await response.text();
        let payload = null;

        if (text) {
            try {
                payload = JSON.parse(text);
            } catch (error) {
                throw new Error(fallbackMessage);
            }
        }

        if (!response.ok || !payload || payload.status !== "success") {
            throw new Error(payload && payload.message ? payload.message : fallbackMessage);
        }

        return payload.data;
    }

    async function parseJsonListResponse(response, fallbackMessage) {
        const text = await response.text();
        if (!response.ok) {
            throw new Error(fallbackMessage);
        }
        if (!text) {
            return [];
        }
        try {
            const payload = JSON.parse(text);
            return Array.isArray(payload) ? payload : [];
        } catch (error) {
            throw new Error(fallbackMessage);
        }
    }

    function safeParseJson(text) {
        if (!text || !text.trim()) {
            return null;
        }
        try {
            return JSON.parse(text);
        } catch (error) {
            return null;
        }
    }

    function buildPurchaseItemDraft(detail) {
        const payload = safeParseJson(detail.reviewedResult) || safeParseJson(detail.rawOcrResult);
        const items = payload && Array.isArray(payload.items) ? payload.items : [];
        const defaultExpectedDate = detail.extractedDate || "";

        return items.map(function (item) {
            return {
                itemId: null,
                itemName: item.name || item.itemName || "",
                quantity: item.quantity || 1,
                unitPrice: item.unitPrice || item.amount || 0,
                expectedDate: item.expectedDate || defaultExpectedDate || null,
                note: "OCR 초안"
            };
        });
    }

    function buildExpenseDraft(detail) {
        const payload = safeParseJson(detail.reviewedResult) || safeParseJson(detail.rawOcrResult) || {};
        return {
            employeeId: payload.employeeId || "",
            expenseDate: detail.extractedDate || payload.transactionDate || payload.expenseDate || "",
            category: payload.category || "",
            amount: detail.extractedAmount || payload.totalAmount || payload.amount || "",
            description: payload.description || payload.memo || `OCR 문서 ${detail.id} (${detail.originalFilename}) 비용 등록`,
            status: payload.status || "PENDING"
        };
    }

    function buildVoucherDraft(detail) {
        const payload = safeParseJson(detail.reviewedResult) || safeParseJson(detail.rawOcrResult) || {};
        const amount = detail.extractedAmount || payload.totalAmount || payload.amount || "";

        return {
            voucherDate: detail.extractedDate || payload.transactionDate || payload.voucherDate || "",
            voucherType: payload.voucherType || "PURCHASE",
            vatType: payload.vatType || "TAX_INVOICE",
            vendorId: payload.vendorId || "",
            vendorNameSnapshot: detail.extractedVendor || payload.vendorNameSnapshot || payload.vendorName || "",
            description: payload.description || payload.memo || `OCR 문서 ${detail.id} (${detail.originalFilename}) 전표 생성`,
            supplyAmount: payload.supplyAmount || amount || "",
            vatAmount: payload.vatAmount || "",
            feeAmount: payload.feeAmount || 0,
            businessAccountId: payload.businessAccountId || "",
            settlementAccountId: payload.settlementAccountId || ""
        };
    }

    function buildReviewDraft(detail) {
        const payload = safeParseJson(detail.reviewedResult) || safeParseJson(detail.rawOcrResult) || {};
        const resolvedAmount = detail.extractedAmount !== null && detail.extractedAmount !== undefined && detail.extractedAmount !== ""
            ? detail.extractedAmount
            : (payload.totalAmount !== undefined ? payload.totalAmount : payload.amount);

        return {
            documentType: payload.documentType || detail.documentType || "RECEIPT",
            vendorName: payload.vendorName || detail.extractedVendor || "",
            transactionDate: payload.transactionDate || detail.extractedDate || "",
            totalAmount: resolvedAmount ?? "",
            currency: payload.currency || "KRW",
            items: Array.isArray(payload.items) ? payload.items : [],
            confidence: payload.confidence ?? null,
            notes: payload.notes || ""
        };
    }

    function initUploadPage() {
        const form = document.getElementById("ocrUploadForm");
        if (!form) {
            return;
        }

        const message = document.getElementById("uploadMessage");
        const submitButton = document.getElementById("uploadSubmitBtn");
        const resultBox = document.getElementById("uploadResult");

        if (!window.ddukSession.requireRole(["ADMIN", "HR", "INVENTORY"])) {
            return;
        }

        form.addEventListener("submit", async function (event) {
            event.preventDefault();

            const fileInput = document.getElementById("ocrFile");
            const documentType = document.getElementById("documentType").value;

            if (!fileInput.files || fileInput.files.length === 0) {
                setMessage(message, "업로드할 파일을 선택해.", "error");
                return;
            }

            const formData = new FormData();
            formData.append("file", fileInput.files[0]);
            formData.append("documentType", documentType);

            submitButton.disabled = true;
            submitButton.textContent = "업로드 중...";
            setMessage(message, "", "");
            resultBox.classList.add("hidden");

            try {
                const response = await fetch(`${getApiBaseUrl()}/api/v1/ai/ocr/documents`, {
                    method: "POST",
                    headers: getAuthHeaders(),
                    body: formData
                });

                const data = await parseApiResponse(response, "OCR 업로드에 실패했어.");
                document.getElementById("resultDocumentId").textContent = data.id;
                document.getElementById("resultFilename").textContent = data.originalFilename;
                document.getElementById("resultStatus").textContent = data.processingStatus;
                resultBox.classList.remove("hidden");
                setMessage(message, data.message || "OCR 문서를 등록했어.", "success");
                form.reset();
            } catch (error) {
                setMessage(message, error.message, "error");
            } finally {
                submitButton.disabled = false;
                submitButton.textContent = "업로드";
            }
        });
    }

    function initDocumentBoxPage() {
        const tableBody = document.getElementById("ocrTableBody");
        if (!tableBody) {
            return;
        }

        if (!window.ddukSession.requireRole(["ADMIN"])) {
            return;
        }

        const message = document.getElementById("ocrBoxMessage");
        const refreshButton = document.getElementById("refreshOcrDocumentsBtn");
        const keywordInput = document.getElementById("ocrKeyword");
        const statusFilter = document.getElementById("ocrStatusFilter");
        const reviewFilter = document.getElementById("ocrReviewFilter");
        const linkFilter = document.getElementById("ocrLinkFilter");
        const typeFilter = document.getElementById("ocrTypeFilter");
        const previewImage = document.getElementById("ocrPreviewImage");
        const previewFallback = document.getElementById("ocrPreviewFallback");
        const previewHint = document.getElementById("ocrPreviewHint");
        const rawResult = document.getElementById("ocrRawResult");
        const detailSummary = document.getElementById("ocrDetailSummary");
        const retryButton = document.getElementById("retryOcrBtn");
        const unlinkButton = document.getElementById("unlinkOcrBtn");
        const deleteButton = document.getElementById("deleteOcrBtn");
        const reviewedResultInput = document.getElementById("ocrReviewedResultInput");
        const reviewVendorName = document.getElementById("reviewVendorName");
        const reviewTransactionDate = document.getElementById("reviewTransactionDate");
        const reviewTotalAmount = document.getElementById("reviewTotalAmount");
        const reviewCurrency = document.getElementById("reviewCurrency");
        const reviewNotes = document.getElementById("reviewNotes");
        const reviewStatusBadge = document.getElementById("ocrReviewStatusBadge");
        const approveButton = document.getElementById("approveOcrBtn");
        const rejectButton = document.getElementById("rejectOcrBtn");
        const purchaseVendorId = document.getElementById("purchaseVendorId");
        const purchaseExpectedDate = document.getElementById("purchaseExpectedDate");
        const purchaseNote = document.getElementById("purchaseNote");
        const purchaseItemsJson = document.getElementById("purchaseItemsJson");
        const linkPurchaseOrderButton = document.getElementById("linkPurchaseOrderBtn");
        const vendorSearchKeyword = document.getElementById("vendorSearchKeyword");
        const searchVendorButton = document.getElementById("searchVendorBtn");
        const vendorSearchResults = document.getElementById("vendorSearchResults");
        const itemSearchKeyword = document.getElementById("itemSearchKeyword");
        const searchItemButton = document.getElementById("searchItemBtn");
        const itemSearchResults = document.getElementById("itemSearchResults");
        const expenseEmployeeId = document.getElementById("expenseEmployeeId");
        const expenseDate = document.getElementById("expenseDate");
        const expenseCategory = document.getElementById("expenseCategory");
        const expenseAmount = document.getElementById("expenseAmount");
        const expenseDescription = document.getElementById("expenseDescription");
        const expenseStatus = document.getElementById("expenseStatus");
        const linkExpenseButton = document.getElementById("linkExpenseBtn");
        const voucherDate = document.getElementById("voucherDate");
        const voucherType = document.getElementById("voucherType");
        const voucherVatType = document.getElementById("voucherVatType");
        const voucherVendorId = document.getElementById("voucherVendorId");
        const voucherVendorName = document.getElementById("voucherVendorName");
        const voucherDescription = document.getElementById("voucherDescription");
        const voucherSupplyAmount = document.getElementById("voucherSupplyAmount");
        const voucherVatAmount = document.getElementById("voucherVatAmount");
        const voucherFeeAmount = document.getElementById("voucherFeeAmount");
        const voucherBusinessAccountId = document.getElementById("voucherBusinessAccountId");
        const voucherSettlementAccountId = document.getElementById("voucherSettlementAccountId");
        const voucherBusinessAccountKeyword = document.getElementById("voucherBusinessAccountKeyword");
        const voucherSettlementAccountKeyword = document.getElementById("voucherSettlementAccountKeyword");
        const searchBusinessAccountBtn = document.getElementById("searchBusinessAccountBtn");
        const searchSettlementAccountBtn = document.getElementById("searchSettlementAccountBtn");
        const businessAccountResults = document.getElementById("businessAccountResults");
        const settlementAccountResults = document.getElementById("settlementAccountResults");
        const linkVoucherButton = document.getElementById("linkVoucherBtn");
        let selectedDocument = null;

        async function loadDocuments() {
            tableBody.innerHTML = `
                <tr>
                    <td colspan="9" class="px-6 py-10 text-center text-gray-400">OCR 문서를 불러오는 중이야.</td>
                </tr>
            `;

            try {
                const params = new URLSearchParams({
                    page: "0",
                    size: "20",
                    sort: "createdAt,desc"
                });

                if (keywordInput.value.trim()) {
                    params.set("keyword", keywordInput.value.trim());
                }
                if (statusFilter.value) {
                    params.set("processingStatus", statusFilter.value);
                }
                if (reviewFilter.value) {
                    params.set("reviewStatus", reviewFilter.value);
                }
                if (linkFilter.value) {
                    params.set("linkStatus", linkFilter.value);
                }
                if (typeFilter.value) {
                    params.set("documentType", typeFilter.value);
                }

                const response = await fetch(`${getApiBaseUrl()}/api/v1/admin/ocr-documents?${params.toString()}`, {
                    headers: getAuthHeaders({ "Content-Type": "application/json" })
                });
                const pageData = await parseApiResponse(response, "OCR 문서 목록을 불러오지 못했어.");
                renderDocuments(pageData.content || []);
            } catch (error) {
                setMessage(message, error.message, "error");
                tableBody.innerHTML = `
                    <tr>
                        <td colspan="9" class="px-6 py-10 text-center text-red-500">OCR 문서 목록을 불러오지 못했어.</td>
                    </tr>
                `;
            }
        }

        function renderDocuments(documents) {
            if (!documents.length) {
                tableBody.innerHTML = `
                    <tr>
                        <td colspan="9" class="px-6 py-10 text-center text-gray-400">조건에 맞는 OCR 문서가 없어.</td>
                    </tr>
                `;
                return;
            }

            tableBody.innerHTML = documents.map(function (document) {
                const linkLabel = document.linkedDomainType
                    ? `${translateLinkStatus(document.linkStatus)} / ${translateLinkedDomainType(document.linkedDomainType)}${document.linkedDomainId ? ` #${document.linkedDomainId}` : ""}`
                    : translateLinkStatus(document.linkStatus);

                const isSelected = selectedDocument && selectedDocument.id === document.id;
                const rowClass = isSelected 
                    ? "border-b border-indigo-100 bg-indigo-50/40 hover:bg-indigo-50/60 text-center font-medium" 
                    : "border-b border-gray-100 hover:bg-gray-50/60 text-center";

                return `
                    <tr class="${rowClass}" data-row-id="${document.id}">
                        <td class="px-6 py-4 text-sm font-semibold text-gray-900 text-center">${document.id}</td>
                        <td class="px-6 py-4 text-sm text-gray-700 text-center">${document.originalFilename}</td>
                        <td class="px-6 py-4 text-sm text-gray-600 text-center">${translateDocumentType(document.documentType)}</td>
                        <td class="px-6 py-4 text-sm font-medium text-gray-800 text-center">${translateProcessingStatus(document.processingStatus)}</td>
                        <td class="px-6 py-4 text-sm text-center"><span class="rounded-full px-2.5 py-1 text-xs font-semibold ${badgeClass(document.reviewStatus)}">${translateReviewStatus(document.reviewStatus)}</span></td>
                        <td class="px-6 py-4 text-sm text-center"><span class="rounded-full px-2.5 py-1 text-xs font-semibold ${badgeClass(document.linkStatus)}">${linkLabel}</span></td>
                        <td class="px-6 py-4 text-sm text-gray-600 text-center">${document.extractedVendor || "-"}</td>
                        <td class="px-6 py-4 text-sm text-gray-600 text-center">${formatCurrency(document.extractedAmount)}</td>
                        <td class="px-6 py-4 text-center">
                            <button type="button" class="view-ocr-detail rounded-lg border border-indigo-100 bg-indigo-50 px-3 py-1.5 text-xs font-semibold text-indigo-600 hover:bg-indigo-100" data-id="${document.id}">
                                상세 보기
                            </button>
                        </td>
                    </tr>
                `;
            }).join("");

            tableBody.querySelectorAll(".view-ocr-detail").forEach(function (button) {
                button.addEventListener("click", function () {
                    openDocumentDetail(button.dataset.id);
                });
            });
        }

        async function openDocumentDetail(id) {
            try {
                const detailResponse = await fetch(`${getApiBaseUrl()}/api/v1/admin/ocr-documents/${id}`, {
                    headers: getAuthHeaders({ "Content-Type": "application/json" })
                });
                const detail = await parseApiResponse(detailResponse, "OCR 문서 상세를 불러오지 못했어.");
                selectedDocument = detail;

                // 테이블 행 하이라이트 실시간 갱신
                tableBody.querySelectorAll("tr[data-row-id]").forEach(function (row) {
                    if (Number(row.dataset.rowId) === Number(id)) {
                        row.className = "border-b border-indigo-100 bg-indigo-50/40 hover:bg-indigo-50/60 text-center font-medium";
                    } else {
                        row.className = "border-b border-gray-100 hover:bg-gray-50/60 text-center";
                    }
                });

                const linkLabel = detail.linkedDomainType
                    ? `${translateLinkStatus(detail.linkStatus)} / ${translateLinkedDomainType(detail.linkedDomainType)}${detail.linkedDomainId ? ` #${detail.linkedDomainId}` : ""}`
                    : translateLinkStatus(detail.linkStatus);

                detailSummary.innerHTML = `
                    <div class="grid grid-cols-1 gap-3 md:grid-cols-2">
                        <div><span class="font-semibold text-gray-500">문서 ID</span><div class="mt-1 text-sm text-gray-900">${detail.id}</div></div>
                        <div><span class="font-semibold text-gray-500">처리 상태</span><div class="mt-1 text-sm text-gray-900">${translateProcessingStatus(detail.processingStatus)}</div></div>
                        <div><span class="font-semibold text-gray-500">문서 유형</span><div class="mt-1 text-sm text-gray-900">${translateDocumentType(detail.documentType)}</div></div>
                        <div><span class="font-semibold text-gray-500">업로드 시각</span><div class="mt-1 text-sm text-gray-900">${formatDateTime(detail.createdAt)}</div></div>
                        <div><span class="font-semibold text-gray-500">검수 상태</span><div class="mt-1 text-sm text-gray-900">${translateReviewStatus(detail.reviewStatus)}</div></div>
                        <div><span class="font-semibold text-gray-500">링크 상태</span><div class="mt-1 text-sm text-gray-900">${linkLabel}</div></div>
                        <div><span class="font-semibold text-gray-500">거래처</span><div class="mt-1 text-sm text-gray-900">${detail.extractedVendor || "-"}</div></div>
                        <div><span class="font-semibold text-gray-500">금액</span><div class="mt-1 text-sm text-gray-900">${formatCurrency(detail.extractedAmount)}</div></div>
                        <div><span class="font-semibold text-gray-500">링크 시각</span><div class="mt-1 text-sm text-gray-900">${formatDateTime(detail.linkedAt)}</div></div>
                        <div><span class="font-semibold text-gray-500">링크 작업자</span><div class="mt-1 text-sm text-gray-900">${detail.linkedByMemberId || "-"}</div></div>
                    </div>
                `;

                rawResult.textContent = detail.rawOcrResult || detail.errorMessage || "표시할 OCR 결과가 없어.";
                reviewedResultInput.value = detail.reviewedResult || detail.rawOcrResult || "";
                reviewStatusBadge.className = `rounded-full px-3 py-1 text-xs font-semibold ${badgeClass(detail.reviewStatus)}`;
                reviewStatusBadge.textContent = translateReviewStatus(detail.reviewStatus) || "검수 대기";

                prefillReviewForm(detail);
                prefillPurchaseLinkForm(detail);
                prefillExpenseLinkForm(detail);
                prefillVoucherLinkForm(detail);
                syncReviewControls(detail);
                syncRetryControls(detail);
                syncUnlinkControls(detail);
                syncDeleteControls(detail);
                syncPurchaseLinkControls(detail);
                syncExpenseLinkControls(detail);
                syncVoucherLinkControls(detail);
                await loadPreview(detail.fileEndpoint, detail.mimeType);
            } catch (error) {
                setMessage(message, error.message, "error");
            }
        }

        function prefillReviewForm(detail) {
            const draft = buildReviewDraft(detail);
            reviewVendorName.value = draft.vendorName || "";
            reviewTransactionDate.value = draft.transactionDate || "";
            reviewTotalAmount.value = draft.totalAmount === null || draft.totalAmount === undefined ? "" : draft.totalAmount;
            reviewCurrency.value = draft.currency || "KRW";
            reviewNotes.value = draft.notes || "";
            syncReviewJsonPreview(detail);
        }

        function buildReviewPayload(detail) {
            const baseDraft = buildReviewDraft(detail);
            const amountValue = reviewTotalAmount.value.trim();
            const currencyValue = reviewCurrency.value.trim().toUpperCase();
            const notesValue = reviewNotes.value.trim();

            return {
                documentType: detail.documentType,
                vendorName: reviewVendorName.value.trim() || null,
                transactionDate: reviewTransactionDate.value || null,
                totalAmount: amountValue === "" ? null : Number(amountValue),
                currency: currencyValue || "KRW",
                items: Array.isArray(baseDraft.items) ? baseDraft.items : [],
                confidence: baseDraft.confidence ?? null,
                notes: notesValue || null
            };
        }

        function renderExcelTable(detail) {
            const container = document.getElementById("ocrExcelTableContainer");
            if (!container) return;

            const payload = buildReviewPayload(detail);
            const items = Array.isArray(payload.items) ? payload.items : [];

            let html = `
                <table class="min-w-full border-collapse border border-slate-200 text-xs bg-white">
                    <thead>
                        <tr class="bg-slate-100 text-slate-600 font-semibold border-b border-slate-200">
                            <th class="border border-slate-200 px-3 py-2.5 w-10 text-center bg-slate-50/80"></th>
                            <th class="border border-slate-200 px-3 py-2.5 text-left text-slate-700">품목명</th>
                            <th class="border border-slate-200 px-3 py-2.5 w-20 text-center text-slate-700">수량</th>
                            <th class="border border-slate-200 px-3 py-2.5 w-28 text-right text-slate-700">단가</th>
                            <th class="border border-slate-200 px-3 py-2.5 w-28 text-right text-slate-700">금액</th>
                        </tr>
                    </thead>
                    <tbody>
            `;

            if (items.length === 0) {
                html += `
                    <tr>
                        <td class="border border-slate-200 px-3 py-8 text-center text-slate-400 font-medium" colspan="5">
                            추출된 세부 품목 정보가 없습니다.
                        </td>
                    </tr>
                `;
            } else {
                items.forEach((item, idx) => {
                    const name = item.name || item.itemName || "-";
                    const qty = item.quantity ?? 1;
                    const price = item.unitPrice ?? 0;
                    const amt = item.amount ?? (qty * price);

                    html += `
                        <tr class="hover:bg-slate-50/50 transition">
                            <td class="border border-slate-200 px-3 py-2 text-center bg-slate-50/40 font-semibold text-slate-500">${idx + 1}</td>
                            <td class="border border-slate-200 px-3 py-2 text-slate-800 font-medium">${name}</td>
                            <td class="border border-slate-200 px-3 py-2 text-center text-slate-600 font-medium">${qty}</td>
                            <td class="border border-slate-200 px-3 py-2 text-right text-slate-600 font-mono">${formatCurrencyWithoutSymbol(price)}</td>
                            <td class="border border-slate-200 px-3 py-2 text-right text-slate-800 font-bold font-mono">${formatCurrencyWithoutSymbol(amt)}</td>
                        </tr>
                    `;
                });
            }

            html += `
                    </tbody>
                </table>
            `;

            // 기본 메타데이터 요약 테이블 추가
            html += `
                <div class="mt-4 border-t border-slate-200 pt-4 px-1 pb-1">
                    <span class="mb-2.5 block text-xs font-bold text-slate-600 flex items-center gap-1.5">
                      <span class="inline-block w-1.5 h-3 bg-indigo-500 rounded-sm"></span>
                      추출 기본 정보 요약
                    </span>
                    <div class="overflow-x-auto rounded-xl border border-slate-200 max-w-md bg-white">
                        <table class="min-w-full border-collapse text-xs">
                            <tbody>
                                <tr class="border-b border-slate-100">
                                    <td class="px-4 py-2 w-32 bg-slate-50/60 font-semibold text-slate-500 text-center border-r border-slate-100">거래처명</td>
                                    <td class="px-4 py-2 text-slate-800 font-medium">${payload.vendorName || "-"}</td>
                                </tr>
                                <tr class="border-b border-slate-100">
                                    <td class="px-4 py-2 bg-slate-50/60 font-semibold text-slate-500 text-center border-r border-slate-100">거래일자</td>
                                    <td class="px-4 py-2 text-slate-800 font-medium">${payload.transactionDate || "-"}</td>
                                </tr>
                                <tr class="border-b border-slate-100">
                                    <td class="px-4 py-2 bg-slate-50/60 font-semibold text-slate-500 text-center border-r border-slate-100">총 금액</td>
                                    <td class="px-4 py-2 text-slate-800 font-bold font-mono">${formatCurrency(payload.totalAmount)}</td>
                                </tr>
                                <tr>
                                    <td class="px-4 py-2 bg-slate-50/60 font-semibold text-slate-500 text-center border-r border-slate-100">통화</td>
                                    <td class="px-4 py-2 text-slate-800 font-medium">${payload.currency || "KRW"}</td>
                                </tr>
                            </tbody>
                        </table>
                    </div>
                </div>
            `;

            container.innerHTML = html;
        }

        function syncReviewJsonPreview(detail) {
            if (!detail) {
                if (reviewedResultInput) reviewedResultInput.value = "";
                const container = document.getElementById("ocrExcelTableContainer");
                if (container) container.innerHTML = '<div class="text-xs text-slate-400 p-4 text-center">문서를 선택하면 엑셀 시트 요약이 여기에 표시됩니다.</div>';
                return;
            }
            const payload = buildReviewPayload(detail);
            if (reviewedResultInput) {
                reviewedResultInput.value = JSON.stringify(payload, null, 2);
            }
            renderExcelTable(detail);
        }

        function renderPurchaseItemsTable() {
            const container = document.getElementById("purchaseItemsTableContainer");
            if (!container) return;

            let items = [];
            try {
                items = JSON.parse(purchaseItemsJson.value || "[]");
                if (!Array.isArray(items)) items = [];
            } catch (e) {
                items = [];
            }

            let html = `
                <table class="min-w-full border-collapse border border-slate-200 text-xs bg-white">
                    <thead>
                        <tr class="bg-slate-100 text-slate-600 font-semibold border-b border-slate-200">
                            <th class="border border-slate-200 px-2 py-2.5 w-10 text-center bg-slate-50/80"></th>
                            <th class="border border-slate-200 px-3 py-2.5 text-left text-slate-700">품목명</th>
                            <th class="border border-slate-200 px-3 py-2.5 w-24 text-center text-slate-700">수량</th>
                            <th class="border border-slate-200 px-3 py-2.5 w-28 text-right text-slate-700">단가</th>
                            <th class="border border-slate-200 px-3 py-2.5 w-28 text-right text-slate-700">금액</th>
                            <th class="border border-slate-200 px-3 py-2.5 w-32 text-center text-slate-700">납기일</th>
                            <th class="border border-slate-200 px-3 py-2.5 text-left text-slate-700">비고</th>
                            <th class="border border-slate-200 px-2 py-2.5 w-12 text-center text-slate-700">삭제</th>
                        </tr>
                    </thead>
                    <tbody>
            `;

            if (items.length === 0) {
                html += `
                    <tr>
                        <td class="border border-slate-200 px-3 py-8 text-center text-slate-400 font-medium" colspan="8">
                            발주 품목이 없습니다. 우측 품목 찾기나 거래처 검색으로 추가해 주세요.
                        </td>
                    </tr>
                `;
            } else {
                items.forEach((item, idx) => {
                    const name = item.itemName || "-";
                    const qty = item.quantity ?? 1;
                    const price = item.unitPrice ?? 0;
                    const amt = qty * price;
                    const expectedDate = item.expectedDate || "";
                    const note = item.note || "";

                    html += `
                        <tr class="hover:bg-slate-50/30">
                            <td class="border border-slate-200 px-2 py-1 text-center bg-slate-50/40 font-semibold text-slate-500">${idx + 1}</td>
                            <td class="border border-slate-200 px-3 py-1 font-medium text-slate-800">${name}</td>
                            <td class="border border-slate-200 px-2 py-1 text-center">
                                <input type="number" min="1" class="purchase-item-qty w-full rounded border border-slate-200 px-2 py-1 text-center font-medium focus:border-indigo-500 focus:outline-none" data-idx="${idx}" value="${qty}">
                            </td>
                            <td class="border border-slate-200 px-2 py-1 text-right">
                                <input type="number" min="0" class="purchase-item-price w-full rounded border border-slate-200 px-2 py-1 text-right font-mono focus:border-indigo-500 focus:outline-none" data-idx="${idx}" value="${price}">
                            </td>
                            <td class="border border-slate-200 px-3 py-1 text-right text-slate-800 font-bold font-mono bg-slate-50/20">${formatCurrencyWithoutSymbol(amt)}</td>
                            <td class="border border-slate-200 px-2 py-1 text-center">
                                <input type="date" class="purchase-item-date w-full rounded border border-slate-200 px-2 py-1 text-center focus:border-indigo-500 focus:outline-none" data-idx="${idx}" value="${expectedDate}">
                            </td>
                            <td class="border border-slate-200 px-2 py-1">
                                <input type="text" class="purchase-item-note w-full rounded border border-slate-200 px-2 py-1 focus:border-indigo-500 focus:outline-none" data-idx="${idx}" value="${note}">
                            </td>
                            <td class="border border-slate-200 px-2 py-1 text-center">
                                <button type="button" class="delete-purchase-item-row text-rose-500 hover:text-rose-700 transition" data-idx="${idx}">
                                    <i data-lucide="trash-2" class="h-4 w-4 mx-auto"></i>
                                </button>
                            </td>
                        </tr>
                    `;
                });
            }

            html += `
                    </tbody>
                </table>
            `;

            container.innerHTML = html;

            if (window.lucide) {
                window.lucide.createIcons({
                    attrs: {
                        class: ["lucide"]
                    },
                    nameAttr: "data-lucide",
                    node: container
                });
            }

            container.querySelectorAll(".purchase-item-qty").forEach(input => {
                input.addEventListener("change", e => {
                    const idx = Number(e.target.dataset.idx);
                    const val = Number(e.target.value) || 1;
                    updateItemInJson(idx, "quantity", val);
                });
            });

            container.querySelectorAll(".purchase-item-price").forEach(input => {
                input.addEventListener("change", e => {
                    const idx = Number(e.target.dataset.idx);
                    const val = Number(e.target.value) || 0;
                    updateItemInJson(idx, "unitPrice", val);
                });
            });

            container.querySelectorAll(".purchase-item-date").forEach(input => {
                input.addEventListener("change", e => {
                    const idx = Number(e.target.dataset.idx);
                    const val = e.target.value;
                    updateItemInJson(idx, "expectedDate", val);
                });
            });

            container.querySelectorAll(".purchase-item-note").forEach(input => {
                input.addEventListener("change", e => {
                    const idx = Number(e.target.dataset.idx);
                    const val = e.target.value;
                    updateItemInJson(idx, "note", val);
                });
            });

            container.querySelectorAll(".delete-purchase-item-row").forEach(btn => {
                btn.addEventListener("click", e => {
                    const button = e.target.closest("button");
                    const idx = Number(button.dataset.idx);
                    deleteItemFromJson(idx);
                });
            });

            if (selectedDocument) {
                const disabled = selectedDocument.reviewStatus !== "APPROVED" || selectedDocument.linkStatus === "LINKED";
                container.querySelectorAll("input, button").forEach(function (el) {
                    el.disabled = disabled;
                });
            }
        }

        function updateItemInJson(idx, field, value) {
            let items = [];
            try {
                items = JSON.parse(purchaseItemsJson.value || "[]");
            } catch (e) {
                items = [];
            }
            if (items[idx]) {
                items[idx][field] = value;
                purchaseItemsJson.value = JSON.stringify(items, null, 2);
                renderPurchaseItemsTable();
            }
        }

        function deleteItemFromJson(idx) {
            let items = [];
            try {
                items = JSON.parse(purchaseItemsJson.value || "[]");
            } catch (e) {
                items = [];
            }
            items.splice(idx, 1);
            purchaseItemsJson.value = JSON.stringify(items, null, 2);
            renderPurchaseItemsTable();
        }

        function prefillPurchaseLinkForm(detail) {
            vendorSearchKeyword.value = detail.extractedVendor || "";
            purchaseExpectedDate.value = detail.extractedDate || "";
            purchaseNote.value = detail.linkStatus === "LINKED"
                ? `OCR 문서 ${detail.id}는 이미 연결된 상태야.`
                : `OCR 문서 ${detail.id} (${detail.originalFilename}) 기반 발주`;
            purchaseItemsJson.value = JSON.stringify(buildPurchaseItemDraft(detail), null, 2);
            vendorSearchResults.innerHTML = "";
            itemSearchResults.innerHTML = "";
            renderPurchaseItemsTable();
        }

        function prefillExpenseLinkForm(detail) {
            const draft = buildExpenseDraft(detail);
            expenseEmployeeId.value = draft.employeeId;
            expenseDate.value = draft.expenseDate;
            expenseCategory.value = draft.category;
            expenseAmount.value = draft.amount;
            expenseDescription.value = draft.description;
            expenseStatus.value = draft.status;
        }

        function prefillVoucherLinkForm(detail) {
            const draft = buildVoucherDraft(detail);
            voucherDate.value = draft.voucherDate;
            voucherType.value = draft.voucherType;
            voucherVatType.value = draft.vatType;
            voucherVendorId.value = draft.vendorId;
            voucherVendorName.value = draft.vendorNameSnapshot;
            voucherDescription.value = draft.description;
            voucherSupplyAmount.value = draft.supplyAmount;
            voucherVatAmount.value = draft.vatAmount;
            voucherFeeAmount.value = draft.feeAmount;
            voucherBusinessAccountId.value = draft.businessAccountId;
            voucherSettlementAccountId.value = draft.settlementAccountId;
            voucherBusinessAccountKeyword.value = "";
            voucherSettlementAccountKeyword.value = "";
            businessAccountResults.innerHTML = "";
            settlementAccountResults.innerHTML = "";
        }

        function syncReviewControls(detail) {
            const disabled = !detail || detail.processingStatus !== "PARSED" || detail.linkStatus === "LINKED";
            approveButton.disabled = disabled;
            rejectButton.disabled = disabled;
            [reviewVendorName, reviewTransactionDate, reviewTotalAmount, reviewCurrency, reviewNotes]
                .forEach(function (element) {
                    element.disabled = disabled;
                });
        }

        function syncRetryControls(detail) {
            retryButton.disabled = !detail || detail.processingStatus !== "FAILED" || detail.linkStatus === "LINKED";
        }

        function syncDeleteControls(detail) {
            if (deleteButton) {
                deleteButton.disabled = !detail || detail.linkStatus === "LINKED";
            }
        }

        function syncUnlinkControls(detail) {
            unlinkButton.disabled = !detail || detail.linkStatus !== "LINKED";
        }

        function syncPurchaseLinkControls(detail) {
            const disabled = !detail || detail.reviewStatus !== "APPROVED" || detail.linkStatus === "LINKED";
            [
                purchaseVendorId, purchaseExpectedDate, purchaseNote, purchaseItemsJson,
                linkPurchaseOrderButton, vendorSearchKeyword, searchVendorButton, itemSearchKeyword, searchItemButton
            ].forEach(function (element) {
                element.disabled = disabled;
            });

            const tableContainer = document.getElementById("purchaseItemsTableContainer");
            if (tableContainer) {
                tableContainer.querySelectorAll("input, button").forEach(function (el) {
                    el.disabled = disabled;
                });
            }
        }

        function syncExpenseLinkControls(detail) {
            const disabled = !detail || detail.reviewStatus !== "APPROVED" || detail.linkStatus === "LINKED";
            [expenseEmployeeId, expenseDate, expenseCategory, expenseAmount, expenseDescription, expenseStatus, linkExpenseButton]
                .forEach(function (element) {
                    element.disabled = disabled;
                });
        }

        function syncVoucherLinkControls(detail) {
            const disabled = !detail || detail.reviewStatus !== "APPROVED" || detail.linkStatus === "LINKED";
            [
                voucherDate, voucherType, voucherVatType, voucherVendorId, voucherVendorName, voucherDescription,
                voucherSupplyAmount, voucherVatAmount, voucherFeeAmount, voucherBusinessAccountId, voucherSettlementAccountId,
                voucherBusinessAccountKeyword, voucherSettlementAccountKeyword, searchBusinessAccountBtn, searchSettlementAccountBtn,
                linkVoucherButton
            ].forEach(function (element) {
                element.disabled = disabled;
            });
        }

        async function searchVendors() {
            const keyword = vendorSearchKeyword.value.trim();
            if (!keyword) {
                vendorSearchResults.innerHTML = `<div class="text-xs text-slate-400">검색어를 입력해.</div>`;
                return;
            }

            vendorSearchResults.innerHTML = `<div class="text-xs text-slate-400">거래처를 찾는 중이야.</div>`;
            try {
                const response = await fetch(`${getApiBaseUrl()}/api/v1/inventory/vendors/search?keyword=${encodeURIComponent(keyword)}`, {
                    headers: getAuthHeaders({ "Content-Type": "application/json" })
                });
                const vendors = await parseJsonListResponse(response, "거래처 검색에 실패했어.");
                renderVendorSearchResults(vendors || []);
            } catch (error) {
                vendorSearchResults.innerHTML = `<div class="text-xs text-rose-500">${error.message}</div>`;
            }
        }

        function renderVendorSearchResults(vendors) {
            if (!vendors.length) {
                vendorSearchResults.innerHTML = `<div class="text-xs text-slate-400">검색 결과가 없어.</div>`;
                return;
            }

            vendorSearchResults.innerHTML = vendors.slice(0, 8).map(function (vendor) {
                return `
                    <button type="button" class="vendor-search-result flex w-full items-start justify-between rounded-2xl border border-slate-200 bg-white px-4 py-3 text-left transition hover:border-sky-300 hover:bg-sky-50" data-vendor-id="${vendor.id}" data-vendor-name="${vendor.name}">
                        <span>
                            <span class="block font-semibold text-slate-900">${vendor.name}</span>
                            <span class="mt-1 block text-xs text-slate-500">${vendor.vendorCode || "-"} / ${vendor.representativeName || "-"}</span>
                        </span>
                        <span class="text-xs font-semibold text-sky-600">ID ${vendor.id}</span>
                    </button>
                `;
            }).join("");

            vendorSearchResults.querySelectorAll(".vendor-search-result").forEach(function (button) {
                button.addEventListener("click", function () {
                    purchaseVendorId.value = button.dataset.vendorId || "";
                    voucherVendorId.value = button.dataset.vendorId || "";
                    voucherVendorName.value = button.dataset.vendorName || "";
                    vendorSearchKeyword.value = button.dataset.vendorName || "";
                    setMessage(message, `거래처 ${button.dataset.vendorName}를 선택했어.`, "success");
                });
            });
        }

        async function searchItems() {
            const keyword = itemSearchKeyword.value.trim();
            if (!keyword) {
                itemSearchResults.innerHTML = `<div class="text-xs text-slate-400">검색어를 입력해.</div>`;
                return;
            }

            itemSearchResults.innerHTML = `<div class="text-xs text-slate-400">품목을 찾는 중이야.</div>`;
            try {
                const response = await fetch(`${getApiBaseUrl()}/api/v1/inventory/items/search?name=${encodeURIComponent(keyword)}`, {
                    headers: getAuthHeaders({ "Content-Type": "application/json" })
                });
                const items = await parseJsonListResponse(response, "품목 검색에 실패했어.");
                renderItemSearchResults(items || []);
            } catch (error) {
                itemSearchResults.innerHTML = `<div class="text-xs text-rose-500">${error.message}</div>`;
            }
        }

        function renderItemSearchResults(items) {
            if (!items.length) {
                itemSearchResults.innerHTML = `<div class="text-xs text-slate-400">검색 결과가 없어.</div>`;
                return;
            }

            itemSearchResults.innerHTML = items.slice(0, 10).map(function (item) {
                return `
                    <button type="button" class="item-search-result flex w-full items-start justify-between rounded-2xl border border-slate-200 bg-white px-4 py-3 text-left transition hover:border-sky-300 hover:bg-sky-50"
                        data-item-id="${item.id}"
                        data-item-name="${item.name || ""}"
                        data-unit-price="${item.unitPrice || 0}">
                        <span>
                            <span class="block font-semibold text-slate-900">${item.name}</span>
                            <span class="mt-1 block text-xs text-slate-500">${item.category || "-"} / ${item.spec || "-"} / ${item.unit || "-"}</span>
                        </span>
                        <span class="text-xs font-semibold text-sky-600">ID ${item.id}</span>
                    </button>
                `;
            }).join("");

            itemSearchResults.querySelectorAll(".item-search-result").forEach(function (button) {
                button.addEventListener("click", function () {
                    let itemsDraft;
                    try {
                        itemsDraft = JSON.parse(purchaseItemsJson.value || "[]");
                        if (!Array.isArray(itemsDraft)) {
                            itemsDraft = [];
                        }
                    } catch (error) {
                        itemsDraft = [];
                    }
                    itemsDraft.push({
                        itemId: Number(button.dataset.itemId),
                        itemName: button.dataset.itemName || "",
                        quantity: 1,
                        unitPrice: button.dataset.unitPrice ? Number(button.dataset.unitPrice) : 0,
                        expectedDate: purchaseExpectedDate.value || null,
                        note: "품목 검색으로 추가"
                    });
                    purchaseItemsJson.value = JSON.stringify(itemsDraft, null, 2);
                    itemSearchKeyword.value = button.dataset.itemName || "";
                    setMessage(message, `품목 ${button.dataset.itemName}를 발주 초안에 추가했어.`, "success");
                    renderPurchaseItemsTable();
                });
            });
        }

        function validateReviewForm(detail) {
            if (!detail) {
                return "Select an OCR document first.";
            }

            if (reviewTransactionDate.value && Number.isNaN(new Date(reviewTransactionDate.value).getTime())) {
                return "Transaction date is invalid.";
            }

            if (reviewTotalAmount.value.trim() !== "" && Number.isNaN(Number(reviewTotalAmount.value.trim()))) {
                return "Total amount must be numeric.";
            }

            if (!reviewCurrency.value.trim()) {
                return "Currency is required.";
            }

            return null;
        }

        async function searchAccounts(keyword, type, cashOnly, resultContainer, onSelect) {
            if (!keyword.trim()) {
                resultContainer.innerHTML = `<div class="text-xs text-slate-400">검색어를 입력해.</div>`;
                return;
            }

            resultContainer.innerHTML = `<div class="text-xs text-slate-400">계정을 찾는 중이야.</div>`;
            try {
                const params = new URLSearchParams();
                params.set("keyword", keyword.trim());
                if (type) {
                    params.set("type", type);
                }
                params.set("cashOnly", cashOnly ? "true" : "false");

                const response = await fetch(`${getApiBaseUrl()}/api/v1/accounting/vouchers/accounts/search?${params.toString()}`, {
                    headers: getAuthHeaders({ "Content-Type": "application/json" })
                });
                const accounts = await parseApiResponse(response, "계정 검색에 실패했어.");
                if (!accounts.length) {
                    resultContainer.innerHTML = `<div class="text-xs text-slate-400">검색 결과가 없어.</div>`;
                    return;
                }
                resultContainer.innerHTML = accounts.slice(0, 10).map(function (account) {
                    return `
                        <button type="button" class="account-search-result flex w-full items-start justify-between rounded-2xl border border-slate-200 bg-white px-4 py-3 text-left transition hover:border-sky-300 hover:bg-sky-50"
                            data-account-id="${account.id}"
                            data-account-code="${account.code}"
                            data-account-name="${account.name}">
                            <span>
                                <span class="block font-semibold text-slate-900">${account.name}</span>
                                <span class="mt-1 block text-xs text-slate-500">${account.code} / ${account.type}</span>
                            </span>
                            <span class="text-xs font-semibold text-sky-600">ID ${account.id}</span>
                        </button>
                    `;
                }).join("");
                resultContainer.querySelectorAll(".account-search-result").forEach(function (button) {
                    button.addEventListener("click", function () {
                        onSelect(button.dataset);
                    });
                });
            } catch (error) {
                resultContainer.innerHTML = `<div class="text-xs text-rose-500">${error.message}</div>`;
            }
        }

        function updateVoucherAccountSelections() {
            searchBusinessAccountBtn.addEventListener("click", function () {
                const type = voucherType.value === "SALES" ? "REVENUE" : "EXPENSE";
                searchAccounts(voucherBusinessAccountKeyword.value, type, false, businessAccountResults, function (dataset) {
                    voucherBusinessAccountId.value = dataset.accountId || "";
                    voucherBusinessAccountKeyword.value = `${dataset.accountCode} ${dataset.accountName}`.trim();
                    setMessage(message, `사업 계정 ${dataset.accountName}를 선택했어.`, "success");
                });
            });
            searchSettlementAccountBtn.addEventListener("click", function () {
                searchAccounts(voucherSettlementAccountKeyword.value, "ASSET", true, settlementAccountResults, function (dataset) {
                    voucherSettlementAccountId.value = dataset.accountId || "";
                    voucherSettlementAccountKeyword.value = `${dataset.accountCode} ${dataset.accountName}`.trim();
                    setMessage(message, `결제 계정 ${dataset.accountName}를 선택했어.`, "success");
                });
            });
        }

        async function updateReview(reviewStatus) {
            if (!selectedDocument) {
                setMessage(message, "먼저 OCR 문서를 선택해.", "error");
                return;
            }

            const validationMessage = validateReviewForm(selectedDocument);
            if (validationMessage) {
                setMessage(message, validationMessage, "error");
                return;
            }

            try {
                const reviewedPayload = JSON.stringify(buildReviewPayload(selectedDocument), null, 2);
                reviewedResultInput.value = reviewedPayload;
                const response = await fetch(`${getApiBaseUrl()}/api/v1/admin/ocr-documents/${selectedDocument.id}/review`, {
                    method: "PATCH",
                    headers: getAuthHeaders({ "Content-Type": "application/json" }),
                    body: JSON.stringify({
                        reviewStatus: reviewStatus,
                        reviewedResult: reviewedPayload
                    })
                });
                const detail = await parseApiResponse(response, "OCR 검수 상태를 변경하지 못했어.");
                setMessage(message, `OCR 문서 ${detail.id} 검수 상태를 ${detail.reviewStatus}로 변경했어.`, "success");
                await openDocumentDetail(detail.id);
                await loadDocuments();
            } catch (error) {
                setMessage(message, error.message, "error");
            }
        }

        async function retryDocument() {
            if (!selectedDocument) {
                setMessage(message, "먼저 OCR 문서를 선택해.", "error");
                return;
            }
            try {
                const response = await fetch(`${getApiBaseUrl()}/api/v1/admin/ocr-documents/${selectedDocument.id}/retry`, {
                    method: "POST",
                    headers: getAuthHeaders({ "Content-Type": "application/json" })
                });
                const detail = await parseApiResponse(response, "OCR 재처리에 실패했어.");
                setMessage(message, `OCR 문서 ${detail.id}를 다시 분석했어.`, "success");
                await openDocumentDetail(detail.id);
                await loadDocuments();
            } catch (error) {
                setMessage(message, error.message, "error");
            }
        }

        async function deleteDocument() {
            if (!selectedDocument) {
                setMessage(message, "먼저 OCR 문서를 선택해.", "error");
                return;
            }

            if (!confirm(`OCR 문서 ${selectedDocument.id} (${selectedDocument.originalFilename})를 정말 삭제하시겠습니까?`)) {
                return;
            }

            try {
                const response = await fetch(`${getApiBaseUrl()}/api/v1/admin/ocr-documents/${selectedDocument.id}`, {
                    method: "DELETE",
                    headers: getAuthHeaders({ "Content-Type": "application/json" })
                });

                const text = await response.text();
                let data = null;
                if (text) {
                    try {
                        data = JSON.parse(text);
                    } catch (e) {}
                }

                if (!response.ok) {
                    throw new Error(data && data.message ? data.message : "OCR 문서 삭제에 실패했어.");
                }

                setMessage(message, data && data.message ? data.message : "OCR 문서를 삭제했어.", "success");
                selectedDocument = null;
                detailSummary.innerHTML = "문서를 선택하면 상세 정보가 여기 표시돼.";
                rawResult.textContent = "아직 선택한 문서가 없어.";
                previewImage.classList.add("hidden");
                previewImage.removeAttribute("src");
                previewHint.classList.remove("hidden");
                previewFallback.classList.add("hidden");
                
                if (deleteButton) deleteButton.disabled = true;
                if (unlinkButton) unlinkButton.disabled = true;
                if (retryButton) retryButton.disabled = true;

                await loadDocuments();
            } catch (error) {
                setMessage(message, error.message, "error");
            }
        }

        async function unlinkDocument() {
            if (!selectedDocument) {
                setMessage(message, "먼저 OCR 문서를 선택해.", "error");
                return;
            }

            try {
                const response = await fetch(`${getApiBaseUrl()}/api/v1/admin/ocr-documents/${selectedDocument.id}/link`, {
                    method: "DELETE",
                    headers: getAuthHeaders({ "Content-Type": "application/json" })
                });
                const detail = await parseApiResponse(response, "OCR 문서 연결 해제에 실패했어.");
                setMessage(message, `OCR 문서 ${detail.id} 연결을 해제했어.`, "success");
                await openDocumentDetail(detail.id);
                await loadDocuments();
            } catch (error) {
                setMessage(message, error.message, "error");
            }
        }

        async function unlinkSelectedDocument() {
            if (!selectedDocument) {
                setMessage(message, "Select an OCR document first.", "error");
                return;
            }

            try {
                const response = await fetch(`${getApiBaseUrl()}/api/v1/admin/ocr-documents/${selectedDocument.id}/link`, {
                    method: "DELETE",
                    headers: getAuthHeaders({ "Content-Type": "application/json" })
                });
                const detail = await parseApiResponse(response, "Failed to unlink the OCR document.");
                setMessage(message, `OCR document ${detail.id} unlinked.`, "success");
                await openDocumentDetail(detail.id);
                await loadDocuments();
            } catch (error) {
                setMessage(message, error.message, "error");
            }
        }

        async function linkPurchaseOrder() {
            if (!selectedDocument) {
                setMessage(message, "먼저 OCR 문서를 선택해.", "error");
                return;
            }

            let items;
            try {
                items = JSON.parse(purchaseItemsJson.value);
            } catch (error) {
                setMessage(message, "발주 품목 JSON 형식이 올바르지 않아.", "error");
                return;
            }

            try {
                const response = await fetch(`${getApiBaseUrl()}/api/v1/admin/ocr-documents/${selectedDocument.id}/link/purchase-orders`, {
                    method: "POST",
                    headers: getAuthHeaders({ "Content-Type": "application/json" }),
                    body: JSON.stringify({
                        vendorId: purchaseVendorId.value ? Number(purchaseVendorId.value) : null,
                        expectedDate: purchaseExpectedDate.value || null,
                        note: purchaseNote.value || null,
                        items: Array.isArray(items) ? items.map(function (item) {
                            return {
                                itemId: item.itemId === null || item.itemId === undefined || item.itemId === "" ? null : Number(item.itemId),
                                itemName: item.itemName || null,
                                quantity: item.quantity === null || item.quantity === undefined || item.quantity === "" ? null : Number(item.quantity),
                                unitPrice: item.unitPrice,
                                expectedDate: item.expectedDate || purchaseExpectedDate.value || null,
                                note: item.note || null
                            };
                        }) : []
                    })
                });
                const purchaseOrder = await parseApiResponse(response, "구매 발주 연결에 실패했어.");
                setMessage(message, `구매 발주 ${purchaseOrder.purchaseOrderNo}를 생성하고 OCR 문서와 연결했어.`, "success");
                await openDocumentDetail(selectedDocument.id);
                await loadDocuments();
            } catch (error) {
                setMessage(message, error.message, "error");
            }
        }

        async function linkExpense() {
            if (!selectedDocument) {
                setMessage(message, "먼저 OCR 문서를 선택해.", "error");
                return;
            }
            try {
                const response = await fetch(`${getApiBaseUrl()}/api/v1/admin/ocr-documents/${selectedDocument.id}/link/expenses`, {
                    method: "POST",
                    headers: getAuthHeaders({ "Content-Type": "application/json" }),
                    body: JSON.stringify({
                        employeeId: expenseEmployeeId.value ? Number(expenseEmployeeId.value) : null,
                        expenseDate: expenseDate.value || null,
                        category: expenseCategory.value || null,
                        amount: expenseAmount.value ? Number(expenseAmount.value) : null,
                        description: expenseDescription.value || null,
                        status: expenseStatus.value || null
                    })
                });
                const expense = await parseApiResponse(response, "비용 연결에 실패했어.");
                setMessage(message, `비용 ${expense.id}를 생성하고 OCR 문서와 연결했어.`, "success");
                await openDocumentDetail(selectedDocument.id);
                await loadDocuments();
            } catch (error) {
                setMessage(message, error.message, "error");
            }
        }

        async function linkVoucher() {
            if (!selectedDocument) {
                setMessage(message, "먼저 OCR 문서를 선택해.", "error");
                return;
            }
            try {
                const response = await fetch(`${getApiBaseUrl()}/api/v1/admin/ocr-documents/${selectedDocument.id}/link/vouchers`, {
                    method: "POST",
                    headers: getAuthHeaders({ "Content-Type": "application/json" }),
                    body: JSON.stringify({
                        voucherDate: voucherDate.value || null,
                        voucherType: voucherType.value || null,
                        vatType: voucherVatType.value || null,
                        vendorId: voucherVendorId.value ? Number(voucherVendorId.value) : null,
                        vendorNameSnapshot: voucherVendorName.value || null,
                        description: voucherDescription.value || null,
                        supplyAmount: voucherSupplyAmount.value ? Number(voucherSupplyAmount.value) : null,
                        vatAmount: voucherVatAmount.value ? Number(voucherVatAmount.value) : null,
                        feeAmount: voucherFeeAmount.value ? Number(voucherFeeAmount.value) : null,
                        businessAccountId: voucherBusinessAccountId.value ? Number(voucherBusinessAccountId.value) : null,
                        settlementAccountId: voucherSettlementAccountId.value ? Number(voucherSettlementAccountId.value) : null
                    })
                });
                const voucher = await parseApiResponse(response, "전표 연결에 실패했어.");
                setMessage(message, `전표 ${voucher.voucherNo}를 생성하고 OCR 문서와 연결했어.`, "success");
                await openDocumentDetail(selectedDocument.id);
                await loadDocuments();
            } catch (error) {
                setMessage(message, error.message, "error");
            }
        }

        async function loadPreview(fileEndpoint, mimeType) {
            previewImage.classList.add("hidden");
            previewFallback.classList.add("hidden");
            previewHint.classList.add("hidden");
            if (previewImage.src && typeof previewImage.src === "string" && previewImage.src.startsWith("blob:")) {
                URL.revokeObjectURL(previewImage.src);
            }
            previewImage.removeAttribute("src");

            try {
                const response = await fetch(`${getApiBaseUrl()}${fileEndpoint}`, {
                    headers: getAuthHeaders()
                });
                if (!response.ok) {
                    throw new Error();
                }
                const blob = await response.blob();
                if (mimeType && mimeType.startsWith("image/")) {
                    const objectUrl = URL.createObjectURL(blob);
                    previewImage.src = objectUrl;
                    previewImage.classList.remove("hidden");
                } else {
                    previewFallback.classList.remove("hidden");
                }
            } catch (error) {
                previewFallback.classList.remove("hidden");
            }
        }

        refreshButton.addEventListener("click", function () {
            setMessage(message, "", "");
            loadDocuments();
        });
        [statusFilter, reviewFilter, linkFilter, typeFilter].forEach(function (filter) {
            filter.addEventListener("change", function () {
                setMessage(message, "", "");
                loadDocuments();
            });
        });
        keywordInput.addEventListener("keydown", function (event) {
            if (event.key === "Enter") {
                loadDocuments();
            }
        });
        vendorSearchKeyword.addEventListener("keydown", function (event) {
            if (event.key === "Enter") {
                searchVendors();
            }
        });
        itemSearchKeyword.addEventListener("keydown", function (event) {
            if (event.key === "Enter") {
                searchItems();
            }
        });
        voucherBusinessAccountKeyword.addEventListener("keydown", function (event) {
            if (event.key === "Enter") {
                searchBusinessAccountBtn.click();
            }
        });
        voucherSettlementAccountKeyword.addEventListener("keydown", function (event) {
            if (event.key === "Enter") {
                searchSettlementAccountBtn.click();
            }
        });
        [reviewVendorName, reviewTransactionDate, reviewTotalAmount, reviewCurrency, reviewNotes].forEach(function (element) {
            element.addEventListener("input", function () {
                syncReviewJsonPreview(selectedDocument);
            });
        });

        searchVendorButton.addEventListener("click", searchVendors);
        searchItemButton.addEventListener("click", searchItems);
        updateVoucherAccountSelections();

        approveButton.addEventListener("click", function () {
            updateReview("APPROVED");
        });
        rejectButton.addEventListener("click", function () {
            updateReview("REJECTED");
        });
        unlinkButton.addEventListener("click", unlinkSelectedDocument);
        retryButton.addEventListener("click", retryDocument);
        if (deleteButton) {
            deleteButton.addEventListener("click", deleteDocument);
        }
        linkPurchaseOrderButton.addEventListener("click", linkPurchaseOrder);
        linkExpenseButton.addEventListener("click", linkExpense);
        linkVoucherButton.addEventListener("click", linkVoucher);

        loadDocuments();
    }

    document.addEventListener("DOMContentLoaded", function () {
        initUploadPage();
        initDocumentBoxPage();
    });
})();
