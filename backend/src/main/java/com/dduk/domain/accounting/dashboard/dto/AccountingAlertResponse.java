package com.dduk.domain.accounting.dashboard.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AccountingAlertResponse {
    private AccountingAlertSeverity severity;
    private String category;
    private String title;
    private String message;
    private String actionLabel;
    private String actionUrl;
}
