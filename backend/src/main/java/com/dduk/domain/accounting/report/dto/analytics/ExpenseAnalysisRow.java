package com.dduk.domain.accounting.report.dto.analytics;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@Builder
public class ExpenseAnalysisRow {
    private String accountCode;
    private String accountName;
    private BigDecimal amount;
    private BigDecimal ratio;
}
