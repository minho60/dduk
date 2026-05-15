package com.dduk.domain.accounting.period;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 회계 기간 마감 서비스
 * - CLOSED 기간 내 전표 생성/수정/역분개 금지
 */
@Service
@RequiredArgsConstructor
public class AccountingPeriodService {

    private final AccountingPeriodRepository accountingPeriodRepository;

    private final com.dduk.domain.accounting.journal.JournalEntryRepository journalEntryRepository;

    @Transactional(readOnly = true)
    public List<AccountingPeriod> getAllPeriods() {
        return accountingPeriodRepository.findAll();
    }
    
    @Transactional(readOnly = true)
    public boolean isClosed(int fiscalYear, int fiscalMonth) {
        return accountingPeriodRepository
                .existsByFiscalYearAndFiscalMonthAndStatus(fiscalYear, fiscalMonth, "CLOSED");
    }

    /**
     * 회계 기간 마감
     * - 미결 전표(DRAFT, APPROVED)가 존재하면 마감 불가
     * - 이미 마감된 기간은 예외 발생
     */
    @Transactional
    public AccountingPeriod closePeriod(int fiscalYear, int fiscalMonth, String closedBy) {
        // 미결 전표 체크
        boolean hasPending = journalEntryRepository.existsByFiscalYearAndFiscalMonthAndStatusIn(
                fiscalYear, fiscalMonth, List.of("DRAFT", "APPROVED"));
        if (hasPending) {
            throw new IllegalStateException(
                    String.format("%d-%02d 기간에 승인되지 않거나 기표되지 않은 전표가 존재하여 마감할 수 없습니다.", fiscalYear, fiscalMonth));
        }

        AccountingPeriod period = accountingPeriodRepository
                .findByFiscalYearAndFiscalMonth(fiscalYear, fiscalMonth)
                .orElseGet(() -> AccountingPeriod.builder()
                        .fiscalYear(fiscalYear)
                        .fiscalMonth(fiscalMonth)
                        .status("OPEN")
                        .build());

        period.close(closedBy);
        return accountingPeriodRepository.save(period);
    }

    /**
     * 회계 기간 재오픈
     * - OPEN 상태면 예외 발생
     * - CLOSED 상태인 경우에만 OPEN으로 전환
     */
    @Transactional
    public AccountingPeriod reopenPeriod(int fiscalYear, int fiscalMonth, String reopenedBy) {
        AccountingPeriod period = accountingPeriodRepository
                .findByFiscalYearAndFiscalMonth(fiscalYear, fiscalMonth)
                .orElseThrow(() -> new IllegalArgumentException(
                        "존재하지 않는 회계 기간입니다: " + fiscalYear + "-" + String.format("%02d", fiscalMonth)));

        period.reopen(reopenedBy);
        return accountingPeriodRepository.save(period);
    }
}
