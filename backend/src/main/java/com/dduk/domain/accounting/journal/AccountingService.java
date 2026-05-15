package com.dduk.domain.accounting.journal;

import com.dduk.domain.accounting.AccountingConstants;
import com.dduk.domain.accounting.ledger.Account;
import com.dduk.domain.accounting.ledger.AccountRepository;
import com.dduk.domain.hr.payroll.Payroll;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AccountingService {

    private final JournalEntryRepository journalEntryRepository;
    private final AccountRepository accountRepository;

    @Transactional(rollbackFor = Exception.class)
    public JournalEntry createAndPost(LocalDate date, String description, List<Map<String, Object>> items, String sourceType, Long sourceId) {
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
                .status(AccountingConstants.JOURNAL_STATUS_POSTED)
                .sourceType(sourceType)
                .sourceId(sourceId)
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

            if (AccountingConstants.SIDE_DEBIT.equals(side)) {
                debitSum = debitSum.add(amount);
            } else if (AccountingConstants.SIDE_CREDIT.equals(side)) {
                creditSum = creditSum.add(amount);
            }
        }

        // 2. Double Entry Validation
        if (debitSum.compareTo(BigDecimal.ZERO) == 0 || creditSum.compareTo(BigDecimal.ZERO) == 0) {
             throw new RuntimeException("금액은 0보다 커야 합니다.");
        }

        if (debitSum.compareTo(creditSum) != 0) {
            throw new RuntimeException("차대 불일치: 차변(" + debitSum + ")과 대변(" + creditSum + ")의 합계가 일치해야 합니다.");
        }

        return journalEntryRepository.save(entry);
    }

    /**
     * 급여 정보를 바탕으로 회계 전표 생성
     */
    @Transactional(rollbackFor = Exception.class)
    public JournalEntry createPayrollJournal(Payroll payroll) {
        // 중복 전표 체크 (Source 기반)
        if (journalEntryRepository.existsBySourceTypeAndSourceId(AccountingConstants.SOURCE_PAYROLL, payroll.getId())) {
             throw new RuntimeException("이미 해당 급여에 대한 회계 전표가 존재합니다.");
        }

        String description = String.format("%s 급여 확정 반영 (사번: %s)", 
            payroll.getPayMonth(), payroll.getEmployee().getEmployeeNo());
        
        List<Map<String, Object>> items = new ArrayList<>();
        items.add(Map.of("accountCode", AccountingConstants.PAYROLL_EXPENSE, "amount", payroll.getBaseSalary().add(payroll.getAllowanceAmount()), "side", AccountingConstants.SIDE_DEBIT));
        items.add(Map.of("accountCode", AccountingConstants.WITHHOLDING_PAYABLE, "amount", payroll.getDeductionAmount(), "side", AccountingConstants.SIDE_CREDIT));
        items.add(Map.of("accountCode", AccountingConstants.SALARY_PAYABLE, "amount", payroll.getNetSalary(), "side", AccountingConstants.SIDE_CREDIT));

        return createAndPost(LocalDate.now(), description, items, AccountingConstants.SOURCE_PAYROLL, payroll.getId());
    }

    /**
     * 매입 발주 정보를 바탕으로 회계 전표 생성
     */
    @Transactional(rollbackFor = Exception.class)
    public JournalEntry createPurchaseJournal(com.dduk.domain.inventory.purchase.PurchaseOrder order) {
        // 중복 전표 체크
        if (journalEntryRepository.existsBySourceTypeAndSourceId(AccountingConstants.SOURCE_PURCHASE, order.getId())) {
             throw new RuntimeException("이미 해당 발주에 대한 회계 전표가 존재합니다.");
        }

        String description = String.format("%s 매입 확정 (%s)", 
            order.getPurchaseOrderNo(), order.getVendor().getName());

        List<Map<String, Object>> items = new ArrayList<>();
        
        // 차변: 재고자산
        items.add(Map.of(
            "accountCode", AccountingConstants.INVENTORY_ASSET,
            "amount", order.getTotalAmount(),
            "side", AccountingConstants.SIDE_DEBIT
        ));

        // 대변: 외상매입금
        items.add(Map.of(
            "accountCode", AccountingConstants.ACCOUNTS_PAYABLE,
            "amount", order.getTotalAmount(),
            "side", AccountingConstants.SIDE_CREDIT
        ));

        return createAndPost(order.getOrderDate(), description, items, AccountingConstants.SOURCE_PURCHASE, order.getId());
    }
}
