package com.dduk.repository.inventory;

import com.dduk.entity.inventory.Inventory;
import com.dduk.entity.inventory.Item;
import com.dduk.entity.inventory.StockMovement;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface StockMovementRepository extends JpaRepository<StockMovement, Long> {

    List<StockMovement> findByItem(Item item);

    List<StockMovement> findByInventory(Inventory inventory);

    List<StockMovement> findAllByOrderByIdDesc();
}
