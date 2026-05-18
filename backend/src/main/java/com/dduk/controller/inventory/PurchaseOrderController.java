package com.dduk.controller.inventory;

import com.dduk.config.PrincipalDetails;
import com.dduk.service.inventory.PurchaseService;
import com.dduk.dto.inventory.PurchaseOrderCreateDto;
import com.dduk.dto.inventory.PurchaseOrderResponseDto;
import com.dduk.dto.inventory.PurchaseOrderStatusUpdateDto;
import com.dduk.entity.inventory.PurchaseOrder;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/inventory/purchase-orders")
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
    public PurchaseOrderResponseDto createPurchaseOrder(
            @RequestBody PurchaseOrderCreateDto requestDto,
            @AuthenticationPrincipal PrincipalDetails principalDetails
    ) {
        Long memberId = principalDetails != null ? principalDetails.getMember().getId() : null;
        return purchaseService.createPurchaseOrder(requestDto, memberId);
    }

    @PatchMapping("/{id}/status")
    public PurchaseOrderResponseDto updatePurchaseOrderStatus(
            @PathVariable Long id,
            @RequestBody PurchaseOrderStatusUpdateDto requestDto,
            @AuthenticationPrincipal PrincipalDetails principalDetails
    ) {
        Long memberId = principalDetails != null ? principalDetails.getMember().getId() : null;
        return purchaseService.updatePurchaseOrderStatus(id, requestDto, memberId);
    }
}
