package com.dduk.service.inventory;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@Transactional(readOnly = true)
public class PurchaseDashboardService {

    @PersistenceContext
    private EntityManager entityManager;

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
        stats.put("baseDate", LocalDate.now().toString());
        return stats;
    }

    private long count(String sql) {
        Object value = entityManager.createNativeQuery(sql).getSingleResult();
        if (value == null) return 0L;
        if (value instanceof Number number) return number.longValue();
        return Long.parseLong(value.toString());
    }

    private double percentage(long value, long total) {
        if (total <= 0) return 0;
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
        if (value == null) return BigDecimal.ZERO;
        if (value instanceof BigDecimal decimal) return decimal;
        if (value instanceof Number number) return BigDecimal.valueOf(number.doubleValue());
        return new BigDecimal(value.toString());
    }

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
