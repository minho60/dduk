package com.dduk.domain.accounting.report.dto.analytics;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Builder
public class PayrollAnalysisResponse {
    private BigDecimal grossAmount;
    private BigDecimal bonusAmount;
    private BigDecimal deductionAmount;
    private BigDecimal netAmount;
    private List<PayrollDepartmentRow> departments;
}
