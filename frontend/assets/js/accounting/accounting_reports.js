(function () {
  const API_BASE = "/api/v1/accounting/reports/analytics";
  const money = new Intl.NumberFormat("ko-KR", { maximumFractionDigits: 0 });
  let trendChart;
  let compositionChart;
  let voucherFlowChart;

  document.addEventListener("DOMContentLoaded", () => {
    initializeFilters();
    bindEvents();
    loadReport();
    renderIcons();
  });

  function initializeFilters() {
    const now = new Date();
    const start = new Date(now.getFullYear(), now.getMonth(), 1);
    const end = new Date(now.getFullYear(), now.getMonth() + 1, 0);
    byId("startDate").value = toDateInput(start);
    byId("endDate").value = toDateInput(end);
    byId("periodSelect").innerHTML = Array.from({ length: 12 }, (_, index) => {
      const month = index + 1;
      const value = `${now.getFullYear()}-${String(month).padStart(2, "0")}`;
      return `<option value="${value}" ${month === now.getMonth() + 1 ? "selected" : ""}>${value}</option>`;
    }).join("");
  }

  function bindEvents() {
    byId("searchButton").addEventListener("click", loadReport);
    byId("periodSelect").addEventListener("change", applyPeriod);
    byId("excelButton").addEventListener("click", () => download("excel"));
    byId("pdfButton").addEventListener("click", () => download("pdf"));
    byId("printButton").addEventListener("click", () => window.print());
  }

  function applyPeriod() {
    const [year, month] = byId("periodSelect").value.split("-").map(Number);
    const start = new Date(year, month - 1, 1);
    const end = new Date(year, month, 0);
    byId("startDate").value = toDateInput(start);
    byId("endDate").value = toDateInput(end);
  }

  async function loadReport() {
    const query = buildQuery();
    if (!query) return;
    try {
      const response = await fetch(`${API_BASE}?${query}`);
      const payload = await response.json();
      if (!response.ok || payload.status === "error") throw new Error(payload.message || "리포트 조회 중 오류가 발생했습니다.");
      renderReport(payload.data);
    } catch (error) {
      toast(error.message);
    }
  }

  function renderReport(data) {
    renderKpis(data.financialSummary || {});
    renderTrendChart(data.monthlyTrends || []);
    renderCompositionChart(data.balanceComposition || []);
    renderSales(data.salesAnalysis || []);
    renderExpenses(data.expenseAnalysis || []);
    renderAccounts(data.accountAnalysis || []);
    renderVoucherFlow(data.voucherFlows || []);
    renderPayroll(data.payrollAnalysis || {});
    renderIcons();
  }

  function renderKpis(summary) {
    const items = [
      ["총 매출", won(summary.totalRevenue), rate(summary.revenueChangeRate)],
      ["총 비용", won(summary.totalExpense), rate(summary.expenseChangeRate)],
      ["영업이익", won(summary.operatingIncome), "Revenue - Expense"],
      ["당기순이익", won(summary.netIncome), "현재 산식 기준"],
      ["총 자산", won(summary.totalAssets), "ASSET"],
      ["총 부채", won(summary.totalLiabilities), "LIABILITY"],
      ["부채비율", percent(summary.debtRatio), "총부채 / 총자본"],
      ["유동비율", percent(summary.currentRatio), "유동자산 / 유동부채"]
    ];
    byId("kpiGrid").innerHTML = items.map(([label, value, hint]) => `
      <article class="kpi-card ${String(hint).startsWith("+") ? "positive" : String(hint).startsWith("-") ? "negative" : ""}">
        <span>${label}</span>
        <strong>${value}</strong>
        <small>${hint || "-"}</small>
      </article>
    `).join("");
  }

  function renderTrendChart(rows) {
    if (trendChart) trendChart.destroy();
    trendChart = new Chart(byId("trendChart"), {
      type: "line",
      data: {
        labels: rows.map(row => row.period),
        datasets: [
          dataset("매출", rows.map(row => row.revenue), "#2563eb"),
          dataset("비용", rows.map(row => row.expense), "#f59e0b"),
          dataset("영업이익", rows.map(row => row.operatingIncome), "#10b981"),
          dataset("당기순이익", rows.map(row => row.netIncome), "#7c3aed")
        ]
      },
      options: lineOptions()
    });
  }

  function renderCompositionChart(rows) {
    if (compositionChart) compositionChart.destroy();
    compositionChart = new Chart(byId("compositionChart"), {
      type: "doughnut",
      data: {
        labels: rows.map(row => row.label),
        datasets: [{ data: rows.map(row => Number(row.amount || 0)), backgroundColor: ["#2563eb", "#ef4444", "#10b981"], borderWidth: 0 }]
      },
      options: {
        responsive: true,
        maintainAspectRatio: false,
        plugins: {
          legend: { position: "bottom" },
          tooltip: { callbacks: { label: context => `${context.label}: ${won(context.raw)}` } }
        },
        cutout: "62%"
      }
    });
  }

  function renderSales(rows) {
    byId("salesRows").innerHTML = rows.map(row => `
      <tr>
        <td>${row.vendorName}</td>
        <td class="amount">${won(row.salesAmount)}</td>
        <td class="amount">${won(row.vatAmount)}</td>
        <td class="amount">${won(row.netSales)}</td>
        <td class="amount">${rate(row.changeRate)}</td>
      </tr>
    `).join("") || emptyRow(5);
  }

  function renderExpenses(rows) {
    byId("expenseRows").innerHTML = rows.map(row => `
      <tr>
        <td>${row.accountName}</td>
        <td class="amount">${won(row.amount)}</td>
        <td class="amount">${percent(row.ratio)}</td>
      </tr>
    `).join("") || emptyRow(3);
  }

  function renderAccounts(rows) {
    byId("accountRows").innerHTML = rows.map(row => {
      const indent = Math.max(0, (row.level || 1) - 1) * 16;
      return `
        <tr>
          <td>${row.accountCode}</td>
          <td style="padding-left:${indent + 10}px">${row.leaf ? "" : "▸ "}${row.accountName}</td>
          <td>${row.accountType}</td>
          <td class="amount">${won(row.openingBalance)}</td>
          <td class="amount">${won(row.periodDebit)}</td>
          <td class="amount">${won(row.periodCredit)}</td>
          <td class="amount">${won(row.periodChange)}</td>
          <td class="amount">${won(row.closingBalance)}</td>
        </tr>
      `;
    }).join("") || emptyRow(8);
  }

  function renderVoucherFlow(rows) {
    if (voucherFlowChart) voucherFlowChart.destroy();
    voucherFlowChart = new Chart(byId("voucherFlowChart"), {
      type: "bar",
      data: {
        labels: rows.map(row => row.period),
        datasets: [
          barDataset("전체", rows.map(row => row.totalCount), "#64748b"),
          barDataset("승인", rows.map(row => row.approvedCount), "#2563eb"),
          barDataset("POSTED", rows.map(row => row.postedCount), "#10b981"),
          barDataset("취소", rows.map(row => row.cancelledCount), "#ef4444")
        ]
      },
      options: {
        responsive: true,
        maintainAspectRatio: false,
        plugins: { legend: { position: "bottom" } },
        scales: { x: { grid: { display: false } }, y: { beginAtZero: true, ticks: { precision: 0 } } }
      }
    });
  }

  function renderPayroll(summary) {
    byId("payrollSummary").innerHTML = [
      ["급여 총액", won(summary.grossAmount)],
      ["상여 총액", won(summary.bonusAmount)],
      ["공제 총액", won(summary.deductionAmount)],
      ["실지급액", won(summary.netAmount)]
    ].map(([label, value]) => `<div class="metric-row"><span>${label}</span><strong>${value}</strong></div>`).join("");

    byId("payrollRows").innerHTML = (summary.departments || []).map(row => `
      <tr>
        <td>${row.departmentName}</td>
        <td class="amount">${won(row.grossAmount)}</td>
        <td class="amount">${won(row.netAmount)}</td>
      </tr>
    `).join("") || emptyRow(3);
  }

  function buildQuery() {
    const startDate = byId("startDate").value;
    const endDate = byId("endDate").value;
    if (!startDate || !endDate) {
      toast("시작일과 종료일을 입력해 주세요.");
      return null;
    }
    if (endDate < startDate) {
      toast("종료일은 시작일보다 빠를 수 없습니다.");
      return null;
    }
    return new URLSearchParams({
      startDate,
      endDate,
      reportBasis: byId("reportBasis").value,
      reportType: byId("reportType").value
    }).toString();
  }

  function download(type) {
    const query = buildQuery();
    if (!query) return;
    window.location.href = `${API_BASE}/export/${type}?${query}`;
  }

  function dataset(label, data, color) {
    return { label, data: data.map(value => Number(value || 0)), borderColor: color, backgroundColor: color, tension: 0.35, pointRadius: 3, borderWidth: 2 };
  }

  function barDataset(label, data, color) {
    return { label, data: data.map(value => Number(value || 0)), backgroundColor: color, borderRadius: 4 };
  }

  function lineOptions() {
    return {
      responsive: true,
      maintainAspectRatio: false,
      interaction: { mode: "index", intersect: false },
      plugins: {
        legend: { position: "bottom" },
        tooltip: { callbacks: { label: context => `${context.dataset.label}: ${won(context.raw)}` } }
      },
      scales: {
        x: { grid: { display: false } },
        y: { ticks: { callback: value => compactWon(value) }, grid: { color: "rgba(148, 163, 184, 0.18)" } }
      }
    };
  }

  function won(value) {
    return `₩${money.format(Number(value || 0))}`;
  }

  function percent(value) {
    return `${Number(value || 0).toFixed(2)}%`;
  }

  function rate(value) {
    const numeric = Number(value || 0);
    if (numeric === 0) return "0.00%";
    return `${numeric > 0 ? "+" : ""}${numeric.toFixed(2)}%`;
  }

  function compactWon(value) {
    const numeric = Number(value || 0);
    if (Math.abs(numeric) >= 100000000) return `${Math.round(numeric / 100000000)}억`;
    if (Math.abs(numeric) >= 10000) return `${Math.round(numeric / 10000)}만`;
    return money.format(numeric);
  }

  function emptyRow(colspan) {
    return `<tr><td colspan="${colspan}">조회된 데이터가 없습니다.</td></tr>`;
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
