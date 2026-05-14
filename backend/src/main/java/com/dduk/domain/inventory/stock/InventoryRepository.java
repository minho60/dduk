package com.dduk.domain.inventory.stock;

import com.dduk.domain.inventory.item.Inventory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface InventoryRepository extends JpaRepository<Inventory, Long> {
    Optional<Inventory> findByItemIdAndWarehouseId(Long itemId, Long warehouseId);

    @Query("SELECT i FROM Inventory i WHERE i.currentStock <= i.safetyStock")
    List<Inventory> findItemsNeedingReorder();
}
