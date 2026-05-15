package com.dduk.domain.inventory.stock.api.dto;

import com.dduk.domain.inventory.stock.MovementReason;
import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;

@Getter
@Setter
public class InboundRequest {
    private Long itemId;
    private Long warehouseId;
    private int quantity;
    private BigDecimal unitCost;
    private MovementReason reason;
    private String referenceType;
    private String referenceId;
}
