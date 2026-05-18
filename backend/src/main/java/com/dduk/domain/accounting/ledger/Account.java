package com.dduk.domain.accounting.ledger;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "accounts")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Account {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String code;

    @Column(nullable = false)
    private String name;

    /** ASSET, LIABILITY, EQUITY, REVENUE, EXPENSE */
    @Column(nullable = false)
    private String type;

    /** 잔액 정상 방향: DEBIT(차변) / CREDIT(대변) */
    @Column(name = "normal_balance", nullable = false)
    private String normalBalance;

    @Column(nullable = false)
    private Integer level;

    @Column(name = "parent_code")
    private String parentCode;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (isActive == null) isActive = true;
        if (normalBalance == null) {
            normalBalance = "ASSET".equals(type) || "EXPENSE".equals(type) ? "DEBIT" : "CREDIT";
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    /** 계정유형에 따른 잔액 증가 방향 반환 */
    public boolean isDebitNormal() {
        return "DEBIT".equals(this.normalBalance);
    }
}
