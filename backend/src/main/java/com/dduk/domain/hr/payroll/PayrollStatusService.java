package com.dduk.domain.hr.payroll;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class PayrollStatusService {

    private final PayrollRepository payrollRepository;

    private static final Map<String, List<String>> VALID_TRANSITIONS = new HashMap<>();

    static {
        VALID_TRANSITIONS.put("DRAFT", Arrays.asList("CALCULATING", "CANCELLED"));
        VALID_TRANSITIONS.put("CALCULATING", Arrays.asList("CALCULATED", "DRAFT"));
        VALID_TRANSITIONS.put("CALCULATED", Arrays.asList("PENDING_APPROVAL", "DRAFT"));
        VALID_TRANSITIONS.put("PENDING_APPROVAL", Arrays.asList("APPROVED", "DRAFT"));
        VALID_TRANSITIONS.put("APPROVED", Arrays.asList("POSTED", "REVERSED"));
        VALID_TRANSITIONS.put("POSTED", Arrays.asList("PAID", "REVERSED"));
        VALID_TRANSITIONS.put("PAID", Arrays.asList("REVERSED"));
        VALID_TRANSITIONS.put("REVERSED", Arrays.asList());
        VALID_TRANSITIONS.put("CANCELLED", Arrays.asList());
    }

    @Transactional
    public Payroll transition(Long payrollId, String nextStatus, String userId, String reason) {
        Payroll payroll = payrollRepository.findById(payrollId)
                .orElseThrow(() -> new RuntimeException("Payroll record not found"));

        String currentStatus = payroll.getStatus();
        List<String> allowed = VALID_TRANSITIONS.getOrDefault(currentStatus, Arrays.asList());

        if (!allowed.contains(nextStatus)) {
            throw new RuntimeException("Invalid transition: " + currentStatus + " -> " + nextStatus);
        }

        payroll.setStatus(nextStatus);
        // In a real system, we would log this to an audit table
        System.out.println(String.format("[StatusService] %d: %s -> %s (by %s, reason: %s)", 
            payrollId, currentStatus, nextStatus, userId, reason));

        return payrollRepository.save(payroll);
    }

    public boolean isImmutable(String status) {
        return Arrays.asList("APPROVED", "POSTED", "PAID", "REVERSED").contains(status);
    }
}
