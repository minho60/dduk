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

 /*
@Controller
- 화면 반환용
- return "login" → login.html 같은 View를 찾음
- 서버에서 HTML 화면을 렌더링할 때 사용

@RestController
- API 데이터 반환용
- return dto → JSON으로 응답
- React/Vue/앱/외부 시스템이 호출하는 백엔드 API에 사용
 */

@RestController
@RequestMapping("/api/v1/inventory/inbounds")
public class InboundController {
}
