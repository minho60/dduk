package com.dduk.domain.inventory.stock;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface StockMovementRepository extends JpaRepository<StockMovement, Long> {
    List<StockMovement> findByItemId(Long itemId);
    List<StockMovement> findByWarehouseId(Long warehouseId);
    boolean existsByReferenceTypeAndReferenceIdAndMovementType(String referenceType, String referenceId, MovementType movementType);
    
    java.util.Optional<StockMovement> findTopByReferenceNoStartingWithOrderByIdDesc(String prefix);

    @org.springframework.data.jpa.repository.Query("SELECT SUM(m.quantity) FROM StockMovement m WHERE m.movementType = 'OUTBOUND' AND m.createdAt >= :since")
    Long getOutboundVolumeSince(java.time.LocalDateTime since);
}
