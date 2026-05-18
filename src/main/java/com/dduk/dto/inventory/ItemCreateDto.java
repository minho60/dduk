package com.dduk.dto.inventory;

import lombok.Getter;

import java.math.BigDecimal;

@Getter
public class ItemCreateDto {

    private String name;
    private String itemType;
    private String category;
    private String spec;
    private String barcode;
    private String unit;
    private BigDecimal standardCost;
    private BigDecimal unitPrice;
    private Integer safetyStock;
    private Boolean active;
    private Long vendorId;
}
