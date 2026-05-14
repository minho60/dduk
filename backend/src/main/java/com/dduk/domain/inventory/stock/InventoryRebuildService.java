package com.dduk.domain.inventory.stock;

import com.dduk.domain.inventory.item.Inventory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class InventoryRebuildService {

    private final InventoryRepository inventoryRepository;
    private final StockMovementRepository stockMovementRepository;
    private final InventoryValidationService validationService;

    @Transactional(rollbackFor = Exception.class)
    public void rebuildInventories(Long warehouseId, Long itemId) {
        // First get discrepancies
        var discrepancies = validationService.validateInventories(warehouseId, itemId);
        
        for (var dto : discrepancies) {
            Inventory inv = inventoryRepository.findById(dto.getInventoryId())
                    .orElseThrow(() -> new IllegalStateException("Inventory not found"));

            int oldStock = inv.getCurrentStock();
            inv.setCurrentStock(dto.getCalculatedStock());
            inventoryRepository.save(inv);

            // Create an adjustment movement to reflect the rebuild mathematically
            int difference = dto.getCalculatedStock() - oldStock;
            MovementType type = difference > 0 ? MovementType.IN : MovementType.OUT;
            
            StockMovement rebuildMovement = StockMovement.builder()
                    .item(inv.getItem())
                    .warehouse(inv.getWarehouse())
                    .movementType(type)
                    .movementReason(MovementReason.REBUILD_ADJUSTMENT)
                    .quantity(Math.abs(difference))
                    .beforeQuantity(oldStock)
                    .afterQuantity(dto.getCalculatedStock())
                    .referenceType("SYSTEM_REBUILD")
                    .referenceId(null)
                    .build();
            
            stockMovementRepository.save(rebuildMovement);
        }
    }
}
