package com.dduk.service.inventory;

import com.dduk.entity.inventory.Item;
import com.dduk.entity.inventory.ItemType;
import com.dduk.entity.inventory.MovementReason;
import com.dduk.entity.inventory.Warehouse;
import com.dduk.repository.inventory.ItemRepository;
import com.dduk.repository.inventory.WarehouseRepository;
import com.dduk.repository.inventory.WarehouseTransferRepository;
import com.dduk.dto.inventory.WarehouseTransferRequestDto;
import com.dduk.dto.inventory.WarehouseTransferItemDto;
import com.dduk.dto.inventory.WarehouseTransferResponseDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class InventoryTestDataInitializer implements CommandLineRunner {

    private final ItemRepository itemRepository;
    private final WarehouseRepository warehouseRepository;
    private final InventoryService inventoryService;
    private final WarehouseTransferRepository warehouseTransferRepository;
    private final WarehouseTransferService warehouseTransferService;

    @Override
    public void run(String... args) throws Exception {
        // 1. 만약 Warehouses가 아예 없다면 WH-A, WH-B 생성 (H2 등 개발 임시용)
        if (warehouseRepository.count() == 0) {
            Warehouse whMain = Warehouse.builder().warehouseCode("WH-MAIN").warehouseName("본사창고").location("서울시 서초구").status("ACTIVE").build();
            Warehouse whRaw = Warehouse.builder().warehouseCode("WH-RAW").warehouseName("원재료창고").location("경기도 성남시").status("ACTIVE").build();
            Warehouse whFinished = Warehouse.builder().warehouseCode("WH-FINISHED").warehouseName("완제품창고").location("경기도 성남시").status("ACTIVE").build();
            Warehouse whCold = Warehouse.builder().warehouseCode("WH-COLD").warehouseName("냉장창고").location("인천시 서구").status("ACTIVE").build();
            Warehouse whReturn = Warehouse.builder().warehouseCode("WH-RETURN").warehouseName("반품창고").location("경기도 용인시").status("ACTIVE").build();
            warehouseRepository.save(whMain);
            warehouseRepository.save(whRaw);
            warehouseRepository.save(whFinished);
            warehouseRepository.save(whCold);
            warehouseRepository.save(whReturn);

            Item item1 = Item.builder().itemCode("ITM-0001").name("원자재 찹쌀가루").itemType(ItemType.RAW_MATERIAL).category("원재료").spec("20kg/포대 [공급처: (주)뚝딱물산, 위치: WH-RAW-A1, LOT: LOT-202605-A01]").unit("KG").standardCost(new BigDecimal("1200")).unitPrice(new BigDecimal("1800")).active(true).build();
            Item item2 = Item.builder().itemCode("ITM-0002").name("팥 앙금(국산)").itemType(ItemType.RAW_MATERIAL).category("원재료").spec("10kg/캔 [공급처: (주)뚝딱물산, 위치: WH-RAW-A2, LOT: LOT-202605-A02]").unit("KG").standardCost(new BigDecimal("3500")).unitPrice(new BigDecimal("4800")).active(true).build();
            Item item3 = Item.builder().itemCode("ITM-0003").name("유기농 설탕").itemType(ItemType.RAW_MATERIAL).category("원재료").spec("15kg/포대 [공급처: (주)그린테크, 위치: WH-RAW-A3, LOT: LOT-202605-A03]").unit("KG").standardCost(new BigDecimal("1000")).unitPrice(new BigDecimal("1500")).active(true).build();
            Item item4 = Item.builder().itemCode("ITM-0004").name("포장용 필름").itemType(ItemType.PACKAGING).category("부자재").spec("500m/롤 [공급처: (주)그린테크, 위치: WH-MAIN-B1, LOT: LOT-202605-B01]").unit("EA").standardCost(new BigDecimal("12000")).unitPrice(new BigDecimal("18000")).active(true).build();
            Item item5 = Item.builder().itemCode("ITM-0005").name("선물세트 케이스").itemType(ItemType.PACKAGING).category("부자재").spec("100개입/묶음 [공급처: (주)글로벌네트웍스, 위치: WH-MAIN-B2, LOT: LOT-202605-B02]").unit("EA").standardCost(new BigDecimal("2000")).unitPrice(new BigDecimal("3500")).active(true).build();
            Item item6 = Item.builder().itemCode("ITM-0006").name("정통 찹쌀떡").itemType(ItemType.FINISHED_GOOD).category("완제품").spec("50g*10개입 [공급처: 자체생산, 위치: WH-FINISHED-C1, LOT: LOT-202605-C01]").unit("BOX").standardCost(new BigDecimal("4500")).unitPrice(new BigDecimal("8000")).active(true).build();
            Item item7 = Item.builder().itemCode("ITM-0007").name("모듬 경단 세트").itemType(ItemType.FINISHED_GOOD).category("완제품").spec("400g/팩 [공급처: 자체생산, 위치: WH-FINISHED-C2, LOT: LOT-202605-C02]").unit("EA").standardCost(new BigDecimal("3500")).unitPrice(new BigDecimal("6500")).active(true).build();
            Item item8 = Item.builder().itemCode("ITM-0008").name("쑥 찹쌀 반죽").itemType(ItemType.WORK_IN_PROGRESS).category("반제품").spec("10kg/배치 [공급처: 자체생산, 위치: WH-COLD-D1, LOT: LOT-202605-D01]").unit("KG").standardCost(new BigDecimal("2000")).unitPrice(new BigDecimal("3000")).active(true).build();
            Item item9 = Item.builder().itemCode("ITM-0009").name("시향 샘플킷").itemType(ItemType.FINISHED_GOOD).category("샘플상품").spec("1세트 [공급처: 뚝딱세무법인, 위치: WH-MAIN-B3, LOT: LOT-202605-E01]").unit("EA").standardCost(new BigDecimal("1000")).unitPrice(new BigDecimal("2000")).active(true).build();
            Item item10 = Item.builder().itemCode("ITM-0010").name("국화차 패키지").itemType(ItemType.FINISHED_GOOD).category("단종예정상품").spec("10팩입 [공급처: 뚝딱세무법인, 위치: WH-RETURN-E2, LOT: LOT-202605-E02]").unit("BOX").standardCost(new BigDecimal("3000")).unitPrice(new BigDecimal("6000")).active(true).build();
            Item item11 = Item.builder().itemCode("ITM-0011").name("프리미엄 흑임자 경단").itemType(ItemType.FINISHED_GOOD).category("완제품").spec("350g/팩 [공급처: 자체생산, 위치: WH-FINISHED-C3, LOT: LOT-202605-F01]").unit("EA").standardCost(new BigDecimal("4000")).unitPrice(new BigDecimal("7500")).active(true).build();
            Item item12 = Item.builder().itemCode("ITM-0012").name("오곡 꿀떡").itemType(ItemType.FINISHED_GOOD).category("완제품").spec("300g/팩 [공급처: 자체생산, 위치: WH-FINISHED-C4, LOT: LOT-202605-F02]").unit("EA").standardCost(new BigDecimal("2800")).unitPrice(new BigDecimal("5000")).active(true).build();
            Item item13 = Item.builder().itemCode("ITM-0013").name("포장 보자기(명절용)").itemType(ItemType.PACKAGING).category("부자재").spec("50개입/묶음 [공급처: (주)글로벌네트웍스, 위치: WH-MAIN-B4, LOT: LOT-202605-F03]").unit("EA").standardCost(new BigDecimal("15000")).unitPrice(new BigDecimal("25000")).active(true).build();
            Item item14 = Item.builder().itemCode("ITM-0014").name("인절미 쑥가루").itemType(ItemType.RAW_MATERIAL).category("원재료").spec("10kg/포대 [공급처: (주)뚝딱물산, 위치: WH-RAW-A4, LOT: LOT-202605-F04]").unit("KG").standardCost(new BigDecimal("4500")).unitPrice(new BigDecimal("6500")).active(true).build();
            Item item15 = Item.builder().itemCode("ITM-0015").name("천연 호박 가루").itemType(ItemType.RAW_MATERIAL).category("원재료").spec("5kg/캔 [공급처: (주)그린테크, 위치: WH-RAW-A5, LOT: LOT-202605-F05]").unit("KG").standardCost(new BigDecimal("8000")).unitPrice(new BigDecimal("12000")).active(true).build();
            Item item16 = Item.builder().itemCode("ITM-0016").name("호박 인절미(완제)").itemType(ItemType.FINISHED_GOOD).category("완제품").spec("400g/팩 [공급처: 자체생산, 위치: WH-FINISHED-C5, LOT: LOT-202605-G01]").unit("EA").standardCost(new BigDecimal("3800")).unitPrice(new BigDecimal("7000")).active(true).build();
            Item item17 = Item.builder().itemCode("ITM-0017").name("조청 시럽").itemType(ItemType.RAW_MATERIAL).category("원재료").spec("10L/통 [공급처: (주)뚝딱물산, 위치: WH-RAW-A6, LOT: LOT-202605-G02]").unit("KG").standardCost(new BigDecimal("20000")).unitPrice(new BigDecimal("28000")).active(true).build();
            Item item18 = Item.builder().itemCode("ITM-0018").name("포장 완충재").itemType(ItemType.PACKAGING).category("부자재").spec("200개입/BOX [공급처: (주)그린테크, 위치: WH-MAIN-B5, LOT: LOT-202605-G03]").unit("EA").standardCost(new BigDecimal("5000")).unitPrice(new BigDecimal("8000")).active(true).build();
            Item item19 = Item.builder().itemCode("ITM-0019").name("자체 조제 팥소").itemType(ItemType.WORK_IN_PROGRESS).category("반제품").spec("20kg/배치 [공급처: 자체생산, 위치: WH-COLD-D2, LOT: LOT-202605-H01]").unit("KG").standardCost(new BigDecimal("3000")).unitPrice(new BigDecimal("4000")).active(true).build();
            Item item20 = Item.builder().itemCode("ITM-0020").name("프리미엄 찹쌀 떡 선물세트").itemType(ItemType.FINISHED_GOOD).category("완제품").spec("20개입/BOX [공급처: 자체생산, 위치: WH-FINISHED-C6, LOT: LOT-202605-H02]").unit("BOX").standardCost(new BigDecimal("18000")).unitPrice(new BigDecimal("32000")).active(true).build();

            itemRepository.save(item1);
            itemRepository.save(item2);
            itemRepository.save(item3);
            itemRepository.save(item4);
            itemRepository.save(item5);
            itemRepository.save(item6);
            itemRepository.save(item7);
            itemRepository.save(item8);
            itemRepository.save(item9);
            itemRepository.save(item10);
            itemRepository.save(item11);
            itemRepository.save(item12);
            itemRepository.save(item13);
            itemRepository.save(item14);
            itemRepository.save(item15);
            itemRepository.save(item16);
            itemRepository.save(item17);
            itemRepository.save(item18);
            itemRepository.save(item19);
            itemRepository.save(item20);

            inventoryService.increaseStock(item1.getId(), whMain.getId(), 1200, new BigDecimal("1200"), MovementReason.PURCHASE_RECEIVED, "INITIAL", "INIT-001");
            inventoryService.increaseStock(item1.getId(), whRaw.getId(), 4500, new BigDecimal("1180"), MovementReason.PURCHASE_RECEIVED, "INITIAL", "INIT-002");
            inventoryService.increaseStock(item2.getId(), whMain.getId(), 150, new BigDecimal("3500"), MovementReason.PURCHASE_RECEIVED, "INITIAL", "INIT-003");
            inventoryService.increaseStock(item2.getId(), whRaw.getId(), 800, new BigDecimal("3450"), MovementReason.PURCHASE_RECEIVED, "INITIAL", "INIT-004");
            inventoryService.increaseStock(item3.getId(), whRaw.getId(), 1200, new BigDecimal("1000"), MovementReason.PURCHASE_RECEIVED, "INITIAL", "INIT-005");
            inventoryService.increaseStock(item4.getId(), whMain.getId(), 0, new BigDecimal("12000"), MovementReason.PURCHASE_RECEIVED, "INITIAL", "INIT-006");
            inventoryService.increaseStock(item4.getId(), whCold.getId(), 180, new BigDecimal("12100"), MovementReason.PURCHASE_RECEIVED, "INITIAL", "INIT-007");
            inventoryService.increaseStock(item5.getId(), whMain.getId(), 600, new BigDecimal("2000"), MovementReason.PURCHASE_RECEIVED, "INITIAL", "INIT-008");
            inventoryService.increaseStock(item6.getId(), whFinished.getId(), 1500, new BigDecimal("4500"), MovementReason.PURCHASE_RECEIVED, "INITIAL", "INIT-009");
            inventoryService.increaseStock(item6.getId(), whCold.getId(), 800, new BigDecimal("4550"), MovementReason.PURCHASE_RECEIVED, "INITIAL", "INIT-010");
            inventoryService.increaseStock(item7.getId(), whFinished.getId(), 75, new BigDecimal("3500"), MovementReason.PURCHASE_RECEIVED, "INITIAL", "INIT-011");
            inventoryService.increaseStock(item8.getId(), whCold.getId(), 350, new BigDecimal("2000"), MovementReason.PURCHASE_RECEIVED, "INITIAL", "INIT-012");
            inventoryService.increaseStock(item9.getId(), whMain.getId(), 25, new BigDecimal("1000"), MovementReason.PURCHASE_RECEIVED, "INITIAL", "INIT-013");
            inventoryService.increaseStock(item10.getId(), whReturn.getId(), 4, new BigDecimal("3000"), MovementReason.PURCHASE_RECEIVED, "INITIAL", "INIT-014");
            inventoryService.increaseStock(item11.getId(), whFinished.getId(), 400, new BigDecimal("4000"), MovementReason.PURCHASE_RECEIVED, "INITIAL", "INIT-015");
            inventoryService.increaseStock(item12.getId(), whFinished.getId(), 620, new BigDecimal("2800"), MovementReason.PURCHASE_RECEIVED, "INITIAL", "INIT-016");
            inventoryService.increaseStock(item13.getId(), whMain.getId(), 150, new BigDecimal("15000"), MovementReason.PURCHASE_RECEIVED, "INITIAL", "INIT-017");
            inventoryService.increaseStock(item14.getId(), whRaw.getId(), 900, new BigDecimal("4500"), MovementReason.PURCHASE_RECEIVED, "INITIAL", "INIT-018");
            inventoryService.increaseStock(item15.getId(), whRaw.getId(), 400, new BigDecimal("8000"), MovementReason.PURCHASE_RECEIVED, "INITIAL", "INIT-019");
            inventoryService.increaseStock(item16.getId(), whFinished.getId(), 500, new BigDecimal("3800"), MovementReason.PURCHASE_RECEIVED, "INITIAL", "INIT-020");
            inventoryService.increaseStock(item17.getId(), whRaw.getId(), 80, new BigDecimal("20000"), MovementReason.PURCHASE_RECEIVED, "INITIAL", "INIT-021");
            inventoryService.increaseStock(item18.getId(), whMain.getId(), 200, new BigDecimal("5000"), MovementReason.PURCHASE_RECEIVED, "INITIAL", "INIT-022");
            inventoryService.increaseStock(item19.getId(), whCold.getId(), 120, new BigDecimal("3000"), MovementReason.PURCHASE_RECEIVED, "INITIAL", "INIT-023");
            inventoryService.increaseStock(item20.getId(), whFinished.getId(), 15, new BigDecimal("18000"), MovementReason.PURCHASE_RECEIVED, "INITIAL", "INIT-024");
        }

        // 2. 만약 창고 이동 데이터가 부족하다면 실감나는 시드 데이터를 대량 적재
        if (warehouseTransferRepository.count() < 10) {
            log.info("[InventoryTestDataInitializer] 창고 이동 고도화 시드 데이터 자동 생성을 시작합니다.");
            
            // 품목 및 창고 조회 (bootstrap_data.sql 에서 인서트된 녀석들 사용)
            List<Item> items = itemRepository.findAll();
            List<Warehouse> warehouses = warehouseRepository.findAll();
            
            if (items.size() >= 2 && warehouses.size() >= 2) {
                Item item1 = items.get(0); // 얼그레이 티백 등
                Item item2 = items.get(1); // 루이보스 블렌드 등
                Warehouse whMain = warehouses.get(0); // 본사창고
                Warehouse whRaw = warehouses.get(1);  // 원재료창고
                
                // 1. PENDING (승인대기) 긴급 보충 건 생성
                try {
                    WarehouseTransferRequestDto req = WarehouseTransferRequestDto.builder()
                            .sourceWarehouseId(whRaw.getId())
                            .targetWarehouseId(whMain.getId())
                            .remarks("[긴급] 본사창고 원자재 부족분 긴급 보충 요청")
                            .items(Collections.singletonList(
                                    WarehouseTransferItemDto.builder().itemId(item1.getId()).quantity(15).build()
                            ))
                            .build();
                    warehouseTransferService.requestTransfer(req, 1L);
                } catch (Exception e) {
                    log.error("시드 데이터 생성 실패 (PENDING): {}", e.getMessage());
                }

                // 2. APPROVED (이동중) 보충 건 생성
                try {
                    WarehouseTransferRequestDto req = WarehouseTransferRequestDto.builder()
                            .sourceWarehouseId(whRaw.getId())
                            .targetWarehouseId(whMain.getId())
                            .remarks("본사창고 얼그레이 주간 정기 보충 이동")
                            .items(Collections.singletonList(
                                    WarehouseTransferItemDto.builder().itemId(item1.getId()).quantity(20).build()
                            ))
                            .build();
                    WarehouseTransferResponseDto res = warehouseTransferService.requestTransfer(req, 1L);
                    warehouseTransferService.approveTransfer(res.getId(), 1L);
                } catch (Exception e) {
                    log.error("시드 데이터 생성 실패 (APPROVED): {}", e.getMessage());
                }

                // 3. COMPLETED (이동완료) 대량 건 생성
                try {
                    WarehouseTransferRequestDto req = WarehouseTransferRequestDto.builder()
                            .sourceWarehouseId(whRaw.getId())
                            .targetWarehouseId(whMain.getId())
                            .remarks("[생산투입] 원자재창고에서 완제품 공급을 위한 원료 이동 완료")
                            .items(Collections.singletonList(
                                    WarehouseTransferItemDto.builder().itemId(item2.getId()).quantity(10).build()
                            ))
                            .build();
                    WarehouseTransferResponseDto res = warehouseTransferService.requestTransfer(req, 1L);
                    warehouseTransferService.approveTransfer(res.getId(), 1L);
                    warehouseTransferService.completeTransfer(res.getId());
                } catch (Exception e) {
                    log.error("시드 데이터 생성 실패 (COMPLETED): {}", e.getMessage());
                }

                // 4. CANCELLED (반려 사유 탑재) 건 생성
                try {
                    WarehouseTransferRequestDto req = WarehouseTransferRequestDto.builder()
                            .sourceWarehouseId(whMain.getId())
                            .targetWarehouseId(whRaw.getId())
                            .remarks("샘플상품 임시 반납 요청")
                            .items(Collections.singletonList(
                                    WarehouseTransferItemDto.builder().itemId(item1.getId()).quantity(5).build()
                            ))
                            .build();
                    WarehouseTransferResponseDto res = warehouseTransferService.requestTransfer(req, 1L);
                    warehouseTransferService.cancelTransfer(res.getId());
                    warehouseTransferRepository.findById(res.getId()).ifPresent(t -> {
                        t.setRemarks("[반려] 사유: 요청 수량이 가용 한도를 초과하여 취소 후 재상신 요망.");
                        warehouseTransferRepository.save(t);
                    });
                } catch (Exception e) {
                    log.error("시드 데이터 생성 실패 (REJECTED): {}", e.getMessage());
                }

                // 5. CANCELLED (취소 사유 탑재) 건 생성
                try {
                    WarehouseTransferRequestDto req = WarehouseTransferRequestDto.builder()
                            .sourceWarehouseId(whMain.getId())
                            .targetWarehouseId(whRaw.getId())
                            .remarks("중복 등록된 불량 패키지 이동 건")
                            .items(Collections.singletonList(
                                    WarehouseTransferItemDto.builder().itemId(item1.getId()).quantity(10).build()
                            ))
                            .build();
                    WarehouseTransferResponseDto res = warehouseTransferService.requestTransfer(req, 1L);
                    warehouseTransferService.cancelTransfer(res.getId());
                    warehouseTransferRepository.findById(res.getId()).ifPresent(t -> {
                        t.setRemarks("[취소] 사유: 사용자의 단순 변심 및 중복 신청으로 인한 요청 취소.");
                        warehouseTransferRepository.save(t);
                    });
                } catch (Exception e) {
                    log.error("시드 데이터 생성 실패 (CANCELLED): {}", e.getMessage());
                }
            }
            log.info("[InventoryTestDataInitializer] 창고 이동 고도화 시드 데이터 생성이 정합성 있게 완료되었습니다.");
        }
    }
}

