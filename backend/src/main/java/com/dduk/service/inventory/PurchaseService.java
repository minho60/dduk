package com.dduk.service.inventory;

import com.dduk.dto.inventory.PurchaseRequestCreateDto;
import com.dduk.dto.inventory.PurchaseRequestResponseDto;
import com.dduk.entity.admin.Member;
import com.dduk.entity.inventory.Item;
import com.dduk.entity.inventory.PurchaseOrder;
import com.dduk.entity.inventory.PurchaseOrderItem;
import com.dduk.entity.inventory.Vendor;
import com.dduk.repository.admin.MemberRepository;
import com.dduk.repository.inventory.ItemRepository;
import com.dduk.repository.inventory.PurchaseOrderItemRepository;
import com.dduk.repository.inventory.PurchaseOrderRepository;
import com.dduk.repository.inventory.VendorRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * 
   com.dduk.service.inventory
   ├─ VendorService              // 거래처 등록/조회/수정/비활성화
   ├─ PurchaseService            // 구매 요청/승인
   └─ PurchaseOrderService       // 발주 생성/상태 변경 

    InboundService: 입고 등록, 입고 목록 조회, 입고 취소
    OutboundService: 출고 등록, 출고 목록 조회, 출고 취소
    PurchaseService: 구매 요청, 구매 승인, 구매 내역
    PurchaseOrderService: 발주 생성, 발주 상태 변경, 발주서 조회
    InventoryService: 현재 재고 조회, 재고 반영, 안전재고 확인
    VendorService: 거래처 등록, 거래처 조회, 거래처 수정, 거래처 비활성화

   재고 부족 확인(흐름)
    → 구매 요청
    → 구매 승인
    → 발주서 생성
    → 거래처에 발주
    → 입고
    → 재고 반영

   상태(status)로 관리
   REQUESTED(DEFAULT) = 구매 요청
   APPROVED = 구매 승인
   ORDERED = 발주 완료
   RECEIVED = 입고 완료
   CANCELED = 취소

   구매(정의) = 회사 내부의 구매 필요/요청/승인 관리
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PurchaseService {

    private static final String STATUS_REQUESTED = "REQUESTED";
    private static final BigDecimal TAX_RATE = BigDecimal.valueOf(0.1);

    private final ItemRepository itemRepository;
    private final VendorRepository vendorRepository;
    private final MemberRepository memberRepository;
    private final PurchaseOrderRepository purchaseOrderRepository;
    private final PurchaseOrderItemRepository purchaseOrderItemRepository;

    @Transactional
    public PurchaseRequestResponseDto createPurchaseRequest(PurchaseRequestCreateDto requestDto, Long requestedByMemberId) {
        validateCreateRequest(requestDto, requestedByMemberId);

        Item item = itemRepository.findById(requestDto.getItemId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "품목을 찾을 수 없습니다."));
        Vendor vendor = vendorRepository.findById(requestDto.getVendorId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "거래처를 찾을 수 없습니다."));
        Member requestedBy = memberRepository.findById(requestedByMemberId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "요청자를 찾을 수 없습니다."));

        BigDecimal unitPrice = requestDto.getUnitPrice().setScale(2, RoundingMode.HALF_UP);
        BigDecimal supplyAmount = unitPrice
                .multiply(BigDecimal.valueOf(requestDto.getQuantity()))
                .setScale(2, RoundingMode.HALF_UP);
        BigDecimal taxAmount = supplyAmount
                .multiply(TAX_RATE)
                .setScale(2, RoundingMode.HALF_UP);
        BigDecimal totalAmount = supplyAmount.add(taxAmount);

        PurchaseOrder purchaseOrder = PurchaseOrder.builder()
                .purchaseOrderNo(generatePurchaseRequestNo())
                .vendor(vendor)
                .requestedBy(requestedBy)
                .orderDate(LocalDate.now())
                .expectedDate(requestDto.getExpectedDate())
                .status(STATUS_REQUESTED)
                .totalAmount(totalAmount)
                .note(requestDto.getNote())
                .build();
        PurchaseOrder savedPurchaseOrder = purchaseOrderRepository.save(purchaseOrder);

        PurchaseOrderItem purchaseOrderItem = PurchaseOrderItem.builder()
                .purchaseOrder(savedPurchaseOrder)
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
        PurchaseOrderItem savedPurchaseOrderItem = purchaseOrderItemRepository.save(purchaseOrderItem);

        return PurchaseRequestResponseDto.from(savedPurchaseOrder, savedPurchaseOrderItem);
    }

    private void validateCreateRequest(PurchaseRequestCreateDto requestDto, Long requestedByMemberId) {
        if (requestDto == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "구매요청 정보가 필요합니다.");
        }
        if (requestDto.getItemId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "품목을 선택해야 합니다.");
        }
        if (requestDto.getVendorId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "거래처를 선택해야 합니다.");
        }
        if (requestedByMemberId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "요청자 정보가 필요합니다.");
        }
        if (requestDto.getQuantity() == null || requestDto.getQuantity() <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "구매 수량은 1 이상이어야 합니다.");
        }
        if (requestDto.getUnitPrice() == null || requestDto.getUnitPrice().compareTo(BigDecimal.ZERO) < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "단가는 0 이상이어야 합니다.");
        }
    }

    private String generatePurchaseRequestNo() {
        return "PR-" + LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE) + "-" + System.currentTimeMillis();
    }
}
