package com.dduk.entity.inventory;

import com.dduk.entity.admin.Member;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "purchase_orders")
public class PurchaseOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String purchaseOrderNo;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "vendor_id", nullable = false)
    private Vendor vendor;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "requested_by_member_id", nullable = false)
    private Member requestedBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "approved_by_member_id")
    private Member approvedBy;

    @Column(nullable = false)
    private LocalDate orderDate;

    private LocalDate expectedDate;

    @Column(nullable = false, length = 30)
    private String status;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal totalAmount;

    @Column(length = 255)
    private String note;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @Builder
    public PurchaseOrder(
            String purchaseOrderNo,
            Vendor vendor,
            Member requestedBy,
            Member approvedBy,
            LocalDate orderDate,
            LocalDate expectedDate,
            String status,
            BigDecimal totalAmount,
            String note
    ) {
        this.purchaseOrderNo = purchaseOrderNo;
        this.vendor = vendor;
        this.requestedBy = requestedBy;
        this.approvedBy = approvedBy;
        this.orderDate = orderDate;
        this.expectedDate = expectedDate;
        this.status = status;
        this.totalAmount = totalAmount;
        this.note = note;
    }
}
