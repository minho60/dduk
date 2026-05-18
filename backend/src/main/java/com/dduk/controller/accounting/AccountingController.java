package com.dduk.controller.accounting;

import com.dduk.domain.accounting.journal.AccountingService;
import com.dduk.domain.accounting.journal.JournalEntry;
import com.dduk.domain.accounting.journal.JournalLineRequest;
import com.dduk.domain.accounting.ledger.Account;
import com.dduk.domain.accounting.ledger.AccountRepository;
import com.dduk.domain.accounting.period.AccountingPeriod;
import com.dduk.domain.accounting.period.AccountingPeriodService;
import com.dduk.domain.accounting.report.ReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.*;

/**
 * 회계 API Controller
 * Base: /api/v1/accounting
 */
@RestController
@RequestMapping("/api/v1/accounting")
@RequiredArgsConstructor
public class AccountingController {

    private final AccountingService accountingService;
    private final ReportService reportService;
    private final AccountRepository accountRepository;
    private final AccountingPeriodService accountingPeriodService;

    // ── 전표 API ─────────────────────────────────────────────────────────

    /** 전표 생성 (DRAFT) */
    @PostMapping("/journals")
    public ResponseEntity<Map<String, Object>> createJournal(@RequestBody Map<String, Object> request) {
        LocalDate date = LocalDate.parse((String) request.get("date"));
        String description = (String) request.get("description");
        String sourceType = (String) request.getOrDefault("sourceType", null);
        Long sourceId = request.get("sourceId") != null ? Long.valueOf(request.get("sourceId").toString()) : null;

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> rawLines = (List<Map<String, Object>>) request.get("lines");

        List<JournalLineRequest> lineRequests = new ArrayList<>();
        for (Map<String, Object> raw : rawLines) {
            JournalLineRequest req = new JournalLineRequest();
            req.setAccountCode((String) raw.get("accountCode"));
            req.setDebitAmount(raw.get("debitAmount") != null
                    ? new java.math.BigDecimal(raw.get("debitAmount").toString()) : java.math.BigDecimal.ZERO);
            req.setCreditAmount(raw.get("creditAmount") != null
                    ? new java.math.BigDecimal(raw.get("creditAmount").toString()) : java.math.BigDecimal.ZERO);
            req.setDescription((String) raw.getOrDefault("description", null));
            lineRequests.add(req);
        }

        JournalEntry entry = accountingService.createJournal(date, description, lineRequests, sourceType, sourceId);
        return success(entry, "전표가 생성되었습니다.");
    }

    /** 전표 목록 조회 */
    @GetMapping("/journals")
    public ResponseEntity<Map<String, Object>> getJournals(
            @RequestParam(required = false) Integer fiscalYear,
            @RequestParam(required = false) Integer fiscalMonth,
            @RequestParam(required = false) String status) {
        List<JournalEntry> list;
        if (fiscalYear != null && fiscalMonth != null) {
            list = accountingService.findByFiscal(fiscalYear, fiscalMonth);
        } else if (status != null) {
            list = accountingService.findByStatus(status);
        } else {
            list = accountingService.findAll();
        }
        return success(list, "전표 목록 조회 완료");
    }

    /** 전표 상세 조회 */
    @GetMapping("/journals/{id}")
    public ResponseEntity<Map<String, Object>> getJournal(@PathVariable Long id) {
        return success(accountingService.findById(id), "전표 조회 완료");
    }

    /** 전표 승인 (DRAFT → APPROVED) */
    @PostMapping("/journals/{id}/approve")
    public ResponseEntity<Map<String, Object>> approveJournal(@PathVariable Long id) {
        return success(accountingService.approveJournal(id), "전표가 승인되었습니다.");
    }

    /** 전표 기표 (APPROVED → POSTED) */
    @PostMapping("/journals/{id}/post")
    public ResponseEntity<Map<String, Object>> postJournal(@PathVariable Long id) {
        return success(accountingService.postJournal(id), "전표가 기표되었습니다.");
    }

    /** 역분개 (POSTED → REVERSED + 역분개 전표 생성) */
    @PostMapping("/journals/{id}/reverse")
    public ResponseEntity<Map<String, Object>> reverseJournal(@PathVariable Long id) {
        return success(accountingService.reverseJournal(id), "역분개 전표가 생성되었습니다.");
    }

    /** 전표 삭제 (DRAFT 한정) */
    @DeleteMapping("/journals/{id}")
    public ResponseEntity<Map<String, Object>> deleteJournal(@PathVariable Long id) {
        accountingService.deleteJournal(id);
        return success(null, "전표가 삭제되었습니다.");
    }

    // ── 계정과목 API ─────────────────────────────────────────────────────

    /** 계정과목 목록 */
    @GetMapping("/accounts")
    public ResponseEntity<Map<String, Object>> getAccounts(
            @RequestParam(required = false) Boolean activeOnly) {
        List<Account> accounts = (activeOnly != null && activeOnly)
                ? accountRepository.findByIsActiveTrueOrderByCodeAsc()
                : accountRepository.findAll();
        return success(accounts, "계정과목 목록 조회 완료");
    }

    // ── 리포트 API ───────────────────────────────────────────────────────

    /** 합계잔액시산표 */
    @GetMapping("/reports/trial-balance")
    public ResponseEntity<Map<String, Object>> getTrialBalance(
            @RequestParam(required = false) Integer fiscalYear,
            @RequestParam(required = false) Integer fiscalMonth) {
        return success(reportService.getTrialBalance(fiscalYear, fiscalMonth), "합계잔액시산표 조회 완료");
    }

    /** 총계정원장 */
    @GetMapping("/reports/general-ledger")
    public ResponseEntity<Map<String, Object>> getGeneralLedger(
            @RequestParam String accountCode,
            @RequestParam(required = false) Integer fiscalYear,
            @RequestParam(required = false) Integer fiscalMonth) {
        return success(reportService.getGeneralLedger(accountCode, fiscalYear, fiscalMonth), "총계정원장 조회 완료");
    }

    /** 손익계산서 */
    @GetMapping("/reports/profit-loss")
    public ResponseEntity<Map<String, Object>> getProfitLoss(
            @RequestParam(required = false) Integer fiscalYear,
            @RequestParam(required = false) Integer fiscalMonth) {
        return success(reportService.getProfitAndLoss(fiscalYear, fiscalMonth), "손익계산서 조회 완료");
    }

    /** 재무상태표 */
    @GetMapping("/reports/balance-sheet")
    public ResponseEntity<Map<String, Object>> getBalanceSheet(
            @RequestParam(required = false) Integer fiscalYear,
            @RequestParam(required = false) Integer fiscalMonth) {
        return success(reportService.getBalanceSheet(fiscalYear, fiscalMonth), "재무상태표 조회 완료");
    }

    // ── 회계기간 API ─────────────────────────────────────────────────────

    /** 회계기간 목록 */
    @GetMapping("/periods")
    public ResponseEntity<Map<String, Object>> getPeriods() {
        List<AccountingPeriod> periods = accountingPeriodService.getAllPeriods();
        return success(periods, "회계기간 목록 조회 완료");
    }

    /** 회계기간 마감 */
    @PostMapping("/periods/{yearMonth}/close")
    public ResponseEntity<Map<String, Object>> closePeriod(
            @PathVariable String yearMonth,
            @RequestBody(required = false) Map<String, Object> body) {
        String[] parts = yearMonth.split("-");
        int year  = Integer.parseInt(parts[0]);
        int month = Integer.parseInt(parts[1]);
        String closedBy = body != null ? (String) body.getOrDefault("closedBy", "SYSTEM") : "SYSTEM";
        AccountingPeriod period = accountingPeriodService.closePeriod(year, month, closedBy);
        return success(period, year + "-" + String.format("%02d", month) + " 회계기간이 마감되었습니다.");
    }

    /** 회계기간 마감 취소 (재오픈) */
    @PostMapping("/periods/{yearMonth}/reopen")
    public ResponseEntity<Map<String, Object>> reopenPeriod(
            @PathVariable String yearMonth,
            @RequestBody(required = false) Map<String, Object> body) {
        String[] parts = yearMonth.split("-");
        int year  = Integer.parseInt(parts[0]);
        int month = Integer.parseInt(parts[1]);
        String reopenedBy = body != null ? (String) body.getOrDefault("reopenedBy", "SYSTEM") : "SYSTEM";
        AccountingPeriod period = accountingPeriodService.reopenPeriod(year, month, reopenedBy);
        return success(period, year + "-" + String.format("%02d", month) + " 회계기간 마감이 취소되었습니다.");
    }

    // ── 공통 응답 ────────────────────────────────────────────────────────

    private ResponseEntity<Map<String, Object>> success(Object data, String message) {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("status", "success");
        response.put("data", data);
        response.put("message", message);
        return ResponseEntity.ok(response);
    }
}
