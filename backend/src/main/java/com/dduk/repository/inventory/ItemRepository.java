package com.dduk.repository.inventory;

import com.dduk.entity.inventory.Item;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ItemRepository extends JpaRepository<Item, Long> {

    Optional<Item> findByItemCode(String itemCode);
    Optional<Item> findByItemName(String itemName);
    List<Item> findAllByOrderByIdDesc();
}
