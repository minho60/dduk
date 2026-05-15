package com.dduk.controller.inventory;

import com.dduk.domain.inventory.purchase.PurchaseOrder;
import com.dduk.domain.inventory.purchase.PurchaseService;
import com.dduk.domain.inventory.purchase.PurchaseStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/purchase-orders")
@RequiredArgsConstructor
public class PurchaseOrderController {

    private final PurchaseService purchaseService;

    @GetMapping
    public List<PurchaseOrder> getAll() {
        return purchaseService.getAllOrders();
    }

    @GetMapping("/{id}")
    public PurchaseOrder getOne(@PathVariable Long id) {
        return purchaseService.getOrder(id);
    }

    @PostMapping
    public PurchaseOrder create(@RequestBody PurchaseOrder order) {
        return purchaseService.createOrder(order);
    }

    @PatchMapping("/{id}/status")
    public PurchaseOrder updateStatus(@PathVariable Long id, @RequestBody Map<String, String> request) {
        PurchaseStatus nextStatus = PurchaseStatus.valueOf(request.get("status"));
        return purchaseService.transitionStatus(id, nextStatus);
    }
}
