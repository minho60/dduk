package com.dduk.domain.inventory.stock;

import com.dduk.domain.inventory.item.Item;
import com.dduk.domain.inventory.warehouse.Warehouse;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "stock_movements")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class StockMovement {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "item_id", nullable = false, updatable = false)
    private Item item;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "warehouse_id", nullable = false, updatable = false)
    private Warehouse warehouse;

    @Enumerated(EnumType.STRING)
    @Column(name = "movement_type", nullable = false, updatable = false)
    private MovementType movementType;

    @Enumerated(EnumType.STRING)
    @Column(name = "movement_reason", nullable = false, updatable = false)
    private MovementReason movementReason;

    @Column(nullable = false, updatable = false)
    private Integer quantity;

    @Column(name = "before_quantity", nullable = false, updatable = false)
    private Integer beforeQuantity;

    @Column(name = "after_quantity", nullable = false, updatable = false)
    private Integer afterQuantity;

    @Column(name = "reference_type", updatable = false)
    private String referenceType;

    @Column(name = "reference_id", updatable = false)
    private String referenceId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
