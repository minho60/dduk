package com.dduk.domain.accounting.report.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.util.List;

@Getter
@Builder
public class FinancialStatementResponse {
    private FinancialStatementType statementType;
    private LocalDate startDate;
    private LocalDate endDate;
    private List<FinancialStatementSection> sections;
}
