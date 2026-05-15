package com.dduk.domain.inventory.purchase.dto;

import com.dduk.domain.inventory.purchase.PurchaseOrder;
import com.dduk.domain.inventory.purchase.PurchaseOrderItem;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Getter
@Builder
public class PurchaseOrderResponseDto {

    private Long purchaseOrderId;
    private String purchaseOrderNo;
    private String status;
    private Long vendorId;
    private String vendorName;
    private Long requestedByMemberId;
    private String requestedByMemberName;
    private LocalDate orderDate;
    private LocalDate expectedDate;
    private BigDecimal totalAmount;
    private String note;
    private List<PurchaseOrderItemResponseDto> items;

    public static PurchaseOrderResponseDto from(PurchaseOrder purchaseOrder, List<PurchaseOrderItem> purchaseOrderItems) {
        return PurchaseOrderResponseDto.builder()
                .purchaseOrderId(purchaseOrder.getId())
                .purchaseOrderNo(purchaseOrder.getPurchaseOrderNo())
                .status(purchaseOrder.getStatus().name())
                .vendorId(purchaseOrder.getVendor().getId())
                .vendorName(purchaseOrder.getVendor().getName())
                .requestedByMemberId(purchaseOrder.getRequestedBy() != null ? purchaseOrder.getRequestedBy().getId() : null)
                .requestedByMemberName(purchaseOrder.getRequestedBy() != null ? purchaseOrder.getRequestedBy().getName() : null)
                .orderDate(purchaseOrder.getOrderDate())
                .expectedDate(purchaseOrder.getExpectedDate())
                .totalAmount(purchaseOrder.getTotalAmount())
                .note(purchaseOrder.getNote())
                .items(purchaseOrderItems.stream()
                        .map(PurchaseOrderItemResponseDto::from)
                        .toList())
                .build();
    }
}
