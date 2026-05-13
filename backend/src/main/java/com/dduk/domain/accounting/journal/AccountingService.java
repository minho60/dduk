package com.dduk.domain.accounting.journal;

import com.dduk.domain.accounting.ledger.Account;
import com.dduk.domain.accounting.ledger.AccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AccountingService {

    private final JournalEntryRepository journalEntryRepository;
    private final AccountRepository accountRepository;

    @Transactional(rollbackFor = Exception.class)
    public JournalEntry createAndPost(LocalDate date, String description, List<Map<String, Object>> items) {
        if (items == null || items.isEmpty()) {
            throw new RuntimeException("분개 항목이 비어 있습니다.");
        }

        // 1. Fiscal Period Check (Mocked placeholder)
        String yearMonth = date.toString().substring(0, 7);
        if ("2026-04".equals(yearMonth)) { // Example: April is closed
            throw new RuntimeException("마감된 회계 기간(" + yearMonth + ")에는 전표를 생성할 수 없습니다.");
        }

        BigDecimal debitSum = BigDecimal.ZERO;
        BigDecimal creditSum = BigDecimal.ZERO;

        JournalEntry entry = JournalEntry.builder()
                .journalNo("JRN-" + System.currentTimeMillis())
                .transactionDate(date)
                .description(description)
                .status("POSTED")
                .build();

        for (Map<String, Object> itemData : items) {
            String accountCode = (String) itemData.get("accountCode");
            BigDecimal amount = new BigDecimal(itemData.get("amount").toString());
            String side = (String) itemData.get("side");

            Account account = accountRepository.findByCode(accountCode)
                    .orElseThrow(() -> new RuntimeException("계정 코드를 찾을 수 없습니다: " + accountCode));

            JournalItem item = JournalItem.builder()
                    .account(account)
                    .amount(amount)
                    .side(side)
                    .build();

            entry.addItem(item);

            if ("DEBIT".equals(side)) {
                debitSum = debitSum.add(amount);
            } else if ("CREDIT".equals(side)) {
                creditSum = creditSum.add(amount);
            }
        }

        // 2. Double Entry Validation (Double Check)
        if (debitSum.compareTo(BigDecimal.ZERO) == 0 || creditSum.compareTo(BigDecimal.ZERO) == 0) {
             throw new RuntimeException("금액은 0보다 커야 합니다.");
        }

        if (debitSum.compareTo(creditSum) != 0) {
            throw new RuntimeException("차대 불일치: 차변(" + debitSum + ")과 대변(" + creditSum + ")의 합계가 일치해야 합니다.");
        }

        return journalEntryRepository.save(entry);
    }
}
