package com.dduk.service.accounting;

public class AccountingConstants {

    public static final String CASH = "1001";
    public static final String BANK_ACCOUNT = "1002";
    public static final String INVENTORY_ASSET = "1003";
    public static final String ACCOUNTS_RECEIVABLE = "1004";

    public static final String ACCOUNTS_PAYABLE = "2001";
    public static final String UNPAID_AMOUNT = "2002";
    public static final String SALARY_PAYABLE = "2003";
    public static final String WITHHOLDING_PAYABLE = "2004";

    public static final String SALES_REVENUE = "4001";

    public static final String PAYROLL_EXPENSE = "5001";
    public static final String COST_OF_SALES = "5002";
    public static final String WELFARE_EXPENSE = "5003";

    public static final String JOURNAL_STATUS_DRAFT = "DRAFT";
    public static final String JOURNAL_STATUS_POSTED = "POSTED";
    public static final String JOURNAL_STATUS_CANCELLED = "CANCELLED";

    public static final String SIDE_DEBIT = "DEBIT";
    public static final String SIDE_CREDIT = "CREDIT";

    public static final String SOURCE_PAYROLL = "PAYROLL";
    public static final String SOURCE_PURCHASE = "PURCHASE_ORDER";
    public static final String SOURCE_STOCK_IN = "STOCK_INBOUND";
    public static final String SOURCE_MANUAL = "MANUAL";

    public static final String PERIOD_OPEN = "OPEN";
    public static final String PERIOD_CLOSED = "CLOSED";

    private AccountingConstants() {
    }
}
