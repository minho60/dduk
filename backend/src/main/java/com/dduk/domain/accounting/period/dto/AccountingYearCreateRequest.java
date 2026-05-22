package com.dduk.domain.accounting.period.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AccountingYearCreateRequest {
    private Integer fiscalYear;
    private String createdBy;
}
