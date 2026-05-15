package com.dduk.domain.inventory.stock;

import com.dduk.domain.inventory.item.Inventory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface InventoryRepository extends JpaRepository<Inventory, Long> {
    Optional<Inventory> findByItemIdAndWarehouseId(Long itemId, Long warehouseId);

    @Query("SELECT i FROM Inventory i WHERE i.currentStock <= i.safetyStock")
    List<Inventory> findItemsNeedingReorder();

    List<Inventory> findByWarehouseId(Long warehouseId);
    List<Inventory> findByItemId(Long itemId);
    List<Inventory> findByWarehouseIdAndItemId(Long warehouseId, Long itemId);

    @Query("SELECT SUM(i.currentStock) FROM Inventory i")
    Long getTotalStockQuantity();

    @Query("SELECT SUM(i.inventoryValue) FROM Inventory i")
    BigDecimal getTotalInventoryValue();

    @Query("SELECT COUNT(i) FROM Inventory i WHERE i.currentStock <= i.safetyStock")
    Long countLowStockItems();

    @Query("SELECT i.warehouse.warehouseName, SUM(i.currentStock), SUM(i.inventoryValue) " +
           "FROM Inventory i GROUP BY i.warehouse.warehouseName")
    List<Object[]> getStockDistributionByWarehouse();
}
