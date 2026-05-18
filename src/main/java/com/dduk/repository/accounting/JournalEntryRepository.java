package com.dduk.repository.accounting;

import com.dduk.entity.accounting.JournalEntry;
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

    @Query("""
        SELECT l.account.code,
               SUM(l.debitAmount),
               SUM(l.creditAmount)
        FROM JournalEntry e
        JOIN e.lines l
        WHERE e.status = 'POSTED'
          AND (:fiscalYear IS NULL OR e.fiscalYear = :fiscalYear)
          AND (:fiscalMonth IS NULL OR e.fiscalMonth = :fiscalMonth)
        GROUP BY l.account.code
    """)
    List<Object[]> aggregateByAccountCode(
            @Param("fiscalYear") Integer fiscalYear,
            @Param("fiscalMonth") Integer fiscalMonth
    );

    @Query("""
        SELECT e.transactionDate, e.journalNo, e.description,
               l.debitAmount, l.creditAmount, l.description
        FROM JournalEntry e
        JOIN e.lines l
        WHERE e.status = 'POSTED'
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
