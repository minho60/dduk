package com.dduk.domain.accounting.period.dto;

import java.math.BigDecimal;

public record PeriodAggregation(long voucherCount, BigDecimal totalDebit, BigDecimal totalCredit) {
    public static PeriodAggregation empty() {
        return new PeriodAggregation(0L, BigDecimal.ZERO, BigDecimal.ZERO);
    }
}
