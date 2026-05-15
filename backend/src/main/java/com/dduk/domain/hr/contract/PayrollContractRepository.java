package com.dduk.domain.hr.contract;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface PayrollContractRepository extends JpaRepository<PayrollContract, Long> {
    Optional<PayrollContract> findByEmployeeId(Long employeeId);
    Optional<PayrollContract> findByContractNo(String contractNo);
}
