package com.dduk.controller.inventory;

import com.dduk.config.PrincipalDetails;
import com.dduk.dto.inventory.PurchaseRequestCreateDto;
import com.dduk.dto.inventory.PurchaseRequestResponseDto;
import com.dduk.service.inventory.PurchaseService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
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
@RequestMapping("/api/v1/inventory/purchases")
@RequiredArgsConstructor
public class PurchaseController {

    private final PurchaseService purchaseService;

    @PostMapping
    public PurchaseRequestResponseDto createPurchaseRequest(
            @RequestBody PurchaseRequestCreateDto requestDto,
            @AuthenticationPrincipal PrincipalDetails principalDetails
    ) {
        return purchaseService.createPurchaseRequest(requestDto, principalDetails.getMember().getId());
    }
}
