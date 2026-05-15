package com.dduk.controller.inventory;

import com.dduk.config.PrincipalDetails;
import com.dduk.dto.inventory.PurchaseOrderCreateDto;
import com.dduk.dto.inventory.PurchaseOrderResponseDto;
import com.dduk.dto.inventory.PurchaseOrderStatusUpdateDto;
import com.dduk.service.inventory.PurchaseOrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


/**
 * com.dduk.controller.inventory
 ├─ InventoryController        // 재고 조회, 재고 조정
 ├─ InboundController          // 입고 등록/조회/취소
 ├─ OutboundController         // 출고 등록/조회/취소
 ├─ PurchaseController         // 구매 요청/승인/반려
 └─ PurchaseOrderController    // 발주 생성/조회/상태 변경
 */
@RestController
@RequestMapping("/api/v1/inventory/purchase-orders")
@RequiredArgsConstructor
public class PurchaseOrderController {

    private final PurchaseOrderService purchaseOrderService;

    @PostMapping
    public PurchaseOrderResponseDto createPurchaseOrder(
            @RequestBody PurchaseOrderCreateDto requestDto,
            @AuthenticationPrincipal PrincipalDetails principalDetails
    ) {
        Long requestedByMemberId = principalDetails == null ? null : principalDetails.getMember().getId();
        return purchaseOrderService.createPurchaseOrder(requestDto, requestedByMemberId);
    }

    @PatchMapping("/{purchaseOrderId}/status")
    public PurchaseOrderResponseDto updatePurchaseOrderStatus(
            @PathVariable Long purchaseOrderId,
            @RequestBody PurchaseOrderStatusUpdateDto requestDto,
            @AuthenticationPrincipal PrincipalDetails principalDetails
    ) {
        Long approvedByMemberId = principalDetails == null ? null : principalDetails.getMember().getId();
        return purchaseOrderService.updatePurchaseOrderStatus(purchaseOrderId, requestDto, approvedByMemberId);
    }
}
