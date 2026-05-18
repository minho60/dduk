package com.dduk.repository.inventory;

import com.dduk.entity.inventory.Item;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ItemRepository extends JpaRepository<Item, Long> {
}
