package com.dduk.domain.inventory.item;

import com.dduk.domain.inventory.warehouse.Warehouse;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "inventories")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Inventory {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "item_id", nullable = false)
    private Item item;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "warehouse_id", nullable = false)
    private Warehouse warehouse;

    @Column(name = "quantity", nullable = false)
    private Integer currentStock;

    @Column(name = "safety_stock", nullable = false)
    private Integer safetyStock;

    @Column(name = "allocated_quantity", nullable = false)
    private Integer allocatedStock; // 예약수량

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Version
    private Long version;

    public Integer getAvailableStock() {
        return currentStock - allocatedStock;
    }

    public void increaseStock(int amount) {
        if (amount < 0) throw new IllegalArgumentException("Amount must be positive");
        this.currentStock += amount;
    }

    public void decreaseStock(int amount) {
        if (amount < 0) throw new IllegalArgumentException("Amount must be positive");
        if (this.currentStock < amount) throw new IllegalArgumentException("Insufficient stock. Current stock: " + currentStock);
        this.currentStock -= amount;
    }

    @PrePersist
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
        if (currentStock == null) currentStock = 0;
        if (safetyStock == null) safetyStock = 0;
        if (allocatedStock == null) allocatedStock = 0;
    }
}
