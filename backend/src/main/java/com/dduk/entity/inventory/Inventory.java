package com.dduk.entity.inventory;

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

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "inventories")
public class Inventory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "item_id", nullable = false)
    private Item item;

    @Column(nullable = false, length = 100)
    private String location;

    @Column(nullable = false)
    private int quantity;

    @Column(nullable = false)
    private int allocatedQuantity;

    @Column(nullable = false, length = 100)
    private String lotNo;

    @Column(nullable = false)
    private LocalDate expirationDate;

    @Column(nullable = false, length = 30)
    private String status;

    private LocalDateTime lastAdjustedAt;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @Builder
    public Inventory(
            Item item,
            String location,
            int quantity,
            int allocatedQuantity,
            String lotNo,
            LocalDate expirationDate,
            String status,
            LocalDateTime lastAdjustedAt
    ) {
        this.item = item;
        this.location = location;
        this.quantity = quantity;
        this.allocatedQuantity = allocatedQuantity;
        this.lotNo = lotNo;
        this.expirationDate = expirationDate;
        this.status = status;
        this.lastAdjustedAt = lastAdjustedAt;
    }
}
