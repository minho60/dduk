package com.dduk.entity.accounting.voucher;

import com.dduk.entity.accounting.AccountSide;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "voucher_lines")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class VoucherLine {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "voucher_id", nullable = false)
    private Voucher voucher;

    @Column(name = "line_no", nullable = false)
    private Integer lineNo;

    @Column(name = "account_id", nullable = false)
    private Long accountId;

    @Column(name = "account_code", nullable = false)
    private String accountCode;

    @Column(name = "account_name", nullable = false)
    private String accountName;

    @Enumerated(EnumType.STRING)
    @Column(name = "debit_credit", nullable = false)
    private AccountSide debitCredit;

    @Column(name = "supply_amount", nullable = false)
    @Builder.Default
    private BigDecimal supplyAmount = BigDecimal.ZERO;

    @Column(name = "vat_amount", nullable = false)
    @Builder.Default
    private BigDecimal vatAmount = BigDecimal.ZERO;

    @Column(name = "total_amount", nullable = false)
    @Builder.Default
    private BigDecimal totalAmount = BigDecimal.ZERO;

    private Integer quantity;
    private BigDecimal unitPrice;
    private String description;

    @Column(name = "sort_order", nullable = false)
    @Builder.Default
    private Integer sortOrder = 0;

    void setVoucher(Voucher voucher) {
        this.voucher = voucher;
    }
}
