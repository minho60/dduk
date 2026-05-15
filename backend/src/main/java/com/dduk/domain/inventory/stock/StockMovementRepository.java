package com.dduk.domain.inventory.stock;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface StockMovementRepository extends JpaRepository<StockMovement, Long> {
    List<StockMovement> findByItemId(Long itemId);
    List<StockMovement> findByWarehouseId(Long warehouseId);
    boolean existsByReferenceTypeAndReferenceIdAndMovementType(String referenceType, String referenceId, MovementType movementType);
    
    Optional<StockMovement> findTopByReferenceNoStartingWithOrderByIdDesc(String prefix);

    @Query("SELECT SUM(m.quantity) FROM StockMovement m WHERE m.movementType = 'OUTBOUND' AND m.createdAt >= :since")
    Long getOutboundVolumeSince(LocalDateTime since);
}
