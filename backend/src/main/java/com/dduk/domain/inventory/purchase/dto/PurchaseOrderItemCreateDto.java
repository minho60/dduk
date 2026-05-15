package com.dduk.domain.inventory.purchase.dto;

import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
public class PurchaseOrderItemCreateDto {

    private Long itemId;
    private Integer quantity;
    private BigDecimal unitPrice;
    private LocalDate expectedDate;
    private String note;
}
