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
}
