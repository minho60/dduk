package com.dduk.domain.accounting.period.dto;

import com.dduk.domain.accounting.period.entity.ClosingValidationStatus;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ClosingValidationResult {
    private String validationType;
    private ClosingValidationStatus status;
    private String detail;
    private long targetCount;
    private boolean actionRequired;
}
