package com.dduk.domain.inventory.stock;

import com.dduk.domain.inventory.item.Inventory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class InventoryQueryService {

    private final InventoryRepository inventoryRepository;
    private final StockMovementRepository stockMovementRepository;

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

    public java.util.Map<String, Object> getDashboardStats() {
        java.util.Map<String, Object> stats = new java.util.HashMap<>();
        stats.put("totalQuantity", inventoryRepository.getTotalStockQuantity());
        stats.put("totalValue", inventoryRepository.getTotalInventoryValue());
        stats.put("lowStockCount", inventoryRepository.countLowStockItems());
        stats.put("outboundVolume30Days", stockMovementRepository.getOutboundVolumeSince(java.time.LocalDateTime.now().minusDays(30)));
        stats.put("warehouseDistribution", inventoryRepository.getStockDistributionByWarehouse());
        return stats;
    }

    public List<StockMovement> getStockMovements(Long warehouseId, Long itemId, MovementType movementType) {
        // In a real app, this should use a Specification or QueryDSL for dynamic filtering
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
        
        // Order by date and ID desc for consistent ledger view
        results.sort((m1, m2) -> {
            int dateComp = m2.getCreatedAt().compareTo(m1.getCreatedAt());
            if (dateComp != 0) return dateComp;
            return m2.getId().compareTo(m1.getId());
        });

        return results;
    }
}
