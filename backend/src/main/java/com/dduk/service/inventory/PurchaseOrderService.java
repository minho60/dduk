package com.dduk.service.inventory;

import com.dduk.dto.inventory.PurchaseOrderCreateDto;
import com.dduk.dto.inventory.PurchaseOrderItemCreateDto;
import com.dduk.dto.inventory.PurchaseOrderResponseDto;
import com.dduk.dto.inventory.PurchaseOrderStatusUpdateDto;
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
import java.util.ArrayList;
import java.util.List;

/**
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
   
   상태(Status)로 관리
   REQUESTED: 아직 내부 구매요청 단계
   APPROVED: 구매 승인 완료, 발주 가능
   ORDERED: 거래처에 실제 발주 넣음
   RECEIVED: 물건 입고까지 완료
   ANCELED: 요청 또는 발주 취소
   
    1. 구매 요청이 APPROVED 상태인지 확인
    2. 발주 번호 생성 (예: PO-20240601-0001)
    3. PurchaseOrder 엔티티 생성 및 저장
    4. 발주서 PDF 생성 (선택 사항)
    5. 거래처에 발주서 이메일 발송 (선택 사항)
   
    발주(정의) = 거래처에게 실제 주문서를 보내는 것
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PurchaseOrderService {

    private static final String STATUS_ORDERED = "ORDERED";
    private static final String STATUS_REQUESTED = "REQUESTED";
    private static final String STATUS_APPROVED = "APPROVED";
    private static final String STATUS_RECEIVED = "RECEIVED";
    private static final String STATUS_CANCELED = "CANCELED";
    private static final BigDecimal TAX_RATE = BigDecimal.valueOf(0.1);

    private final VendorRepository vendorRepository;
    private final ItemRepository itemRepository;
    private final MemberRepository memberRepository;
    private final PurchaseOrderRepository purchaseOrderRepository;
    private final PurchaseOrderItemRepository purchaseOrderItemRepository;

    @Transactional
    public PurchaseOrderResponseDto createPurchaseOrder(PurchaseOrderCreateDto requestDto, Long requestedByMemberId) {
        validateCreateRequest(requestDto);

        Vendor vendor = vendorRepository.findById(requestDto.getVendorId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "거래처를 찾을 수 없습니다."));
        Member requestedBy = findRequestedBy(requestedByMemberId);

        List<PurchaseOrderLine> lines = new ArrayList<>();
        BigDecimal totalAmount = BigDecimal.ZERO;

        for (PurchaseOrderItemCreateDto itemDto : requestDto.getItems()) {
            validateItemRequest(itemDto);

            Item item = itemRepository.findById(itemDto.getItemId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "품목을 찾을 수 없습니다."));
            BigDecimal unitPrice = itemDto.getUnitPrice().setScale(2, RoundingMode.HALF_UP);
            BigDecimal supplyAmount = unitPrice
                    .multiply(BigDecimal.valueOf(itemDto.getQuantity()))
                    .setScale(2, RoundingMode.HALF_UP);
            BigDecimal taxAmount = supplyAmount
                    .multiply(TAX_RATE)
                    .setScale(2, RoundingMode.HALF_UP);
            BigDecimal lineAmount = supplyAmount.add(taxAmount);
            LocalDate itemExpectedDate = itemDto.getExpectedDate() != null
                    ? itemDto.getExpectedDate()
                    : requestDto.getExpectedDate();

            lines.add(new PurchaseOrderLine(
                    item,
                    itemDto.getQuantity(),
                    unitPrice,
                    supplyAmount,
                    taxAmount,
                    lineAmount,
                    itemExpectedDate,
                    itemDto.getNote()
            ));
            totalAmount = totalAmount.add(lineAmount);
        }

        PurchaseOrder purchaseOrder = PurchaseOrder.builder()
                .purchaseOrderNo(generatePurchaseOrderNo())
                .vendor(vendor)
                .requestedBy(requestedBy)
                .orderDate(LocalDate.now())
                .expectedDate(requestDto.getExpectedDate())
                .status(STATUS_ORDERED)
                .totalAmount(totalAmount)
                .note(requestDto.getNote())
                .build();
        PurchaseOrder savedPurchaseOrder = purchaseOrderRepository.save(purchaseOrder);

        List<PurchaseOrderItem> purchaseOrderItems = new ArrayList<>();

        for (PurchaseOrderLine line : lines) {
            PurchaseOrderItem purchaseOrderItem = PurchaseOrderItem.builder()
                    .purchaseOrder(savedPurchaseOrder)
                    .item(line.item())
                    .quantity(line.quantity())
                    .unit(line.item().getUnit())
                    .unitPrice(line.unitPrice())
                    .supplyAmount(line.supplyAmount())
                    .taxAmount(line.taxAmount())
                    .lineAmount(line.lineAmount())
                    .expectedDate(line.expectedDate())
                    .note(line.note())
                    .build();

            purchaseOrderItems.add(purchaseOrderItemRepository.save(purchaseOrderItem));
        }

        return PurchaseOrderResponseDto.from(savedPurchaseOrder, purchaseOrderItems);
    }

    @Transactional
    public PurchaseOrderResponseDto updatePurchaseOrderStatus(
            Long purchaseOrderId,
            PurchaseOrderStatusUpdateDto requestDto,
            Long approvedByMemberId
    ) {
        PurchaseOrder purchaseOrder = findPurchaseOrder(purchaseOrderId);
        String status = validateStatusRequest(requestDto);
        Member approvedBy = purchaseOrder.getApprovedBy();

        if (STATUS_APPROVED.equals(status)) {
            if (approvedByMemberId == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "승인자 정보가 필요합니다.");
            }
            approvedBy = memberRepository.findById(approvedByMemberId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "승인자를 찾을 수 없습니다."));
        }

        purchaseOrder.updateStatus(status, approvedBy);

        return PurchaseOrderResponseDto.from(
                purchaseOrder,
                purchaseOrderItemRepository.findByPurchaseOrder(purchaseOrder)
        );
    }

    private PurchaseOrder findPurchaseOrder(Long purchaseOrderId) {
        if (purchaseOrderId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "발주서 ID가 필요합니다.");
        }
        return purchaseOrderRepository.findById(purchaseOrderId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "발주서를 찾을 수 없습니다."));
    }

    private void validateCreateRequest(PurchaseOrderCreateDto requestDto) {
        if (requestDto == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "발주 정보가 필요합니다.");
        }
        if (requestDto.getVendorId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "거래처를 선택해야 합니다.");
        }
        if (requestDto.getItems() == null || requestDto.getItems().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "발주 품목이 필요합니다.");
        }
    }

    private Member findRequestedBy(Long requestedByMemberId) {
        if (requestedByMemberId != null) {
            return memberRepository.findById(requestedByMemberId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "요청자를 찾을 수 없습니다."));
        }

        return memberRepository.findByLoginId("inventory")
                .or(() -> memberRepository.findByLoginId("admin"))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "기본 요청자를 찾을 수 없습니다."));
    }

    private void validateItemRequest(PurchaseOrderItemCreateDto itemDto) {
        if (itemDto == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "발주 품목 정보가 필요합니다.");
        }
        if (itemDto.getItemId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "품목을 선택해야 합니다.");
        }
        if (itemDto.getQuantity() == null || itemDto.getQuantity() <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "발주 수량은 1 이상이어야 합니다.");
        }
        if (itemDto.getUnitPrice() == null || itemDto.getUnitPrice().compareTo(BigDecimal.ZERO) < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "단가는 0 이상이어야 합니다.");
        }
    }

    private String validateStatusRequest(PurchaseOrderStatusUpdateDto requestDto) {
        if (requestDto == null || requestDto.getStatus() == null || requestDto.getStatus().trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "상태값이 필요합니다.");
        }

        String status = requestDto.getStatus().trim().toUpperCase();
        if (!STATUS_REQUESTED.equals(status)
                && !STATUS_APPROVED.equals(status)
                && !STATUS_ORDERED.equals(status)
                && !STATUS_RECEIVED.equals(status)
                && !STATUS_CANCELED.equals(status)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "발주서 상태가 올바르지 않습니다.");
        }

        return status;
    }

    private String generatePurchaseOrderNo() {
        return "PO-" + LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE) + "-" + System.currentTimeMillis();
    }

    private record PurchaseOrderLine(
            Item item,
            int quantity,
            BigDecimal unitPrice,
            BigDecimal supplyAmount,
            BigDecimal taxAmount,
            BigDecimal lineAmount,
            LocalDate expectedDate,
            String note
    ) {
    }
}
