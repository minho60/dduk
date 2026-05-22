const API_BASE = '/api/accounting/vouchers';
const ACCOUNT_SEARCH_API = '/api/accounting/accounts/search';
const VENDOR_SEARCH_API = '/api/v1/inventory/vendors/search';

const state = {
  voucherType: 'SALES',
  lookupTarget: null,
  selected: {
    vendor: null,
    businessAccount: null,
    settlementAccount: null,
  },
};

const VAT_ZERO_TYPES = new Set(['ZERO_TAX', 'TAX_FREE', 'EXPORT', 'INVOICE']);
const moneyFormatter = new Intl.NumberFormat('ko-KR', { maximumFractionDigits: 0 });

let vatDebounceTimer = null;
let lookupDebounceTimer = null;

document.addEventListener('DOMContentLoaded', initVoucherPage);

function initVoucherPage() {
  const voucherDate = document.getElementById('voucherDate');
  if (voucherDate) {
    voucherDate.valueAsDate = new Date();
  }

  bindEvents();
  refreshLabels();
  recalculateAmounts();
  refreshJournalPreview();
  loadSummary();
  loadVouchers();

  if (window.lucide) {
    window.lucide.createIcons();
  }
}

function bindEvents() {
  document.querySelectorAll('[data-voucher-type]').forEach((button) => {
    button.addEventListener('click', () => switchVoucherType(button));
  });

  ['supplyAmount', 'vatAmount', 'feeAmount'].forEach((id) => {
    const input = document.getElementById(id);
    if (!input) return;
    input.addEventListener('input', () => {
      sanitizeMoneyInput(input);
      if (id === 'supplyAmount') {
        debounceVatCalculation();
      } else {
        recalculateAmounts();
      }
    });
    input.addEventListener('blur', () => formatMoneyInput(input));
  });

  document.getElementById('vatType')?.addEventListener('change', () => {
    calculateVatFromSupply();
    recalculateAmounts();
  });
  document.getElementById('voucherForm')?.addEventListener('submit', handleSubmit);
  document.getElementById('resetFormButton')?.addEventListener('click', resetForm);
  document.getElementById('cancelButton')?.addEventListener('click', resetForm);
  document.getElementById('reloadButton')?.addEventListener('click', () => {
    loadSummary();
    loadVouchers();
  });

  document.querySelectorAll('[data-open-lookup]').forEach((button) => {
    button.addEventListener('click', () => openLookup(button.dataset.openLookup));
  });

  bindAccountAutocomplete('businessAccountName', 'businessAccount');
  bindAccountAutocomplete('settlementAccountName', 'settlementAccount');

  document.getElementById('lookupSearchButton')?.addEventListener('click', runLookupSearch);
  document.getElementById('lookupKeyword')?.addEventListener('input', () => {
    clearTimeout(lookupDebounceTimer);
    lookupDebounceTimer = setTimeout(runLookupSearch, 250);
  });
  document.getElementById('lookupKeyword')?.addEventListener('keydown', (event) => {
    if (event.key === 'Enter') {
      event.preventDefault();
      runLookupSearch();
    }
  });
}

function bindAccountAutocomplete(inputId, target) {
  const input = document.getElementById(inputId);
  if (!input) return;
  input.addEventListener('input', () => {
    state.selected[target] = null;
    document.getElementById(target === 'businessAccount' ? 'businessAccountId' : 'settlementAccountId').value = '';
    clearTimeout(lookupDebounceTimer);
    lookupDebounceTimer = setTimeout(() => {
      if (input.value.trim().length >= 1) {
        openLookup(target, input.value.trim());
      }
    }, 300);
  });
}

function switchVoucherType(button) {
  state.voucherType = button.dataset.voucherType;
  document.querySelectorAll('[data-voucher-type]').forEach((tab) => tab.classList.toggle('active', tab === button));
  clearSelectedAccounts();
  refreshLabels();
  refreshJournalPreview();
  loadVouchers();
}

function refreshLabels() {
  const isSales = state.voucherType === 'SALES';
  setText('businessAccountLabel', isSales ? '매출계정' : '매입계정');
  setText('settlementAccountLabel', isSales ? '입금계좌' : '출금계좌');
}

function sanitizeMoneyInput(input) {
  const cleaned = input.value.replace(/[^\d]/g, '');
  input.value = cleaned;
}

function debounceVatCalculation() {
  clearTimeout(vatDebounceTimer);
  vatDebounceTimer = setTimeout(() => {
    calculateVatFromSupply();
    recalculateAmounts();
  }, 180);
}

function calculateVatFromSupply() {
  const supplyAmount = parseMoney(getValue('supplyAmount'));
  const vatType = getValue('vatType');
  const vatAmount = VAT_ZERO_TYPES.has(vatType) ? 0 : Math.floor(supplyAmount * 0.1);
  setValue('vatAmount', formatNumber(vatAmount));
}

function recalculateAmounts() {
  const supplyAmount = parseMoney(getValue('supplyAmount'));
  const vatAmount = parseMoney(getValue('vatAmount'));
  setValue('totalAmount', formatNumber(Math.max(supplyAmount + vatAmount, 0)));
  refreshJournalPreview();
}

function formatMoneyInput(input) {
  input.value = formatNumber(parseMoney(input.value));
  recalculateAmounts();
}

function refreshJournalPreview() {
  const lines = buildPreviewLines();
  const tbody = document.getElementById('journalPreviewBody');
  if (!tbody) return;

  tbody.innerHTML = lines.map((line) => `
    <tr>
      <td>${line.side === 'DEBIT' ? '차변' : '대변'}</td>
      <td>${escapeHtml(line.accountName || '계정 선택 필요')}</td>
      <td class="amount">${line.side === 'DEBIT' ? formatWon(line.amount) : '-'}</td>
      <td class="amount">${line.side === 'CREDIT' ? formatWon(line.amount) : '-'}</td>
      <td>${escapeHtml(line.description)}</td>
    </tr>
  `).join('');

  const debit = lines.filter((line) => line.side === 'DEBIT').reduce((sum, line) => sum + line.amount, 0);
  const credit = lines.filter((line) => line.side === 'CREDIT').reduce((sum, line) => sum + line.amount, 0);
  const balanceState = document.getElementById('balanceState');
  if (!balanceState) return;
  balanceState.className = debit > 0 && debit === credit ? 'ok' : 'error';
  balanceState.textContent = debit > 0 && debit === credit
    ? `차대 일치 ${formatWon(debit)}`
    : `차변 ${formatWon(debit)} / 대변 ${formatWon(credit)}`;
}

function buildPreviewLines() {
  const businessAccount = state.selected.businessAccount;
  const settlementAccount = state.selected.settlementAccount;
  const supplyAmount = parseMoney(getValue('supplyAmount'));
  const vatAmount = parseMoney(getValue('vatAmount'));
  const feeAmount = parseMoney(getValue('feeAmount'));
  const totalAmount = supplyAmount + vatAmount;
  const lines = [];

  if (state.voucherType === 'SALES') {
    lines.push({
      side: 'DEBIT',
      accountName: settlementAccount?.name,
      amount: Math.max(totalAmount - feeAmount, 0),
      description: '입금/매출채권',
    });
    if (feeAmount > 0) {
      lines.push({ side: 'DEBIT', accountName: '지급수수료', amount: feeAmount, description: '카드/PG 수수료' });
    }
    lines.push({ side: 'CREDIT', accountName: businessAccount?.name, amount: supplyAmount, description: '매출계정' });
    if (vatAmount > 0) {
      lines.push({ side: 'CREDIT', accountName: '부가세예수금', amount: vatAmount, description: '부가세' });
    }
  } else {
    lines.push({ side: 'DEBIT', accountName: businessAccount?.name, amount: supplyAmount, description: '매입/비용계정' });
    if (vatAmount > 0) {
      lines.push({ side: 'DEBIT', accountName: '부가세대급금', amount: vatAmount, description: '부가세' });
    }
    lines.push({ side: 'CREDIT', accountName: settlementAccount?.name, amount: totalAmount, description: '출금/매입채무' });
  }

  return lines.filter((line) => line.amount > 0 || line.accountName);
}

async function handleSubmit(event) {
  event.preventDefault();
  const submitter = event.submitter;
  const nextStatus = submitter?.dataset.saveAction || 'DRAFT';
  const payload = buildPayload();
  const validationMessage = validatePayload(payload);

  if (validationMessage) {
    showToast(validationMessage, 'error');
    return;
  }

  try {
    const body = await postJson(API_BASE, payload);
    const saved = body.data;
    if (nextStatus === 'REQUESTED') {
      await changeStatus(saved.id, 'REQUESTED');
    }
    showToast('전표가 저장되었습니다.', 'success');
    resetForm();
    await loadSummary();
    await loadVouchers();
  } catch (error) {
    showToast(error.message || '전표 저장에 실패했습니다.', 'error');
  }
}

function buildPayload() {
  return {
    voucherDate: getValue('voucherDate'),
    voucherType: state.voucherType,
    vatType: getValue('vatType'),
    vendorId: state.selected.vendor?.id || null,
    vendorNameSnapshot: getValue('vendorName').trim(),
    supplyAmount: parseMoney(getValue('supplyAmount')),
    vatAmount: parseMoney(getValue('vatAmount')),
    feeAmount: parseMoney(getValue('feeAmount')),
    businessAccountId: Number(getValue('businessAccountId')),
    settlementAccountId: Number(getValue('settlementAccountId')),
    description: getValue('description').trim(),
  };
}

function validatePayload(payload) {
  if (!payload.voucherDate) return '전표일자를 입력해주세요.';
  if (!payload.vendorNameSnapshot) return '거래처를 선택해주세요.';
  if (!payload.supplyAmount || payload.supplyAmount <= 0) return '공급가액은 0보다 커야 합니다.';
  if (!payload.businessAccountId || !state.selected.businessAccount) return '계정과목을 검색해서 선택해주세요.';
  if (!payload.settlementAccountId || !state.selected.settlementAccount) return '입금/출금 계좌를 검색해서 선택해주세요.';
  if (payload.feeAmount < 0) return '수수료는 음수일 수 없습니다.';
  return '';
}

async function changeStatus(id, status) {
  await patchJson(`${API_BASE}/${id}/status?status=${status}`);
}

async function loadSummary() {
  setLoadingText('voucherListBody', 11, '전표 요약을 조회하고 있습니다.');
  const data = await fetchJson(`${API_BASE}/summary`, { data: {} });
  const summary = data.data || {};
  document.querySelectorAll('[data-summary]').forEach((el) => {
    el.textContent = summary[el.dataset.summary] ?? 0;
  });
  document.querySelectorAll('[data-summary-money]').forEach((el) => {
    el.textContent = formatWon(Number(summary[el.dataset.summaryMoney] || 0));
  });
}

async function loadVouchers() {
  setLoadingText('voucherListBody', 11, '전표 목록을 조회하고 있습니다.');
  const data = await fetchJson(`${API_BASE}?type=${state.voucherType}`, { data: [] });
  const vouchers = data.data || [];
  const tbody = document.getElementById('voucherListBody');
  if (!tbody) return;

  if (!vouchers.length) {
    tbody.innerHTML = `
      <tr>
        <td colspan="11" class="empty-state">
          저장된 전표가 없습니다. 계정과목과 거래처를 선택한 뒤 전표를 저장하면 이 목록에 바로 반영됩니다.
        </td>
      </tr>
    `;
    return;
  }

  tbody.innerHTML = vouchers.map((voucher) => {
    const totals = summarizeLines(voucher.lines || []);
    return `
      <tr>
        <td>${escapeHtml(voucher.voucherNo || '')}</td>
        <td>${voucher.voucherType === 'SALES' ? '매출전표' : '매입전표'}</td>
        <td>${escapeHtml(voucher.voucherDate || '')}</td>
        <td>${escapeHtml(voucher.vendorNameSnapshot || '')}</td>
        <td class="amount">${formatWon(totals.supply)}</td>
        <td class="amount">${formatWon(totals.vat)}</td>
        <td class="amount">${formatWon(totals.total)}</td>
        <td><span class="status-pill">${escapeHtml(voucher.status || '')}</span></td>
        <td>${escapeHtml(voucher.createdBy || '')}</td>
        <td>${formatDateTime(voucher.createdAt)}</td>
        <td>${voucher.status === 'POSTED' ? '게시' : '-'}</td>
      </tr>
    `;
  }).join('');
}

function summarizeLines(lines) {
  return lines.reduce((acc, line) => {
    acc.supply += Number(line.supplyAmount || 0);
    acc.vat += Number(line.vatAmount || 0);
    if (line.debitCredit === 'DEBIT') {
      acc.total += Number(line.totalAmount || 0);
    }
    return acc;
  }, { supply: 0, vat: 0, total: 0 });
}

function openLookup(target, initialKeyword = '') {
  state.lookupTarget = target;
  const dialog = document.getElementById('lookupDialog');
  const keyword = document.getElementById('lookupKeyword');
  const title = {
    vendor: '거래처 검색',
    businessAccount: state.voucherType === 'SALES' ? '매출계정 검색' : '매입계정 검색',
    settlementAccount: state.voucherType === 'SALES' ? '입금계좌 검색' : '출금계좌 검색',
  }[target] || '검색';

  setText('lookupTitle', title);
  if (keyword) keyword.value = initialKeyword;
  setHtml('lookupResults', '');
  if (dialog && !dialog.open) dialog.showModal();
  runLookupSearch();
}

async function runLookupSearch() {
  const keyword = getValue('lookupKeyword').trim();
  if (state.lookupTarget === 'vendor') {
    const params = new URLSearchParams();
    if (keyword) params.set('name', keyword);
    const vendors = await fetchJson(`${VENDOR_SEARCH_API}?${params.toString()}`, []);
    renderLookupRows((vendors || []).map((vendor) => ({
      id: vendor.id,
      code: vendor.vendorCode || vendor.businessRegistrationNo || '',
      name: vendor.name,
      type: vendor.status || '',
      level: 1,
      raw: vendor,
    })));
    return;
  }

  const params = new URLSearchParams();
  if (keyword) params.set('keyword', keyword);
  if (state.lookupTarget === 'settlementAccount') {
    params.set('cashOnly', 'true');
  } else {
    params.set('type', state.voucherType === 'SALES' ? 'REVENUE' : 'EXPENSE');
  }
  const accounts = await fetchJson(`${ACCOUNT_SEARCH_API}?${params.toString()}`, { data: [] });
  renderLookupRows((accounts.data || []).map((account) => ({
    id: account.id,
    code: account.code,
    name: account.name,
    type: account.type,
    status: account.status,
    allowPosting: account.allowPosting,
    level: account.level || 1,
    raw: account,
  })));
}

function renderLookupRows(rows) {
  const results = document.getElementById('lookupResults');
  if (!results) return;
  if (!rows.length) {
    results.innerHTML = '<div class="lookup-empty">검색 결과가 없습니다.</div>';
    return;
  }

  results.innerHTML = rows.map((row) => `
    <button type="button" class="lookup-row" data-id="${row.id}" style="--depth:${Math.max((row.level || 1) - 1, 0)}">
      <small>${escapeHtml(row.code)}</small>
      <strong>${escapeHtml(row.name)}</strong>
      <small>${escapeHtml(row.type)} · ${row.allowPosting === false ? '사용불가' : escapeHtml(row.status || 'ACTIVE')}</small>
    </button>
  `).join('');

  results.querySelectorAll('.lookup-row').forEach((button) => {
    const row = rows.find((item) => String(item.id) === button.dataset.id);
    if (row) {
      button.addEventListener('click', () => selectLookupRow(row.raw));
    }
  });
}

function selectLookupRow(row) {
  if (!row) return;
  if (state.lookupTarget === 'vendor') {
    state.selected.vendor = row;
    setValue('vendorName', row.name || '');
    setValue('vendorId', row.id || '');
  } else if (state.lookupTarget === 'businessAccount') {
    state.selected.businessAccount = row;
    setValue('businessAccountName', `${row.code} ${row.name}`);
    setValue('businessAccountId', row.id);
  } else if (state.lookupTarget === 'settlementAccount') {
    state.selected.settlementAccount = row;
    setValue('settlementAccountName', `${row.code} ${row.name}`);
    setValue('settlementAccountId', row.id);
  }

  document.getElementById('lookupDialog')?.close();
  refreshJournalPreview();
}

function resetForm() {
  document.getElementById('voucherForm')?.reset();
  const voucherDate = document.getElementById('voucherDate');
  if (voucherDate) voucherDate.valueAsDate = new Date();
  state.selected.vendor = null;
  clearSelectedAccounts();
  recalculateAmounts();
}

function clearSelectedAccounts() {
  state.selected.businessAccount = null;
  state.selected.settlementAccount = null;
  setValue('businessAccountName', '');
  setValue('businessAccountId', '');
  setValue('settlementAccountName', '');
  setValue('settlementAccountId', '');
}

async function postJson(url, payload) {
  return requestJson(url, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(payload),
  });
}

async function patchJson(url) {
  return requestJson(url, { method: 'PATCH' });
}

async function fetchJson(url, fallback) {
  try {
    return await requestJson(url);
  } catch (error) {
    showToast(error.message || '데이터 조회에 실패했습니다.', 'error');
    return fallback;
  }
}

async function requestJson(url, options = {}) {
  const response = await fetch(url, options);
  const text = await response.text();
  const body = text ? JSON.parse(text) : {};
  if (!response.ok || body.status === 'error') {
    throw new Error(body.message || `요청 실패 (${response.status})`);
  }
  return body;
}

function parseMoney(value) {
  const parsed = Number(String(value || '').replace(/[^\d]/g, ''));
  return Number.isFinite(parsed) && parsed > 0 ? parsed : 0;
}

function formatNumber(value) {
  const safe = Number.isFinite(Number(value)) ? Math.max(Math.floor(Number(value)), 0) : 0;
  return safe ? moneyFormatter.format(safe) : '0';
}

function formatWon(value) {
  return `${formatNumber(value)}원`;
}

function formatDateTime(value) {
  if (!value) return '';
  return String(value).replace('T', ' ').slice(0, 16);
}

function getValue(id) {
  return document.getElementById(id)?.value || '';
}

function setValue(id, value) {
  const el = document.getElementById(id);
  if (el) el.value = value;
}

function setText(id, value) {
  const el = document.getElementById(id);
  if (el) el.textContent = value;
}

function setHtml(id, value) {
  const el = document.getElementById(id);
  if (el) el.innerHTML = value;
}

function setLoadingText(tbodyId, colspan, message) {
  const tbody = document.getElementById(tbodyId);
  if (!tbody) return;
  tbody.innerHTML = `<tr><td colspan="${colspan}" class="empty-state">${escapeHtml(message)}</td></tr>`;
}

function showToast(message, type = 'success') {
  const toast = document.getElementById('toast');
  if (!toast) return;
  toast.textContent = message;
  toast.className = `toast show ${type}`;
  setTimeout(() => {
    toast.className = 'toast';
  }, 2600);
}

function escapeHtml(value) {
  return String(value ?? '')
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&#039;');
}
