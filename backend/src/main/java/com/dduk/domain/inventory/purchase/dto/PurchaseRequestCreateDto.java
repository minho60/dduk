package com.dduk.domain.inventory.purchase.dto;

import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
public class PurchaseRequestCreateDto {

    private Long itemId;
    private Long vendorId;
    private Integer quantity;
    private BigDecimal unitPrice;
    private LocalDate expectedDate;
    private String note;
}
