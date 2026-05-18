package com.dduk.repository.hr;

import com.dduk.entity.hr.PayrollContract;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface PayrollContractRepository extends JpaRepository<PayrollContract, Long> {
    Optional<PayrollContract> findByEmployeeId(Long employeeId);
    Optional<PayrollContract> findByContractNo(String contractNo);
}
