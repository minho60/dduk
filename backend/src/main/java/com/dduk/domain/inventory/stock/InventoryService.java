package com.dduk.domain.inventory.stock;

import com.dduk.domain.inventory.item.Inventory;
import com.dduk.domain.inventory.item.Item;
import com.dduk.domain.inventory.warehouse.Warehouse;
import com.dduk.domain.inventory.item.ItemRepository; // Assuming this exists
import com.dduk.domain.inventory.warehouse.WarehouseRepository; // Assuming this exists
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class InventoryService {

    private final InventoryRepository inventoryRepository;
    private final StockMovementRepository stockMovementRepository;
    private final ItemRepository itemRepository;
    private final WarehouseRepository warehouseRepository;

    @Transactional(rollbackFor = Exception.class)
    public void increaseStock(Long itemId, Long warehouseId, int quantity, MovementReason reason, String refType, String refId) {
        Inventory inventory = getOrCreateInventory(itemId, warehouseId);
        int beforeQuantity = inventory.getCurrentStock();
        
        inventory.increaseStock(quantity);
        inventoryRepository.save(inventory);

        recordMovement(inventory.getItem(), inventory.getWarehouse(), MovementType.IN, reason, quantity, beforeQuantity, inventory.getCurrentStock(), refType, refId);
    }

    @Transactional(rollbackFor = Exception.class)
    public void decreaseStock(Long itemId, Long warehouseId, int quantity, MovementReason reason, String refType, String refId) {
        Inventory inventory = getInventoryOrThrow(itemId, warehouseId);
        int beforeQuantity = inventory.getCurrentStock();
        
        inventory.decreaseStock(quantity);
        inventoryRepository.save(inventory);

        recordMovement(inventory.getItem(), inventory.getWarehouse(), MovementType.OUT, reason, quantity, beforeQuantity, inventory.getCurrentStock(), refType, refId);
    }

    @Transactional(rollbackFor = Exception.class)
    public void transferStock(Long itemId, Long fromWarehouseId, Long toWarehouseId, int quantity, String refType, String refId) {
        Inventory fromInventory = getInventoryOrThrow(itemId, fromWarehouseId);
        int fromBefore = fromInventory.getCurrentStock();
        fromInventory.decreaseStock(quantity);
        inventoryRepository.save(fromInventory);
        recordMovement(fromInventory.getItem(), fromInventory.getWarehouse(), MovementType.TRANSFER_OUT, MovementReason.TRANSFER, quantity, fromBefore, fromInventory.getCurrentStock(), refType, refId);

        Inventory toInventory = getOrCreateInventory(itemId, toWarehouseId);
        int toBefore = toInventory.getCurrentStock();
        toInventory.increaseStock(quantity);
        inventoryRepository.save(toInventory);
        recordMovement(toInventory.getItem(), toInventory.getWarehouse(), MovementType.TRANSFER_IN, MovementReason.TRANSFER, quantity, toBefore, toInventory.getCurrentStock(), refType, refId);
    }

    @Transactional(rollbackFor = Exception.class)
    public void adjustStock(Long itemId, Long warehouseId, int newQuantity, MovementReason reason, String refType, String refId) {
        Inventory inventory = getOrCreateInventory(itemId, warehouseId);
        int beforeQuantity = inventory.getCurrentStock();
        int difference = newQuantity - beforeQuantity;

        if (difference == 0) return;

        MovementType type = difference > 0 ? MovementType.IN : MovementType.OUT;
        
        if (difference > 0) {
            inventory.increaseStock(difference);
        } else {
            inventory.decreaseStock(-difference);
        }
        
        inventoryRepository.save(inventory);
        recordMovement(inventory.getItem(), inventory.getWarehouse(), type, reason, Math.abs(difference), beforeQuantity, newQuantity, refType, refId);
    }



    private Inventory getOrCreateInventory(Long itemId, Long warehouseId) {
        return inventoryRepository.findByItemIdAndWarehouseId(itemId, warehouseId)
                .orElseGet(() -> {
                    Item item = itemRepository.findById(itemId)
                            .orElseThrow(() -> new IllegalArgumentException("Item not found"));
                    Warehouse warehouse = warehouseRepository.findById(warehouseId)
                            .orElseThrow(() -> new IllegalArgumentException("Warehouse not found"));
                    return Inventory.builder()
                            .item(item)
                            .warehouse(warehouse)
                            .currentStock(0)
                            .safetyStock(0)
                            .allocatedStock(0)
                            .build();
                });
    }

    private Inventory getInventoryOrThrow(Long itemId, Long warehouseId) {
        return inventoryRepository.findByItemIdAndWarehouseId(itemId, warehouseId)
                .orElseThrow(() -> new IllegalArgumentException("Inventory not found for item " + itemId + " in warehouse " + warehouseId));
    }

    private void recordMovement(Item item, Warehouse warehouse, MovementType type, MovementReason reason, 
                                int quantity, int beforeQty, int afterQty, String refType, String refId) {
        StockMovement movement = StockMovement.builder()
                .item(item)
                .warehouse(warehouse)
                .movementType(type)
                .movementReason(reason)
                .quantity(quantity)
                .beforeQuantity(beforeQty)
                .afterQuantity(afterQty)
                .referenceType(refType)
                .referenceId(refId)
                .build();
        stockMovementRepository.save(movement);
    }
}
