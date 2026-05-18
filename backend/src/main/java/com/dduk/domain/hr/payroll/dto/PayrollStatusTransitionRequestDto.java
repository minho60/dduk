package com.dduk.domain.hr.payroll.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PayrollStatusTransitionRequestDto {
    private String nextStatus;
    private String userId;
    private String reason;
}
