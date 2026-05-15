package com.dduk.domain.hr.payroll;

import com.dduk.domain.hr.contract.PayrollContract;
import com.dduk.domain.hr.contract.PayrollContractRepository;
import com.dduk.domain.hr.employee.Employee;
import com.dduk.domain.hr.employee.EmployeeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class PayrollService {

    private final PayrollRepository payrollRepository;
    private final PayrollContractRepository contractRepository;
    private final EmployeeRepository employeeRepository;

    /**
     * Calculate and Save Payroll
     */
    @Transactional
    public Payroll calculatePayroll(Long employeeId, String payMonth, Map<String, Object> inputs) {
        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new RuntimeException("Employee not found"));

        PayrollContract contract = contractRepository.findByEmployeeId(employeeId)
                .orElseThrow(() -> new RuntimeException("Payroll Contract not found for employee"));

        BigDecimal baseSalary = contract.getBaseSalary();
        BigDecimal overtimeHours = new BigDecimal(inputs.getOrDefault("overtimeHours", 0).toString());
        BigDecimal bonus = new BigDecimal(inputs.getOrDefault("bonus", 0).toString());
        BigDecimal allowance = new BigDecimal(inputs.getOrDefault("allowance", 0).toString());

        // 1. Overtime Calculation
        BigDecimal hourlyRate = baseSalary.divide(new BigDecimal("209"), 2, RoundingMode.HALF_UP);
        BigDecimal overtimePay = hourlyRate.multiply(new BigDecimal("1.5")).multiply(overtimeHours).setScale(0, RoundingMode.DOWN);

        // 2. Gross Pay
        BigDecimal grossPay = baseSalary.add(overtimePay).add(bonus).add(allowance);

        // 3. Deductions (Simplified for migration)
        BigDecimal pension = grossPay.multiply(new BigDecimal("0.045")).setScale(0, RoundingMode.DOWN); // 4.5%
        BigDecimal health = grossPay.multiply(new BigDecimal("0.03545")).setScale(0, RoundingMode.DOWN); // 3.545%
        BigDecimal longTermCare = health.multiply(new BigDecimal("0.1295")).setScale(0, RoundingMode.DOWN); // 12.95% of health
        BigDecimal employmentInsurance = grossPay.multiply(new BigDecimal("0.009")).setScale(0, RoundingMode.DOWN); // 0.9%

        // 4. Income Tax (Simplified Bracket)
        BigDecimal annualSalary = grossPay.multiply(new BigDecimal("12"));
        BigDecimal incomeTax;
        if (annualSalary.compareTo(new BigDecimal("14000000")) <= 0) {
            incomeTax = annualSalary.multiply(new BigDecimal("0.06")).divide(new BigDecimal("12"), 0, RoundingMode.DOWN);
        } else if (annualSalary.compareTo(new BigDecimal("50000000")) <= 0) {
            incomeTax = annualSalary.multiply(new BigDecimal("0.15")).subtract(new BigDecimal("1260000")).divide(new BigDecimal("12"), 0, RoundingMode.DOWN);
        } else {
            incomeTax = annualSalary.multiply(new BigDecimal("0.24")).subtract(new BigDecimal("5760000")).divide(new BigDecimal("12"), 0, RoundingMode.DOWN);
        }
        BigDecimal localIncomeTax = incomeTax.multiply(new BigDecimal("0.1")).setScale(0, RoundingMode.DOWN);

        BigDecimal totalDeductions = pension.add(health).add(longTermCare).add(employmentInsurance).add(incomeTax).add(localIncomeTax);
        BigDecimal netPay = grossPay.subtract(totalDeductions);

        Payroll payroll = payrollRepository.findByEmployeeIdAndPayMonth(employeeId, payMonth)
                .orElseGet(Payroll::new);

        payroll.setEmployee(employee);
        payroll.setPayMonth(payMonth);
        payroll.setBaseSalary(baseSalary);
        payroll.setAllowanceAmount(bonus.add(allowance).add(overtimePay));
        payroll.setDeductionAmount(totalDeductions);
        payroll.setNetSalary(netPay);
        payroll.setStatus("CALCULATED");
        
        // Detailed Trace (JSON-like string)
        payroll.setCalculationTrace(String.format(
            "{\"base\":%s, \"overtime\":%s, \"gross\":%s, \"deductions\":{\"pension\":%s, \"health\":%s, \"tax\":%s}, \"net\":%s}",
            baseSalary, overtimePay, grossPay, pension, health, incomeTax, netPay
        ));

        return payrollRepository.save(payroll);
    }
}
