package com.dduk.domain.inventory.purchase.dto;

import lombok.Getter;

import java.time.LocalDate;
import java.util.List;

@Getter
public class PurchaseOrderCreateDto {

    private Long vendorId;
    private LocalDate expectedDate;
    private String note;
    private List<PurchaseOrderItemCreateDto> items;
}
