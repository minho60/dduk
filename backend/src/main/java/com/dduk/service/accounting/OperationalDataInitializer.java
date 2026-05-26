package com.dduk.service.accounting;

import com.dduk.dto.accounting.payroll.PayrollLedgerCreateRequest;
import com.dduk.dto.accounting.payroll.PayrollLedgerResponse;
import com.dduk.dto.accounting.voucher.VoucherLineRequest;
import com.dduk.dto.accounting.voucher.VoucherRequest;
import com.dduk.dto.accounting.voucher.VoucherResponse;
import com.dduk.entity.accounting.Account;
import com.dduk.entity.accounting.AccountSide;
import com.dduk.entity.accounting.JournalEntry;
import com.dduk.entity.accounting.payroll.*;
import com.dduk.entity.accounting.period.AccountingPeriod;
import com.dduk.entity.accounting.period.AccountingPeriodStatus;
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
import com.dduk.repository.accounting.voucher.VoucherRepository;
import com.dduk.repository.hr.EmployeeRepository;
import com.dduk.repository.hr.PayrollContractRepository;
import com.dduk.repository.inventory.VendorRepository;
import com.dduk.service.accounting.payroll.PayrollManagementService;
import com.dduk.service.accounting.voucher.VoucherService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Component
@Order(2)
@RequiredArgsConstructor
@Slf4j
public class OperationalDataInitializer {

    private final EmployeeRepository employeeRepository;
    private final PayrollContractRepository payrollContractRepository;
    private final VendorRepository vendorRepository;
    private final AccountingPeriodRepository accountingPeriodRepository;
    private final AccountRepository accountRepository;
    private final VoucherRepository voucherRepository;
    private final VoucherService voucherService;
    private final JournalEntryRepository journalEntryRepository;
    private final PayrollLedgerRepository payrollLedgerRepository;
    private final PayrollManagementService payrollManagementService;

    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void init() {
        log.info("ERP 초기 운영 데이터 적재를 시작합니다...");
        try {
            // 1. 거래처 데이터 적재
            List<Vendor> vendors = seedVendors();

            // 2. 사원 및 급여 계약 데이터 적재
            List<Employee> employees = seedEmployees();

            // 3. 2026년 마감 기간 초기 OPEN 상태로 생성
            List<AccountingPeriod> periods = seedAccountingPeriods();

            // 4. 전표 데이터 적재
            seedVouchers(vendors);

            // 5. 급여대장 데이터 적재
            seedPayrollLedgers(employees);

            // 6. 1월 ~ 4월 마감 기간 CLOSED 상태로 최종 마감 처리
            closePastPeriods(periods);

            log.info("ERP 초기 운영 데이터 적재가 성공적으로 완료되었습니다.");
        } catch (Exception e) {
            log.error("ERP 초기 운영 데이터 적재 중 오류 발생", e);
        }
    }

    private List<Vendor> seedVendors() {
        if (vendorRepository.count() > 0) {
            log.info("이미 거래처 데이터가 존재하여 적재를 건너뜁니다.");
            return vendorRepository.findAll();
        }

        List<Vendor> vendors = new ArrayList<>();
        vendors.add(Vendor.builder()
                .vendorCode("V001")
                .businessRegistrationNo("123-45-67890")
                .name("(주)뚝딱물산")
                .representativeName("김뚝딱")
                .businessType("도소매")
                .businessItem("사무용품")
                .contactName("정대리")
                .contactPhone("010-1111-2222")
                .email("tt@dduk.com")
                .address("서울시 강남구 테헤란로 123")
                .bankName("국민은행")
                .bankAccountNo("110-123-456789")
                .bankAccountHolder("김뚝딱")
                .status("ACTIVE")
                .build());

        vendors.add(Vendor.builder()
                .vendorCode("V002")
                .businessRegistrationNo("123-45-67891")
                .name("(주)그린테크")
                .representativeName("이그린")
                .businessType("제조")
                .businessItem("컴퓨터기기")
                .contactName("김과장")
                .contactPhone("010-1111-2223")
                .email("gt@green.com")
                .address("서울시 서초구 반포대로 456")
                .bankName("신한은행")
                .bankAccountNo("110-123-456790")
                .bankAccountHolder("이그린")
                .status("ACTIVE")
                .build());

        vendors.add(Vendor.builder()
                .vendorCode("V003")
                .businessRegistrationNo("123-45-67892")
                .name("(주)글로벌네트웍스")
                .representativeName("박네트")
                .businessType("서비스")
                .businessItem("물류대행")
                .contactName("최대리")
                .contactPhone("010-1111-2224")
                .email("gn@global.com")
                .address("부산시 중구 중앙대로 789")
                .bankName("우리은행")
                .bankAccountNo("110-123-456791")
                .bankAccountHolder("박네트")
                .status("ACTIVE")
                .build());

        vendors.add(Vendor.builder()
                .vendorCode("V004")
                .businessRegistrationNo("123-45-67893")
                .name("뚝딱세무법인")
                .representativeName("박세무")
                .businessType("전문직")
                .businessItem("세무대리")
                .contactName("박세무")
                .contactPhone("02-123-4567")
                .email("tax@dduk.com")
                .address("서울시 영등포구 여의도대로 12")
                .bankName("하나은행")
                .bankAccountNo("110-123-456792")
                .bankAccountHolder("박세무")
                .status("ACTIVE")
                .build());

        List<Vendor> saved = vendorRepository.saveAll(vendors);
        log.info("초기 거래처 데이터 {}건 적재 완료.", saved.size());
        return saved;
    }

    private List<Employee> seedEmployees() {
        if (employeeRepository.count() > 0) {
            log.info("이미 사원 데이터가 존재하여 적재를 건너뜁니다.");
            return employeeRepository.findAll();
        }

        List<Employee> employees = new ArrayList<>();
        employees.add(Employee.builder()
                .employeeNo("E0001")
                .name("홍길동")
                .department("개발부")
                .position("사원")
                .employmentStatus("ACTIVE")
                .hireDate(LocalDate.of(2026, 1, 1))
                .email("hong@dduk.com")
                .phone("010-1234-5678")
                .build());

        employees.add(Employee.builder()
                .employeeNo("E0002")
                .name("김철수")
                .department("인사부")
                .position("대리")
                .employmentStatus("ACTIVE")
                .hireDate(LocalDate.of(2026, 1, 1))
                .email("kim@dduk.com")
                .phone("010-1234-5679")
                .build());

        employees.add(Employee.builder()
                .employeeNo("E0003")
                .name("이영희")
                .department("회계부")
                .position("과장")
                .employmentStatus("ACTIVE")
                .hireDate(LocalDate.of(2026, 1, 1))
                .email("lee@dduk.com")
                .phone("010-1234-5680")
                .build());

        employees.add(Employee.builder()
                .employeeNo("E0004")
                .name("박민수")
                .department("영업부")
                .position("차장")
                .employmentStatus("ACTIVE")
                .hireDate(LocalDate.of(2026, 1, 1))
                .email("park@dduk.com")
                .phone("010-1234-5681")
                .build());

        List<Employee> savedEmployees = employeeRepository.saveAll(employees);
        log.info("초기 사원 데이터 {}건 적재 완료.", savedEmployees.size());

        // 급여 계약 등록
        List<PayrollContract> contracts = new ArrayList<>();
        contracts.add(PayrollContract.builder()
                .employee(savedEmployees.get(0))
                .contractNo("C20260101-01")
                .baseSalary(new BigDecimal("3500000"))
                .hourlyRate(new BigDecimal("16000"))
                .contractDate(LocalDate.of(2026, 1, 1))
                .status("ACTIVE")
                .build());

        contracts.add(PayrollContract.builder()
                .employee(savedEmployees.get(1))
                .contractNo("C20260101-02")
                .baseSalary(new BigDecimal("4500000"))
                .hourlyRate(new BigDecimal("20000"))
                .contractDate(LocalDate.of(2026, 1, 1))
                .status("ACTIVE")
                .build());

        contracts.add(PayrollContract.builder()
                .employee(savedEmployees.get(2))
                .contractNo("C20260101-03")
                .baseSalary(new BigDecimal("5500000"))
                .hourlyRate(new BigDecimal("25000"))
                .contractDate(LocalDate.of(2026, 1, 1))
                .status("ACTIVE")
                .build());

        contracts.add(PayrollContract.builder()
                .employee(savedEmployees.get(3))
                .contractNo("C20260101-04")
                .baseSalary(new BigDecimal("6500000"))
                .hourlyRate(new BigDecimal("30000"))
                .contractDate(LocalDate.of(2026, 1, 1))
                .status("ACTIVE")
                .build());

        payrollContractRepository.saveAll(contracts);
        log.info("초기 급여 계약 데이터 {}건 적재 완료.", contracts.size());

        return savedEmployees;
    }

    private List<AccountingPeriod> seedAccountingPeriods() {
        if (accountingPeriodRepository.count() > 0) {
            log.info("이미 마감 기간 데이터가 존재하여 적재를 건너뜁니다.");
            return accountingPeriodRepository.findAll();
        }

        List<AccountingPeriod> periods = new ArrayList<>();
        for (int month = 1; month <= 12; month++) {
            LocalDate firstDay = LocalDate.of(2026, month, 1);
            LocalDate lastDay = firstDay.withDayOfMonth(firstDay.lengthOfMonth());
            periods.add(AccountingPeriod.builder()
                    .fiscalYear(2026)
                    .fiscalMonth(month)
                    .startDate(firstDay)
                    .endDate(lastDay)
                    .status(AccountingPeriodStatus.OPEN)
                    .build());
        }

        List<AccountingPeriod> saved = accountingPeriodRepository.saveAll(periods);
        log.info("2026년 회계 마감 기간 {}개월 OPEN 상태로 생성 완료.", saved.size());
        return saved;
    }

    private void seedVouchers(List<Vendor> vendors) {
        if (voucherRepository.count() > 0) {
            log.info("이미 전표 데이터가 존재하여 적재를 건너뜁니다.");
            return;
        }

        // 사용할 계정과목들 사전 조회
        Account cashDep = findAccount("1112"); // 보통예금
        Account ar = findAccount("1121");      // 외상매출금
        Account vatRec = findAccount("1146");  // 부가세대급금
        Account ap = findAccount("2111");      // 외상매입금
        Account payables = findAccount("2121"); // 미지급금
        Account vatPay = findAccount("2128");  // 부가세예수금
        Account sales = findAccount("4110");   // 상품매출
        Account welfare = findAccount("5250"); // 복리후생비
        Account officeSup = findAccount("5380"); // 소모품비

        // 1월 ~ 4월 전표 (POSTED 상태로 승인 기표 처리할 전표들)
        for (int month = 1; month <= 4; month++) {
            LocalDate date = LocalDate.of(2026, month, 15);
            Vendor vendor1 = vendors.get(0);
            Vendor vendor2 = vendors.get(1);

            // 매출 전표
            BigDecimal supplySales = new BigDecimal("10000000").multiply(BigDecimal.valueOf(month));
            BigDecimal vatSales = supplySales.multiply(new BigDecimal("0.10"));
            BigDecimal totalSales = supplySales.add(vatSales);

            VoucherRequest request1 = new VoucherRequest();
            request1.setVoucherDate(date);
            request1.setVoucherType(VoucherType.SALES);
            request1.setVatType(VatType.TAX_INVOICE);
            request1.setVendorId(vendor1.getId());
            request1.setVendorNameSnapshot(vendor1.getName());
            request1.setDescription(month + "월 정기 상품 매출 분");
            
            List<VoucherLineRequest> lines1 = new ArrayList<>();
            lines1.add(line(cashDep, AccountSide.DEBIT, BigDecimal.ZERO, BigDecimal.ZERO, totalSales, "정기 매출 대금 보통예금 입금"));
            lines1.add(line(sales, AccountSide.CREDIT, supplySales, BigDecimal.ZERO, supplySales, "상품 매출액"));
            lines1.add(line(vatPay, AccountSide.CREDIT, BigDecimal.ZERO, vatSales, vatSales, "부가세예수금 수취"));
            request1.setLines(lines1);
            
            VoucherResponse res1 = voucherService.createVoucher(request1);
            postVoucher(res1.getId());

            // 매입 전표
            BigDecimal supplyPurchase = new BigDecimal("4000000").multiply(BigDecimal.valueOf(month));
            BigDecimal vatPurchase = supplyPurchase.multiply(new BigDecimal("0.10"));
            BigDecimal totalPurchase = supplyPurchase.add(vatPurchase);

            VoucherRequest request2 = new VoucherRequest();
            request2.setVoucherDate(date);
            request2.setVoucherType(VoucherType.PURCHASE);
            request2.setVatType(VatType.TAX_INVOICE);
            request2.setVendorId(vendor2.getId());
            request2.setVendorNameSnapshot(vendor2.getName());
            request2.setDescription(month + "월 컴퓨터 비품/소모품 구매");

            List<VoucherLineRequest> lines2 = new ArrayList<>();
            lines2.add(line(officeSup, AccountSide.DEBIT, supplyPurchase, BigDecimal.ZERO, supplyPurchase, "사무 소모품비"));
            lines2.add(line(vatRec, AccountSide.DEBIT, BigDecimal.ZERO, vatPurchase, vatPurchase, "부가세대급금 매입세액"));
            lines2.add(line(cashDep, AccountSide.CREDIT, BigDecimal.ZERO, BigDecimal.ZERO, totalPurchase, "소모품 보통예금 출금"));
            request2.setLines(lines2);

            VoucherResponse res2 = voucherService.createVoucher(request2);
            postVoucher(res2.getId());
        }

        // 5월 전표 (현재 월 - DRAFT, REQUESTED, APPROVED 등의 상태를 믹스하여 생성)
        LocalDate mayDate = LocalDate.of(2026, 5, 20);
        
        // 5월 매출 전표 1 (APPROVED 상태)
        VoucherRequest requestMaySales = new VoucherRequest();
        requestMaySales.setVoucherDate(mayDate);
        requestMaySales.setVoucherType(VoucherType.SALES);
        requestMaySales.setVatType(VatType.TAX_INVOICE);
        requestMaySales.setVendorId(vendors.get(2).getId());
        requestMaySales.setVendorNameSnapshot(vendors.get(2).getName());
        requestMaySales.setDescription("5월 물류서비스 완료분 매출");

        List<VoucherLineRequest> linesMaySales = new ArrayList<>();
        linesMaySales.add(line(ar, AccountSide.DEBIT, BigDecimal.ZERO, BigDecimal.ZERO, new BigDecimal("11000000"), "매출채권(외상매출금) 등록"));
        linesMaySales.add(line(sales, AccountSide.CREDIT, new BigDecimal("10000000"), BigDecimal.ZERO, new BigDecimal("10000000"), "상품 매출액"));
        linesMaySales.add(line(vatPay, AccountSide.CREDIT, BigDecimal.ZERO, new BigDecimal("1000000"), new BigDecimal("1000000"), "부가세예수금"));
        requestMaySales.setLines(linesMaySales);

        VoucherResponse resMaySales = voucherService.createVoucher(requestMaySales);
        approveVoucher(resMaySales.getId());

        // 5월 매입 전표 1 (REQUESTED 상태)
        VoucherRequest requestMayPurchase = new VoucherRequest();
        requestMayPurchase.setVoucherDate(mayDate);
        requestMayPurchase.setVoucherType(VoucherType.PURCHASE);
        requestMayPurchase.setVatType(VatType.TAX_INVOICE);
        requestMayPurchase.setVendorId(vendors.get(3).getId());
        requestMayPurchase.setVendorNameSnapshot(vendors.get(3).getName());
        requestMayPurchase.setDescription("5월 세무대행 전문직 서비스 매입");

        List<VoucherLineRequest> linesMayPurchase = new ArrayList<>();
        linesMayPurchase.add(line(welfare, AccountSide.DEBIT, new BigDecimal("2000000"), BigDecimal.ZERO, new BigDecimal("2000000"), "회계교육비 및 복리비"));
        linesMayPurchase.add(line(vatRec, AccountSide.DEBIT, BigDecimal.ZERO, new BigDecimal("200000"), new BigDecimal("200000"), "부가세대급금"));
        linesMayPurchase.add(line(payables, AccountSide.CREDIT, BigDecimal.ZERO, BigDecimal.ZERO, new BigDecimal("2200000"), "미지급금 거래처 미결제"));
        requestMayPurchase.setLines(linesMayPurchase);

        VoucherResponse resMayPurchase = voucherService.createVoucher(requestMayPurchase);
        requestApprovalVoucher(resMayPurchase.getId());

        // 5월 매입 전표 2 (DRAFT 상태)
        VoucherRequest requestMayDraft = new VoucherRequest();
        requestMayDraft.setVoucherDate(mayDate);
        requestMayDraft.setVoucherType(VoucherType.PURCHASE);
        requestMayDraft.setVatType(VatType.TAX_INVOICE);
        requestMayDraft.setVendorId(vendors.get(0).getId());
        requestMayDraft.setVendorNameSnapshot(vendors.get(0).getName());
        requestMayDraft.setDescription("5월 하순 소모성 필기구 다량 구매");

        List<VoucherLineRequest> linesMayDraft = new ArrayList<>();
        linesMayDraft.add(line(officeSup, AccountSide.DEBIT, new BigDecimal("500000"), BigDecimal.ZERO, new BigDecimal("500000"), "사무실 소모품비"));
        linesMayDraft.add(line(vatRec, AccountSide.DEBIT, BigDecimal.ZERO, new BigDecimal("50000"), new BigDecimal("50000"), "부가세대급금"));
        linesMayDraft.add(line(cashDep, AccountSide.CREDIT, BigDecimal.ZERO, BigDecimal.ZERO, new BigDecimal("550000"), "보통예금 즉시 현금 지출"));
        requestMayDraft.setLines(linesMayDraft);

        voucherService.createVoucher(requestMayDraft);

        log.info("초기 전표 데이터 적재 완료.");
    }

    private void seedPayrollLedgers(List<Employee> employees) {
        if (payrollLedgerRepository.count() > 0) {
            log.info("이미 급여대장 데이터가 존재하여 적재를 건너뜁니다.");
            return;
        }

        // 1월 ~ 4월 급여대장 (CONFIRMED 상태 및 자동전표 승인)
        for (int month = 1; month <= 4; month++) {
            PayrollLedgerCreateRequest request = new PayrollLedgerCreateRequest();
            request.setAttributionYearMonth("2026-" + String.format("%02d", month));
            request.setPayrollType(PayrollType.SALARY);
            request.setTaxType(PayrollTaxType.TAXABLE);
            request.setSettlementCycle(PayrollSettlementCycle.MONTHLY);
            request.setTargetPeriodMode(PayrollTargetPeriodMode.BULK);
            request.setPaymentDate(LocalDate.of(2026, month, 25));
            request.setPaymentYearMonth("2026-" + String.format("%02d", month));
            request.setLedgerName("2026년 " + month + "월 정기 급여대장");
            request.setSettlementItemSelectionMode(PayrollSelectionMode.ALL);
            request.setEmployeeSelectionMode(PayrollSelectionMode.ALL);
            request.setCreatedBy("admin");

            // 생성 및 계산 자동 실행
            PayrollLedgerResponse response = payrollManagementService.createLedger(request, true);
            
            // CONFIRMED 확정 처리
            payrollManagementService.confirmLedger(response.getId());

            // 급여 전표 강제 POSTED 로 변경하여 Ledger에 반영되게 함
            PayrollLedger ledger = payrollLedgerRepository.findById(response.getId()).orElse(null);
            if (ledger != null && ledger.getJournalEntry() != null) {
                JournalEntry je = ledger.getJournalEntry();
                // 강제 상태 변경 및 반영
                je.post();
                journalEntryRepository.save(je);
                
                // 급여대장도 POSTED 로 승격
                ledger.setStatus(PayrollStatus.POSTED);
                payrollLedgerRepository.save(ledger);
            }
        }

        // 5월 급여대장 (현재 월 - CALCULATED 상태로 두어 사용자가 확인하도록 함)
        PayrollLedgerCreateRequest requestMay = new PayrollLedgerCreateRequest();
        requestMay.setAttributionYearMonth("2026-05");
        requestMay.setPayrollType(PayrollType.SALARY);
        requestMay.setTaxType(PayrollTaxType.TAXABLE);
        requestMay.setSettlementCycle(PayrollSettlementCycle.MONTHLY);
        requestMay.setTargetPeriodMode(PayrollTargetPeriodMode.BULK);
        requestMay.setPaymentDate(LocalDate.of(2026, 5, 25));
        requestMay.setPaymentYearMonth("2026-05");
        requestMay.setLedgerName("2026년 5월 정기 급여대장");
        requestMay.setSettlementItemSelectionMode(PayrollSelectionMode.ALL);
        requestMay.setEmployeeSelectionMode(PayrollSelectionMode.ALL);
        requestMay.setCreatedBy("admin");

        payrollManagementService.createLedger(requestMay, true);

        log.info("초기 급여대장 데이터 적재 완료.");
    }

    private void closePastPeriods(List<AccountingPeriod> periods) {
        // 1월 ~ 4월 마감 기간 CLOSED 상태로 업데이트
        for (int i = 0; i < 4; i++) {
            AccountingPeriod period = periods.get(i);
            if (period.getStatus() == AccountingPeriodStatus.CLOSED) {
                log.info("{}월 회계 마감 기간은 이미 CLOSED 상태입니다. 건너뜁니다.", period.getFiscalMonth());
                continue;
            }
            period.close("admin");
            accountingPeriodRepository.save(period);
        }
        log.info("1월 ~ 4월 회계 마감 처리 완료.");
    }

    private Account findAccount(String code) {
        return accountRepository.findByCode(code)
                .orElseThrow(() -> new IllegalStateException("필수 초기 계정과목 누락: " + code));
    }

    private VoucherLineRequest line(Account account, AccountSide side, BigDecimal supply, BigDecimal vat, BigDecimal total, String desc) {
        VoucherLineRequest req = new VoucherLineRequest();
        req.setAccountId(account.getId());
        req.setAccountCode(account.getCode());
        req.setAccountName(account.getName());
        req.setDebitCredit(side);
        req.setSupplyAmount(supply);
        req.setVatAmount(vat);
        req.setTotalAmount(total);
        req.setDescription(desc);
        return req;
    }

    private void requestApprovalVoucher(Long id) {
        voucherService.updateStatus(id, VoucherStatus.REQUESTED);
    }

    private void approveVoucher(Long id) {
        voucherService.updateStatus(id, VoucherStatus.REQUESTED);
        voucherService.updateStatus(id, VoucherStatus.APPROVED);
    }

    private void postVoucher(Long id) {
        voucherService.updateStatus(id, VoucherStatus.REQUESTED);
        voucherService.updateStatus(id, VoucherStatus.APPROVED);
        voucherService.updateStatus(id, VoucherStatus.POSTED);
    }
}
