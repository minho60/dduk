package com.dduk.controller.hr;

import com.dduk.dto.hr.PayrollCalculationRequestDto;
import com.dduk.dto.hr.PayrollStatusTransitionRequestDto;
import com.dduk.entity.hr.Payroll;
import com.dduk.service.hr.PayrollService;
import com.dduk.service.hr.PayrollStatusService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/hr/payroll")
@RequiredArgsConstructor
public class PayrollController {

    private final PayrollService payrollService;
    private final PayrollStatusService statusService;

    @PostMapping("/calculate")
    public Payroll calculate(@RequestBody PayrollCalculationRequestDto request) {
        return payrollService.calculatePayroll(
                request.getEmployeeId(),
                request.getPayMonth(),
                request.getInputs()
        );
    }

    @PostMapping("/{id}/transition")
    public Payroll transition(
            @PathVariable Long id,
            @RequestBody PayrollStatusTransitionRequestDto request
    ) {
        return statusService.transition(
                id,
                request.getNextStatus(),
                request.getUserId(),
                request.getReason()
        );
    }
}
