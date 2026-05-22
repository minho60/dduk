package com.dduk.domain.accounting.period.dto;

import com.dduk.domain.accounting.period.entity.ClosingValidationStatus;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class ClosingValidationResponse {
    private String periodKey;
    private ClosingValidationStatus overallStatus;
    private boolean closable;
    private List<ClosingValidationResult> results;
}
