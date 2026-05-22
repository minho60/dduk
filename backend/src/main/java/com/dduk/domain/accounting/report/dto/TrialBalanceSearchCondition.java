package com.dduk.domain.accounting.report.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class TrialBalanceSearchCondition {
    private LocalDate startDate;
    private LocalDate endDate;
    private ReportBasis reportBasis = ReportBasis.MONTHLY;
    private AccountLevelFilter accountLevel = AccountLevelFilter.ALL;
    private boolean includeZeroBalance;
    private boolean includeSubAccounts = true;
    private boolean summaryOnly;
    private boolean profitLossFormat;
}
