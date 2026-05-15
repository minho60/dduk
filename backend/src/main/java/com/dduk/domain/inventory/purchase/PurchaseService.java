package com.dduk.domain.inventory.purchase;

import com.dduk.domain.accounting.AccountingConstants;
import com.dduk.domain.accounting.autojounal.AutoJournalService;
import com.dduk.domain.inventory.stock.InventoryService;
import com.dduk.domain.inventory.stock.MovementReason;
import com.dduk.domain.inventory.stock.MovementType;
import com.dduk.domain.inventory.stock.StockMovementRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PurchaseService {

    private final PurchaseOrderRepository purchaseOrderRepository;
    private final AutoJournalService autoJournalService;
    private final InventoryService inventoryService;
    private final StockMovementRepository stockMovementRepository;

    public List<PurchaseOrder> getAllOrders() {
        return purchaseOrderRepository.findAll();
    }

    public PurchaseOrder getOrder(Long id) {
        return purchaseOrderRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Purchase Order not found"));
    }

    @Transactional
    public PurchaseOrder createOrder(PurchaseOrder order) {
        return purchaseOrderRepository.save(order);
    }

    @Transactional(rollbackFor = Exception.class)
    public PurchaseOrder transitionStatus(Long id, PurchaseStatus nextStatus) {
        PurchaseOrder order = getOrder(id);
        PurchaseStatus currentStatus = order.getStatus();

        // 1. 상태 전이 유효성 검사 (간략화)
        if (currentStatus == nextStatus) return order;
        
        // 2. 입고 완료 시 재고 증가 및 회계 전표 생성
        if (nextStatus == PurchaseStatus.RECEIVED || nextStatus == PurchaseStatus.COMPLETED) {
            
            if (currentStatus != PurchaseStatus.RECEIVED && currentStatus != PurchaseStatus.COMPLETED) {
                // 중복 입고 방지 (재고)
                boolean alreadyReceived = stockMovementRepository.existsByReferenceTypeAndReferenceIdAndMovementType(
                        "PURCHASE", order.getPurchaseOrderNo(), MovementType.IN
                );
                
                if (!alreadyReceived) {
                    // 재고 증가
                    for (PurchaseOrderItem item : order.getItems()) {
                        inventoryService.increaseStock(
                                item.getItem().getId(),
                                order.getWarehouse().getId(),
                                item.getQuantity(),
                                MovementReason.PURCHASE_RECEIVED,
                                "PURCHASE",
                                order.getPurchaseOrderNo()
                        );
                    }
                }
                
                // 중복 생성 방지는 AccountingService 내부에서 수행됨
                autoJournalService.createAndPostJournal(
                        AccountingConstants.SOURCE_PURCHASE,
                        order.getId(),
                        order
                );
            }
        }

        order.setStatus(nextStatus);
        return purchaseOrderRepository.save(order);
    }
}
