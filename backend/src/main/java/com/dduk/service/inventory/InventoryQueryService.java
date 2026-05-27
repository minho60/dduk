package com.dduk.service.inventory;

import com.dduk.dto.inventory.InventoryDashboardResponseDto;
import com.dduk.dto.inventory.RecentMovementDto;
import com.dduk.dto.inventory.WarehouseDistributionDto;
import com.dduk.entity.inventory.Inventory;
import com.dduk.entity.inventory.MovementType;
import com.dduk.entity.inventory.StockMovement;
import com.dduk.repository.inventory.InventoryRepository;
import com.dduk.repository.inventory.StockMovementRepository;
import com.dduk.repository.inventory.WarehouseTransferRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class InventoryQueryService {

    private final InventoryRepository inventoryRepository;
    private final StockMovementRepository stockMovementRepository;
    private final WarehouseTransferRepository warehouseTransferRepository;

    public List<Inventory> getInventories(Long warehouseId, Long itemId, Boolean lowStockOnly) {
        List<Inventory> results;
        if (warehouseId != null && itemId != null) {
            results = inventoryRepository.findByWarehouseIdAndItemId(warehouseId, itemId);
        } else if (warehouseId != null) {
            results = inventoryRepository.findByWarehouseId(warehouseId);
        } else if (itemId != null) {
            results = inventoryRepository.findByItemId(itemId);
        } else {
            results = inventoryRepository.findAll();
        }

        if (Boolean.TRUE.equals(lowStockOnly)) {
            return results.stream()
                    .filter(i -> i.getCurrentStock() <= i.getSafetyStock())
                    .collect(Collectors.toList());
        }
        return results;
    }

    public List<Inventory> getReorderRecommendations() {
        return inventoryRepository.findItemsNeedingReorder();
    }

    public InventoryDashboardResponseDto getDashboardStats() {
        Long totalQty = inventoryRepository.getTotalStockQuantity();
        BigDecimal totalVal = inventoryRepository.getTotalInventoryValue();
        Long lowStock = inventoryRepository.countLowStockItems();
        Long outboundVol = stockMovementRepository.getOutboundVolumeSince(LocalDateTime.now().minusDays(30));
        Long pendingTransfers = warehouseTransferRepository.countPendingTransfers();

        // 1. 창고별 재고 분포 Read Model 변환
        List<Object[]> distributionRaw = inventoryRepository.getStockDistributionByWarehouse();
        List<WarehouseDistributionDto> distribution = distributionRaw.stream()
                .map(row -> WarehouseDistributionDto.builder()
                        .warehouseName((String) row[0])
                        .totalStock(row[1] != null ? (Long) row[1] : 0L)
                        .totalValue(row[2] != null ? (BigDecimal) row[2] : BigDecimal.ZERO)
                        .build())
                .collect(Collectors.toList());

        // 2. 최근 5건 변동 이력 Read Model 변환
        List<StockMovement> movements = stockMovementRepository.findAll();
        movements.sort((m1, m2) -> {
            int dateComp = m2.getCreatedAt().compareTo(m1.getCreatedAt());
            if (dateComp != 0) return dateComp;
            return m2.getId().compareTo(m1.getId());
        });

        List<RecentMovementDto> recentMovements = movements.stream().limit(5)
                .map(m -> RecentMovementDto.builder()
                        .id(m.getId())
                        .createdAt(m.getCreatedAt())
                        .referenceNo(m.getReferenceNo())
                        .movementType(m.getMovementType())
                        .itemName(m.getItem().getName())
                        .quantity(m.getQuantity())
                        .warehouseName(m.getWarehouse().getWarehouseName())
                        .build())
                .collect(Collectors.toList());

        return InventoryDashboardResponseDto.builder()
                .totalQuantity(totalQty != null ? totalQty : 0L)
                .totalValue(totalVal != null ? totalVal : BigDecimal.ZERO)
                .lowStockCount(lowStock != null ? lowStock : 0L)
                .outboundVolume30Days(outboundVol != null ? outboundVol : 0L)
                .pendingTransferCount(pendingTransfers != null ? pendingTransfers : 0L)
                .warehouseDistribution(distribution)
                .recentMovements(recentMovements)
                .build();
    }

    public List<StockMovement> getStockMovements(Long warehouseId, Long itemId, MovementType movementType) {
        List<StockMovement> results = stockMovementRepository.findAll();
        
        if (warehouseId != null) {
            results = results.stream().filter(m -> m.getWarehouse().getId().equals(warehouseId)).collect(Collectors.toList());
        }
        if (itemId != null) {
            results = results.stream().filter(m -> m.getItem().getId().equals(itemId)).collect(Collectors.toList());
        }
        if (movementType != null) {
            results = results.stream().filter(m -> m.getMovementType() == movementType).collect(Collectors.toList());
        }
        
        results.sort((m1, m2) -> {
            int dateComp = m2.getCreatedAt().compareTo(m1.getCreatedAt());
            if (dateComp != 0) return dateComp;
            return m2.getId().compareTo(m1.getId());
        });

        return results;
    }
}
