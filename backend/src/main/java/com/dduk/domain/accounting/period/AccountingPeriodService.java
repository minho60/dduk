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
     * - 이미 마감된 기간은 예외 발생
     * - 존재하지 않는 기간이면 신규 생성 후 마감
     */
    @Transactional
    public AccountingPeriod closePeriod(int fiscalYear, int fiscalMonth, String closedBy) {
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
