package com.dduk.repository.inventory;

import com.dduk.entity.inventory.PurchaseOrder;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PurchaseOrderRepository extends JpaRepository<PurchaseOrder, Long> {

    Optional<PurchaseOrder> findByPurchaseOrderNo(String purchaseOrderNo);

    List<PurchaseOrder> findAllByOrderByIdDesc();
}
