package com.dduk.domain.accounting.payroll.repository;

import com.dduk.domain.accounting.payroll.entity.PayrollLedger;
import com.dduk.domain.accounting.payroll.entity.PayrollStatus;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface PayrollLedgerRepository extends JpaRepository<PayrollLedger, Long> {

    List<PayrollLedger> findTop100ByOrderByPaymentDateDescIdDesc();

    long countByStatus(PayrollStatus status);

    @Query("""
            select count(l)
            from PayrollLedger l
            where l.paymentYearMonth = :yearMonth
              and l.status not in :closedStatuses
            """)
    long countOpenLedgersByPaymentYearMonth(
            @Param("yearMonth") String yearMonth,
            @Param("closedStatuses") List<PayrollStatus> closedStatuses
    );

    @Query("select coalesce(sum(l.netAmount), 0) from PayrollLedger l where l.status in :statuses")
    BigDecimal sumNetAmountByStatusIn(@Param("statuses") List<PayrollStatus> statuses);

    @Query("select coalesce(sum(l.deductionAmount), 0) from PayrollLedger l where l.status in :statuses")
    BigDecimal sumDeductionAmountByStatusIn(@Param("statuses") List<PayrollStatus> statuses);

    @EntityGraph(attributePaths = {
            "employees",
            "employees.employee",
            "journalEntry"
    })
    Optional<PayrollLedger> findWithEmployeesById(Long id);
}
