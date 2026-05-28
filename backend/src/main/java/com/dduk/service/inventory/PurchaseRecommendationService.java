package com.dduk.service.inventory;

import com.dduk.dto.inventory.PurchaseRecommendationDto;
import com.dduk.entity.inventory.Inventory;
import com.dduk.entity.inventory.MovementType;
import com.dduk.entity.inventory.StockMovement;
import com.dduk.repository.inventory.InventoryRepository;
import com.dduk.repository.inventory.StockMovementRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PurchaseRecommendationService {

    private final InventoryRepository inventoryRepository;
    private final StockMovementRepository stockMovementRepository;

    // 리드타임 기본값 (일). 향후 아이템/벤더 마스터에서 동적으로 가져올 수 있도록 설계.
    private static final int DEFAULT_LEAD_TIME_DAYS = 7;

    /**
     * 규칙 기반 자동발주 추천 목록을 계산하여 반환한다.
     * 로직: 권장발주수량 = (최근30일평균사용량 × 리드타임) + 안전재고 - 현재고
     * 음수이면 발주 불필요로 제외.
     */
    public List<PurchaseRecommendationDto> getRecommendations() {
        // 1. 모든 재고를 FETCH JOIN으로 조회 (LazyInitializationException 방지)
        List<Inventory> allInventories = inventoryRepository.findAllWithItemAndWarehouse();

        // 2. 최근 30일 출고 이력 조회 (OUTBOUND, TRANSFER_OUT 포함)
        LocalDateTime thirtyDaysAgo = LocalDateTime.now().minusDays(30);
        List<StockMovement> recentMovements = stockMovementRepository.findAllWithFetch().stream()
                .filter(m -> m.getCreatedAt().isAfter(thirtyDaysAgo))
                .filter(m -> m.getMovementType() == MovementType.OUTBOUND
                          || m.getMovementType() == MovementType.TRANSFER_OUT)
                .collect(Collectors.toList());

        // 3. (itemId, warehouseId) 조합별 최근 30일 출고량 집계
        // key: "itemId_warehouseId"
        Map<String, Long> outboundMap = recentMovements.stream()
                .collect(Collectors.groupingBy(
                        m -> m.getItem().getId() + "_" + m.getWarehouse().getId(),
                        Collectors.summingLong(StockMovement::getQuantity)
                ));

        List<PurchaseRecommendationDto> recommendations = new ArrayList<>();

        for (Inventory inv : allInventories) {
            String key = inv.getItem().getId() + "_" + inv.getWarehouse().getId();
            long outboundLast30 = outboundMap.getOrDefault(key, 0L);
            double avgDailyUsage = outboundLast30 / 30.0;
            double avgMonthlyUsage = outboundLast30; // 30일 총량

            // 권장발주수량 계산
            // = (avgDailyUsage * leadTimeDays) + safetyStock - currentStock
            int recommendedQty = (int) Math.ceil(avgDailyUsage * DEFAULT_LEAD_TIME_DAYS)
                    + inv.getSafetyStock() - inv.getCurrentStock();

            // 안전재고 이하이거나 권장발주수량이 양수인 경우만 추천
            boolean needsReorder = inv.getCurrentStock() <= inv.getSafetyStock() || recommendedQty > 0;
            if (!needsReorder) continue;

            // 권장발주수량이 음수면 최소 1로 설정 (안전재고 이하 품목은 반드시 포함)
            if (recommendedQty <= 0) {
                recommendedQty = Math.max(inv.getSafetyStock() - inv.getCurrentStock(), 1);
            }

            // 긴급도 산정
            String urgency = calcUrgency(inv.getCurrentStock(), inv.getSafetyStock());

            // 예상 소진일 계산
            int daysUntilStockout;
            if (avgDailyUsage <= 0) {
                daysUntilStockout = inv.getCurrentStock() <= 0 ? 0 : 999; // 미사용 품목
            } else {
                daysUntilStockout = (int) Math.floor(inv.getCurrentStock() / avgDailyUsage);
            }

            // 기본 공급처 이름
            String vendorName = "-";
            try {
                if (inv.getItem().getDefaultVendor() != null) {
                    vendorName = inv.getItem().getDefaultVendor().getName();
                }
            } catch (Exception e) {
                // LAZY 로딩 실패 시 무시
            }

            recommendations.add(PurchaseRecommendationDto.builder()
                    .inventoryId(inv.getId())
                    .itemId(inv.getItem().getId())
                    .itemCode(inv.getItem().getItemCode())
                    .itemName(inv.getItem().getName())
                    .itemCategory(inv.getItem().getCategory())
                    .unit(inv.getItem().getUnit())
                    .warehouseId(inv.getWarehouse().getId())
                    .warehouseName(inv.getWarehouse().getWarehouseName())
                    .currentStock(inv.getCurrentStock())
                    .safetyStock(inv.getSafetyStock())
                    .allocatedStock(inv.getAllocatedStock())
                    .availableStock(inv.getAvailableStock())
                    .averageCost(inv.getAverageCost())
                    .avgMonthlyUsage(avgMonthlyUsage)
                    .recommendedOrderQty(recommendedQty)
                    .urgency(urgency)
                    .daysUntilStockout(daysUntilStockout)
                    .defaultVendorName(vendorName)
                    .leadTimeDays(DEFAULT_LEAD_TIME_DAYS)
                    .build());
        }

        // 긴급도 우선순위 정렬: CRITICAL > HIGH > MEDIUM > LOW
        recommendations.sort(Comparator
                .comparingInt(r -> urgencyOrder(r.getUrgency())));

        return recommendations;
    }

    private String calcUrgency(int currentStock, int safetyStock) {
        if (currentStock <= 0) return "CRITICAL";
        if (currentStock <= safetyStock / 2) return "HIGH";
        if (currentStock <= safetyStock) return "MEDIUM";
        return "LOW";
    }

    private int urgencyOrder(String urgency) {
        return switch (urgency) {
            case "CRITICAL" -> 0;
            case "HIGH" -> 1;
            case "MEDIUM" -> 2;
            default -> 3;
        };
    }
}
