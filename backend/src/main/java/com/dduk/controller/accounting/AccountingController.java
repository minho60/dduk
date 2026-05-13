package com.dduk.controller.accounting;

import com.dduk.domain.accounting.journal.AccountingService;
import com.dduk.domain.accounting.journal.JournalEntry;
import com.dduk.domain.accounting.report.ReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/accounting")
@RequiredArgsConstructor
public class AccountingController {

    private final AccountingService accountingService;
    private final ReportService reportService;

    @PostMapping("/journal")
    public JournalEntry createJournal(@RequestBody Map<String, Object> request) {
        LocalDate date = LocalDate.parse((String) request.get("date"));
        String description = (String) request.get("description");
        List<Map<String, Object>> items = (List<Map<String, Object>>) request.get("items");
        return accountingService.createAndPost(date, description, items);
    }

    @GetMapping("/report/balance-sheet")
    public Map<String, Object> getBalanceSheet() {
        return reportService.generateBalanceSheet();
    }

    @GetMapping("/report/profit-loss")
    public Map<String, Object> getProfitLoss() {
        return reportService.generateProfitAndLoss();
    }
}
