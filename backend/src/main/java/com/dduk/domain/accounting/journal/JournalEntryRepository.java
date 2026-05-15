package com.dduk.domain.accounting.journal;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface JournalEntryRepository extends JpaRepository<JournalEntry, Long> {

    boolean existsBySourceTypeAndSourceId(String sourceType, Long sourceId);

    List<JournalEntry> findByStatus(String status);

    List<JournalEntry> findByFiscalYearAndFiscalMonth(Integer fiscalYear, Integer fiscalMonth);
    
    boolean existsByFiscalYearAndFiscalMonthAndStatusIn(Integer fiscalYear, Integer fiscalMonth, List<String> statuses);

    List<JournalEntry> findByFiscalYearOrderByTransactionDateDesc(Integer fiscalYear);

    /** 특정 계정코드의 기표된 전표 라인 집계 (합계잔액시산표용) */
    @Query("""
        SELECT l.account.code,
               SUM(l.debitAmount),
               SUM(l.creditAmount)
        FROM JournalEntry e
        JOIN e.lines l
        WHERE e.status IN ('POSTED', 'REVERSED')
          AND (:fiscalYear IS NULL OR e.fiscalYear = :fiscalYear)
          AND (:fiscalMonth IS NULL OR e.fiscalMonth = :fiscalMonth)
        GROUP BY l.account.code
    """)
    List<Object[]> aggregateByAccountCode(
            @Param("fiscalYear") Integer fiscalYear,
            @Param("fiscalMonth") Integer fiscalMonth
    );

    /** 특정 계정의 전표 라인 이력 (총계정원장용) */
    @Query("""
        SELECT e.transactionDate, e.journalNo, e.description,
               l.debitAmount, l.creditAmount, l.description
        FROM JournalEntry e
        JOIN e.lines l
        WHERE e.status IN ('POSTED', 'REVERSED')
          AND l.account.code = :accountCode
          AND (:fiscalYear IS NULL OR e.fiscalYear = :fiscalYear)
          AND (:fiscalMonth IS NULL OR e.fiscalMonth = :fiscalMonth)
        ORDER BY e.transactionDate ASC, e.id ASC
    """)
    List<Object[]> findGeneralLedgerByAccount(
            @Param("accountCode") String accountCode,
            @Param("fiscalYear") Integer fiscalYear,
            @Param("fiscalMonth") Integer fiscalMonth
    );
}
