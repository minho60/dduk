package com.dduk.domain.inventory.stock.api.dto;

import com.dduk.domain.inventory.stock.MovementReason;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class OutboundRequest {
    private Long itemId;
    private Long warehouseId;
    private int quantity;
    private MovementReason reason;
    private String referenceType;
    private String referenceId;
}
