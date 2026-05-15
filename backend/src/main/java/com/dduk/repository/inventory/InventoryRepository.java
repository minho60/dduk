package com.dduk.repository.inventory;

import com.dduk.entity.inventory.Inventory;
import com.dduk.entity.inventory.Item;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface InventoryRepository extends JpaRepository<Inventory, Long> {

    Optional<Inventory> saveInventory(Inventory inventory);
    List<Inventory> findByItem(Item item);
    List<Inventory> findAllByOrderByIdDesc();
}
