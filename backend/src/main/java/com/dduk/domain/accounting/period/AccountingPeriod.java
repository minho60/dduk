package com.dduk.domain.accounting.period;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * 회계 기간 마감 관리
 * - OPEN: 전표 생성/수정 가능
 * - CLOSED: 전표 생성/수정/역분개 금지
 */
@Entity
@Table(name = "accounting_periods")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class AccountingPeriod {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "fiscal_year", nullable = false)
    private Integer fiscalYear;

    @Column(name = "fiscal_month", nullable = false)
    private Integer fiscalMonth;

    /** OPEN / CLOSED */
    @Column(nullable = false)
    private String status;

    @Column(name = "closed_at")
    private LocalDateTime closedAt;

    @Column(name = "closed_by")
    private String closedBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    // ===================== 도메인 메서드 =====================

    public boolean isClosed() {
        return "CLOSED".equals(this.status);
    }

    public void close(String closedBy) {
        if (isClosed()) {
            throw new IllegalStateException("이미 마감된 회계 기간입니다: " + fiscalYear + "-" + fiscalMonth);
        }
        this.status = "CLOSED";
        this.closedAt = LocalDateTime.now();
        this.closedBy = closedBy;
    }

    public void reopen(String reopenedBy) {
        if (!isClosed()) {
            throw new IllegalStateException("마감된 상태가 아닌 회계 기간은 재오픈할 수 없습니다: " + fiscalYear + "-" + fiscalMonth);
        }
        this.status = "OPEN";
        this.closedAt = null;
        this.closedBy = null;
    }

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (status == null) status = "OPEN";
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public String getPeriodKey() {
        return fiscalYear + "-" + String.format("%02d", fiscalMonth);
    }
}
