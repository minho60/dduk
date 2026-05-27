(function () {
  const API_PATH = '/api/v1/accounting/monthly-closing';
  const state = {
    periods: [],
    selected: null,
    lastValidation: new Map()
  };

  const money = new Intl.NumberFormat("ko-KR", { style: "currency", currency: "KRW", maximumFractionDigits: 0 });
  const number = new Intl.NumberFormat("ko-KR");

  document.addEventListener("DOMContentLoaded", () => {
    const now = new Date();
    byId("filterYear").value = now.getFullYear();
    byId("periodYear").value = now.getFullYear();
    byId("periodMonth").value = now.getMonth() + 1;
    setDefaultDates();
    bindEvents();
    loadAll();
    renderIcons();
  });

  function bindEvents() {
    byId("reloadButton").addEventListener("click", loadAll);
    byId("createPeriodButton").addEventListener("click", () => byId("periodDialog").showModal());
    byId("createYearButton").addEventListener("click", createYearPeriods);
    byId("savePeriodButton").addEventListener("click", createPeriod);
    byId("periodMonth").addEventListener("change", setDefaultDates);
    byId("periodYear").addEventListener("change", setDefaultDates);
  }

  async function loadAll() {
    await Promise.all([loadPeriods(), loadSummary()]);
  }

  async function loadPeriods() {
    const year = byId("filterYear").value;
    const response = await api(`/periods${year ? `?fiscalYear=${encodeURIComponent(year)}` : ""}`);
    state.periods = response.data || [];
    renderPeriods();
  }

  async function loadSummary(period) {
    const target = period || state.selected;
    const query = target ? `?fiscalYear=${target.fiscalYear}&fiscalMonth=${target.fiscalMonth}` : "";
    try {
      const response = await api(`/summary${query}`);
      renderSummary(response.data);
    } catch (error) {
      renderSummary(null);
    }
  }

  function renderPeriods() {
    const body = byId("periodTableBody");
    body.innerHTML = "";
    if (!state.periods.length) {
      body.innerHTML = `<tr><td colspan="12">등록된 회계기간이 없습니다.</td></tr>`;
      return;
    }
    body.innerHTML = state.periods.map(period => {
      const canClose = period.status === "OPEN" || period.status === "REOPENED" || period.status === "PRE_CLOSING";
      const canReopen = period.status === "CLOSED";
      return `
        <tr data-period-id="${period.id}">
          <td><button class="text-link" type="button" data-action="select" data-id="${period.id}">${period.periodKey}</button></td>
          <td>${period.startDate || "-"}</td>
          <td>${period.endDate || "-"}</td>
          <td>${pill(period.status)}</td>
          <td class="amount">${number.format(period.voucherCount || 0)}</td>
          <td class="amount">${money.format(period.totalDebit || 0)}</td>
          <td class="amount">${money.format(period.totalCredit || 0)}</td>
          <td>${pill(period.validationStatus || "SUCCESS")}</td>
          <td>${formatDateTime(period.closedAt)}</td>
          <td>${period.closedBy || "-"}</td>
          <td>${period.reopened ? "Y" : "N"}</td>
          <td>
            <div class="row-actions">
              <button type="button" data-action="validate" data-id="${period.id}" title="마감 검증" aria-label="마감 검증"><i data-lucide="check-circle"></i></button>
              <button type="button" data-action="close" data-id="${period.id}" title="월 마감 실행" aria-label="월 마감 실행" ${canClose ? "" : "disabled"}><i data-lucide="lock"></i></button>
              <button type="button" data-action="reopen" data-id="${period.id}" title="마감 취소" aria-label="마감 취소" ${canReopen ? "" : "disabled"}><i data-lucide="unlock"></i></button>
              <button type="button" data-action="logs" data-id="${period.id}" title="마감 로그" aria-label="마감 로그"><i data-lucide="history"></i></button>
            </div>
          </td>
        </tr>
      `;
    }).join("");
    body.querySelectorAll("button[data-action]").forEach(button => {
      button.addEventListener("click", handlePeriodAction);
    });
    renderIcons();
  }

  async function handlePeriodAction(event) {
    try {
      const action = event.currentTarget.dataset.action;
      const period = state.periods.find(item => String(item.id) === event.currentTarget.dataset.id);
      if (!period) return;
      state.selected = period;
      byId("selectedPeriodHint").textContent = `${period.periodKey} 선택됨`;
      await loadSummary(period);
      if (action === "select") return;
      if (action === "validate") return validatePeriod(period);
      if (action === "close") return closePeriod(period);
      if (action === "reopen") return reopenPeriod(period);
      if (action === "logs") return loadLogs(period);
    } catch (error) {
      toast(error.message);
    }
  }

  async function validatePeriod(period) {
    const response = await api(`/periods/${period.fiscalYear}/${period.fiscalMonth}/validate`, {
      method: "POST",
      body: { actor: "SYSTEM" }
    });
    state.lastValidation.set(period.periodKey, response.data);
    renderValidation(response.data);
  }

  async function closePeriod(period) {
    const validation = state.lastValidation.get(period.periodKey);
    if (!validation || !validation.closable) {
      await validatePeriod(period);
      const refreshed = state.lastValidation.get(period.periodKey);
      if (!refreshed || !refreshed.closable) {
        toast("마감 검증 오류를 먼저 처리해야 합니다.");
        return;
      }
    }
    await api(`/periods/${period.fiscalYear}/${period.fiscalMonth}/close`, {
      method: "POST",
      body: { actor: "SYSTEM" }
    });
    toast(`${period.periodKey} 월 마감이 완료되었습니다.`);
    await loadAll();
  }

  async function reopenPeriod(period) {
    await api(`/periods/${period.fiscalYear}/${period.fiscalMonth}/reopen`, {
      method: "POST",
      body: { actor: "SYSTEM" }
    });
    toast(`${period.periodKey} 기간이 재오픈되었습니다.`);
    await loadAll();
  }

  async function loadLogs(period) {
    const response = await api(`/periods/${period.id}/logs`);
    const logs = response.data || [];
    byId("logList").innerHTML = logs.length ? logs.map(log => `
      <div class="log-item">
        <strong>${log.actionType} · ${log.actor || "SYSTEM"}</strong>
        <span>${formatDateTime(log.actionAt)} · ${log.fromStatus || "-"} → ${log.toStatus || "-"}</span>
        <p>${log.message || ""}</p>
      </div>
    `).join("") : `<div class="log-item"><strong>로그 없음</strong><p>아직 기록된 마감 로그가 없습니다.</p></div>`;
    byId("logDialog").showModal();
  }

  async function createPeriod() {
    const payload = {
      fiscalYear: Number(byId("periodYear").value),
      fiscalMonth: Number(byId("periodMonth").value),
      startDate: byId("periodStartDate").value,
      endDate: byId("periodEndDate").value,
      createdBy: "SYSTEM"
    };
    if (!payload.fiscalYear || !payload.fiscalMonth || !payload.startDate || !payload.endDate) {
      toast("필수값을 입력해주세요.");
      return;
    }
    if (payload.startDate >= payload.endDate) {
      toast("시작일은 종료일보다 이전이어야 합니다.");
      return;
    }
    try {
      await api("/periods", { method: "POST", body: payload });
      byId("periodDialog").close();
      toast("회계기간이 생성되었습니다.");
      await loadAll();
    } catch (error) {
      toast(error.message);
    }
  }

  async function createYearPeriods() {
    const fiscalYear = Number(byId("filterYear").value);
    if (!fiscalYear) {
      toast("회계연도를 입력해주세요.");
      return;
    }
    try {
      await api("/periods/year", {
        method: "POST",
        body: { fiscalYear, createdBy: "SYSTEM" }
      });
      toast(`${fiscalYear}년 회계기간을 생성했습니다.`);
      await loadAll();
    } catch (error) {
      toast(error.message);
    }
  }

  function renderSummary(summary) {
    setText("currentPeriod", summary?.currentPeriod || "-");
    setText("status", summary?.status || "-");
    setText("unapprovedVoucherCount", `${number.format(summary?.unapprovedVoucherCount || 0)}건`);
    setText("unpostedVoucherCount", `${number.format(summary?.unpostedVoucherCount || 0)}건`);
    setMoney("totalDebit", summary?.totalDebit || 0);
    setMoney("totalCredit", summary?.totalCredit || 0);
    setText("mismatchStatus", summary?.mismatchStatus || "-");
  }

  function renderValidation(validation) {
    byId("validationTitle").textContent = `${validation.periodKey} 마감 검증`;
    byId("validationTableBody").innerHTML = (validation.results || []).map(row => `
      <tr>
        <td>${row.validationType}</td>
        <td>${pill(row.status)}</td>
        <td>${row.detail}</td>
        <td class="amount">${number.format(row.targetCount || 0)}</td>
        <td>${row.actionRequired ? "필요" : "없음"}</td>
      </tr>
    `).join("");
    byId("validationDialog").showModal();
    renderIcons();
  }

  /** Unified API helper – delegates to window.ddukApi (apiClient.js) */
  async function api(path, options = {}) {
    const url = `${API_PATH}${path}`;
    const method = (options.method || "GET").toUpperCase();
    let payload;
    if (method === "GET") {
      payload = await window.ddukApi.get(url);
    } else if (method === "POST") {
      payload = await window.ddukApi.post(url, options.body);
    } else if (method === "PUT") {
      payload = await window.ddukApi.put(url, options.body);
    } else if (method === "PATCH") {
      payload = await window.ddukApi.patch(url, options.body);
    } else if (method === "DELETE") {
      payload = await window.ddukApi.delete(url);
    }
    if (payload && payload.status === "error") {
      throw new Error(payload.message || "요청 처리 중 오류가 발생했습니다.");
    }
    return payload || {};
  }

  function setDefaultDates() {
    const year = Number(byId("periodYear").value);
    const month = Number(byId("periodMonth").value);
    if (!year || !month) return;
    const start = new Date(year, month - 1, 1);
    const end = new Date(year, month, 0);
    byId("periodStartDate").value = toDateInput(start);
    byId("periodEndDate").value = toDateInput(end);
  }

  function pill(value) {
    return `<span class="status-pill status-${value}">${value}</span>`;
  }

  function setText(key, value) {
    const element = document.querySelector(`[data-summary="${key}"]`);
    if (element) element.textContent = value;
  }

  function setMoney(key, value) {
    const element = document.querySelector(`[data-summary-money="${key}"]`);
    if (element) element.textContent = money.format(value);
  }

  function formatDateTime(value) {
    if (!value) return "-";
    return String(value).replace("T", " ").slice(0, 16);
  }

  function toDateInput(date) {
    return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, "0")}-${String(date.getDate()).padStart(2, "0")}`;
  }

  function byId(id) {
    return document.getElementById(id);
  }

  function toast(message) {
    const element = byId("toast");
    element.textContent = message;
    element.classList.add("show");
    window.setTimeout(() => element.classList.remove("show"), 2800);
  }

  function renderIcons() {
    if (window.lucide) window.lucide.createIcons();
  }
})();
