package com.dduk.repository.inventory;

import com.dduk.entity.inventory.Inventory;
import com.dduk.entity.inventory.Item;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface InventoryRepository extends JpaRepository<Inventory, Long> {

    List<Inventory> findByItem(Item item);

    List<Inventory> findByItemId(Long itemId);

    List<Inventory> findByLocation(String location);

    Optional<Inventory> findByItemIdAndLocationAndLotNo(Long itemId, String location, String lotNo);

    List<Inventory> findAllByOrderByIdDesc();
}
