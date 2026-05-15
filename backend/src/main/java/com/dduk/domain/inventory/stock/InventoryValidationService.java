package com.dduk.domain.inventory.stock;

import com.dduk.domain.inventory.item.Inventory;
import com.dduk.domain.inventory.stock.api.dto.ValidationResultDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class InventoryValidationService {

    private final InventoryRepository inventoryRepository;
    private final StockMovementRepository stockMovementRepository;

    public List<ValidationResultDto> validateInventories(Long warehouseId, Long itemId) {
        List<Inventory> inventories;
        if (warehouseId != null && itemId != null) {
            inventories = inventoryRepository.findByWarehouseIdAndItemId(warehouseId, itemId);
        } else if (warehouseId != null) {
            inventories = inventoryRepository.findByWarehouseId(warehouseId);
        } else if (itemId != null) {
            inventories = inventoryRepository.findByItemId(itemId);
        } else {
            inventories = inventoryRepository.findAll();
        }

        List<ValidationResultDto> discrepancies = new ArrayList<>();

        for (Inventory inv : inventories) {
            List<StockMovement> movements = stockMovementRepository.findByWarehouseId(inv.getWarehouse().getId())
                    .stream()
                    .filter(m -> m.getItem().getId().equals(inv.getItem().getId()))
                    .collect(Collectors.toList());

            int calculatedStock = 0;
            for (StockMovement m : movements) {
                if (m.getMovementType() == MovementType.IN || m.getMovementType() == MovementType.TRANSFER_IN) {
                    calculatedStock += m.getQuantity();
                } else if (m.getMovementType() == MovementType.OUT || m.getMovementType() == MovementType.TRANSFER_OUT) {
                    calculatedStock -= m.getQuantity();
                }
            }

            if (calculatedStock != inv.getCurrentStock()) {
                discrepancies.add(ValidationResultDto.builder()
                        .inventoryId(inv.getId())
                        .warehouseId(inv.getWarehouse().getId())
                        .itemId(inv.getItem().getId())
                        .currentStock(inv.getCurrentStock())
                        .calculatedStock(calculatedStock)
                        .isMatch(false)
                        .build());
            }
        }

        return discrepancies;
    }
}
