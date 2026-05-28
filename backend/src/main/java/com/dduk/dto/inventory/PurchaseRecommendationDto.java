package com.dduk.dto.inventory;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@Builder
public class PurchaseRecommendationDto {
    private Long inventoryId;
    private Long itemId;
    private String itemCode;
    private String itemName;
    private String itemCategory;
    private String unit;
    private Long warehouseId;
    private String warehouseName;
    private int currentStock;
    private int safetyStock;
    private int allocatedStock;
    private int availableStock;
    private BigDecimal averageCost;
    // 추천 계산 결과
    private double avgMonthlyUsage;  // 최근 30일 평균 출고량
    private int recommendedOrderQty; // 권장발주수량
    private String urgency;          // CRITICAL(품절), HIGH(안전재고이하), MEDIUM(임박), LOW
    private int daysUntilStockout;   // 예상 소진일
    private String defaultVendorName; // 기본 공급처
    private int leadTimeDays;         // 리드타임(고정값 7일, 향후 확장)
}
