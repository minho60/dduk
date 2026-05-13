package com.dduk.domain.accounting.report;

import com.dduk.domain.accounting.journal.JournalItem;
import com.dduk.domain.accounting.journal.JournalEntryRepository;
import com.dduk.domain.accounting.ledger.Account;
import com.dduk.domain.accounting.ledger.AccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReportService {

    private final AccountRepository accountRepository;
    private final JournalEntryRepository journalEntryRepository;

    public Map<String, Object> generateBalanceSheet() {
        List<Account> accounts = accountRepository.findAll();
        Map<String, BigDecimal> balances = calculateBalances();

        List<Map<String, Object>> assets = filterByAccountType(accounts, balances, "ASSET");
        List<Map<String, Object>> liabilities = filterByAccountType(accounts, balances, "LIABILITY");
        List<Map<String, Object>> equity = filterByAccountType(accounts, balances, "EQUITY");

        BigDecimal totalAssets = calculateTotal(assets);
        BigDecimal totalLiabilities = calculateTotal(liabilities);
        BigDecimal totalEquity = calculateTotal(equity);

        Map<String, Object> report = new HashMap<>();
        report.put("title", "Balance Sheet");
        report.put("assets", assets);
        report.put("liabilities", liabilities);
        report.put("equity", equity);
        report.put("totalAssets", totalAssets);
        report.put("totalLiabilities", totalLiabilities);
        report.put("totalEquity", totalEquity);
        report.put("isBalanced", totalAssets.compareTo(totalLiabilities.add(totalEquity)) == 0);

        return report;
    }

    public Map<String, Object> generateProfitAndLoss() {
        List<Account> accounts = accountRepository.findAll();
        Map<String, BigDecimal> balances = calculateBalances();

        List<Map<String, Object>> revenue = filterByAccountType(accounts, balances, "REVENUE");
        List<Map<String, Object>> expenses = filterByAccountType(accounts, balances, "EXPENSE");

        BigDecimal totalRevenue = calculateTotal(revenue);
        BigDecimal totalExpenses = calculateTotal(expenses);
        BigDecimal netIncome = totalRevenue.subtract(totalExpenses);

        Map<String, Object> report = new HashMap<>();
        report.put("title", "Profit & Loss Statement");
        report.put("revenue", revenue);
        report.put("expenses", expenses);
        report.put("totalRevenue", totalRevenue);
        report.put("totalExpenses", totalExpenses);
        report.put("netIncome", netIncome);

        return report;
    }

    private Map<String, BigDecimal> calculateBalances() {
        // In a real system, this would be optimized or use a summary table
        // For now, we aggregate from all journal entries
        Map<String, BigDecimal> balances = new HashMap<>();
        journalEntryRepository.findAll().forEach(entry -> {
            if ("POSTED".equals(entry.getStatus())) {
                entry.getItems().forEach(item -> {
                    String code = item.getAccount().getCode();
                    String type = item.getAccount().getType();
                    BigDecimal amount = item.getAmount();
                    
                    BigDecimal current = balances.getOrDefault(code, BigDecimal.ZERO);
                    if ("DEBIT".equals(item.getSide())) {
                        if ("ASSET".equals(type) || "EXPENSE".equals(type)) current = current.add(amount);
                        else current = current.subtract(amount);
                    } else {
                        if ("LIABILITY".equals(type) || "EQUITY".equals(type) || "REVENUE".equals(type)) current = current.add(amount);
                        else current = current.subtract(amount);
                    }
                    balances.put(code, current);
                });
            }
        });
        return balances;
    }

    private List<Map<String, Object>> filterByAccountType(List<Account> accounts, Map<String, BigDecimal> balances, String type) {
        return accounts.stream()
                .filter(a -> type.equals(a.getType()))
                .map(a -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("code", a.getCode());
                    map.put("name", a.getName());
                    map.put("balance", balances.getOrDefault(a.getCode(), BigDecimal.ZERO));
                    return map;
                })
                .collect(Collectors.toList());
    }

    private BigDecimal calculateTotal(List<Map<String, Object>> items) {
        return items.stream()
                .map(i -> (BigDecimal) i.get("balance"))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
