package com.dduk.domain.inventory.stock.api;

import com.dduk.domain.inventory.item.Inventory;
import com.dduk.domain.inventory.stock.InventoryQueryService;
import com.dduk.domain.inventory.stock.InventoryService;
import com.dduk.domain.inventory.stock.MovementType;
import com.dduk.domain.inventory.stock.StockMovement;
import com.dduk.domain.inventory.stock.api.dto.TransferRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.dduk.domain.inventory.stock.InventoryValidationService;
import com.dduk.domain.inventory.stock.InventoryRebuildService;
import com.dduk.domain.inventory.stock.api.dto.ValidationResultDto;

@RestController
@RequestMapping("/api/v1/inventories")
@RequiredArgsConstructor
public class InventoryController {

    private final InventoryService inventoryService;
    private final InventoryQueryService inventoryQueryService;
    private final InventoryValidationService inventoryValidationService;
    private final InventoryRebuildService inventoryRebuildService;

    @GetMapping
    public ResponseEntity<Map<String, Object>> getInventories(
            @RequestParam(required = false) Long warehouseId,
            @RequestParam(required = false) Long itemId,
            @RequestParam(required = false) Boolean lowStockOnly) {
        
        List<Inventory> data = inventoryQueryService.getInventories(warehouseId, itemId, lowStockOnly);
        return buildSuccessResponse(data);
    }

    @GetMapping("/stock-movements")
    public ResponseEntity<Map<String, Object>> getStockMovements(
            @RequestParam(required = false) Long warehouseId,
            @RequestParam(required = false) Long itemId,
            @RequestParam(required = false) MovementType movementType) {
        
        List<StockMovement> data = inventoryQueryService.getStockMovements(warehouseId, itemId, movementType);
        return buildSuccessResponse(data);
    }

    @PostMapping("/transfer")
    public ResponseEntity<Map<String, Object>> transferStock(@RequestBody TransferRequest request) {
        inventoryService.transferStock(
                request.getItemId(),
                request.getFromWarehouseId(),
                request.getToWarehouseId(),
                request.getQuantity(),
                request.getReferenceType(),
                request.getReferenceId()
        );
        return buildSuccessResponse("Transfer completed successfully");
    }

    @GetMapping("/reorder-recommendations")
    public ResponseEntity<Map<String, Object>> getReorderRecommendations() {
        List<Inventory> data = inventoryQueryService.getReorderRecommendations();
        return buildSuccessResponse(data);
    }

    @GetMapping("/validate")
    public ResponseEntity<Map<String, Object>> validateInventories(
            @RequestParam(required = false) Long warehouseId,
            @RequestParam(required = false) Long itemId) {
        List<ValidationResultDto> data = inventoryValidationService.validateInventories(warehouseId, itemId);
        return buildSuccessResponse(data);
    }

    @PostMapping("/rebuild")
    public ResponseEntity<Map<String, Object>> rebuildInventories(
            @RequestParam(required = false) Long warehouseId,
            @RequestParam(required = false) Long itemId) {
        inventoryRebuildService.rebuildInventories(warehouseId, itemId);
        return buildSuccessResponse("Rebuild completed successfully");
    }

    private ResponseEntity<Map<String, Object>> buildSuccessResponse(Object data) {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        response.put("data", data);
        response.put("message", "요청이 완료되었습니다.");
        return ResponseEntity.ok(response);
    }
}
