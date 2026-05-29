package com.dduk.service.accounting;

import com.dduk.dto.accounting.payroll.PayrollLedgerCreateRequest;
import com.dduk.dto.accounting.payroll.PayrollLedgerResponse;
import com.dduk.dto.accounting.voucher.VoucherLineRequest;
import com.dduk.dto.accounting.voucher.VoucherRequest;
import com.dduk.dto.accounting.voucher.VoucherResponse;
import com.dduk.entity.accounting.Account;
import com.dduk.entity.accounting.AccountSide;
import com.dduk.entity.accounting.AccountType;
import com.dduk.entity.accounting.payroll.*;
import com.dduk.entity.accounting.period.*;
import com.dduk.entity.accounting.voucher.Voucher;
import com.dduk.entity.accounting.voucher.enums.VatType;
import com.dduk.entity.accounting.voucher.enums.VoucherStatus;
import com.dduk.entity.accounting.voucher.enums.VoucherType;
import com.dduk.entity.hr.Employee;
import com.dduk.entity.hr.PayrollContract;
import com.dduk.entity.inventory.Vendor;
import com.dduk.repository.accounting.AccountRepository;
import com.dduk.repository.accounting.JournalEntryRepository;
import com.dduk.repository.accounting.payroll.PayrollLedgerRepository;
import com.dduk.repository.accounting.period.AccountingPeriodRepository;
import com.dduk.repository.accounting.period.ClosingLogRepository;
import com.dduk.repository.accounting.voucher.VoucherRepository;
import com.dduk.repository.hr.EmployeeRepository;
import com.dduk.repository.hr.PayrollContractRepository;
import com.dduk.repository.inventory.VendorRepository;
import com.dduk.service.accounting.payroll.PayrollManagementService;
import com.dduk.service.accounting.voucher.VoucherService;
import com.dduk.service.accounting.report.TrialBalanceReportService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.*;

/**
 * DDUK ERP 회계관리 통합 데이터 시더 (Phase Seeder 아키텍처)
 * 6개월치 실무 수준의 전표/분개/시산표/급여 데이터를 일관성 있게 구성합니다.
 */
@Component
@Order(2)
@RequiredArgsConstructor
@Slf4j
public class AccountingDataSeeder {

    private final VoucherService voucherService;
    private final PayrollManagementService payrollManagementService;
    private final EmployeeRepository employeeRepository;
    private final PayrollContractRepository payrollContractRepository;
    private final VendorRepository vendorRepository;
    private final AccountingPeriodRepository accountingPeriodRepository;
    private final ClosingLogRepository closingLogRepository;
    private final VoucherRepository voucherRepository;
    private final JournalEntryRepository journalEntryRepository;
    private final PayrollLedgerRepository payrollLedgerRepository;
    private final AccountRepository accountRepository;
    private final TrialBalanceReportService trialBalanceReportService;

    @Value("${app.accounting.seed:true}")
    private boolean seedEnabled;

    @Value("${app.accounting.seed-mode:true}")
    private boolean seedModeEnabled;

    @EventListener(ApplicationReadyEvent.class)
    public void init() {
        if (!seedEnabled) {
            log.info("[AccountingDataSeeder] Seeding is disabled in configuration.");
            return;
        }
        try {
            seedPipeline();
        } catch (Exception e) {
            log.error("[AccountingDataSeeder] Critical error occurred during the seeding pipeline", e);
        }
    }

    @Transactional
    public void seedPipeline() {
        if (voucherRepository.count() > 0 || journalEntryRepository.count() > 0) {
            log.info("[AccountingDataSeeder] Existing vouchers or journals detected. Skipping seeding to prevent duplicate data.");
            return;
        }

        log.info("[AccountingDataSeeder] Starting DDUK ERP Accounting Phase-based Seed Pipeline...");

        // Phase 1: 마스터 데이터 (회계기간, 거래처)
        seedPhase1_MasterData();

        // Phase 2: 조직 및 인사 데이터 (사원, 급여계약)
        seedPhase2_OrgAndHR();

        // Phase 3: 임시 전표 데이터 생성 (Voucher DRAFT)
        seedPhase3_VouchersDraft();

        // Phase 4: 전표 상태 전이 및 기표 처리 (Voucher POSTED)
        seedPhase4_VoucherPosting();

        // Phase 5: 급여대장 데이터 생성 및 회계 연동 (Payroll & Posting)
        seedPhase5_PayrollAndPosting();

        // Phase 6: 시산표/리포트 정합성 보장 빌드 및 마감 이력 적용 (Reporting Rebuild)
        seedPhase6_ReportingRebuild();

        log.info("[AccountingDataSeeder] DDUK ERP Accounting Seed Pipeline completed successfully!");
    }

    /**
     * Phase 1: 마스터 데이터 시딩
     */
    private void seedPhase1_MasterData() {
        log.info(">> [Phase 1] Seeding Master Data (Accounting periods and corporate vendors)...");

        // 1. 회계기간 생성 (모두 OPEN 상태로 기동하여 시더 작동 시 뮤테이션 검증 통과)
        for (int year : List.of(2025, 2026)) {
            for (int month = 1; month <= 12; month++) {
                if (!accountingPeriodRepository.existsByFiscalYearAndFiscalMonth(year, month)) {
                    YearMonth ym = YearMonth.of(year, month);
                    accountingPeriodRepository.save(AccountingPeriod.builder()
                            .fiscalYear(year)
                            .fiscalMonth(month)
                            .startDate(ym.atDay(1))
                            .endDate(ym.atEndOfMonth())
                            .status(AccountingPeriodStatus.OPEN)
                            .build());
                }
            }
        }

        // 2. 거래처 등록
        if (vendorRepository.count() == 0) {
            vendorRepository.save(Vendor.builder()
                    .vendorCode("V005")
                    .businessRegistrationNo("120-81-12345")
                    .name("(주)아망티")
                    .representativeName("홍길동")
                    .businessType("도소매")
                    .businessItem("차류 및 식음료")
                    .status("ACTIVE")
                    .memo("식음료 직매입 도매처")
                    .build());

            vendorRepository.save(Vendor.builder()
                    .vendorCode("V001")
                    .businessRegistrationNo("101-81-54321")
                    .name("국민카드")
                    .representativeName("이순신")
                    .businessType("금융")
                    .businessItem("신용카드")
                    .status("ACTIVE")
                    .memo("카드 PG 결제사")
                    .build());

            vendorRepository.save(Vendor.builder()
                    .vendorCode("V002")
                    .businessRegistrationNo("104-86-98765")
                    .name("삼전세무법인")
                    .representativeName("강감찬")
                    .businessType("서비스")
                    .businessItem("세무회계대행")
                    .status("ACTIVE")
                    .memo("세무 대리인")
                    .build());

            vendorRepository.save(Vendor.builder()
                    .vendorCode("V003")
                    .businessRegistrationNo("105-82-11111")
                    .name("(주)더블유인포")
                    .representativeName("을지문덕")
                    .businessType("제조")
                    .businessItem("소모품 및 비품")
                    .status("ACTIVE")
                    .memo("소모품 고정 거래처")
                    .build());
        }
    }

    /**
     * Phase 2: 조직 및 인사 데이터 시딩
     */
    private void seedPhase2_OrgAndHR() {
        log.info(">> [Phase 2] Seeding Org & HR Data (Employees and ACTIVE payroll contracts)...");

        if (employeeRepository.count() == 0) {
            Employee emp1 = employeeRepository.save(Employee.builder()
                    .employeeNo("EMP20240001")
                    .name("김철수")
                    .department("개발팀")
                    .position("팀장")
                    .employmentStatus("ACTIVE")
                    .hireDate(LocalDate.of(2024, 1, 1))
                    .email("chulsoo.kim@dduk.com")
                    .phone("010-1234-5678")
                    .build());

            payrollContractRepository.save(PayrollContract.builder()
                    .employee(emp1)
                    .contractNo("CON-20240001")
                    .baseSalary(new BigDecimal("5000000"))
                    .contractDate(LocalDate.of(2025, 1, 1))
                    .status("ACTIVE")
                    .build());

            Employee emp2 = employeeRepository.save(Employee.builder()
                    .employeeNo("EMP20240002")
                    .name("이영희")
                    .department("기획팀")
                    .position("대리")
                    .employmentStatus("ACTIVE")
                    .hireDate(LocalDate.of(2024, 2, 1))
                    .email("younghee.lee@dduk.com")
                    .phone("010-2345-6789")
                    .build());

            payrollContractRepository.save(PayrollContract.builder()
                    .employee(emp2)
                    .contractNo("CON-20240002")
                    .baseSalary(new BigDecimal("4000000"))
                    .contractDate(LocalDate.of(2025, 1, 1))
                    .status("ACTIVE")
                    .build());

            Employee emp3 = employeeRepository.save(Employee.builder()
                    .employeeNo("EMP20240003")
                    .name("박민수")
                    .department("인사팀")
                    .position("사원")
                    .employmentStatus("ACTIVE")
                    .hireDate(LocalDate.of(2024, 3, 1))
                    .email("minsu.park@dduk.com")
                    .phone("010-3456-7890")
                    .build());

            payrollContractRepository.save(PayrollContract.builder()
                    .employee(emp3)
                    .contractNo("CON-20240003")
                    .baseSalary(new BigDecimal("3200000"))
                    .contractDate(LocalDate.of(2025, 1, 1))
                    .status("ACTIVE")
                    .build());

            Employee emp4 = employeeRepository.save(Employee.builder()
                    .employeeNo("EMP20240004")
                    .name("최지우")
                    .department("회계팀")
                    .position("과장")
                    .employmentStatus("ACTIVE")
                    .hireDate(LocalDate.of(2024, 4, 1))
                    .email("jiwoo.choi@dduk.com")
                    .phone("010-4567-8901")
                    .build());

            payrollContractRepository.save(PayrollContract.builder()
                    .employee(emp4)
                    .contractNo("CON-20240004")
                    .baseSalary(new BigDecimal("4500000"))
                    .contractDate(LocalDate.of(2025, 1, 1))
                    .status("ACTIVE")
                    .build());
        }
    }

    /**
     * Phase 3: 임시 전표 데이터 생성 (Voucher DRAFT)
     */
    private void seedPhase3_VouchersDraft() {
        log.info(">> [Phase 3] Seeding Voucher DRAFT data (mostly in May 2026)...");

        List<Vendor> vendors = vendorRepository.findAll();
        if (vendors.isEmpty()) return;
        Vendor amnt = vendors.get(0); // 아망티

        // 매출/매입 계정과 보통예금 계정을 코드로 조회
        Account revenueAccount = accountRepository.findByCode("4110").orElse(null);
        Account bankAccount = accountRepository.findByCode("1112").orElse(null);
        Account welfareAccount = accountRepository.findByCode("5230").orElse(null); // 복리후생비

        if (revenueAccount == null || bankAccount == null || welfareAccount == null) {
            log.warn("Required accounts (4110, 1112, 5230) missing for draft vouchers.");
            return;
        }

        // 2026년 5월 DRAFT 전표 생성 (직접 사용자가 전표화면에서 볼 수 있도록 함)
        VoucherRequest salesReq = new VoucherRequest();
        salesReq.setVoucherDate(LocalDate.of(2026, 5, 28));
        salesReq.setVoucherType(VoucherType.SALES);
        salesReq.setVatType(VatType.TAX_INVOICE);
        salesReq.setVendorId(amnt.getId());
        salesReq.setVendorNameSnapshot(amnt.getName());
        salesReq.setSupplyAmount(new BigDecimal("6000000"));
        salesReq.setVatAmount(new BigDecimal("600000"));
        salesReq.setFeeAmount(BigDecimal.ZERO);
        salesReq.setBusinessAccountId(revenueAccount.getId());
        salesReq.setSettlementAccountId(bankAccount.getId());
        salesReq.setDescription("아망티 차류 상품 판매 매출 대금 미정산");
        voucherService.createVoucher(salesReq);

        VoucherRequest purReq = new VoucherRequest();
        purReq.setVoucherDate(LocalDate.of(2026, 5, 29));
        purReq.setVoucherType(VoucherType.PURCHASE);
        purReq.setVatType(VatType.TAX_INVOICE);
        purReq.setVendorId(amnt.getId());
        purReq.setVendorNameSnapshot(amnt.getName());
        purReq.setSupplyAmount(new BigDecimal("1500000"));
        purReq.setVatAmount(new BigDecimal("150000"));
        purReq.setFeeAmount(BigDecimal.ZERO);
        purReq.setBusinessAccountId(welfareAccount.getId());
        purReq.setSettlementAccountId(bankAccount.getId());
        purReq.setDescription("탕비실 소모 허브차 음료 박스 매입");
        voucherService.createVoucher(purReq);
    }

    /**
     * Phase 4: 전표 상태 전이 및 기표 처리 (Voucher POSTED)
     */
    private void seedPhase4_VoucherPosting() {
        log.info(">> [Phase 4] Seeding Voucher POSTED data over the past 6 months (including 100M capital injection)...");

        List<Vendor> vendors = vendorRepository.findAll();
        if (vendors.isEmpty()) return;
        Vendor amnt = vendors.get(0); // 아망티
        Vendor card = vendors.size() > 1 ? vendors.get(1) : amnt;
        Vendor tax = vendors.size() > 2 ? vendors.get(2) : amnt;
        Vendor info = vendors.size() > 3 ? vendors.get(3) : amnt;

        // 계정과목
        Account bankAccount = accountRepository.findByCode("1112").orElse(null); // 보통예금
        Account capitalAccount = accountRepository.findByCode("3110").orElse(null); // 보통주자본금
        Account revenueAccount = accountRepository.findByCode("4110").orElse(null); // 상품매출
        Account welfareAccount = accountRepository.findByCode("5230").orElse(null); // 복리후생비
        Account rentAccount = accountRepository.findByCode("5280").orElse(null); // 지급임차료
        Account utilityAccount = accountRepository.findByCode("5300").orElse(null); // 수도광열비
        Account officeAccount = accountRepository.findByCode("5330").orElse(null); // 소모품비

        if (bankAccount == null || capitalAccount == null || revenueAccount == null || welfareAccount == null) {
            log.warn("Required accounts missing for voucher posting phase.");
            return;
        }

        // 1. 2025년 12월 초기 자본금 전입 전표 (1억 원)
        VoucherRequest capReq = new VoucherRequest();
        capReq.setVoucherDate(LocalDate.of(2025, 12, 1));
        capReq.setVoucherType(VoucherType.GENERAL);
        capReq.setVatType(VatType.ZERO_TAX);
        capReq.setVendorNameSnapshot("주주총회 수납");
        capReq.setSupplyAmount(new BigDecimal("100000000"));
        capReq.setVatAmount(BigDecimal.ZERO);
        capReq.setFeeAmount(BigDecimal.ZERO);
        capReq.setDescription("ERP 회사 설립 초기 출자 법정 자본금 납입");

        VoucherLineRequest capLine1 = new VoucherLineRequest();
        capLine1.setAccountId(bankAccount.getId());
        capLine1.setAccountCode(bankAccount.getCode());
        capLine1.setAccountName(bankAccount.getName());
        capLine1.setDebitCredit(AccountSide.DEBIT);
        capLine1.setTotalAmount(new BigDecimal("100000000"));
        capLine1.setSupplyAmount(BigDecimal.ZERO);
        capLine1.setVatAmount(BigDecimal.ZERO);
        capLine1.setDescription("보통예금 자본 전입");

        VoucherLineRequest capLine2 = new VoucherLineRequest();
        capLine2.setAccountId(capitalAccount.getId());
        capLine2.setAccountCode(capitalAccount.getCode());
        capLine2.setAccountName(capitalAccount.getName());
        capLine2.setDebitCredit(AccountSide.CREDIT);
        capLine2.setTotalAmount(new BigDecimal("100000000"));
        capLine2.setSupplyAmount(BigDecimal.ZERO);
        capLine2.setVatAmount(BigDecimal.ZERO);
        capLine2.setDescription("보통주자본금 법정 발행");

        capReq.setLines(List.of(capLine1, capLine2));
        createAndTransitionVoucher(capReq, VoucherStatus.POSTED);

        // 2. 최근 5개월(2026년 1월 ~ 5월) 매출/매입 전표 생성
        int[] supplyAmountsSales = {12000000, 15000000, 18000000, 22000000, 16000000};
        int[] supplyAmountsPurchase = {3500000, 4200000, 3800000, 5000000, 2800000};

        for (int i = 0; i < 5; i++) {
            int year = 2026;
            int month = i + 1;

            // (1) 월별 매출 전표 기표 (보통예금으로 매출 입금)
            VoucherRequest salesV = new VoucherRequest();
            salesV.setVoucherDate(LocalDate.of(year, month, 15));
            salesV.setVoucherType(VoucherType.SALES);
            salesV.setVatType(VatType.TAX_INVOICE);
            salesV.setVendorId(amnt.getId());
            salesV.setVendorNameSnapshot(amnt.getName());
            salesV.setSupplyAmount(new BigDecimal(supplyAmountsSales[i]));
            salesV.setVatAmount(new BigDecimal(supplyAmountsSales[i] / 10));
            salesV.setFeeAmount(BigDecimal.ZERO);
            salesV.setBusinessAccountId(revenueAccount.getId());
            salesV.setSettlementAccountId(bankAccount.getId());
            salesV.setDescription(month + "월 정기 제품 및 용역 납품 매출대금 수금");
            createAndTransitionVoucher(salesV, VoucherStatus.POSTED);

            // (2) 월별 매입/비용 전표 기표 (보통예금 출금)
            VoucherRequest purV = new VoucherRequest();
            purV.setVoucherDate(LocalDate.of(year, month, 20));
            purV.setVoucherType(VoucherType.PURCHASE);
            purV.setVatType(VatType.TAX_INVOICE);
            purV.setVendorId(info.getId());
            purV.setVendorNameSnapshot(info.getName());
            purV.setSupplyAmount(new BigDecimal(supplyAmountsPurchase[i]));
            purV.setVatAmount(new BigDecimal(supplyAmountsPurchase[i] / 10));
            purV.setFeeAmount(BigDecimal.ZERO);
            
            // 비용 계정 분배
            Account targetExpense = welfareAccount;
            if (month == 2) targetExpense = rentAccount != null ? rentAccount : welfareAccount;
            if (month == 3) targetExpense = utilityAccount != null ? utilityAccount : welfareAccount;
            if (month == 4) targetExpense = officeAccount != null ? officeAccount : welfareAccount;
            
            purV.setBusinessAccountId(targetExpense.getId());
            purV.setSettlementAccountId(bankAccount.getId());
            purV.setDescription(month + "월 사무 유지를 위한 정기 공통비용 집행");
            createAndTransitionVoucher(purV, VoucherStatus.POSTED);
        }

        // 추가로 5월달에 승인 대기중(REQUESTED)인 전표 세팅 (사용자 필터링 확인용)
        VoucherRequest requestedSales = new VoucherRequest();
        requestedSales.setVoucherDate(LocalDate.of(2026, 5, 27));
        requestedSales.setVoucherType(VoucherType.SALES);
        requestedSales.setVatType(VatType.TAX_INVOICE);
        requestedSales.setVendorId(amnt.getId());
        requestedSales.setVendorNameSnapshot(amnt.getName());
        requestedSales.setSupplyAmount(new BigDecimal("7500000"));
        requestedSales.setVatAmount(new BigDecimal("750000"));
        requestedSales.setFeeAmount(BigDecimal.ZERO);
        requestedSales.setBusinessAccountId(revenueAccount.getId());
        requestedSales.setSettlementAccountId(bankAccount.getId());
        requestedSales.setDescription("기획 도메인 컨설팅 매출 전표 승인 요청건");
        createAndTransitionVoucher(requestedSales, VoucherStatus.REQUESTED);
    }

    /**
     * Phase 5: 급여대장 데이터 생성 및 회계 연동 (Payroll & Posting)
     */
    private void seedPhase5_PayrollAndPosting() {
        log.info(">> [Phase 5] Seeding Payroll Ledger data (2026-02 ~ 2026-05)...");

        // 2026년 2월, 3월, 4월 급여대장은 CONFIRMED + POSTED 처리
        seedPayrollHelper("2026-02", LocalDate.of(2026, 2, 25), true);
        seedPayrollHelper("2026-03", LocalDate.of(2026, 3, 25), true);
        seedPayrollHelper("2026-04", LocalDate.of(2026, 4, 25), true);

        // 2026년 5월(당월) 급여대장은 CALCULATED까지만 수행 (UI 대기 상태)
        seedPayrollHelper("2026-05", LocalDate.of(2026, 5, 25), false);
    }

    /**
     * Phase 6: 시산표/리포트 정합성 보장 빌드 및 마감 이력 적용 (Reporting Rebuild)
     */
    private void seedPhase6_ReportingRebuild() {
        log.info(">> [Phase 6] Rebuilding Reporting (rebuildAll) and locking past accounting periods...");

        // 1. 시산표 강제 재빌드 (UI에서 0원 노출 차단)
        trialBalanceReportService.rebuildAll();

        // 2. 과거 월 (2025-12 ~ 2026-04) 회계기간 마감(CLOSED) 처리 및 ClosingLog 남김
        closePeriodHelper(2025, 12);
        closePeriodHelper(2026, 1);
        closePeriodHelper(2026, 2);
        closePeriodHelper(2026, 3);
        closePeriodHelper(2026, 4);
    }

    /**
     * 전표 상태 기입 헬퍼
     */
    private void createAndTransitionVoucher(VoucherRequest req, VoucherStatus targetStatus) {
        VoucherResponse res = voucherService.createVoucher(req);
        if (targetStatus == VoucherStatus.DRAFT) {
            return;
        }
        voucherService.updateStatus(res.getId(), VoucherStatus.REQUESTED);
        if (targetStatus == VoucherStatus.REQUESTED) {
            return;
        }
        voucherService.updateStatus(res.getId(), VoucherStatus.APPROVED);
        if (targetStatus == VoucherStatus.APPROVED) {
            return;
        }
        voucherService.updateStatus(res.getId(), VoucherStatus.POSTED);
    }

    /**
     * 급여 자동 계산 및 확정 헬퍼
     */
    private void seedPayrollHelper(String yearMonth, LocalDate paymentDate, boolean confirm) {
        try {
            PayrollLedgerCreateRequest req = new PayrollLedgerCreateRequest();
            req.setAttributionYearMonth(yearMonth);
            req.setPaymentYearMonth(yearMonth);
            req.setPayrollType(PayrollType.SALARY);
            req.setTaxType(PayrollTaxType.TAXABLE);
            req.setSettlementCycle(PayrollSettlementCycle.MONTHLY);
            req.setTargetPeriodMode(PayrollTargetPeriodMode.BULK);
            req.setPaymentDate(paymentDate);
            req.setLedgerName(yearMonth + " 정기 급여대장");
            req.setSettlementItemSelectionMode(PayrollSelectionMode.ALL);
            req.setEmployeeSelectionMode(PayrollSelectionMode.ALL);
            req.setCreatedBy("system");

            // 생성 및 즉시 계산 실행
            PayrollLedgerResponse res = payrollManagementService.createLedger(req, true);

            // 확정 및 분개 기표 완료
            if (confirm) {
                payrollManagementService.confirmLedger(res.getId());
            }
        } catch (Exception e) {
            log.error("Failed to seed payroll for period: {}", yearMonth, e);
        }
    }

    /**
     * 과거 회계기간 마감 헬퍼
     */
    private void closePeriodHelper(int year, int month) {
        accountingPeriodRepository.findByFiscalYearAndFiscalMonth(year, month)
                .ifPresent(period -> {
                    try {
                        // 강제 상태 변경 및 감사 기록
                        period.close("system");
                        accountingPeriodRepository.save(period);

                        closingLogRepository.save(ClosingLog.builder()
                                .accountingPeriod(period)
                                .actionType(ClosingActionType.MONTH_CLOSED)
                                .fromStatus(AccountingPeriodStatus.OPEN)
                                .toStatus(AccountingPeriodStatus.CLOSED)
                                .actor("system")
                                .ipAddress("127.0.0.1")
                                .message("Seeder closed this historical period during bootstrap.")
                                .build());
                    } catch (Exception e) {
                        log.error("Failed to lock period {}-{}", year, month, e);
                    }
                });
    }
}
