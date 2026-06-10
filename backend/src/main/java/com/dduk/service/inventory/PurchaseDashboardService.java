package com.dduk.service.inventory;

import com.dduk.entity.admin.TaskHistory;
import com.dduk.entity.admin.TaskHistoryStatus;
import com.dduk.entity.admin.TaskHistoryType;
import com.dduk.repository.admin.TaskHistoryRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PurchaseDashboardService {

    private static final String PURCHASE_PRICE_ACTION = "collect_purchase_orders";
    private static final String DEFAULT_VENDOR_NAME = "아망티";

    @PersistenceContext
    private EntityManager entityManager;

    private final TaskHistoryRepository taskHistoryRepository;
    private final ObjectMapper objectMapper;

    public Map<String, Object> getStats() {
        long orderCount = count("""
                SELECT COUNT(*)
                FROM purchase_orders
                """);
        long completedOrderCount = count("""
                SELECT COUNT(*)
                FROM purchase_orders
                WHERE UPPER(status) = 'COMPLETED'
                """);
        long receivingCount = count("""
                SELECT COUNT(*)
                FROM purchase_orders
                WHERE UPPER(status) = 'RECEIVING'
                """);
        long delayedReceivingCount = count("""
                SELECT COUNT(*)
                FROM purchase_orders
                WHERE UPPER(status) = 'INBOUND_DELAY'
                   OR (
                       expected_date IS NOT NULL
                       AND expected_date < CURRENT_DATE
                       AND UPPER(status) NOT IN ('RECEIVING', 'RECEIVED', 'COMPLETED', 'CANCELLED')
                   )
                """);
        long receivedCount = count("""
                SELECT COUNT(*)
                FROM purchase_orders po
                WHERE UPPER(po.status) = 'RECEIVED'
                """);

        Map<String, Object> stats = new HashMap<>();
        stats.put("orderCount", orderCount);
        stats.put("completedOrderCount", completedOrderCount);
        stats.put("receivingCount", receivingCount);
        stats.put("delayedReceivingCount", delayedReceivingCount);
        stats.put("receivedCount", receivedCount);
        stats.put("orderCompletionRate", percentage(completedOrderCount, orderCount));
        stats.put("receivingCompletionRate", percentage(receivedCount, orderCount));
        stats.put("delayRate", percentage(delayedReceivingCount, orderCount));
        stats.put("accountsReceivableAmount", accountBalance("ASSET", "미수", "외상매출"));
        stats.put("accountsPayableAmount", accountBalance("LIABILITY", "외상", "미지급"));
        stats.put("statusCounts", statusCounts());
        stats.put("monthlyOrders", monthlyOrders());
        stats.put("recentOrders", recentOrders());
        stats.put("rpaComparison", purchasePriceComparison());
        stats.put("baseDate", LocalDate.now().toString());
        return stats;
    }

    private Map<String, Object> purchasePriceComparison() {
        long activeTaskCount = countActivePurchasePriceTasks();

        Map<String, Object> comparison = new LinkedHashMap<>();
        comparison.put("taskType", "PURCHASE_PRICE");
        comparison.put("actionName", PURCHASE_PRICE_ACTION);
        comparison.put("collectionMode", "QUEUED_SNAPSHOT");
        comparison.put("available", false);
        comparison.put("status", "IDLE");
        comparison.put("resultStatus", "EMPTY");
        comparison.put("vendorName", DEFAULT_VENDOR_NAME);
        comparison.put("message", "아직 저장된 구매 가격 수집 결과가 없어.");
        comparison.put("latestTaskId", null);
        comparison.put("latestTaskStatus", null);
        comparison.put("latestTaskRequestedAt", null);
        comparison.put("latestTaskCompletedAt", null);
        comparison.put("latestTaskErrorMessage", null);
        comparison.put("latestTask", null);
        comparison.put("latestSuccessfulTaskId", null);
        comparison.put("latestCollectedAt", null);
        comparison.put("latestCollectedAveragePrice", null);
        comparison.put("erpBaselineAveragePrice", null);
        comparison.put("erpItemsCount", 0);
        comparison.put("priceDelta", null);
        comparison.put("collectedItemsCount", 0);
        comparison.put("comparedItemsCount", 0);
        comparison.put("activeTaskCount", activeTaskCount);
        comparison.put("canRequestCollection", activeTaskCount == 0);
        comparison.put("sourceFilePath", null);
        comparison.put("items", List.of());
        comparison.put("collectedSnapshotItems", List.of());
        comparison.put("matchedItems", List.of());

        Optional<TaskHistory> latestTask = taskHistoryRepository.findFirstByTaskTypeAndActionNameOrderByRequestedAtDescIdDesc(
                TaskHistoryType.RPA,
                PURCHASE_PRICE_ACTION
        );
        latestTask.ifPresent(task -> {
            comparison.put("status", task.getStatus().name());
            comparison.put("latestTaskId", task.getTaskId());
            comparison.put("latestTaskStatus", task.getStatus().name());
            comparison.put("latestTaskRequestedAt", formatDateTime(task.getRequestedAt()));
            comparison.put("latestTaskCompletedAt", formatDateTime(task.getCompletedAt()));
            comparison.put("latestTaskErrorMessage", task.getErrorMessage());
            comparison.put("latestTask", summarizeTask(task));
        });

        Optional<TaskHistory> latestSuccess = taskHistoryRepository
                .findFirstByTaskTypeAndActionNameAndStatusOrderByCompletedAtDescIdDesc(
                        TaskHistoryType.RPA,
                        PURCHASE_PRICE_ACTION,
                        TaskHistoryStatus.SUCCESS
                );

        if (latestSuccess.isEmpty()) {
            applyStatusMessageWithoutSnapshot(comparison, latestTask.orElse(null));
            return comparison;
        }

        TaskHistory successfulTask = latestSuccess.get();
        comparison.put("latestSuccessfulTaskId", successfulTask.getTaskId());
        comparison.put("latestCollectedAt", formatDateTime(successfulTask.getCompletedAt()));

        Map<String, Object> responsePayload = parseMap(successfulTask.getResponsePayload());
        String sourceFilePath = readString(responsePayload, "data", "filePath");
        comparison.put("sourceFilePath", sourceFilePath);

        if (sourceFilePath == null || sourceFilePath.isBlank()) {
            comparison.put("resultStatus", "MISSING_FILE");
            comparison.put("message", buildSnapshotMessage(latestTask.orElse(successfulTask), "최근 성공 이력은 있지만 결과 파일 경로를 찾지 못했어."));
            return comparison;
        }

        Path resolvedPath = resolveProjectPath(sourceFilePath);
        if (resolvedPath == null || !Files.exists(resolvedPath)) {
            comparison.put("resultStatus", "MISSING_FILE");
            comparison.put("message", buildSnapshotMessage(latestTask.orElse(successfulTask), "최근 성공 이력은 있지만 결과 파일이 현재 서버에 없어."));
            return comparison;
        }

        List<Map<String, Object>> collectedItems = readCollectedItems(resolvedPath);
        if (collectedItems.isEmpty()) {
            comparison.put("resultStatus", "EMPTY_RESULT");
            comparison.put("message", buildSnapshotMessage(latestTask.orElse(successfulTask), "최근 수집 파일은 있지만 비교할 구매 가격 데이터가 비어 있어."));
            return comparison;
        }

        String vendorName = firstNonBlankVendor(collectedItems).orElse(DEFAULT_VENDOR_NAME);
        comparison.put("vendorName", vendorName);
        comparison.put("collectedItemsCount", collectedItems.size());
        comparison.put("collectedSnapshotItems", buildCollectedSnapshotRows(collectedItems));

        BigDecimal collectedAveragePrice = averagePrice(collectedItems, "unitPrice");
        comparison.put("latestCollectedAveragePrice", collectedAveragePrice);

        List<Map<String, Object>> erpItems = loadErpItemsForVendor(vendorName);
        comparison.put("erpItemsCount", erpItems.size());
        if (erpItems.isEmpty()) {
            comparison.put("resultStatus", "NO_ERP_BASELINE");
            comparison.put("available", true);
            comparison.put("message", buildSnapshotMessage(latestTask.orElse(successfulTask), "최근 수집 결과는 있지만 ERP 기준 단가가 아직 없어."));
            comparison.put("items", buildCollectedSnapshotRows(collectedItems));
            return comparison;
        }

        BigDecimal erpAveragePrice = averagePrice(erpItems, "erpPrice");
        comparison.put("erpBaselineAveragePrice", erpAveragePrice);

        List<Map<String, Object>> comparedItems = buildComparedItems(collectedItems, erpItems, false);
        if (comparedItems.isEmpty()) {
            comparedItems = buildComparedItems(collectedItems, erpItems, true);
        }
        comparison.put("matchedItems", comparedItems);
        comparison.put("items", buildCollectedSnapshotWithMatches(collectedItems, erpItems));
        comparison.put("comparedItemsCount", comparedItems.size());

        if (collectedAveragePrice != null && erpAveragePrice != null) {
            comparison.put("priceDelta", collectedAveragePrice.subtract(erpAveragePrice).setScale(0, RoundingMode.HALF_UP));
        }

        comparison.put("available", true);
        comparison.put("resultStatus", "READY");
        comparison.put("message", buildReadyMessage(latestTask.orElse(successfulTask), comparedItems.isEmpty(), successfulTask.getCompletedAt()));
        return comparison;
    }

    private long countActivePurchasePriceTasks() {
        return taskHistoryRepository.countByTaskTypeAndActionNameAndStatusIn(
                TaskHistoryType.RPA,
                PURCHASE_PRICE_ACTION,
                Set.of(TaskHistoryStatus.REQUESTED, TaskHistoryStatus.RUNNING)
        );
    }

    private void applyStatusMessageWithoutSnapshot(Map<String, Object> comparison, TaskHistory latestTask) {
        if (latestTask == null) {
            comparison.put("message", "아직 구매 단가 수집을 한 번도 요청하지 않았어.");
            return;
        }

        switch (latestTask.getStatus()) {
            case REQUESTED -> comparison.put("message", "구매 단가 수집 요청이 접수됐어. 완료되면 최근 비교 결과가 여기에 반영돼.");
            case RUNNING -> comparison.put("message", "아망티 가격 수집이 진행 중이야. 새 결과가 저장되면 최근 비교 결과가 갱신돼.");
            case FAILED -> comparison.put(
                    "message",
                    latestTask.getErrorMessage() == null || latestTask.getErrorMessage().isBlank()
                            ? "최근 구매 단가 수집이 실패했고 아직 보여줄 성공 스냅샷이 없어."
                            : "최근 구매 단가 수집이 실패했어. " + latestTask.getErrorMessage()
            );
            case SUCCESS -> comparison.put("message", "최근 성공 이력은 있지만 아직 보여줄 비교 결과를 정리하지 못했어.");
            default -> comparison.put("message", "구매 단가 수집 상태를 다시 확인해줘.");
        }
    }

    private String buildSnapshotMessage(TaskHistory latestTask, String fallback) {
        if (latestTask == null) {
            return fallback;
        }

        return switch (latestTask.getStatus()) {
            case REQUESTED -> "새 구매 단가 수집 요청이 접수됐어. 완료 전까지는 마지막 저장 결과를 기준으로 보여줄게.";
            case RUNNING -> "아망티 가격을 다시 수집 중이야. 완료 전까지는 마지막 저장 결과를 기준으로 보여줄게.";
            case FAILED -> latestTask.getErrorMessage() == null || latestTask.getErrorMessage().isBlank()
                    ? "최근 수집은 실패했고 마지막 저장 결과만 유지하고 있어."
                    : "최근 수집은 실패해서 마지막 저장 결과를 유지하고 있어. " + latestTask.getErrorMessage();
            default -> fallback;
        };
    }

    private String buildReadyMessage(TaskHistory latestTask, boolean noDelta, LocalDateTime collectedAt) {
        String snapshotTime = formatRelativeSnapshot(collectedAt);
        String base = noDelta
                ? snapshotTime + " 저장된 단가 스냅샷 기준으로는 ERP와 다른 품목이 없었어."
                : snapshotTime + " 저장된 아망티 단가 스냅샷과 ERP 기준 단가를 비교해두었어.";
        return buildSnapshotMessage(latestTask, base);
    }

    private String formatRelativeSnapshot(LocalDateTime collectedAt) {
        if (collectedAt == null) {
            return "최근";
        }
        return collectedAt.toLocalDate().equals(LocalDate.now()) ? "오늘" : collectedAt.toLocalDate() + " 기준";
    }

    private String formatDateTime(LocalDateTime value) {
        return value == null ? null : value.toString();
    }

    private Map<String, Object> summarizeTask(TaskHistory task) {
        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("taskId", task.getTaskId());
        summary.put("status", task.getStatus().name());
        summary.put("actionName", task.getActionName());
        summary.put("requestedAt", formatDateTime(task.getRequestedAt()));
        summary.put("startedAt", formatDateTime(task.getStartedAt()));
        summary.put("completedAt", formatDateTime(task.getCompletedAt()));
        summary.put("errorMessage", task.getErrorMessage());
        return summary;
    }

    private List<Map<String, Object>> buildCollectedSnapshotRows(List<Map<String, Object>> collectedItems) {
        return collectedItems.stream()
                .limit(12)
                .map(item -> {
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("productName", item.get("productName"));
                    row.put("vendorName", item.get("vendorName"));
                    row.put("collectedPrice", toBigDecimal(item.get("unitPrice")));
                    row.put("erpPrice", null);
                    row.put("priceDelta", null);
                    row.put("matchType", "COLLECTED_ONLY");
                    return row;
                })
                .toList();
    }

    private List<Map<String, Object>> buildCollectedSnapshotWithMatches(List<Map<String, Object>> collectedItems, List<Map<String, Object>> erpItems) {
        List<Map<String, Object>> result = new ArrayList<>();
        for (Map<String, Object> collectedItem : collectedItems.stream().limit(12).toList()) {
            String productName = String.valueOf(collectedItem.get("productName"));
            String normalized = normalizeName(productName);
            Map<String, Object> erpItem = findLooseMatch(normalized, erpItems);
            BigDecimal collectedPrice = toBigDecimal(collectedItem.get("unitPrice"));
            BigDecimal erpPrice = erpItem == null ? null : toBigDecimal(erpItem.get("erpPrice"));

            Map<String, Object> row = new LinkedHashMap<>();
            row.put("productName", productName);
            row.put("vendorName", collectedItem.get("vendorName"));
            row.put("collectedPrice", collectedPrice);
            row.put("erpPrice", erpPrice);
            row.put("priceDelta", collectedPrice == null || erpPrice == null ? null : collectedPrice.subtract(erpPrice).setScale(0, RoundingMode.HALF_UP));
            row.put("matchType", erpItem == null ? "COLLECTED_ONLY" : normalized.equals(normalizeName(String.valueOf(erpItem.get("itemName")))) ? "EXACT" : "LOOSE");
            result.add(row);
        }
        return result;
    }

    private List<Map<String, Object>> summarizeCollectedOnly(List<Map<String, Object>> collectedItems) {
        return collectedItems.stream()
                .limit(3)
                .map(item -> {
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("productName", item.get("productName"));
                    row.put("vendorName", item.get("vendorName"));
                    row.put("collectedPrice", item.get("unitPrice"));
                    row.put("erpPrice", null);
                    row.put("priceDelta", null);
                    row.put("matchType", "COLLECTED_ONLY");
                    return row;
                })
                .toList();
    }

    private List<Map<String, Object>> buildComparedItems(List<Map<String, Object>> collectedItems, List<Map<String, Object>> erpItems, boolean includeSamePrice) {
        Map<String, Map<String, Object>> erpIndex = new LinkedHashMap<>();
        for (Map<String, Object> erpItem : erpItems) {
            String normalizedName = normalizeName(String.valueOf(erpItem.get("itemName")));
            if (!normalizedName.isBlank()) {
                erpIndex.putIfAbsent(normalizedName, erpItem);
            }
        }

        List<Map<String, Object>> result = new ArrayList<>();
        Set<String> usedKeys = new LinkedHashSet<>();

        for (Map<String, Object> collectedItem : collectedItems) {
            String productName = String.valueOf(collectedItem.get("productName"));
            String normalized = normalizeName(productName);
            Map<String, Object> erpItem = erpIndex.get(normalized);
            if (erpItem == null) {
                erpItem = findLooseMatch(normalized, erpItems);
            }
            if (erpItem == null || usedKeys.contains(normalized)) {
                continue;
            }

            BigDecimal collectedPrice = toBigDecimal(collectedItem.get("unitPrice"));
            BigDecimal erpPrice = toBigDecimal(erpItem.get("erpPrice"));
            if (collectedPrice == null || erpPrice == null) {
                continue;
            }
            if (!includeSamePrice && collectedPrice.compareTo(erpPrice) == 0) {
                continue;
            }

            Map<String, Object> row = new LinkedHashMap<>();
            row.put("productName", productName);
            row.put("vendorName", collectedItem.get("vendorName"));
            row.put("collectedPrice", collectedPrice);
            row.put("erpPrice", erpPrice);
            row.put("priceDelta", collectedPrice.subtract(erpPrice).setScale(0, RoundingMode.HALF_UP));
            String matchType = normalized.equals(normalizeName(String.valueOf(erpItem.get("itemName")))) ? "EXACT" : "LOOSE";
            if (includeSamePrice && collectedPrice.compareTo(erpPrice) == 0) {
                matchType = "SNAPSHOT";
            }
            row.put("matchType", matchType);
            result.add(row);
            usedKeys.add(normalized);
        }

        return result;
    }

    private Map<String, Object> findLooseMatch(String normalizedCollectedName, List<Map<String, Object>> erpItems) {
        if (normalizedCollectedName.isBlank()) {
            return null;
        }
        for (Map<String, Object> erpItem : erpItems) {
            String normalizedErpName = normalizeName(String.valueOf(erpItem.get("itemName")));
            if (normalizedErpName.contains(normalizedCollectedName) || normalizedCollectedName.contains(normalizedErpName)) {
                return erpItem;
            }
        }
        return null;
    }

    @SuppressWarnings("unchecked")
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

    @SuppressWarnings("unchecked")
    private String readString(Map<String, Object> root, String parentKey, String childKey) {
        Object parent = root.get(parentKey);
        if (parent instanceof Map<?, ?> nested) {
            Object value = nested.get(childKey);
            return value == null ? null : String.valueOf(value);
        }
        return null;
    }

    private List<Map<String, Object>> readCollectedItems(Path filePath) {
        try {
            return objectMapper.readValue(filePath.toFile(), new TypeReference<List<Map<String, Object>>>() {});
        } catch (IOException exception) {
            return List.of();
        }
    }

    private Optional<String> firstNonBlankVendor(List<Map<String, Object>> collectedItems) {
        return collectedItems.stream()
                .map(item -> item.get("vendorName"))
                .filter(value -> value != null && !String.valueOf(value).isBlank())
                .map(String::valueOf)
                .findFirst();
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> loadErpItemsForVendor(String vendorName) {
        String sql = """
                SELECT i.name,
                       v.name,
                       COALESCE(i.unit_price, i.standard_cost, AVG(inv.average_cost), 0)
                FROM items i
                JOIN vendors v ON v.id = i.default_vendor_id
                LEFT JOIN inventories inv ON inv.item_id = i.id
                WHERE i.is_active = true
                  AND UPPER(v.name) LIKE UPPER(CONCAT('%', :vendorName, '%'))
                GROUP BY i.id, i.name, v.name, i.unit_price, i.standard_cost
                ORDER BY i.name
                """;

        Query query = entityManager.createNativeQuery(sql).setParameter("vendorName", vendorName);
        return ((List<Object[]>) query.getResultList()).stream()
                .map(row -> {
                    Map<String, Object> item = new LinkedHashMap<>();
                    item.put("itemName", row[0]);
                    item.put("vendorName", row[1]);
                    item.put("erpPrice", toBigDecimal(row[2]));
                    return item;
                })
                .toList();
    }

    private BigDecimal averagePrice(List<Map<String, Object>> rows, String priceKey) {
        List<BigDecimal> prices = rows.stream()
                .map(row -> toBigDecimal(row.get(priceKey)))
                .filter(price -> price != null && price.compareTo(BigDecimal.ZERO) > 0)
                .toList();

        if (prices.isEmpty()) {
            return null;
        }

        BigDecimal total = prices.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        return total.divide(BigDecimal.valueOf(prices.size()), 0, RoundingMode.HALF_UP);
    }

    private BigDecimal toBigDecimal(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof BigDecimal decimal) {
            return decimal;
        }
        if (value instanceof Number number) {
            return BigDecimal.valueOf(number.doubleValue()).setScale(0, RoundingMode.HALF_UP);
        }
        try {
            return new BigDecimal(String.valueOf(value).replace(",", "")).setScale(0, RoundingMode.HALF_UP);
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    private String normalizeName(String value) {
        if (value == null) {
            return "";
        }
        return value.replaceAll("[^\\p{IsAlphabetic}\\p{IsDigit}]", "").toUpperCase();
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

    private long count(String sql) {
        Object value = entityManager.createNativeQuery(sql).getSingleResult();
        if (value == null) {
            return 0L;
        }
        if (value instanceof Number number) {
            return number.longValue();
        }
        return Long.parseLong(value.toString());
    }

    private double percentage(long value, long total) {
        if (total <= 0) {
            return 0;
        }
        return Math.round((value * 1000.0 / total)) / 10.0;
    }

    private BigDecimal accountBalance(String accountType, String keywordA, String keywordB) {
        String sql = """
                SELECT COALESCE(SUM(
                    CASE
                        WHEN a.normal_balance = 'DEBIT' THEN ji.debit_amount - ji.credit_amount
                        ELSE ji.credit_amount - ji.debit_amount
                    END
                ), 0)
                FROM journal_items ji
                JOIN accounts a ON a.id = ji.account_id
                JOIN journal_entries je ON je.id = ji.journal_entry_id
                WHERE a.type = :accountType
                  AND (a.name LIKE CONCAT('%', :keywordA, '%') OR a.name LIKE CONCAT('%', :keywordB, '%'))
                  AND UPPER(je.status) <> 'CANCELLED'
                """;
        Object value = entityManager.createNativeQuery(sql)
                .setParameter("accountType", accountType)
                .setParameter("keywordA", keywordA)
                .setParameter("keywordB", keywordB)
                .getSingleResult();
        if (value == null) {
            return BigDecimal.ZERO;
        }
        if (value instanceof BigDecimal decimal) {
            return decimal;
        }
        if (value instanceof Number number) {
            return BigDecimal.valueOf(number.doubleValue());
        }
        return new BigDecimal(value.toString());
    }

    @SuppressWarnings("unchecked")
    private Map<String, Long> statusCounts() {
        String sql = """
                SELECT
                    CASE
                        WHEN UPPER(status) IN ('ORDERED', 'PENDING', 'REQUESTED') THEN 'ORDERED'
                        ELSE UPPER(status)
                    END AS display_status,
                    COUNT(*)
                FROM purchase_orders
                GROUP BY display_status
                ORDER BY COUNT(*) DESC
                """;
        Map<String, Long> result = new LinkedHashMap<>();
        for (Object[] row : (List<Object[]>) entityManager.createNativeQuery(sql).getResultList()) {
            result.put(String.valueOf(row[0]), ((Number) row[1]).longValue());
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> monthlyOrders() {
        String sql = """
                SELECT DATE_FORMAT(order_date, '%Y-%m') AS ym,
                       COUNT(*) AS order_count,
                       COALESCE(SUM(total_amount), 0) AS total_amount
                FROM purchase_orders
                WHERE order_date >= DATE_SUB(CURRENT_DATE, INTERVAL 5 MONTH)
                GROUP BY DATE_FORMAT(order_date, '%Y-%m')
                ORDER BY ym
                """;
        Query query = entityManager.createNativeQuery(sql);
        return ((List<Object[]>) query.getResultList()).stream()
                .map(row -> {
                    Map<String, Object> item = new HashMap<>();
                    item.put("month", row[0]);
                    item.put("count", ((Number) row[1]).longValue());
                    item.put("amount", row[2] instanceof BigDecimal decimal ? decimal : new BigDecimal(row[2].toString()));
                    return item;
                })
                .toList();
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> recentOrders() {
        String sql = """
                SELECT po.purchase_order_no,
                       v.name,
                       po.status,
                       po.expected_date,
                       po.total_amount
                FROM purchase_orders po
                JOIN vendors v ON v.id = po.vendor_id
                ORDER BY po.created_at DESC
                LIMIT 6
                """;
        Query query = entityManager.createNativeQuery(sql);
        return ((List<Object[]>) query.getResultList()).stream()
                .map(row -> {
                    Map<String, Object> item = new HashMap<>();
                    item.put("purchaseOrderNo", row[0]);
                    item.put("vendorName", row[1]);
                    item.put("status", row[2]);
                    item.put("expectedDate", row[3] == null ? null : row[3].toString());
                    item.put("totalAmount", row[4] instanceof BigDecimal decimal ? decimal : new BigDecimal(row[4].toString()));
                    return item;
                })
                .toList();
    }
}
