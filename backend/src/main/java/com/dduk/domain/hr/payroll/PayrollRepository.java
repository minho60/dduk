package com.dduk.domain.hr.payroll;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface PayrollRepository extends JpaRepository<Payroll, Long> {
    List<Payroll> findByPayMonth(String payMonth);
    Optional<Payroll> findByEmployeeIdAndPayMonth(Long employeeId, String payMonth);
}
