package com.dduk.service.inventory;

import com.dduk.dto.inventory.InventoryDashboardResponseDto;
import com.dduk.dto.inventory.RecentMovementDto;
import com.dduk.dto.inventory.WarehouseDistributionDto;
import com.dduk.entity.admin.TaskHistory;
import com.dduk.entity.admin.TaskHistoryStatus;
import com.dduk.entity.admin.TaskHistoryType;
import com.dduk.entity.inventory.Inventory;
import com.dduk.entity.inventory.MovementType;
import com.dduk.entity.inventory.StockMovement;
import com.dduk.repository.admin.TaskHistoryRepository;
import com.dduk.repository.inventory.InventoryRepository;
import com.dduk.repository.inventory.StockMovementRepository;
import com.dduk.repository.inventory.WarehouseTransferRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class InventoryQueryService {

    private static final String INVENTORY_SHORTAGE_ACTION = "check_inventory_shortage";
    private static final String DEFAULT_VENDOR_NAME = "아망티";

    private final InventoryRepository inventoryRepository;
    private final StockMovementRepository stockMovementRepository;
    private final WarehouseTransferRepository warehouseTransferRepository;
    private final TaskHistoryRepository taskHistoryRepository;
    private final ObjectMapper objectMapper;

    public List<Inventory> getInventories(Long warehouseId, Long itemId, Boolean lowStockOnly) {
        List<Inventory> results;
        if (warehouseId != null && itemId != null) {
            results = inventoryRepository.findByWarehouseIdAndItemIdWithFetch(warehouseId, itemId);
        } else if (warehouseId != null) {
            results = inventoryRepository.findByWarehouseIdWithFetch(warehouseId);
        } else if (itemId != null) {
            results = inventoryRepository.findByItemIdWithFetch(itemId);
        } else {
            results = inventoryRepository.findAllWithItemAndWarehouse();
        }

        if (Boolean.TRUE.equals(lowStockOnly)) {
            return results.stream()
                    .filter(i -> i.getCurrentStock() <= i.getSafetyStock())
                    .collect(Collectors.toList());
        }
        return results;
    }

    public List<Inventory> getReorderRecommendations() {
        return inventoryRepository.findItemsNeedingReorderWithFetch();
    }

    public InventoryDashboardResponseDto getDashboardStats() {
        Long totalQty = inventoryRepository.getTotalStockQuantity();
        BigDecimal totalVal = inventoryRepository.getTotalInventoryValue();
        Long lowStock = inventoryRepository.countLowStockItems();
        Long outboundVol = stockMovementRepository.getOutboundVolumeSince(LocalDateTime.now().minusDays(30));
        Long pendingTransfers = warehouseTransferRepository.countPendingTransfers();

        List<Object[]> distributionRaw = inventoryRepository.getStockDistributionByWarehouse();
        List<WarehouseDistributionDto> distribution = distributionRaw.stream()
                .map(row -> WarehouseDistributionDto.builder()
                        .warehouseName((String) row[0])
                        .totalStock(row[1] != null ? ((Number) row[1]).longValue() : 0L)
                        .totalValue(row[2] instanceof BigDecimal ? (BigDecimal) row[2] : (row[2] != null ? BigDecimal.valueOf(((Number) row[2]).doubleValue()) : BigDecimal.ZERO))
                        .build())
                .collect(Collectors.toList());

        List<StockMovement> movements = stockMovementRepository.findAllWithFetch();
        LocalDateTime thirtyDaysAgo = LocalDateTime.now().minusDays(30);
        List<RecentMovementDto> recentMovements = movements.stream()
                .filter(m -> m.getCreatedAt() != null && m.getCreatedAt().isAfter(thirtyDaysAgo))
                .map(m -> RecentMovementDto.builder()
                        .id(m.getId())
                        .createdAt(m.getCreatedAt())
                        .referenceNo(m.getReferenceNo())
                        .movementType(m.getMovementType())
                        .itemName(m.getItem().getName())
                        .quantity(m.getQuantity())
                        .warehouseName(m.getWarehouse().getWarehouseName())
                        .build())
                .collect(Collectors.toList());

        return InventoryDashboardResponseDto.builder()
                .totalQuantity(totalQty != null ? totalQty : 0L)
                .totalValue(totalVal != null ? totalVal : BigDecimal.ZERO)
                .lowStockCount(lowStock != null ? lowStock : 0L)
                .outboundVolume30Days(outboundVol != null ? outboundVol : 0L)
                .pendingTransferCount(pendingTransfers != null ? pendingTransfers : 0L)
                .warehouseDistribution(distribution)
                .recentMovements(recentMovements)
                .inventoryShortageRpa(buildInventoryShortageRpa())
                .build();
    }

    public List<StockMovement> getStockMovements(Long warehouseId, Long itemId, MovementType movementType) {
        if (warehouseId != null && itemId != null && movementType != null) {
            return stockMovementRepository.findByWarehouseIdAndItemIdWithFetch(warehouseId, itemId)
                    .stream().filter(m -> m.getMovementType() == movementType)
                    .collect(Collectors.toList());
        } else if (warehouseId != null && movementType != null) {
            return stockMovementRepository.findByWarehouseIdAndMovementTypeWithFetch(warehouseId, movementType);
        } else if (itemId != null && movementType != null) {
            return stockMovementRepository.findByItemIdAndMovementTypeWithFetch(itemId, movementType);
        } else if (warehouseId != null && itemId != null) {
            return stockMovementRepository.findByWarehouseIdAndItemIdWithFetch(warehouseId, itemId);
        } else if (warehouseId != null) {
            return stockMovementRepository.findByWarehouseIdWithFetch(warehouseId);
        } else if (itemId != null) {
            return stockMovementRepository.findByItemIdWithFetch(itemId);
        } else if (movementType != null) {
            return stockMovementRepository.findByMovementTypeWithFetch(movementType);
        } else {
            return stockMovementRepository.findAllWithFetch();
        }
    }

    private Map<String, Object> buildInventoryShortageRpa() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("taskType", "INVENTORY_SHORTAGE");
        result.put("actionName", INVENTORY_SHORTAGE_ACTION);
        result.put("available", false);
        result.put("status", "EMPTY");
        result.put("vendorName", DEFAULT_VENDOR_NAME);
        result.put("message", "아직 완료된 재고 부족 조회 결과가 없어.");
        result.put("latestTaskId", null);
        result.put("latestCollectedAt", null);
        result.put("rpaAlertCount", 0);
        result.put("erpLowStockCount", inventoryRepository.countLowStockItems());
        result.put("matchedLowStockCount", 0);
        result.put("items", List.of());

        Optional<TaskHistory> latestTask = taskHistoryRepository
                .findFirstByTaskTypeAndActionNameAndStatusOrderByCompletedAtDescIdDesc(
                        TaskHistoryType.RPA,
                        INVENTORY_SHORTAGE_ACTION,
                        TaskHistoryStatus.SUCCESS
                );

        List<Inventory> lowStockItems = inventoryRepository.findItemsNeedingReorderWithFetch();
        if (latestTask.isEmpty()) {
            result.put("items", summarizeErpOnly(lowStockItems));
            return result;
        }

        TaskHistory taskHistory = latestTask.get();
        result.put("latestTaskId", taskHistory.getTaskId());
        result.put("latestCollectedAt", taskHistory.getCompletedAt() == null ? null : taskHistory.getCompletedAt().toString());

        String sourceFilePath = readString(parseMap(taskHistory.getResponsePayload()), "data", "filePath");
        if (sourceFilePath == null || sourceFilePath.isBlank()) {
            result.put("status", "MISSING_FILE");
            result.put("message", "최근 재고 부족 조회 이력은 있지만 결과 파일 경로를 찾지 못했어.");
            result.put("items", summarizeErpOnly(lowStockItems));
            return result;
        }

        Path resolvedPath = resolveProjectPath(sourceFilePath);
        if (resolvedPath == null || !Files.exists(resolvedPath)) {
            result.put("status", "MISSING_FILE");
            result.put("message", "최근 재고 부족 조회 이력은 있지만 결과 파일이 없어.");
            result.put("items", summarizeErpOnly(lowStockItems));
            return result;
        }

        List<Map<String, Object>> alerts = readAlertItems(resolvedPath);
        result.put("rpaAlertCount", alerts.size());
        result.put("vendorName", firstVendor(alerts).orElse(DEFAULT_VENDOR_NAME));

        if (alerts.isEmpty()) {
            result.put("status", "EMPTY_RESULT");
            result.put("message", "최근 재고 부족 조회 파일은 있지만 비교할 경고 항목이 비어 있어.");
            result.put("items", summarizeErpOnly(lowStockItems));
            return result;
        }

        List<Map<String, Object>> mergedItems = mergeLowStockItems(lowStockItems, alerts);
        long matchedCount = mergedItems.stream()
                .filter(item -> "MATCHED".equals(item.get("matchType")))
                .count();

        result.put("available", true);
        result.put("status", matchedCount > 0 ? "READY" : "ERP_ONLY");
        result.put("matchedLowStockCount", matchedCount);
        result.put(
                "message",
                matchedCount > 0
                        ? "ERP 부족 재고와 최근 조회 경고를 함께 볼 수 있어."
                        : "ERP 부족 재고는 있지만, 최근 조회 결과와 직접 맞는 품목은 아직 없어."
        );
        result.put("items", mergedItems);
        return result;
    }

    private List<Map<String, Object>> mergeLowStockItems(List<Inventory> lowStockItems, List<Map<String, Object>> alerts) {
        Map<String, Map<String, Object>> alertIndex = alerts.stream()
                .collect(Collectors.toMap(
                        alert -> normalizeName(String.valueOf(alert.get("productName"))),
                        alert -> alert,
                        (left, right) -> left,
                        LinkedHashMap::new
                ));

        List<Map<String, Object>> rows = lowStockItems.stream()
                .limit(5)
                .map(inventory -> {
                    String itemName = inventory.getItem().getName();
                    Map<String, Object> alert = alertIndex.get(normalizeName(itemName));
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("itemName", itemName);
                    row.put("warehouseName", inventory.getWarehouse().getWarehouseName());
                    row.put("availableStock", inventory.getAvailableStock());
                    row.put("safetyStock", inventory.getSafetyStock());
                    row.put("shortageGap", inventory.getSafetyStock() - inventory.getAvailableStock());
                    row.put("rpaStockStatus", alert == null ? "미확인" : alert.get("stockStatus"));
                    row.put("expectedRestockDate", alert == null ? null : alert.get("expectedRestockDate"));
                    row.put("recommendedAction", alert == null ? "ERP 기준으로 수동 확인" : alert.get("recommendedAction"));
                    row.put("matchType", alert == null ? "ERP_ONLY" : "MATCHED");
                    return row;
                })
                .collect(Collectors.toList());

        if (!rows.isEmpty()) {
            return rows;
        }

        return alerts.stream()
                .limit(5)
                .map(alert -> {
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("itemName", alert.get("productName"));
                    row.put("warehouseName", "-");
                    row.put("availableStock", null);
                    row.put("safetyStock", null);
                    row.put("shortageGap", null);
                    row.put("rpaStockStatus", alert.get("stockStatus"));
                    row.put("expectedRestockDate", alert.get("expectedRestockDate"));
                    row.put("recommendedAction", alert.get("recommendedAction"));
                    row.put("matchType", "RPA_ONLY");
                    return row;
                })
                .collect(Collectors.toList());
    }

    private List<Map<String, Object>> summarizeErpOnly(List<Inventory> lowStockItems) {
        return lowStockItems.stream()
                .limit(5)
                .map(inventory -> {
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("itemName", inventory.getItem().getName());
                    row.put("warehouseName", inventory.getWarehouse().getWarehouseName());
                    row.put("availableStock", inventory.getAvailableStock());
                    row.put("safetyStock", inventory.getSafetyStock());
                    row.put("shortageGap", inventory.getSafetyStock() - inventory.getAvailableStock());
                    row.put("rpaStockStatus", "미확인");
                    row.put("expectedRestockDate", null);
                    row.put("recommendedAction", "ERP 기준으로 수동 확인");
                    row.put("matchType", "ERP_ONLY");
                    return row;
                })
                .collect(Collectors.toList());
    }

    private Map<String, Object> parseMap(String json) {
        if (json == null || json.isBlank()) {
            return Map.of();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<Map<String, Object>>() {});
        } catch (IOException exception) {
            return Map.of();
        }
    }

    private String readString(Map<String, Object> root, String parentKey, String childKey) {
        Object parent = root.get(parentKey);
        if (parent instanceof Map<?, ?> nested) {
            Object value = nested.get(childKey);
            return value == null ? null : String.valueOf(value);
        }
        return null;
    }

    private List<Map<String, Object>> readAlertItems(Path filePath) {
        try {
            return objectMapper.readValue(filePath.toFile(), new TypeReference<List<Map<String, Object>>>() {});
        } catch (IOException exception) {
            return List.of();
        }
    }

    private Optional<String> firstVendor(List<Map<String, Object>> alerts) {
        return alerts.stream()
                .map(item -> item.get("vendorName"))
                .filter(value -> value != null && !String.valueOf(value).isBlank())
                .map(String::valueOf)
                .findFirst();
    }

    private Path resolveProjectPath(String filePath) {
        Path rawPath = Paths.get(filePath);
        Path workingDir = Paths.get(System.getProperty("user.dir"));
        Path projectRoot = "backend".equalsIgnoreCase(workingDir.getFileName().toString())
                ? workingDir.getParent()
                : workingDir;
        Path outputRoot = projectRoot.resolve("rpa").resolve("outputs").normalize();
        Path resolved = rawPath.isAbsolute()
                ? rawPath.normalize()
                : projectRoot.resolve(filePath).normalize();

        if (!resolved.startsWith(outputRoot)) {
            return null;
        }
        return resolved;
    }

    private String normalizeName(String value) {
        if (value == null) {
            return "";
        }
        return value.replaceAll("[^\\p{IsAlphabetic}\\p{IsDigit}]", "").toUpperCase();
    }
}
