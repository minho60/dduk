package com.dduk.domain.inventory.purchase;

public enum PurchaseStatus {
    DRAFT,          // 초안
    APPROVED,       // 승인
    ORDERED,        // 발주완료
    RECEIVED,       // 입고완료 (회계 반영 시점)
    COMPLETED,      // 종결
    CANCELLED       // 취소
}
