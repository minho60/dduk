package com.dduk.domain.accounting.dashboard.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.Map;

@Getter
@Builder
public class VoucherDashboardSummary {
    private long todayVoucherCount;
    private long draftCount;
    private long pendingApprovalCount;
    private long approvedCount;
    private long postedCount;
    private long cancelledCount;
    private Map<String, Long> statusCounts;
}
