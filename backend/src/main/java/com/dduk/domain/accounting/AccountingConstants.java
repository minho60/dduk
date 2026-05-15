package com.dduk.domain.accounting;

public class AccountingConstants {
    // Account Codes
    public static final String PAYROLL_EXPENSE = "5001";      // 급여비용
    public static final String WITHHOLDING_PAYABLE = "2001";  // 예수금 (4대보험/세금)
    public static final String SALARY_PAYABLE = "2002";       // 미지급급여 (미지급금)
    
    // Status
    public static final String JOURNAL_STATUS_DRAFT = "DRAFT";
    public static final String JOURNAL_STATUS_POSTED = "POSTED";
    public static final String JOURNAL_STATUS_CANCELLED = "CANCELLED";
    
    // Sides
    public static final String SIDE_DEBIT = "DEBIT";
    public static final String SIDE_CREDIT = "CREDIT";

    // Source Types
    public static final String SOURCE_PAYROLL = "PAYROLL";
    public static final String SOURCE_PURCHASE = "PURCHASE";
    public static final String SOURCE_SALES = "SALES";
    public static final String SOURCE_EXPENSE = "EXPENSE";

    // Account Codes (Extended)
    public static final String INVENTORY_ASSET = "1002";      // 재고자산
    public static final String ACCOUNTS_PAYABLE = "2003";     // 외상매입금
}
