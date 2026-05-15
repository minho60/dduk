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
    public void increaseStock(Long itemId, Long warehouseId, int quantity, java.math.BigDecimal unitCost, MovementReason reason, String refType, String refId) {
        Inventory inventory = getOrCreateInventory(itemId, warehouseId);
        int beforeQuantity = inventory.getCurrentStock();
        
        inventory.increaseStock(quantity, unitCost);
        inventoryRepository.save(inventory);

        String refNo = generateReferenceNo("IN");
        recordMovement(inventory.getItem(), inventory.getWarehouse(), MovementType.INBOUND, reason, refNo, quantity, unitCost, inventory.getInventoryValue(), beforeQuantity, inventory.getCurrentStock(), refType, refId);
    }

    @Transactional(rollbackFor = Exception.class)
    public void decreaseStock(Long itemId, Long warehouseId, int quantity, MovementReason reason, String refType, String refId) {
        Inventory inventory = getInventoryOrThrow(itemId, warehouseId);
        int beforeQuantity = inventory.getCurrentStock();
        
        java.math.BigDecimal currentAvgCost = inventory.getAverageCost();
        inventory.decreaseStock(quantity);
        inventoryRepository.save(inventory);

        String refNo = generateReferenceNo("OUT");
        recordMovement(inventory.getItem(), inventory.getWarehouse(), MovementType.OUTBOUND, reason, refNo, quantity, currentAvgCost, currentAvgCost.multiply(java.math.BigDecimal.valueOf(quantity)), beforeQuantity, inventory.getCurrentStock(), refType, refId);
    }

    @Transactional(rollbackFor = Exception.class)
    public void transferStock(Long itemId, Long fromWarehouseId, Long toWarehouseId, int quantity, String refType, String refId) {
        Inventory fromInventory = getInventoryOrThrow(itemId, fromWarehouseId);
        int fromBefore = fromInventory.getCurrentStock();
        java.math.BigDecimal transferCost = fromInventory.getAverageCost();
        
        fromInventory.decreaseStock(quantity);
        inventoryRepository.save(fromInventory);
        
        String outRefNo = generateReferenceNo("TRF-OUT");
        recordMovement(fromInventory.getItem(), fromInventory.getWarehouse(), MovementType.TRANSFER_OUT, MovementReason.TRANSFER, outRefNo, quantity, transferCost, transferCost.multiply(java.math.BigDecimal.valueOf(quantity)), fromBefore, fromInventory.getCurrentStock(), refType, refId);

        Inventory toInventory = getOrCreateInventory(itemId, toWarehouseId);
        int toBefore = toInventory.getCurrentStock();
        toInventory.increaseStock(quantity, transferCost);
        inventoryRepository.save(toInventory);
        
        String inRefNo = generateReferenceNo("TRF-IN");
        recordMovement(toInventory.getItem(), toInventory.getWarehouse(), MovementType.TRANSFER_IN, MovementReason.TRANSFER, inRefNo, quantity, transferCost, transferCost.multiply(java.math.BigDecimal.valueOf(quantity)), toBefore, toInventory.getCurrentStock(), refType, refId);
    }

    @Transactional(rollbackFor = Exception.class)
    public void adjustStock(Long itemId, Long warehouseId, int newQuantity, MovementReason reason, String refType, String refId) {
        Inventory inventory = getOrCreateInventory(itemId, warehouseId);
        int beforeQuantity = inventory.getCurrentStock();
        int difference = newQuantity - beforeQuantity;

        if (difference == 0) return;

        MovementType type = difference > 0 ? MovementType.INBOUND : MovementType.OUTBOUND;
        java.math.BigDecimal cost = inventory.getAverageCost(); // Use current avg cost for adjustment if not provided
        
        if (difference > 0) {
            inventory.increaseStock(difference, cost);
        } else {
            inventory.decreaseStock(-difference);
        }
        
        inventoryRepository.save(inventory);
        String refNo = generateReferenceNo("ADJ");
        recordMovement(inventory.getItem(), inventory.getWarehouse(), type, reason, refNo, Math.abs(difference), cost, cost.multiply(java.math.BigDecimal.valueOf(Math.abs(difference))), beforeQuantity, newQuantity, refType, refId);
    }

    private synchronized String generateReferenceNo(String prefix) {
        String datePrefix = prefix + "-" + java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd").format(java.time.LocalDateTime.now());
        return stockMovementRepository.findTopByReferenceNoStartingWithOrderByIdDesc(datePrefix)
                .map(m -> {
                    String lastNo = m.getReferenceNo();
                    int sequence = Integer.parseInt(lastNo.substring(lastNo.length() - 4)) + 1;
                    return datePrefix + "-" + String.format("%04d", sequence);
                })
                .orElse(datePrefix + "-0001");
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
                            .averageCost(java.math.BigDecimal.ZERO)
                            .inventoryValue(java.math.BigDecimal.ZERO)
                            .build();
                });
    }

    private Inventory getInventoryOrThrow(Long itemId, Long warehouseId) {
        return inventoryRepository.findByItemIdAndWarehouseId(itemId, warehouseId)
                .orElseThrow(() -> new IllegalArgumentException("Inventory not found for item " + itemId + " in warehouse " + warehouseId));
    }

    private void recordMovement(Item item, Warehouse warehouse, MovementType type, MovementReason reason, String refNo,
                                int quantity, java.math.BigDecimal unitCost, java.math.BigDecimal totalAmount,
                                int beforeQty, int afterQty, String refType, String refId) {
        StockMovement movement = StockMovement.builder()
                .item(item)
                .warehouse(warehouse)
                .movementType(type)
                .movementReason(reason)
                .referenceNo(refNo)
                .quantity(quantity)
                .unitCost(unitCost)
                .totalAmount(totalAmount)
                .beforeQuantity(beforeQty)
                .afterQuantity(afterQty)
                .referenceType(refType)
                .referenceId(refId)
                .build();
        stockMovementRepository.save(movement);
    }
}
