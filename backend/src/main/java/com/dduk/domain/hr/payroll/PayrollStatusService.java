package com.dduk.domain.hr.payroll;

import com.dduk.domain.accounting.AccountingConstants;
import com.dduk.domain.accounting.autojounal.AutoJournalService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class PayrollStatusService {

    private final PayrollRepository payrollRepository;
    private final AutoJournalService autoJournalService;

    private static final Map<String, List<String>> VALID_TRANSITIONS = new HashMap<>();

    static {
        VALID_TRANSITIONS.put("DRAFT", Arrays.asList("CALCULATING", "CANCELLED"));
        VALID_TRANSITIONS.put("CALCULATING", Arrays.asList("CALCULATED", "DRAFT"));
        VALID_TRANSITIONS.put("CALCULATED", Arrays.asList("PENDING_APPROVAL", "DRAFT"));
        VALID_TRANSITIONS.put("PENDING_APPROVAL", Arrays.asList("APPROVED", "DRAFT"));
        VALID_TRANSITIONS.put("APPROVED", Arrays.asList("POSTED", "REVERSED"));
        VALID_TRANSITIONS.put("POSTED", Arrays.asList("PAID", "REVERSED"));
        VALID_TRANSITIONS.put("PAID", Arrays.asList("REVERSED"));
        VALID_TRANSITIONS.put("REVERSED", List.of());
        VALID_TRANSITIONS.put("CANCELLED", List.of());
    }

    @Transactional(rollbackFor = Exception.class)
    public Payroll transition(Long payrollId, String nextStatus, String userId, String reason) {
        Payroll payroll = payrollRepository.findById(payrollId)
                .orElseThrow(() -> new RuntimeException("Payroll record not found"));

        String currentStatus = payroll.getStatus();
        
        // 1. 이미 POSTED인 경우 중복 처리 방지
        if ("POSTED".equals(currentStatus) && "POSTED".equals(nextStatus)) {
            return payroll;
        }

        List<String> allowed = VALID_TRANSITIONS.getOrDefault(currentStatus, List.of());

        if (!allowed.contains(nextStatus)) {
            throw new RuntimeException("Invalid transition: " + currentStatus + " -> " + nextStatus);
        }

        // 2. POSTED 전환 시 회계 전표 생성 (트랜잭션 내 포함)
        if ("POSTED".equals(nextStatus)) {
            autoJournalService.createAndPostJournal(
                    AccountingConstants.SOURCE_PAYROLL,
                    payroll.getId(),
                    payroll
            );
        }

        payroll.setStatus(nextStatus);
        
        log.info("[StatusService] {}: {} -> {} (by {}, reason: {})", 
            payrollId, currentStatus, nextStatus, userId, reason);

        return payrollRepository.save(payroll);
    }

    public boolean isImmutable(String status) {
        return Arrays.asList("APPROVED", "POSTED", "PAID", "REVERSED").contains(status);
    }
}
