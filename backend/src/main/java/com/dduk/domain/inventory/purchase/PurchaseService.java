package com.dduk.domain.inventory.purchase;

import com.dduk.domain.accounting.AccountingConstants;
import com.dduk.domain.accounting.autojounal.AutoJournalService;
import com.dduk.domain.inventory.item.Item;
import com.dduk.domain.inventory.item.ItemRepository;
import com.dduk.domain.inventory.purchase.dto.*;
import com.dduk.domain.inventory.stock.InventoryService;
import com.dduk.domain.inventory.stock.MovementReason;
import com.dduk.domain.inventory.stock.MovementType;
import com.dduk.domain.inventory.stock.StockMovementRepository;
import com.dduk.domain.inventory.vendor.Vendor;
import com.dduk.domain.inventory.vendor.VendorRepository;
import com.dduk.domain.inventory.warehouse.Warehouse;
import com.dduk.domain.inventory.warehouse.WarehouseRepository;
import com.dduk.entity.admin.Member;
import com.dduk.repository.admin.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PurchaseService {

    private final PurchaseOrderRepository purchaseOrderRepository;
    private final VendorRepository vendorRepository;
    private final MemberRepository memberRepository;
    private final ItemRepository itemRepository;
    private final WarehouseRepository warehouseRepository;
    private final AutoJournalService autoJournalService;
    private final InventoryService inventoryService;
    private final StockMovementRepository stockMovementRepository;

    private static final BigDecimal TAX_RATE = new BigDecimal("0.1");

    public List<PurchaseOrder> getAllOrders() {
        return purchaseOrderRepository.findAll();
    }

    public PurchaseOrder getOrder(Long id) {
        return purchaseOrderRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "발주서를 찾을 수 없습니다."));
    }

    @Transactional
    public PurchaseOrder createOrder(PurchaseOrder order) {
        return purchaseOrderRepository.save(order);
    }

    @Transactional
    public PurchaseOrderResponseDto createPurchaseOrder(PurchaseOrderCreateDto requestDto, Long requestedByMemberId) {
        validateCreateRequest(requestDto);

        Vendor vendor = vendorRepository.findById(requestDto.getVendorId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "거래처를 찾을 수 없습니다."));
        Member requestedBy = findMember(requestedByMemberId);
        
        // 기본 창고 설정 (실제 구현에서는 요청에서 받거나 기본값을 설정해야 함)
        Warehouse warehouse = warehouseRepository.findAll().stream().findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "등록된 창고가 없습니다."));

        BigDecimal totalAmount = BigDecimal.ZERO;
        List<PurchaseOrderItem> items = new ArrayList<>();

        PurchaseOrder purchaseOrder = PurchaseOrder.builder()
                .purchaseOrderNo(generatePurchaseOrderNo())
                .vendor(vendor)
                .warehouse(warehouse)
                .requestedBy(requestedBy)
                .orderDate(LocalDate.now())
                .expectedDate(requestDto.getExpectedDate())
                .status(PurchaseStatus.ORDERED)
                .totalAmount(BigDecimal.ZERO) // 임시
                .note(requestDto.getNote())
                .build();

        for (PurchaseOrderItemCreateDto itemDto : requestDto.getItems()) {
            Item item = itemRepository.findById(itemDto.getItemId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "품목을 찾을 수 없습니다."));
            
            BigDecimal unitPrice = itemDto.getUnitPrice().setScale(2, RoundingMode.HALF_UP);
            BigDecimal supplyAmount = unitPrice.multiply(BigDecimal.valueOf(itemDto.getQuantity())).setScale(2, RoundingMode.HALF_UP);
            BigDecimal taxAmount = supplyAmount.multiply(TAX_RATE).setScale(2, RoundingMode.HALF_UP);
            BigDecimal lineAmount = supplyAmount.add(taxAmount);

            PurchaseOrderItem orderItem = PurchaseOrderItem.builder()
                    .purchaseOrder(purchaseOrder)
                    .item(item)
                    .quantity(itemDto.getQuantity())
                    .unitPrice(unitPrice)
                    .supplyAmount(supplyAmount)
                    .taxAmount(taxAmount)
                    .lineAmount(lineAmount)
                    .expectedDate(itemDto.getExpectedDate() != null ? itemDto.getExpectedDate() : requestDto.getExpectedDate())
                    .note(itemDto.getNote())
                    .build();
            
            items.add(orderItem);
            totalAmount = totalAmount.add(lineAmount);
        }

        purchaseOrder.setTotalAmount(totalAmount);
        purchaseOrder.setItems(items);
        
        PurchaseOrder savedOrder = purchaseOrderRepository.save(purchaseOrder);
        return PurchaseOrderResponseDto.from(savedOrder, items);
    }

    @Transactional
    public PurchaseOrderResponseDto updatePurchaseOrderStatus(Long id, PurchaseOrderStatusUpdateDto requestDto, Long memberId) {
        PurchaseStatus nextStatus = PurchaseStatus.valueOf(requestDto.getStatus().toUpperCase());
        PurchaseOrder order = transitionStatus(id, nextStatus);
        
        if (nextStatus == PurchaseStatus.APPROVED) {
            order.setApprovedBy(findMember(memberId));
            purchaseOrderRepository.save(order);
        }

        return PurchaseOrderResponseDto.from(order, order.getItems());
    }

    @Transactional(rollbackFor = Exception.class)
    public PurchaseOrder transitionStatus(Long id, PurchaseStatus nextStatus) {
        PurchaseOrder order = getOrder(id);
        PurchaseStatus currentStatus = order.getStatus();

        if (currentStatus == nextStatus) return order;
        
        if (nextStatus == PurchaseStatus.RECEIVED || nextStatus == PurchaseStatus.COMPLETED) {
            if (currentStatus != PurchaseStatus.RECEIVED && currentStatus != PurchaseStatus.COMPLETED) {
                boolean alreadyReceived = stockMovementRepository.existsByReferenceTypeAndReferenceIdAndMovementType(
                        "PURCHASE", order.getPurchaseOrderNo(), MovementType.INBOUND
                );
                
                if (!alreadyReceived) {
                    for (PurchaseOrderItem item : order.getItems()) {
                        inventoryService.increaseStock(
                                item.getItem().getId(),
                                order.getWarehouse().getId(),
                                item.getQuantity(),
                                item.getUnitPrice(),
                                MovementReason.PURCHASE_RECEIVED,
                                "PURCHASE",
                                order.getPurchaseOrderNo()
                        );
                    }
                }
                
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

    private void validateCreateRequest(PurchaseOrderCreateDto requestDto) {
        if (requestDto == null || requestDto.getVendorId() == null || requestDto.getItems() == null || requestDto.getItems().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "발주 정보가 부족합니다.");
        }
    }

    private Member findMember(Long memberId) {
        if (memberId == null) return null;
        return memberRepository.findById(memberId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "사용자를 찾을 수 없습니다."));
    }

    @Transactional
    public PurchaseRequestResponseDto createPurchaseRequest(PurchaseRequestCreateDto requestDto, Long requestedByMemberId) {
        validatePurchaseRequest(requestDto, requestedByMemberId);

        Item item = itemRepository.findById(requestDto.getItemId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "품목을 찾을 수 없습니다."));
        Vendor vendor = vendorRepository.findById(requestDto.getVendorId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "거래처를 찾을 수 없습니다."));
        Member requestedBy = findMember(requestedByMemberId);
        
        Warehouse warehouse = warehouseRepository.findAll().stream().findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "등록된 창고가 없습니다."));

        BigDecimal unitPrice = requestDto.getUnitPrice().setScale(2, RoundingMode.HALF_UP);
        BigDecimal supplyAmount = unitPrice.multiply(BigDecimal.valueOf(requestDto.getQuantity())).setScale(2, RoundingMode.HALF_UP);
        BigDecimal taxAmount = supplyAmount.multiply(TAX_RATE).setScale(2, RoundingMode.HALF_UP);
        BigDecimal totalAmount = supplyAmount.add(taxAmount);

        PurchaseOrder purchaseOrder = PurchaseOrder.builder()
                .purchaseOrderNo(generatePurchaseRequestNo())
                .vendor(vendor)
                .warehouse(warehouse)
                .requestedBy(requestedBy)
                .orderDate(LocalDate.now())
                .expectedDate(requestDto.getExpectedDate())
                .status(PurchaseStatus.DRAFT) // 요청 상태를 DRAFT로 매핑 (혹은 REQUESTED 추가 가능)
                .totalAmount(totalAmount)
                .note(requestDto.getNote())
                .build();

        PurchaseOrderItem orderItem = PurchaseOrderItem.builder()
                .purchaseOrder(purchaseOrder)
                .item(item)
                .quantity(requestDto.getQuantity())
                .unit(item.getUnit())
                .unitPrice(unitPrice)
                .supplyAmount(supplyAmount)
                .taxAmount(taxAmount)
                .lineAmount(totalAmount)
                .expectedDate(requestDto.getExpectedDate())
                .note(requestDto.getNote())
                .build();

        purchaseOrder.getItems().add(orderItem);
        PurchaseOrder savedOrder = purchaseOrderRepository.save(purchaseOrder);
        
        return PurchaseRequestResponseDto.from(savedOrder, orderItem);
    }

    private void validatePurchaseRequest(PurchaseRequestCreateDto requestDto, Long requestedByMemberId) {
        if (requestDto == null || requestDto.getItemId() == null || requestDto.getVendorId() == null || requestedByMemberId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "구매 요청 정보가 부족합니다.");
        }
    }

    private String generatePurchaseOrderNo() {
        return "PO-" + LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE) + "-" + System.currentTimeMillis();
    }

    private String generatePurchaseRequestNo() {
        return "PR-" + LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE) + "-" + System.currentTimeMillis();
    }
}
