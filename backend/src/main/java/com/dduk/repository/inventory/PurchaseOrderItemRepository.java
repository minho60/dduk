package com.dduk.repository.inventory;

import com.dduk.entity.inventory.PurchaseOrder;
import com.dduk.entity.inventory.PurchaseOrderItem;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PurchaseOrderItemRepository extends JpaRepository<PurchaseOrderItem, Long> {

    List<PurchaseOrderItem> findByPurchaseOrder(PurchaseOrder purchaseOrder);

    List<PurchaseOrderItem> findAllByOrderByIdDesc();
}
