package com.dduk.domain.inventory.item;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

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
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
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

    @Column(name = "average_cost", nullable = false, precision = 19, scale = 4)
    private java.math.BigDecimal averageCost;

    @Column(name = "inventory_value", nullable = false, precision = 19, scale = 4)
    private java.math.BigDecimal inventoryValue;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Version
    private Long version;

    public Integer getAvailableStock() {
        return currentStock - allocatedStock;
    }

    public void increaseStock(int amount, java.math.BigDecimal unitCost) {
        if (amount < 0) throw new IllegalArgumentException("Amount must be positive");
        
        java.math.BigDecimal totalCostOfNewItems = unitCost.multiply(java.math.BigDecimal.valueOf(amount));
        java.math.BigDecimal currentTotalValue = this.inventoryValue != null ? this.inventoryValue : java.math.BigDecimal.ZERO;
        
        int newTotalQty = this.currentStock + amount;
        java.math.BigDecimal newTotalValue = currentTotalValue.add(totalCostOfNewItems);
        
        if (newTotalQty > 0) {
            this.averageCost = newTotalValue.divide(java.math.BigDecimal.valueOf(newTotalQty), 4, java.math.RoundingMode.HALF_UP);
        }
        
        this.currentStock = newTotalQty;
        this.inventoryValue = newTotalValue;
    }

    public void decreaseStock(int amount) {
        if (amount < 0) throw new IllegalArgumentException("Amount must be positive");
        if (this.currentStock < amount) throw new IllegalArgumentException("Insufficient stock. Current stock: " + currentStock);
        
        this.currentStock -= amount;
        // When decreasing stock, we use the current average cost to reduce the inventory value
        this.inventoryValue = this.averageCost.multiply(java.math.BigDecimal.valueOf(this.currentStock));
    }

    @PrePersist
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
        if (currentStock == null) currentStock = 0;
        if (safetyStock == null) safetyStock = 0;
        if (allocatedStock == null) allocatedStock = 0;
        if (averageCost == null) averageCost = java.math.BigDecimal.ZERO;
        if (inventoryValue == null) inventoryValue = java.math.BigDecimal.ZERO;
    }
}
