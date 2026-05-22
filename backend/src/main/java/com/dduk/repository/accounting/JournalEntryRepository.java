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

    long countByFiscalYearAndFiscalMonthAndStatusIn(Integer fiscalYear, Integer fiscalMonth, List<String> statuses);

    @Query("""
        SELECT count(e)
        FROM JournalEntry e
        WHERE e.fiscalYear = :fiscalYear
          AND e.fiscalMonth = :fiscalMonth
          AND e.status <> 'POSTED'
    """)
    long countUnpostedByFiscalPeriod(@Param("fiscalYear") Integer fiscalYear, @Param("fiscalMonth") Integer fiscalMonth);

    @Query("""
        SELECT coalesce(sum(e.totalDebit), 0), coalesce(sum(e.totalCredit), 0)
        FROM JournalEntry e
        WHERE e.fiscalYear = :fiscalYear
          AND e.fiscalMonth = :fiscalMonth
    """)
    Object[] sumEntryTotalsByFiscalPeriod(@Param("fiscalYear") Integer fiscalYear, @Param("fiscalMonth") Integer fiscalMonth);

    @Query("""
        SELECT count(e)
        FROM JournalEntry e
        WHERE e.fiscalYear = :fiscalYear
          AND e.fiscalMonth = :fiscalMonth
          AND e.totalDebit <> e.totalCredit
    """)
    long countUnbalancedByFiscalPeriod(@Param("fiscalYear") Integer fiscalYear, @Param("fiscalMonth") Integer fiscalMonth);

    @Query("""
        SELECT count(e)
        FROM JournalEntry e
        WHERE e.fiscalYear = :fiscalYear
          AND e.fiscalMonth = :fiscalMonth
          AND e.lines IS EMPTY
    """)
    long countEntriesWithoutLines(@Param("fiscalYear") Integer fiscalYear, @Param("fiscalMonth") Integer fiscalMonth);

    @Query("""
        SELECT count(l)
        FROM JournalEntry e
        JOIN e.lines l
        WHERE e.fiscalYear = :fiscalYear
          AND e.fiscalMonth = :fiscalMonth
          AND (l.debitAmount < 0 OR l.creditAmount < 0)
    """)
    long countNegativeLines(@Param("fiscalYear") Integer fiscalYear, @Param("fiscalMonth") Integer fiscalMonth);

    @Query("""
        SELECT count(l)
        FROM JournalEntry e
        JOIN e.lines l
        JOIN l.account a
        WHERE e.fiscalYear = :fiscalYear
          AND e.fiscalMonth = :fiscalMonth
          AND (a.deleted = true OR a.status <> 'ACTIVE' OR a.allowPosting = false OR a.children IS NOT EMPTY)
    """)
    long countInvalidPostingAccountLines(@Param("fiscalYear") Integer fiscalYear, @Param("fiscalMonth") Integer fiscalMonth);

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
