package com.dduk.domain.accounting.period.repository;

import com.dduk.domain.accounting.period.entity.ClosingLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ClosingLogRepository extends JpaRepository<ClosingLog, Long> {

    List<ClosingLog> findByAccountingPeriodIdOrderByActionAtDesc(Long accountingPeriodId);

    List<ClosingLog> findTop10ByOrderByActionAtDesc();
}
