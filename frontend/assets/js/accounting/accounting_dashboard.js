(function () {
  const API_BASE = "/api/v1/accounting/dashboard";
  const money = new Intl.NumberFormat("ko-KR", { maximumFractionDigits: 0 });
  let profitLossChart;
  let compositionChart;

  document.addEventListener("DOMContentLoaded", () => {
    initializePeriodControls();
    bindEvents();
    loadDashboard();
    renderIcons();
  });

  function initializePeriodControls() {
    const now = new Date();
    byId("filterYear").value = now.getFullYear();
    byId("filterMonth").innerHTML = Array.from({ length: 12 }, (_, index) => {
      const month = index + 1;
      return `<option value="${month}" ${month === now.getMonth() + 1 ? "selected" : ""}>${month}월</option>`;
    }).join("");
  }

  function bindEvents() {
    byId("reloadButton").addEventListener("click", loadDashboard);
    byId("filterYear").addEventListener("change", loadDashboard);
    byId("filterMonth").addEventListener("change", loadDashboard);
  }

  async function loadDashboard() {
    try {
      const query = new URLSearchParams({
        fiscalYear: byId("filterYear").value,
        fiscalMonth: byId("filterMonth").value
      });
      const response = await fetch(`${API_BASE}?${query}`);
      const payload = await response.json();
      if (!response.ok || payload.status === "error") throw new Error(payload.message || "대시보드 조회 중 오류가 발생했습니다.");
      renderDashboard(payload.data);
    } catch (error) {
      toast(error.message);
    }
  }

  function renderDashboard(data) {
    const period = data.periodSummary || {};
    byId("periodTitle").textContent = `${period.currentPeriod || "-"} 회계 통합 현황`;
    byId("generatedAt").textContent = `생성 시각 ${formatDateTime(data.generatedAt)}`;
    renderBadge(byId("periodStatusBadge"), period.status || "NOT_CREATED");
    renderBadge(byId("validationBadge"), period.validationStatus || "UNKNOWN");
    renderKpis(data.kpiSummary || {});
    renderVoucherSummary(data.voucherSummary || {});
    renderProfitLossChart(data.financialTrends || []);
    renderCompositionChart(data.accountComposition || []);
    renderCashFlow(data.cashFlowSummary || {});
    renderPayroll(data.payrollSummary || {});
    renderPeriod(period);
    renderTrialBalance(data.trialBalanceSummary || {});
    renderAlerts(data.alerts || []);
    renderActivities(data.recentActivities || []);
    renderQuickActions(period);
    renderIcons();
  }

  function renderKpis(kpi) {
    const items = [
      ["당월 총 매출", won(kpi.monthlyRevenue), rateLabel(kpi.monthlyRevenueChangeRate)],
      ["당월 총 비용", won(kpi.monthlyExpense), rateLabel(kpi.monthlyExpenseChangeRate)],
      ["영업이익", won(kpi.operatingIncome), "Revenue - Expense"],
      ["당기순이익", won(kpi.netIncome), "Ledger 기준"],
      ["총 자산", won(kpi.totalAssets), "ASSET 집계"],
      ["총 부채", won(kpi.totalLiabilities), "LIABILITY 집계"],
      ["미승인 전표", `${number(kpi.unapprovedVoucherCount)}건`, "DRAFT / REQUESTED"],
      ["월 마감 상태", kpi.monthlyClosingStatus || "-", "AccountingPeriod"]
    ];
    byId("kpiGrid").innerHTML = items.map(([label, value, hint]) => `
      <article class="erp-kpi-card ${String(hint).startsWith("-") ? "negative" : String(hint).startsWith("+") ? "positive" : ""}">
        <span>${label}</span>
        <strong>${value}</strong>
        <small>${hint || "-"}</small>
      </article>
    `).join("");
  }

  function renderVoucherSummary(summary) {
    const items = [
      ["오늘 등록", summary.todayVoucherCount],
      ["승인 대기", summary.pendingApprovalCount],
      ["DRAFT", summary.draftCount],
      ["APPROVED", summary.approvedCount],
      ["POSTED", summary.postedCount],
      ["CANCELLED", summary.cancelledCount]
    ];
    byId("voucherStatusGrid").innerHTML = items.map(([label, value]) => `
      <div class="status-tile"><span>${label}</span><strong>${number(value)}건</strong></div>
    `).join("");
  }

  function renderProfitLossChart(rows) {
    const ctx = byId("profitLossChart");
    if (profitLossChart) profitLossChart.destroy();
    profitLossChart = new Chart(ctx, {
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
      options: chartOptions()
    });
  }

  function renderCompositionChart(rows) {
    const ctx = byId("compositionChart");
    if (compositionChart) compositionChart.destroy();
    compositionChart = new Chart(ctx, {
      type: "doughnut",
      data: {
        labels: rows.map(row => row.label),
        datasets: [{
          data: rows.map(row => Number(row.amount || 0)),
          backgroundColor: ["#2563eb", "#ef4444", "#10b981"],
          borderWidth: 0
        }]
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

  function renderCashFlow(summary) {
    byId("cashFlowBasis").textContent = summary.basis || "-";
    byId("cashFlowList").innerHTML = metricRows([
      ["현금 유입", won(summary.cashInflow)],
      ["현금 유출", won(summary.cashOutflow)],
      ["순현금 흐름", won(summary.netCashFlow)]
    ]);
  }

  function renderPayroll(summary) {
    byId("payrollList").innerHTML = metricRows([
      ["이번 달 급여 총액", won(summary.monthlyPayrollAmount)],
      ["급여 계산 완료", summary.calculationCompleted ? "완료" : "처리 필요"],
      ["미지급 급여", won(summary.unpaidPayrollAmount)],
      ["지급 예정일", summary.nextPaymentDate || "-"],
      ["미정산 급여", `${number(summary.unsettledCount)}건`]
    ]);
  }

  function renderPeriod(period) {
    byId("periodList").innerHTML = metricRows([
      ["현재 회계기간", period.currentPeriod || "-"],
      ["마감 상태", period.status || "NOT_CREATED"],
      ["검증 결과", period.validationStatus || "-"],
      ["미게시 전표 수", `${number(period.unpostedVoucherCount)}건`],
      ["차변/대변 일치", period.balanced ? "일치" : "불일치"],
      ["총 차변 / 총 대변", `${won(period.totalDebit)} / ${won(period.totalCredit)}`]
    ]);
  }

  function renderTrialBalance(summary) {
    byId("trialBalanceList").innerHTML = metricRows([
      ["총 차변", won(summary.totalDebit)],
      ["총 대변", won(summary.totalCredit)],
      ["불일치 여부", summary.balanced ? "정상" : "불일치"]
    ]);
    const rows = summary.majorAccounts || [];
    byId("majorAccounts").innerHTML = rows.map(row => `
      <div class="rank-row">
        <span>${row.accountCode}</span>
        <strong>${row.accountName}</strong>
        <span>${won(row.closingBalance)}</span>
      </div>
    `).join("") || `<div class="rank-row"><span>-</span><strong>주요 계정 잔액 없음</strong><span>-</span></div>`;
  }

  function renderAlerts(alerts) {
    byId("alertList").innerHTML = alerts.map(alert => `
      <div class="alert-item ${alert.severity}">
        <strong>${alert.title}</strong>
        <p>${alert.message}</p>
        <a href="${alert.actionUrl || "#"}">${alert.actionLabel || "확인"}</a>
      </div>
    `).join("");
  }

  function renderActivities(rows) {
    byId("activityRows").innerHTML = rows.map(row => `
      <div class="activity-row">
        <span>${row.activityType}</span>
        <span>${row.target || "-"}</span>
        <span>${row.actor || "-"}</span>
        <span>${formatDateTime(row.activityAt)}</span>
        <span>${row.status || "-"}</span>
      </div>
    `).join("") || `<div class="activity-row"><span>-</span><span>최근 활동 없음</span><span>-</span><span>-</span><span>-</span></div>`;
  }

  function renderQuickActions(period) {
    const locked = period.status === "CLOSED" || period.status === "ARCHIVED";
    const actions = [
      ["매출전표 등록", "file-plus", "voucher_management.html", locked],
      ["매입전표 등록", "receipt", "voucher_management.html", locked],
      ["급여 계산", "wallet", "payroll_management.html", locked],
      ["월 마감 검증", "calendar-check", "monthly_closing.html", false],
      ["합계잔액시산표", "table-2", "trial_balance.html", false],
      ["회계리포트", "file-bar-chart", "accounting_reports.html", false]
    ];
    byId("quickActions").innerHTML = actions.map(([label, icon, href, disabled]) => `
      <a class="quick-action ${disabled ? "disabled" : ""}" href="${href}" aria-disabled="${disabled}">
        <i data-lucide="${icon}"></i><span>${label}</span>
      </a>
    `).join("");
  }

  function metricRows(items) {
    return items.map(([label, value]) => `
      <div class="metric-row"><span>${label}</span><strong>${value}</strong></div>
    `).join("");
  }

  function dataset(label, data, color) {
    return {
      label,
      data: data.map(value => Number(value || 0)),
      borderColor: color,
      backgroundColor: color,
      tension: 0.35,
      pointRadius: 3,
      borderWidth: 2
    };
  }

  function chartOptions() {
    return {
      responsive: true,
      maintainAspectRatio: false,
      interaction: { mode: "index", intersect: false },
      plugins: {
        legend: { position: "bottom" },
        tooltip: { callbacks: { label: context => `${context.dataset.label}: ${won(context.raw)}` } }
      },
      scales: {
        y: { ticks: { callback: value => compactWon(value) }, grid: { color: "rgba(148, 163, 184, 0.18)" } },
        x: { grid: { display: false } }
      }
    };
  }

  function renderBadge(element, value) {
    element.textContent = value;
    element.className = "status-badge";
    if (["WARNING", "PRE_CLOSING", "REOPENED"].includes(value)) element.classList.add("warning");
    if (["ERROR", "CRITICAL", "CLOSED", "ARCHIVED", "NOT_CREATED"].includes(value)) element.classList.add("error");
    if (["UNKNOWN"].includes(value)) element.classList.add("muted");
  }

  function rateLabel(value) {
    const numeric = Number(value || 0);
    if (numeric === 0) return "전월 대비 0%";
    return `전월 대비 ${numeric > 0 ? "+" : ""}${numeric.toFixed(2)}%`;
  }

  function won(value) {
    return `₩${money.format(Number(value || 0))}`;
  }

  function compactWon(value) {
    const numeric = Number(value || 0);
    if (Math.abs(numeric) >= 100000000) return `${Math.round(numeric / 100000000)}억`;
    if (Math.abs(numeric) >= 10000) return `${Math.round(numeric / 10000)}만`;
    return money.format(numeric);
  }

  function number(value) {
    return money.format(Number(value || 0));
  }

  function formatDateTime(value) {
    if (!value) return "-";
    const date = new Date(value);
    if (Number.isNaN(date.getTime())) return value;
    return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, "0")}-${String(date.getDate()).padStart(2, "0")} ${String(date.getHours()).padStart(2, "0")}:${String(date.getMinutes()).padStart(2, "0")}`;
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
