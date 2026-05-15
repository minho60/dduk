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

    public List<StockMovement> getStockMovements(Long warehouseId, Long itemId, MovementType movementType) {
        // Simplified filtering for MVP. In a real app, use Criteria API or QueryDSL for dynamic queries.
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
        
        return results;
    }
}
