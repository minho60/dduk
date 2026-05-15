package com.dduk.domain.accounting.period;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AccountingPeriodRepository extends JpaRepository<AccountingPeriod, Long> {

    Optional<AccountingPeriod> findByFiscalYearAndFiscalMonth(Integer fiscalYear, Integer fiscalMonth);

    boolean existsByFiscalYearAndFiscalMonthAndStatus(Integer fiscalYear, Integer fiscalMonth, String status);
}
