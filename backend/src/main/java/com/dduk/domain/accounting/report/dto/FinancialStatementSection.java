package com.dduk.domain.accounting.report.dto;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Builder
public class FinancialStatementSection {
    private String sectionCode;
    private String sectionName;
    private BigDecimal amount;
    private List<TrialBalanceRow> rows;
}
