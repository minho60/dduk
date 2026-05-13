package com.dduk.controller.hr;

import com.dduk.domain.hr.payroll.Payroll;
import com.dduk.domain.hr.payroll.PayrollService;
import com.dduk.domain.hr.payroll.PayrollStatusService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/hr/payroll")
@RequiredArgsConstructor
public class PayrollController {

    private final PayrollService payrollService;
    private final PayrollStatusService statusService;

    @PostMapping("/calculate")
    public Payroll calculate(@RequestBody Map<String, Object> request) {
        Long employeeId = Long.valueOf(request.get("employeeId").toString());
        String payMonth = (String) request.get("payMonth");
        Map<String, Object> inputs = (Map<String, Object>) request.get("inputs");
        return payrollService.calculatePayroll(employeeId, payMonth, inputs);
    }

    @PostMapping("/{id}/transition")
    public Payroll transition(@PathVariable Long id, @RequestBody Map<String, String> request) {
        String nextStatus = request.get("nextStatus");
        String userId = request.get("userId");
        String reason = request.get("reason");
        return statusService.transition(id, nextStatus, userId, reason);
    }
}
