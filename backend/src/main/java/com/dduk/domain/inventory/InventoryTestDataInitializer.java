package com.dduk.domain.inventory;

import com.dduk.domain.inventory.item.*;
import com.dduk.domain.inventory.stock.*;
import com.dduk.domain.inventory.warehouse.*;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import java.math.BigDecimal;

@Component
@RequiredArgsConstructor
public class InventoryTestDataInitializer implements CommandLineRunner {

    private final ItemRepository itemRepository;
    private final WarehouseRepository warehouseRepository;
    private final InventoryService inventoryService;

    @Override
    public void run(String... args) throws Exception {
        if (warehouseRepository.count() > 0) return;

        // 1. Warehouses
        Warehouse whA = Warehouse.builder().warehouseCode("WH-A").warehouseName("제1 물류센터 (서울)").location("서울 금천구").status("ACTIVE").build();
        Warehouse whB = Warehouse.builder().warehouseCode("WH-B").warehouseName("제2 물류센터 (용인)").location("경기 용인시").status("ACTIVE").build();
        warehouseRepository.save(whA);
        warehouseRepository.save(whB);

        // 2. Items
        Item item1 = Item.builder()
                .itemCode("RM-TEA-001")
                .name("유기농 녹차 잎 (1kg)")
                .itemType(ItemType.RAW_MATERIAL)
                .unit("EA")
                .standardCost(new BigDecimal("15000"))
                .unitPrice(new BigDecimal("25000"))
                .active(true)
                .build();
        Item item2 = Item.builder()
                .itemCode("FG-TEA-001")
                .name("프리미엄 녹차 티백 (20개입)")
                .itemType(ItemType.FINISHED_GOOD)
                .unit("BOX")
                .standardCost(new BigDecimal("3000"))
                .unitPrice(new BigDecimal("8000"))
                .active(true)
                .build();
        itemRepository.save(item1);
        itemRepository.save(item2);

        // 3. Initial Movements (Inbound)
        inventoryService.increaseStock(item1.getId(), whA.getId(), 100, new BigDecimal("14500"), MovementReason.PURCHASE_RECEIVED, "INITIAL", "INIT-001");
        inventoryService.increaseStock(item1.getId(), whA.getId(), 50, new BigDecimal("16000"), MovementReason.PURCHASE_RECEIVED, "INITIAL", "INIT-002"); // Should update avg cost
        
        inventoryService.increaseStock(item2.getId(), whB.getId(), 500, new BigDecimal("2800"), MovementReason.PURCHASE_RECEIVED, "INITIAL", "INIT-003");

        // 4. Transfer
        inventoryService.transferStock(item1.getId(), whA.getId(), whB.getId(), 30, "INTERNAL", "TRF-001");

        // 5. Outbound
        inventoryService.decreaseStock(item2.getId(), whB.getId(), 100, MovementReason.SALES_SHIPPED, "ORDER", "ORD-001");
    }
}
