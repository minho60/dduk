package com.dduk.domain.hr.payroll.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.Map;

@Getter
@Setter
public class PayrollCalculationRequestDto {
    private Long employeeId;
    private String payMonth;
    private Map<String, Object> inputs;
}
