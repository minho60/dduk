package com.dduk.domain.accounting;

/**
 * 회계 도메인 공통 상수
 */
public class AccountingConstants {

    // ── 계정코드 ──────────────────────────────────────────────
    // 자산
    public static final String CASH              = "1001";  // 현금
    public static final String BANK_ACCOUNT      = "1002";  // 보통예금
    public static final String INVENTORY_ASSET   = "1003";  // 재고자산
    public static final String ACCOUNTS_RECEIVABLE = "1004"; // 외상매출금

    // 부채
    public static final String ACCOUNTS_PAYABLE  = "2001";  // 외상매입금
    public static final String UNPAID_AMOUNT     = "2002";  // 미지급금
    public static final String SALARY_PAYABLE    = "2003";  // 미지급비용(급여)
    public static final String WITHHOLDING_PAYABLE = "2004"; // 예수금

    // 수익
    public static final String SALES_REVENUE     = "4001";  // 매출액

    // 비용
    public static final String PAYROLL_EXPENSE   = "5001";  // 급여
    public static final String COST_OF_SALES     = "5002";  // 매출원가
    public static final String WELFARE_EXPENSE   = "5003";  // 복리후생비

    // ── 전표 상태 ─────────────────────────────────────────────
    public static final String JOURNAL_STATUS_DRAFT    = "DRAFT";
    public static final String JOURNAL_STATUS_APPROVED = "APPROVED";
    public static final String JOURNAL_STATUS_POSTED   = "POSTED";
    public static final String JOURNAL_STATUS_REVERSED = "REVERSED";

    // ── 차대변 방향 ───────────────────────────────────────────
    public static final String SIDE_DEBIT  = "DEBIT";
    public static final String SIDE_CREDIT = "CREDIT";

    // ── 소스 유형 ─────────────────────────────────────────────
    public static final String SOURCE_PAYROLL      = "PAYROLL";
    public static final String SOURCE_PURCHASE     = "PURCHASE_ORDER";
    public static final String SOURCE_STOCK_IN     = "STOCK_INBOUND";
    public static final String SOURCE_MANUAL       = "MANUAL";
    public static final String SOURCE_REVERSAL     = "REVERSAL";

    // ── 회계기간 상태 ─────────────────────────────────────────
    public static final String PERIOD_OPEN   = "OPEN";
    public static final String PERIOD_CLOSED = "CLOSED";

    private AccountingConstants() {}
}
