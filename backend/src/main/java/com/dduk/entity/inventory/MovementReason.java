package com.dduk.entity.inventory;

public enum MovementReason {
    PURCHASE_RECEIVED,
    SALES_SHIPPED,
    MANUAL_ADJUST,
    TRANSFER,
    REBUILD_ADJUSTMENT,
    RETURNED_FROM_CUSTOMER,
    RETURNED_TO_VENDOR,
    PRODUCTION_CONSUMED
}
