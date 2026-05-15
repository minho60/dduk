package com.dduk.domain.accounting.journal;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 전표 헤더 (Aggregate Root)
 * - 상태 흐름: DRAFT → APPROVED → POSTED → REVERSED
 * - POSTED 이후 불변(immutable) 처리
 * - JournalLine 은 반드시 이 클래스를 통해서만 추가
 */
@Entity
@Table(name = "journal_entries")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class JournalEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "journal_no", nullable = false, unique = true)
    private String journalNo;

    @Column(name = "transaction_date", nullable = false)
    private LocalDate transactionDate;

    @Column(nullable = false)
    private String description;

    /**
     * 전표 상태
     * DRAFT: 임시저장 | APPROVED: 승인 | POSTED: 기표(불변) | REVERSED: 역분개됨
     */
    @Column(nullable = false)
    private String status;

    /** 원천 도메인 유형 (PAYROLL, PURCHASE_ORDER, STOCK_INBOUND, MANUAL) */
    @Column(name = "source_type")
    private String sourceType;

    /** 원천 엔티티 ID */
    @Column(name = "source_id")
    private Long sourceId;

    /** 차변 합계 */
    @Column(name = "total_debit", nullable = false)
    private BigDecimal totalDebit;

    /** 대변 합계 */
    @Column(name = "total_credit", nullable = false)
    private BigDecimal totalCredit;

    /** 생성자 (로그인 ID 또는 시스템 식별자) */
    @Column(name = "created_by")
    private String createdBy;

    /** 회계 연도 */
    @Column(name = "fiscal_year")
    private Integer fiscalYear;

    /** 회계 월 */
    @Column(name = "fiscal_month")
    private Integer fiscalMonth;

    @OneToMany(mappedBy = "journalEntry", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<JournalLine> lines = new ArrayList<>();

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    // ===================== Aggregate 도메인 메서드 =====================

    /** 전표 라인 추가 - Aggregate 경유 강제 */
    public void addLine(JournalLine line) {
        assertNotPosted();
        lines.add(line);
        line.setJournalEntry(this);
        recalculateTotals();
    }

    /** DRAFT → APPROVED 상태 전이 */
    public void approve() {
        if (!"DRAFT".equals(this.status)) {
            throw new IllegalStateException("DRAFT 상태의 전표만 승인할 수 있습니다. 현재 상태: " + this.status);
        }
        this.status = "APPROVED";
    }

    /** APPROVED → POSTED 상태 전이 (불변 전환) */
    public void post() {
        if (!"APPROVED".equals(this.status)) {
            throw new IllegalStateException("APPROVED 상태의 전표만 기표할 수 있습니다. 현재 상태: " + this.status);
        }
        this.status = "POSTED";
    }

    /** POSTED → REVERSED 표시 (역분개 전표 생성 후 호출) */
    public void markReversed() {
        if (!"POSTED".equals(this.status)) {
            throw new IllegalStateException("POSTED 상태의 전표만 역분개할 수 있습니다. 현재 상태: " + this.status);
        }
        this.status = "REVERSED";
    }

    /** POSTED 이후 수정 불가 검증 */
    public void assertNotPosted() {
        if ("POSTED".equals(this.status) || "REVERSED".equals(this.status)) {
            throw new IllegalStateException("기표된 전표(POSTED/REVERSED)는 수정할 수 없습니다.");
        }
    }

    /** 전표 라인 조회 (불변 뷰) */
    public List<JournalLine> getLines() {
        return Collections.unmodifiableList(lines);
    }

    /** 차대변 합계 재계산 */
    private void recalculateTotals() {
        this.totalDebit = lines.stream()
                .map(JournalLine::getDebitAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        this.totalCredit = lines.stream()
                .map(JournalLine::getCreditAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (status == null) status = "DRAFT";
        if (totalDebit == null) totalDebit = BigDecimal.ZERO;
        if (totalCredit == null) totalCredit = BigDecimal.ZERO;
        if (transactionDate != null) {
            fiscalYear = transactionDate.getYear();
            fiscalMonth = transactionDate.getMonthValue();
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
