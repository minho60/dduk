package com.dduk.repository.accounting.voucher;

import com.dduk.entity.accounting.voucher.Voucher;
import com.dduk.entity.accounting.voucher.enums.VoucherStatus;
import com.dduk.entity.accounting.voucher.enums.VoucherType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface VoucherRepository extends JpaRepository<Voucher, Long> {
    Optional<Voucher> findByVoucherNo(String voucherNo);

    List<Voucher> findTop100ByOrderByVoucherDateDescIdDesc();

    List<Voucher> findByVoucherTypeOrderByVoucherDateDescIdDesc(VoucherType voucherType);

    long countByStatus(VoucherStatus status);

    long countByVoucherDate(LocalDate voucherDate);

    @Query("""
            SELECT DISTINCT v
            FROM Voucher v
            LEFT JOIN FETCH v.lines
            LEFT JOIN FETCH v.journalEntry
            ORDER BY v.voucherDate DESC, v.id DESC
    """)
    List<Voucher> findAllWithLinesForList();

    @Query("""
            select count(v)
            from Voucher v
            where v.voucherDate between :startDate and :endDate
              and v.status not in :statuses
            """)
    long countByVoucherDateBetweenAndStatusNotIn(LocalDate startDate, LocalDate endDate, List<VoucherStatus> statuses);

    long countByVoucherDateBetween(LocalDate startDate, LocalDate endDate);

    long countByVoucherDateBetweenAndStatus(LocalDate startDate, LocalDate endDate, VoucherStatus status);

    @Query("""
            select
                coalesce(sum(case when l.debitCredit = com.dduk.entity.accounting.AccountSide.DEBIT then l.totalAmount else 0 end), 0),
                coalesce(sum(case when l.debitCredit = com.dduk.entity.accounting.AccountSide.CREDIT then l.totalAmount else 0 end), 0)
            from Voucher v
            join v.lines l
            where v.voucherDate between :startDate and :endDate
            """)
    Object[] sumDebitCreditByVoucherDateBetween(LocalDate startDate, LocalDate endDate);
}
