package com.dduk.domain.accounting.dashboard.dto;

import com.dduk.domain.accounting.period.entity.AccountingPeriodStatus;
import com.dduk.domain.accounting.period.entity.ClosingValidationStatus;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@Builder
public class PeriodDashboardSummary {
    private String currentPeriod;
    private Integer fiscalYear;
    private Integer fiscalMonth;
    private AccountingPeriodStatus status;
    private ClosingValidationStatus validationStatus;
    private long unpostedVoucherCount;
    private boolean balanced;
    private BigDecimal totalDebit;
    private BigDecimal totalCredit;
    private boolean periodFound;
}
