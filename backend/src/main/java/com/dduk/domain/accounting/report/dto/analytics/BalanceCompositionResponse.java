package com.dduk.domain.accounting.report.dto.analytics;

import com.dduk.entity.accounting.AccountType;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@AllArgsConstructor
public class BalanceCompositionResponse {
    private AccountType accountType;
    private String label;
    private BigDecimal amount;
    private BigDecimal ratio;
}
