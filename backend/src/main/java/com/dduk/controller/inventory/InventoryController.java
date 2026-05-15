package com.dduk.controller.inventory;

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
@RequestMapping("/api/v1/inventory")
public class InventoryController {
}
